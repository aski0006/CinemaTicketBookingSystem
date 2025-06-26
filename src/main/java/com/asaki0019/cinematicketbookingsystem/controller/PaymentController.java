package com.asaki0019.cinematicketbookingsystem.controller;

import com.asaki0019.cinematicketbookingsystem.entities.PaymentCallbackRequest;
import com.asaki0019.cinematicketbookingsystem.services.OrderService;
import com.asaki0019.cinematicketbookingsystem.services.MembershipOrderService;
import com.asaki0019.cinematicketbookingsystem.utils.PaymentGatewayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private MembershipOrderService membershipOrderService;

    /**
     * 处理支付宝支付回调
     */
    @PostMapping("/callback")
    public ResponseEntity<?> handlePaymentCallback(HttpServletRequest request) {
        // 兼容支付宝 form-data 和 JSON 及沙箱参数名
        String orderNo = request.getParameter("orderNo");
        if (orderNo == null)
            orderNo = request.getParameter("out_trade_no");

        String paymentStatus = request.getParameter("paymentStatus");
        if (paymentStatus == null)
            paymentStatus = request.getParameter("trade_status");

        String transactionId = request.getParameter("transactionId");
        if (transactionId == null)
            transactionId = request.getParameter("trade_no");

        String amountStr = request.getParameter("amount");
        if (amountStr == null)
            amountStr = request.getParameter("total_amount");
        Double amount = null;
        try {
            amount = amountStr != null ? Double.valueOf(amountStr) : null;
        } catch (Exception e) {
            amount = null;
        }
        // 兼容 JSON（如果参数为 null，尝试解析 JSON）
        if (orderNo == null || paymentStatus == null || transactionId == null || amount == null) {
            try {
                String body = request.getReader().lines().reduce("", (acc, line) -> acc + line);
                if (body != null && !body.isEmpty()) {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    java.util.Map<String, Object> map = mapper.readValue(body, java.util.Map.class);
                    if (orderNo == null && map.get("orderNo") != null)
                        orderNo = map.get("orderNo").toString();
                    if (orderNo == null && map.get("out_trade_no") != null)
                        orderNo = map.get("out_trade_no").toString();
                    if (paymentStatus == null && map.get("paymentStatus") != null)
                        paymentStatus = map.get("paymentStatus").toString();
                    if (paymentStatus == null && map.get("trade_status") != null)
                        paymentStatus = map.get("trade_status").toString();
                    if (transactionId == null && map.get("transactionId") != null)
                        transactionId = map.get("transactionId").toString();
                    if (transactionId == null && map.get("trade_no") != null)
                        transactionId = map.get("trade_no").toString();
                    if (amount == null && map.get("amount") != null)
                        amount = Double.valueOf(map.get("amount").toString());
                    if (amount == null && map.get("total_amount") != null)
                        amount = Double.valueOf(map.get("total_amount").toString());
                }
            } catch (Exception ignore) {
            }
        }
        if (orderNo == null || paymentStatus == null || transactionId == null || amount == null) {
            return ResponseEntity.badRequest().body("参数不完整");
        }
        if (orderNo.startsWith("VIP") || orderNo.startsWith("SVIP")) {
            // 状态转换
            String mappedStatus = "FAILED";
            if ("TRADE_SUCCESS".equalsIgnoreCase(paymentStatus) || "SUCCESS".equalsIgnoreCase(paymentStatus)) {
                mappedStatus = "SUCCESS";
            }
            membershipOrderService.handlePaymentCallback(
                    orderNo,
                    mappedStatus,
                    transactionId,
                    amount);
        } else {
            PaymentCallbackRequest callbackRequest = new PaymentCallbackRequest();
            callbackRequest.setOrderNo(orderNo);
            callbackRequest.setPaymentStatus(paymentStatus);
            callbackRequest.setTransactionId(transactionId);
            callbackRequest.setAmount(amount);
            orderService.processPaymentCallback(callbackRequest);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * 查询订单支付状态
     */
    @GetMapping("/query/{orderNo}")
    public ResponseEntity<Map<String, Object>> queryOrderStatus(@PathVariable String orderNo) {
        try {
            Map<String, Object> result = new HashMap<>();
            if (orderNo.startsWith("VIP") || orderNo.startsWith("SVIP")) {
                // 会员订单
                result = membershipOrderService.getOrderStatusByOrderNo(orderNo);
            } else {
                // 普通订单
                result = orderService.getOrderStatusByOrderNo(orderNo);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 申请退款
     */
    @PostMapping("/refund")
    public ResponseEntity<Map<String, Object>> refundOrder(
            @RequestParam String orderNo,
            @RequestParam Double refundAmount,
            @RequestParam String reason) {
        try {
            Map<String, Object> result = PaymentGatewayUtils.refund(
                    PaymentGatewayUtils.PayType.ALIPAY,
                    orderNo,
                    refundAmount,
                    reason);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 模拟支付宝支付成功（仅用于测试）
     */
    @PostMapping("/simulate/success/{orderNo}")
    public ResponseEntity<Map<String, Object>> simulatePaymentSuccess(@PathVariable String orderNo) {
        try {
            boolean success = PaymentGatewayUtils.simulateAlipayPaymentSuccess(orderNo);
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", success ? "模拟支付成功" : "模拟支付失败");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 模拟支付宝支付失败（仅用于测试）
     */
    @PostMapping("/simulate/failed/{orderNo}")
    public ResponseEntity<Map<String, Object>> simulatePaymentFailed(@PathVariable String orderNo) {
        try {
            boolean success = PaymentGatewayUtils.simulateAlipayPaymentFailed(orderNo);
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", success ? "模拟支付失败成功" : "模拟支付失败操作失败");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 获取订单信息（仅用于测试）
     */
    @GetMapping("/order-info/{orderNo}")
    public ResponseEntity<Map<String, Object>> getOrderInfo(@PathVariable String orderNo) {
        try {
            PaymentGatewayUtils.AlipayOrderInfo orderInfo = PaymentGatewayUtils.getAlipayOrderInfo(orderNo);
            if (orderInfo != null) {
                Map<String, Object> result = new HashMap<>();
                result.put("orderNo", orderInfo.getOrderNo());
                result.put("amount", orderInfo.getAmount());
                result.put("status", orderInfo.getStatus());
                result.put("tradeNo", orderInfo.getTradeNo());
                result.put("subject", orderInfo.getSubject());
                result.put("createTime", orderInfo.getCreateTime());
                return ResponseEntity.ok(result);
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "订单不存在");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 清空所有订单（仅用于测试）
     */
    @DeleteMapping("/clear-orders")
    public ResponseEntity<Map<String, Object>> clearOrders() {
        try {
            PaymentGatewayUtils.clearAlipayOrders();
            Map<String, Object> result = new HashMap<>();
            result.put("message", "已清空所有支付宝订单");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}