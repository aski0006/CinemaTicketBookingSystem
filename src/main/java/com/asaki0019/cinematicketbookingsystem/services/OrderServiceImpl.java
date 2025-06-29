package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.aop.LogAspect.NotLogInAOP;
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
import com.asaki0019.cinematicketbookingsystem.utils.JwtTokenUtils;
import org.springframework.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;

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

    @Autowired
    private HallRepository hallRepository;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    private static final String ORDER_CACHE_PREFIX = "order:";
    private static final String SEAT_LOCK_PREFIX = "seat_lock:";
    private static final int ORDER_CACHE_MINUTES = 15;

    @Value("${payment.callback.url:http://localhost:8081/api/payments/callback}")
    private String callbackUrl;

    @Value("${payment.return.url:http://localhost:8081/return_url}")
    private String returnUrl;

    @Override
    @Transactional
    @NotLogInAOP
    public Map<String, Object> createOrder(OrderRequest orderRequest) {
        // 参数校验
        if (orderRequest == null || orderRequest.getSessionId() == null
                || orderRequest.getSeatPositions() == null || orderRequest.getSeatPositions().isEmpty()
                || orderRequest.getPaymentMethod() == null) {
            throw new IllegalArgumentException("订单参数不合法");
        }
        Long sessionId = orderRequest.getSessionId();
        List<List<Integer>> seatPositions = orderRequest.getSeatPositions();

        // 1. 直接查Redis二维座位数组
        String redisKey = "session_seats:" + sessionId;
        String json = RedisCacheUtils.get(redisKey);
        if (json == null || json.isEmpty()) {
            throw new RuntimeException("场次座位信息不存在");
        }
        List<List<Map<String, Object>>> seatStatus;
        try {
            seatStatus = objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            throw new RuntimeException("座位信息解析失败");
        }

        // 2. 校验所有选中座位当前状态为AVAILABLE，否则返回友好错误
        List<List<Integer>> unavailableSeats = new ArrayList<>();
        for (List<Integer> pos : seatPositions) {
            int row = pos.get(0), col = pos.get(1);
            Map<String, Object> cell = seatStatus.get(row).get(col);
            if (cell == null || "NULL".equals(cell.get("type"))) {
                unavailableSeats.add(List.of(row, col));
                continue;
            }
            if (!"AVAILABLE".equals(cell.get("status"))) {
                unavailableSeats.add(List.of(row, col));
            }
        }
        if (!unavailableSeats.isEmpty()) {
            System.out.println("[ERROR] 不可用座位: " + unavailableSeats + ", seatStatus结构=" + seatStatus);
            throw new RuntimeException("部分座位已被锁定或占用，请重新选择: " + unavailableSeats);
        }
        // 3. 校验通过后，统一将这些座位全部标记为LOCKED
        double totalAmount = 0;
        List<Map<String, Object>> lockedSeats = new ArrayList<>();
        for (List<Integer> pos : seatPositions) {
            int row = pos.get(0), col = pos.get(1);
            Map<String, Object> cell = seatStatus.get(row).get(col);
            double priceFactor = cell.get("priceFactor") != null
                    ? Double.parseDouble(cell.get("priceFactor").toString())
                    : 1.0;
            totalAmount += 50.0 * priceFactor; // 假设基础价50
            cell.put("status", "LOCKED");
            lockedSeats.add(Map.of(
                    "row", row,
                    "col", col,
                    "type", cell.get("type"),
                    "priceFactor", priceFactor));
        }
        // 4. 写回Redis，锁定15分钟
        try {
            RedisCacheUtils.set(redisKey, objectMapper.writeValueAsString(seatStatus), 15 * 60);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("座位锁定写入Redis失败", e);
        }

        try {
            // 5. 创建订单（Order表/OrderSeat表如需）
            Order newOrder = new Order();
            // 优先用OrderRequest的userId，如果为null则从JWT解析
            Long userId = orderRequest.getUserId();
            if (userId == null) {
                String authHeader = httpServletRequest.getHeader("Authorization");
                if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    try {
                        com.asaki0019.cinematicketbookingsystem.entities.User user = JwtTokenUtils
                                .getUserFromToken(token);
                        if (user != null && user.getId() != null) {
                            userId = user.getId();
                        }
                    } catch (Exception ignore) {
                    }
                }
            }
            newOrder.setUserId(userId);
            newOrder.setSessionId(sessionId);
            newOrder.setPaymentMethod(orderRequest.getPaymentMethod());
            newOrder.setOrderNo(UUID.randomUUID().toString());
            newOrder.setStatus("PENDING_PAYMENT");
            newOrder.setCreateTime(LocalDateTime.now());
            newOrder.setTotalAmount(totalAmount);
            // 先写入数据库
            newOrder = orderRepository.saveAndFlush(newOrder);
            System.out.println(newOrder);
            // 如需存OrderSeat，存row/col/type/priceFactor
            for (Map<String, Object> seat : lockedSeats) {
                OrderSeat os = new OrderSeat();
                os.setOrderId(newOrder.getId());
                os.setRowNo((Integer) seat.get("row"));
                os.setColNo((Integer) seat.get("col"));
                os.setType(seat.get("type").toString());
                os.setPriceFactor(Double.parseDouble(seat.get("priceFactor").toString()));
                os.setFinalPrice(50.0 * os.getPriceFactor()); // 可选：存最终价格
                orderSeatRepository.save(os);
            }

            // 6. 缓存订单、调用支付网关、返回payUrl等
            try {
                String orderJson = objectMapper.writeValueAsString(newOrder);
                RedisCacheUtils.set("order:" + newOrder.getOrderNo(), orderJson, 15 * 60);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new RuntimeException("订单缓存失败", e);
            }
            Map<String, String> extraParams = new HashMap<>();
            extraParams.put("return_url", returnUrl);
            Map<String, Object> paymentResult = PaymentGatewayUtils.unifiedOrder(
                    PaymentGatewayUtils.PayType.ALIPAY,
                    newOrder.getOrderNo(),
                    totalAmount,
                    "电影票购买",
                    "场次ID:" + sessionId,
                    callbackUrl,
                    extraParams);

            Map<String, Object> result = new HashMap<>();
            result.put("id", newOrder.getId());
            result.put("order_no", newOrder.getOrderNo());
            result.put("total_amount", totalAmount);
            result.put("payUrl", paymentResult.get("payUrl"));
            result.put("e_ticket_url", "http://example.com/eticket/" + newOrder.getOrderNo());
            return result;
        } catch (Exception ex) {
            // 恢复本次锁定的座位状态为AVAILABLE
            for (List<Integer> pos : seatPositions) {
                int row = pos.get(0), col = pos.get(1);
                Map<String, Object> cell = seatStatus.get(row).get(col);
                if (cell != null && !"NULL".equals(cell.get("type"))) {
                    cell.put("status", "AVAILABLE");
                }
            }
            try {
                RedisCacheUtils.set(redisKey, objectMapper.writeValueAsString(seatStatus), 15 * 60);
            } catch (Exception ignore) {
            }
            throw ex;
        }
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
        // 优先通过SessionServiceImpl的getSessionByIdWithRedis获取session，兼容Redis场次
        Session session = null;
        try {
            session = sessionService.getSessionByIdWithRedis(order.getSessionId());
        } catch (Exception e) {
            session = null;
        }
        Movie movie = (session != null) ? movieRepository.findById(session.getMovieId()).orElse(null) : null;
        List<OrderSeat> seats = orderSeatRepository.findByOrderId(order.getId());

        String movieTitle = (movie != null) ? movie.getTitle() : "未知影片";
        LocalDateTime sessionTime = (session != null && session.getStartTime() != null) ? session.getStartTime() : null;

        UserOrderDTO dto = new UserOrderDTO(
                order.getId(),
                order.getOrderNo(),
                movieTitle,
                sessionTime,
                seats.size(),
                order.getTotalAmount(),
                order.getStatus());
        return dto;
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
        String redisKey = "session_seats:" + order.getSessionId();
        String json = RedisCacheUtils.get(redisKey);
        if (json == null || json.isEmpty())
            return;
        List<List<Map<String, Object>>> seatStatus;
        try {
            seatStatus = objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return;
        }
        // 取出订单的所有row/col
        List<OrderSeat> orderSeats = orderSeatRepository.findByOrderId(order.getId());
        for (OrderSeat os : orderSeats) {
            int row = os.getRowNo();
            int col = os.getColNo();
            if ("SUCCESS".equals(status)) {
                // 修改为OCCUPIED
                Map<String, Object> cell = seatStatus.get(row).get(col);
                if (cell != null && !"NULL".equals(cell.get("type"))) {
                    cell.put("status", "OCCUPIED");
                }
            } else {
                // 支付失败恢复为AVAILABLE
                Map<String, Object> cell = seatStatus.get(row).get(col);
                if (cell != null && !"NULL".equals(cell.get("type"))) {
                    cell.put("status", "AVAILABLE");
                }
            }
        }
        try {
            RedisCacheUtils.set(redisKey, objectMapper.writeValueAsString(seatStatus));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // ignore
        }
        // 订单状态更新
        String statusUpper = status != null ? status.toUpperCase() : "";
        if ("SUCCESS".equals(statusUpper) || "TRADE_SUCCESS".equals(statusUpper) || "COMPLETED".equals(statusUpper)
                || "FINISH".equals(statusUpper)) {
            order.setStatus("FINISH");
            order.setPaymentTime(LocalDateTime.now());
            // 生成电子票等
            String eTicketUrl = generateETicket(order);
            order.setETicketUrl(eTicketUrl);
        } else {
            order.setStatus("PAYMENT_FAILED");
        }
        orderRepository.save(order);
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
        System.out.println(callbackRequest.getPaymentStatus());
        if ("TRADE_SUCCESS".equalsIgnoreCase(callbackRequest.getPaymentStatus())) {
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