package com.asaki0019.cinematicketbookingsystem.controller;

import com.asaki0019.cinematicketbookingsystem.dto.PaymentStatusDTO;
import com.asaki0019.cinematicketbookingsystem.dto.UserOrderDTO;
import com.asaki0019.cinematicketbookingsystem.entities.OrderRequest;
import com.asaki0019.cinematicketbookingsystem.services.OrderService;
import com.asaki0019.cinematicketbookingsystem.utils.JwtTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("")
    public Object createOrder(@RequestHeader(name = "Authorization", required = false) String token,
            @RequestBody OrderRequest orderRequest) {
        if (!checkJwt(token)) {
            return Map.of("error", "未登录或token已过期");
        }
        try {
            return orderService.createOrder(orderRequest);
        } catch (Exception e) {
            return Map.of("errorCode", "SEAT_UNAVAILABLE", "errorMessage", e.getMessage());
        }
    }

    @GetMapping("/users/{userId}")
    public List<UserOrderDTO> getOrdersByUserId(@PathVariable Long userId,
            @RequestParam(required = false) String status) {
        return orderService.getUserOrders(userId, status);
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable Long orderId) {
        Map<String, Object> result = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{orderId}/payment-status")
    public ResponseEntity<PaymentStatusDTO> getPaymentStatus(@PathVariable Long orderId) {
        PaymentStatusDTO status = orderService.getPaymentStatus(orderId);
        return ResponseEntity.ok(status);
    }

    private boolean checkJwt(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return false;
        }
        token = token.substring(7);
        return JwtTokenUtils.validateToken(token);
    }
}