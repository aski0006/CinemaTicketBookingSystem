package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Order;
import com.asaki0019.cinematicketbookingsystem.entities.OrderRequest;
import com.asaki0019.cinematicketbookingsystem.repository.OrderRepository;
import com.asaki0019.cinematicketbookingsystem.utils.PaymentGatewayUtils;
import com.asaki0019.cinematicketbookingsystem.utils.RedisCacheUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ORDER_CACHE_PREFIX = "order:";
    private static final String SEAT_LOCK_PREFIX = "seat_lock:";
    private static final int ORDER_CACHE_MINUTES = 15;
    private static final String CALLBACK_URL = "http://localhost:8081/api/payments/callback";
    private static final String RETURN_URL = "http://localhost:8081/return_url";

    @Override
    @Transactional
    public Map<String, Object> createOrder(OrderRequest orderRequest) {
        // 1. 验证座位是否可用并加锁
        for (Long seatId : orderRequest.getSeatIds()) {
            String lockKey = SEAT_LOCK_PREFIX + seatId;
            if (RedisCacheUtils.exists(lockKey)) {
                throw new RuntimeException("座位已被锁定");
            }
            RedisCacheUtils.set(lockKey, orderRequest.getUserId().toString(), ORDER_CACHE_MINUTES * 60);
        }

        // 2. 创建订单
        Order order = new Order();
        order.setUserId(orderRequest.getUserId());
        order.setSessionId(orderRequest.getSessionId());
        order.setOrderNo(generateOrderNo());
        order.setStatus("PENDING_PAYMENT");
        order.setCreateTime(LocalDateTime.now());

        // 计算总金额 (实际项目中需要根据座位类型和会员等级计算)
        double totalAmount = calculateTotalAmount(orderRequest.getSeatIds());
        order.setTotalAmount(totalAmount);

        // 3. 保存订单
        order = orderRepository.save(order);

        // 4. 缓存订单信息
        try {
            String orderJson = objectMapper.writeValueAsString(order);
            RedisCacheUtils.set(ORDER_CACHE_PREFIX + order.getOrderNo(), orderJson, ORDER_CACHE_MINUTES * 60);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("订单缓存失败");
        }

        // 5. 调用支付网关
        Map<String, String> extraParams = new HashMap<>();
        extraParams.put("return_url", RETURN_URL);

        Map<String, Object> paymentResult = PaymentGatewayUtils.unifiedOrder(
                PaymentGatewayUtils.PayType.valueOf(orderRequest.getPaymentMethod()),
                order.getOrderNo(),
                totalAmount,
                "电影票购买",
                "场次ID:" + orderRequest.getSessionId(),
                CALLBACK_URL,
                extraParams);

        // 6. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("id", order.getId());
        result.put("order_no", order.getOrderNo());
        result.put("total_amount", totalAmount);
        result.put("payment_url", paymentResult.get("payUrl"));
        return result;
    }

    @Override
    public List<Order> getUserOrders(Long userId, String status) {
        return orderRepository.findByUserIdAndStatus(userId, status);
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
    public Map<String, Object> getOrderPaymentStatus(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        Map<String, Object> result = new HashMap<>();
        result.put("order_no", order.getOrderNo());
        result.put("status", order.getStatus());
        result.put("last_update", order.getPaymentTime());
        return result;
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

    // 辅助方法
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + new Random().nextInt(1000);
    }

    private double calculateTotalAmount(List<Long> seatIds) {
        // TODO: 实际项目中需要根据座位类型和会员等级计算
        return seatIds.size() * 50.0;
    }

    private List<Long> getSeatIds(Order order) {
        // TODO: 实际项目中需要从OrderSeat表中获取
        return new ArrayList<>();
    }

    private String generateETicket(Order order) {
        // TODO: 实际项目中需要生成电子票
        return "https://your-domain/e-tickets/" + order.getOrderNo() + ".pdf";
    }
}