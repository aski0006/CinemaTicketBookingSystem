package com.asaki0019.cinematicketbookingsystem.controller;

import com.asaki0019.cinematicketbookingsystem.dto.RefundRequestDTO;
import com.asaki0019.cinematicketbookingsystem.entities.Order;
import com.asaki0019.cinematicketbookingsystem.services.AdminOrderService;
import com.asaki0019.cinematicketbookingsystem.utils.JwtTokenUtils;
import com.asaki0019.cinematicketbookingsystem.utils.LogSystem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/orders")
public class AdminOrderController {
    @Autowired
    private AdminOrderService adminOrderService;

    private boolean checkJwt(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return false;
        }
        token = token.substring(7);
        return JwtTokenUtils.validateToken(token);
    }

    /**
     * 查询订单列表
     * /admin/orders?status=&startDate=&endDate=
     */
    @GetMapping("")
    public Object getOrderList(@RequestHeader("Authorization") String token,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (!checkJwt(token)) {
            return Map.of("error", "未登录或token已过期");
        }
        List<Order> orders = adminOrderService.searchOrders(status, startDate, endDate);
        Map<String, Object> resp = new HashMap<>();
        resp.put("orders", orders);
        resp.put("total", orders.size());
        return resp;
    }

    /**
     * 查询订单详情
     * /admin/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderDetails(@PathVariable Long orderId) {
        Order order = adminOrderService.getOrderDetails(orderId);
        return ResponseEntity.ok(order);
    }

    /**
     * 订单退款
     * /admin/orders/{orderId}/refund
     */
    @PostMapping("/{orderId}/refund")
    public ResponseEntity<Order> refundOrder(@PathVariable Long orderId, @RequestBody RefundRequestDTO refundRequest) {
        Order refundedOrder = adminOrderService.refundOrder(
                orderId,
                refundRequest.getRefundAmount(),
                refundRequest.getReason());
        return ResponseEntity.ok(refundedOrder);
    }
}