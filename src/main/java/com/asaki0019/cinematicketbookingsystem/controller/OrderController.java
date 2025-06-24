package com.asaki0019.cinematicketbookingsystem.controller;

import com.asaki0019.cinematicketbookingsystem.entities.Order;
import com.asaki0019.cinematicketbookingsystem.entities.OrderRequest;
import com.asaki0019.cinematicketbookingsystem.services.OrderService;
import com.asaki0019.cinematicketbookingsystem.utils.JwtTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestHeader("Authorization") String token,
            @RequestBody OrderRequest orderRequest) {
        // 验证JWT token
        if (!JwtTokenUtils.validateToken(token)) {
            return ResponseEntity.status(401).body("未登录或token已过期");
        }

        // 从token中获取用户信息并设置到请求中
        orderRequest.setUserId(JwtTokenUtils.getUserFromToken(token).getId());

        try {
            Map<String, Object> result = orderService.createOrder(orderRequest);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/orders/users/{userId}")
    public ResponseEntity<?> getUserOrders(@PathVariable Long userId,
            @RequestParam(required = false) String status) {
        try {
            List<Order> orders = orderService.getUserOrders(userId, status);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/orders/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        try {
            Map<String, Object> result = orderService.cancelOrder(orderId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/orders/{orderId}/payment-status")
    public ResponseEntity<?> getOrderPaymentStatus(@PathVariable Long orderId) {
        try {
            Map<String, Object> result = orderService.getOrderPaymentStatus(orderId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/payments/callback")
    public ResponseEntity<?> handlePaymentCallback(@RequestParam String orderNo,
            @RequestParam String paymentStatus,
            @RequestParam String transactionId,
            @RequestParam double amount) {
        try {
            orderService.handlePaymentCallback(orderNo, paymentStatus, transactionId, amount);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}