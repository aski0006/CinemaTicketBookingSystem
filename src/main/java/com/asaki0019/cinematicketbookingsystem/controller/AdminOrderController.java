package com.asaki0019.cinematicketbookingsystem.controller;

import com.asaki0019.cinematicketbookingsystem.entities.Order;
import com.asaki0019.cinematicketbookingsystem.services.AdminOrderService;
import com.asaki0019.cinematicketbookingsystem.utils.JwtTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
        return token != null && JwtTokenUtils.validateToken(token);
    }

    /**
     * 查询订单列表
     * /admin/orders?status=&startDate=&endDate=
     */
    @GetMapping
    public Object getOrderList(@RequestHeader("Authorization") String token,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (!checkJwt(token)) {
            return Map.of("error", "未登录或token已过期");
        }
        LocalDate start = startDate != null && !startDate.isEmpty() ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null && !endDate.isEmpty() ? LocalDate.parse(endDate) : null;
        List<Order> orders = adminOrderService.getOrderList(status, start, end);
        Map<String, Object> resp = new HashMap<>();
        resp.put("orders", orders);
        resp.put("total", orders.size());
        return resp;
    }

    /**
     * 订单退款
     * /admin/orders/{orderId}/refund
     */
    @PostMapping("/{orderId}/refund")
    public Object refundOrder(@RequestHeader("Authorization") String token,
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> req) {
        if (!checkJwt(token)) {
            return Map.of("error", "未登录或token已过期");
        }
        Double refundAmount = req.get("refundAmount") != null ? Double.valueOf(req.get("refundAmount").toString())
                : null;
        String reason = req.get("reason") != null ? req.get("reason").toString() : "";
        Order refunded = adminOrderService.refundOrder(orderId, refundAmount, reason);
        if (refunded == null) {
            return Map.of("error", "订单不存在或无法退款");
        }
        return refunded;
    }
}