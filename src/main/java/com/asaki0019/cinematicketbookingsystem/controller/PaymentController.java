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
    public ResponseEntity<?> handlePaymentCallback(
            @RequestBody PaymentCallbackRequest callbackRequest,
            @RequestHeader(value = "X-Signature", required = false) String signature) {

        // 在真实场景中，必须校验来自支付网关的签名
        if (!PaymentGatewayUtils.isValidSignature(callbackRequest.toString(), signature)) {
            // 可以记录此非法请求或作为安全事件处理
            // return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid
            // signature.");
        }

        // 根据订单号判断是普通订单还是会员订单
        String orderNo = callbackRequest.getOrderNo();
        if (orderNo.startsWith("VIP") || orderNo.startsWith("SVIP")) {
            // 会员订单
            membershipOrderService.handlePaymentCallback(
                    orderNo,
                    callbackRequest.getPaymentStatus(),
                    callbackRequest.getTransactionId(),
                    callbackRequest.getAmount());
        } else {
            // 普通订单
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
            Map<String, Object> result = PaymentGatewayUtils.queryOrder(
                    PaymentGatewayUtils.PayType.ALIPAY,
                    orderNo);
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