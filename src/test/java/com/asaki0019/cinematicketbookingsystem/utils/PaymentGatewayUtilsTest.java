package com.asaki0019.cinematicketbookingsystem.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 支付宝支付网关工具类测试
 */
public class PaymentGatewayUtilsTest {

    @BeforeEach
    void setUp() {
        // 清空测试数据
        PaymentGatewayUtils.clearAlipayOrders();
    }

    @Test
    @DisplayName("测试支付宝网页支付下单")
    void testAlipayPageOrder() {
        String orderNo = "TEST_ORDER_" + System.currentTimeMillis();
        double amount = 100.00;
        String subject = "测试商品";
        String body = "测试商品描述";
        String notifyUrl = "http://localhost:8081/api/payments/callback";

        Map<String, String> extra = new HashMap<>();
        extra.put("pay_method", "page");
        extra.put("return_url", "http://localhost:8081/return_url");

        Map<String, Object> result = PaymentGatewayUtils.unifiedOrder(
                PaymentGatewayUtils.PayType.ALIPAY,
                orderNo,
                amount,
                subject,
                body,
                notifyUrl,
                extra);

        assertNotNull(result);
        assertEquals(orderNo, result.get("orderNo"));
        assertEquals(amount, result.get("amount"));
        assertEquals("alipay", result.get("type"));
        assertNotNull(result.get("payUrl"));
        assertTrue(result.get("payUrl").toString().contains("form"));
    }

    @Test
    @DisplayName("测试支付宝App支付下单")
    void testAlipayAppOrder() {
        String orderNo = "TEST_APP_ORDER_" + System.currentTimeMillis();
        double amount = 50.00;
        String subject = "App测试商品";
        String body = "App测试商品描述";
        String notifyUrl = "http://localhost:8081/api/payments/callback";

        Map<String, String> extra = new HashMap<>();
        extra.put("pay_method", "app");
        extra.put("return_url", "http://localhost:8081/return_url");

        Map<String, Object> result = PaymentGatewayUtils.unifiedOrder(
                PaymentGatewayUtils.PayType.ALIPAY,
                orderNo,
                amount,
                subject,
                body,
                notifyUrl,
                extra);

        assertNotNull(result);
        assertEquals(orderNo, result.get("orderNo"));
        assertEquals(amount, result.get("amount"));
        assertEquals("alipay", result.get("type"));
        assertNotNull(result.get("appPayOrderString"));
    }

    @Test
    @DisplayName("测试查询订单状态")
    void testQueryOrder() {
        // 先创建订单
        String orderNo = "TEST_QUERY_ORDER_" + System.currentTimeMillis();
        double amount = 75.00;

        Map<String, String> extra = new HashMap<>();
        extra.put("pay_method", "page");

        PaymentGatewayUtils.unifiedOrder(
                PaymentGatewayUtils.PayType.ALIPAY,
                orderNo,
                amount,
                "查询测试商品",
                "查询测试描述",
                "http://localhost:8081/api/payments/callback",
                extra);

        // 查询订单
        Map<String, Object> result = PaymentGatewayUtils.queryOrder(
                PaymentGatewayUtils.PayType.ALIPAY,
                orderNo);

        assertNotNull(result);
        assertEquals(orderNo, result.get("orderNo"));
        assertEquals("WAIT_PAY", result.get("status"));
        assertEquals(amount, result.get("amount"));
    }

    @Test
    @DisplayName("测试模拟支付成功")
    void testSimulatePaymentSuccess() {
        String orderNo = "TEST_SUCCESS_ORDER_" + System.currentTimeMillis();
        double amount = 25.00;

        Map<String, String> extra = new HashMap<>();
        extra.put("pay_method", "page");

        PaymentGatewayUtils.unifiedOrder(
                PaymentGatewayUtils.PayType.ALIPAY,
                orderNo,
                amount,
                "成功测试商品",
                "成功测试描述",
                "http://localhost:8081/api/payments/callback",
                extra);

        // 模拟支付成功
        boolean success = PaymentGatewayUtils.simulateAlipayPaymentSuccess(orderNo);
        assertTrue(success);

        // 查询订单状态
        Map<String, Object> result = PaymentGatewayUtils.queryOrder(
                PaymentGatewayUtils.PayType.ALIPAY,
                orderNo);
        assertEquals("SUCCESS", result.get("status"));
    }

    @Test
    @DisplayName("测试模拟支付失败")
    void testSimulatePaymentFailed() {
        String orderNo = "TEST_FAILED_ORDER_" + System.currentTimeMillis();
        double amount = 30.00;

        Map<String, String> extra = new HashMap<>();
        extra.put("pay_method", "page");

        PaymentGatewayUtils.unifiedOrder(
                PaymentGatewayUtils.PayType.ALIPAY,
                orderNo,
                amount,
                "失败测试商品",
                "失败测试描述",
                "http://localhost:8081/api/payments/callback",
                extra);

        // 模拟支付失败
        boolean success = PaymentGatewayUtils.simulateAlipayPaymentFailed(orderNo);
        assertTrue(success);

        // 查询订单状态
        Map<String, Object> result = PaymentGatewayUtils.queryOrder(
                PaymentGatewayUtils.PayType.ALIPAY,
                orderNo);
        assertEquals("FAILED", result.get("status"));
    }

    @Test
    @DisplayName("测试退款功能")
    void testRefund() {
        String orderNo = "TEST_REFUND_ORDER_" + System.currentTimeMillis();
        double amount = 200.00;
        double refundAmount = 100.00;

        Map<String, String> extra = new HashMap<>();
        extra.put("pay_method", "page");

        PaymentGatewayUtils.unifiedOrder(
                PaymentGatewayUtils.PayType.ALIPAY,
                orderNo,
                amount,
                "退款测试商品",
                "退款测试描述",
                "http://localhost:8081/api/payments/callback",
                extra);

        // 先支付成功
        PaymentGatewayUtils.simulateAlipayPaymentSuccess(orderNo);

        // 申请退款
        Map<String, Object> refundResult = PaymentGatewayUtils.refund(
                PaymentGatewayUtils.PayType.ALIPAY,
                orderNo,
                refundAmount,
                "测试退款");

        assertNotNull(refundResult);
        assertEquals("SUCCESS", refundResult.get("refundStatus"));
        assertEquals(refundAmount, refundResult.get("refundAmount"));
        assertEquals("测试退款", refundResult.get("refundReason"));

        // 查询订单状态
        Map<String, Object> result = PaymentGatewayUtils.queryOrder(
                PaymentGatewayUtils.PayType.ALIPAY,
                orderNo);
        assertEquals("REFUNDED", result.get("status"));
    }

    @Test
    @DisplayName("测试退款金额超过订单金额")
    void testRefundAmountExceedsOrderAmount() {
        String orderNo = "TEST_REFUND_EXCEED_ORDER_" + System.currentTimeMillis();
        double amount = 50.00;
        double refundAmount = 100.00;

        Map<String, String> extra = new HashMap<>();
        extra.put("pay_method", "page");

        PaymentGatewayUtils.unifiedOrder(
                PaymentGatewayUtils.PayType.ALIPAY,
                orderNo,
                amount,
                "超额退款测试商品",
                "超额退款测试描述",
                "http://localhost:8081/api/payments/callback",
                extra);

        // 先支付成功
        PaymentGatewayUtils.simulateAlipayPaymentSuccess(orderNo);

        // 申请超额退款
        assertThrows(RuntimeException.class, () -> {
            PaymentGatewayUtils.refund(
                    PaymentGatewayUtils.PayType.ALIPAY,
                    orderNo,
                    refundAmount,
                    "超额退款测试");
        });
    }

    @Test
    @DisplayName("测试未支付订单退款")
    void testRefundUnpaidOrder() {
        String orderNo = "TEST_REFUND_UNPAID_ORDER_" + System.currentTimeMillis();
        double amount = 60.00;
        double refundAmount = 30.00;

        Map<String, String> extra = new HashMap<>();
        extra.put("pay_method", "page");

        PaymentGatewayUtils.unifiedOrder(
                PaymentGatewayUtils.PayType.ALIPAY,
                orderNo,
                amount,
                "未支付退款测试商品",
                "未支付退款测试描述",
                "http://localhost:8081/api/payments/callback",
                extra);

        // 直接申请退款（未支付）
        assertThrows(RuntimeException.class, () -> {
            PaymentGatewayUtils.refund(
                    PaymentGatewayUtils.PayType.ALIPAY,
                    orderNo,
                    refundAmount,
                    "未支付退款测试");
        });
    }

    @Test
    @DisplayName("测试查询不存在的订单")
    void testQueryNonExistentOrder() {
        String orderNo = "NON_EXISTENT_ORDER_" + System.currentTimeMillis();

        assertThrows(RuntimeException.class, () -> {
            PaymentGatewayUtils.queryOrder(
                    PaymentGatewayUtils.PayType.ALIPAY,
                    orderNo);
        });
    }

    @Test
    @DisplayName("测试获取订单信息")
    void testGetOrderInfo() {
        String orderNo = "TEST_INFO_ORDER_" + System.currentTimeMillis();
        double amount = 80.00;

        Map<String, String> extra = new HashMap<>();
        extra.put("pay_method", "page");

        PaymentGatewayUtils.unifiedOrder(
                PaymentGatewayUtils.PayType.ALIPAY,
                orderNo,
                amount,
                "信息测试商品",
                "信息测试描述",
                "http://localhost:8081/api/payments/callback",
                extra);

        PaymentGatewayUtils.AlipayOrderInfo orderInfo = PaymentGatewayUtils.getAlipayOrderInfo(orderNo);
        assertNotNull(orderInfo);
        assertEquals(orderNo, orderInfo.getOrderNo());
        assertEquals(amount, orderInfo.getAmount());
        assertEquals("WAIT_PAY", orderInfo.getStatus());
        assertNotNull(orderInfo.getTradeNo());
        assertTrue(orderInfo.getTradeNo().startsWith("ALIPAY_"));
    }

    @Test
    @DisplayName("测试清空订单")
    void testClearOrders() {
        // 创建多个订单
        for (int i = 0; i < 3; i++) {
            String orderNo = "TEST_CLEAR_ORDER_" + i + "_" + System.currentTimeMillis();
            Map<String, String> extra = new HashMap<>();
            extra.put("pay_method", "page");

            PaymentGatewayUtils.unifiedOrder(
                    PaymentGatewayUtils.PayType.ALIPAY,
                    orderNo,
                    10.00 + i,
                    "清空测试商品" + i,
                    "清空测试描述" + i,
                    "http://localhost:8081/api/payments/callback",
                    extra);
        }

        // 清空订单
        PaymentGatewayUtils.clearAlipayOrders();

        // 验证订单已被清空
        String orderNo = "TEST_CLEAR_ORDER_0_" + System.currentTimeMillis();
        assertThrows(RuntimeException.class, () -> {
            PaymentGatewayUtils.queryOrder(
                    PaymentGatewayUtils.PayType.ALIPAY,
                    orderNo);
        });
    }
}