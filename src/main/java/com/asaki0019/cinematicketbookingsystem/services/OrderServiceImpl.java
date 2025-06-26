package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.dto.PaymentStatusDTO;
import com.asaki0019.cinematicketbookingsystem.dto.UserOrderDTO;
import com.asaki0019.cinematicketbookingsystem.entities.*;
import com.asaki0019.cinematicketbookingsystem.repository.*;
import com.asaki0019.cinematicketbookingsystem.utils.PaymentGatewayUtils;
import com.asaki0019.cinematicketbookingsystem.utils.RedisCacheUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderSeatRepository orderSeatRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisCacheUtils redisCacheUtils;

    @Autowired
    private UserRepository userRepository;

    private static final String ORDER_CACHE_PREFIX = "order:";
    private static final String SEAT_LOCK_PREFIX = "seat_lock:";
    private static final int ORDER_CACHE_MINUTES = 15;

    @Value("${payment.callback.url:http://localhost:8081/api/payments/callback}")
    private String callbackUrl;

    @Value("${payment.return.url:http://localhost:8081/return_url}")
    private String returnUrl;

    @Override
    @Transactional
    public Map<String, Object> createOrder(OrderRequest orderRequest) {
        // 参数校验
        if (orderRequest == null || orderRequest.getUserId() == null || orderRequest.getSessionId() == null
                || orderRequest.getSeatIds() == null || orderRequest.getSeatIds().isEmpty()
                || orderRequest.getPaymentMethod() == null) {
            throw new IllegalArgumentException("订单参数不合法");
        }

        // 1. 验证座位是否可用并加锁
        for (Long seatId : orderRequest.getSeatIds()) {
            String lockKey = SEAT_LOCK_PREFIX + seatId;
            if (RedisCacheUtils.exists(lockKey)) {
                throw new RuntimeException("座位已被锁定");
            }
            RedisCacheUtils.set(lockKey, orderRequest.getUserId().toString(), ORDER_CACHE_MINUTES * 60);
        }

        // 2. 创建订单
        Order newOrder = new Order();
        newOrder.setUserId(orderRequest.getUserId());
        newOrder.setSessionId(orderRequest.getSessionId());
        newOrder.setPaymentMethod(orderRequest.getPaymentMethod());
        newOrder.setOrderNo(UUID.randomUUID().toString());
        newOrder.setStatus("PENDING_PAYMENT");
        newOrder.setCreateTime(LocalDateTime.now());

        // 计算总金额 (实际项目中需要根据座位类型和会员等级计算)
        Integer memberLevel = userRepository.findById(orderRequest.getUserId())
                .map(User::getMemberLevel).orElse(0);
        double totalAmount = calculateTotalAmount(orderRequest.getSeatIds(), memberLevel);
        newOrder.setTotalAmount(totalAmount);

        // 3. 保存订单
        newOrder = orderRepository.save(newOrder);

        // 4. 缓存订单信息
        try {
            String orderJson = objectMapper.writeValueAsString(newOrder);
            RedisCacheUtils.set(ORDER_CACHE_PREFIX + newOrder.getOrderNo(), orderJson, ORDER_CACHE_MINUTES * 60);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("订单缓存失败");
        }

        // 5. 调用支付网关
        Map<String, String> extraParams = new HashMap<>();
        extraParams.put("return_url", returnUrl);

        Map<String, Object> paymentResult = PaymentGatewayUtils.unifiedOrder(
                PaymentGatewayUtils.PayType.ALIPAY,
                newOrder.getOrderNo(),
                totalAmount,
                "电影票购买",
                "场次ID:" + orderRequest.getSessionId(),
                callbackUrl,
                extraParams);

        // 6. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("id", newOrder.getId());
        result.put("order_no", newOrder.getOrderNo());
        result.put("total_amount", totalAmount);
        result.put("e_ticket_url", "http://example.com/eticket/" + newOrder.getOrderNo());
        return result;
    }

    @Override
    public List<UserOrderDTO> getUserOrders(Long userId, String status) {
        List<Order> orders = (status != null && !status.isEmpty())
                ? orderRepository.findByUserIdAndStatus(userId, status)
                : orderRepository.findByUserId(userId);

        return orders.stream()
                .map(this::convertToUserOrderDTO)
                .collect(Collectors.toList());
    }

    private UserOrderDTO convertToUserOrderDTO(Order order) {
        Session session = sessionRepository.findById(order.getSessionId()).orElse(null);
        Movie movie = (session != null) ? movieRepository.findById(session.getMovieId()).orElse(null) : null;
        List<OrderSeat> seats = orderSeatRepository.findByOrderId(order.getId());

        return new UserOrderDTO(
                order.getId(),
                order.getOrderNo(),
                (movie != null) ? movie.getTitle() : "N/A",
                (session != null) ? session.getStartTime() : null,
                seats.size(),
                order.getTotalAmount(),
                order.getStatus());
    }

    @Override
    @Transactional
    public Map<String, Object> cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许取消");
        }

        order.setStatus("CANCELLED");
        orderRepository.save(order);

        // 释放座位锁
        for (Long seatId : getSeatIds(order)) {
            RedisCacheUtils.del(SEAT_LOCK_PREFIX + seatId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("order_id", orderId);
        result.put("new_status", "CANCELLED");
        return result;
    }

    @Override
    public PaymentStatusDTO getPaymentStatus(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        return new PaymentStatusDTO(order.getOrderNo(), order.getStatus(), order.getUpdatedAt());
    }

    @Override
    @Transactional
    public void handlePaymentCallback(String orderNo, String status, String transactionId, double amount) {
        Order order = orderRepository.findByOrderNo(orderNo);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if ("SUCCESS".equals(status)) {
            order.setStatus("COMPLETED");
            order.setPaymentTime(LocalDateTime.now());
            orderRepository.save(order);

            // 生成电子票
            String eTicketUrl = generateETicket(order);
            order.setETicketUrl(eTicketUrl);
            orderRepository.save(order);
        } else {
            order.setStatus("PAYMENT_FAILED");
            orderRepository.save(order);

            // 释放座位锁
            for (Long seatId : getSeatIds(order)) {
                RedisCacheUtils.del(SEAT_LOCK_PREFIX + seatId);
            }
        }
    }

    @Override
    @Transactional
    public void processPaymentCallback(PaymentCallbackRequest callbackRequest) {
        Order order = orderRepository.findByOrderNo(callbackRequest.getOrderNo());
        if (order == null) {
            // 记录日志：未找到对应订单
            return;
        }

        // 幂等性检查：如果订单状态已经完成或取消，则不再处理
        if ("COMPLETED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            return;
        }

        if ("SUCCESS".equalsIgnoreCase(callbackRequest.getPaymentStatus())) {
            order.setStatus("COMPLETED");
            order.setPaymentTime(LocalDateTime.now());
            // 在真实场景中，这里可能还需要记录支付流水号等信息
        } else {
            // 支付失败或超时，更新状态为失败，并释放锁定的座位
            order.setStatus("FAILED");
            List<OrderSeat> orderSeats = orderSeatRepository.findByOrderId(order.getId());
            List<Long> seatIds = orderSeats.stream().map(OrderSeat::getSeatId).toList();
            List<Seat> seats = seatRepository.findAllById(seatIds);
            seats.forEach(seat -> {
                seat.setStatus("AVAILABLE"); // 将座位状态恢复为可用
            });
            seatRepository.saveAll(seats);
        }

        orderRepository.save(order);
    }

    // 辅助方法
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + new Random().nextInt(1000);
    }

    private double calculateTotalAmount(List<Long> seatIds, Integer memberLevel) {
        if (memberLevel == null)
            memberLevel = 0;
        double base = seatIds.size() * 50.0;
        double discount = 1 - 0.1 * memberLevel;
        if (discount < 0)
            discount = 0;
        return base * discount;
    }

    private List<Long> getSeatIds(Order order) {
        List<OrderSeat> orderSeats = orderSeatRepository.findByOrderId(order.getId());
        List<Long> seatIds = new ArrayList<>();
        for (OrderSeat os : orderSeats) {
            seatIds.add(os.getSeatId());
        }
        return seatIds;
    }

    private String generateETicket(Order order) {
        // 生成二维码内容为订单号
        try {
            return com.asaki0019.cinematicketbookingsystem.utils.QRCodeUtils.generateQRCodeBase64(
                    "订单号:" + order.getOrderNo(), 300, 300);
        } catch (Exception e) {
            return "二维码生成失败";
        }
    }

    @Override
    public Map<String, Object> getOrderStatusByOrderNo(String orderNo) {
        Order order = orderRepository.findByOrderNo(orderNo);
        if (order == null)
            throw new RuntimeException("订单不存在");
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("status", order.getStatus());
        result.put("amount", order.getTotalAmount());
        result.put("userId", order.getUserId());
        result.put("sessionId", order.getSessionId());
        result.put("paymentTime", order.getPaymentTime());
        return result;
    }
}