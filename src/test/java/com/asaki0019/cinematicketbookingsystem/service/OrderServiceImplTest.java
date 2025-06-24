package com.asaki0019.cinematicketbookingsystem.service;

import com.asaki0019.cinematicketbookingsystem.entities.OrderRequest;
import com.asaki0019.cinematicketbookingsystem.services.OrderService;
import com.asaki0019.cinematicketbookingsystem.services.UserService;
import com.asaki0019.cinematicketbookingsystem.utils.PaymentGatewayUtils;
import com.asaki0019.cinematicketbookingsystem.utils.RedisCacheUtils;
import com.asaki0019.cinematicketbookingsystem.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

@SpringBootTest
@Transactional
public class OrderServiceImplTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderRepository orderRepository;

    private Long testUserId;
    private static MockedStatic<PaymentGatewayUtils> paymentGatewayUtilsMock;

    // 新增唯一sessionId和seatIds
    private Long testSessionId;
    private List<Long> testSeatIds;

    @BeforeEach
    void setUp() {
        // 创建一个测试用户
        String username = "orderuser" + System.currentTimeMillis();
        Map<String, Object> userResult = userService.register(username, "password", username + "@test.com",
                String.valueOf(System.currentTimeMillis()));
        testUserId = (Long) userResult.get("id");

        // 用唯一ID避免冲突
        testSessionId = System.currentTimeMillis();
        testSeatIds = List.of(testSessionId + 1, testSessionId + 2);

        // 清理Redis座位锁
        for (Long seatId : testSeatIds) {
            RedisCacheUtils.del("seat:lock:" + testSessionId + ":" + seatId);
        }

        // 静态mock PaymentGatewayUtils.unifiedOrder
        if (paymentGatewayUtilsMock != null) {
            paymentGatewayUtilsMock.close();
        }
        paymentGatewayUtilsMock = mockStatic(PaymentGatewayUtils.class);
        Map<String, Object> mockPaymentResult = new HashMap<>();
        mockPaymentResult.put("payUrl", "http://mock-payment-url.com");
        paymentGatewayUtilsMock.when(() -> PaymentGatewayUtils.unifiedOrder(any(), anyString(), anyDouble(),
                anyString(), anyString(), anyString(), any()))
                .thenReturn(mockPaymentResult);
    }

    @Test
    void testCreateOrder() {
        // 构造订单请求
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setUserId(testUserId);
        orderRequest.setSessionId(testSessionId);
        orderRequest.setSeatIds(testSeatIds);
        orderRequest.setPaymentMethod("ALIPAY");

        // 创建订单
        Map<String, Object> result = orderService.createOrder(orderRequest);

        // 验证结果
        assertNotNull(result.get("id"));
        assertNotNull(result.get("order_no"));
        assertEquals(100.0, (Double) result.get("total_amount")); // 2 seats * 50.0
        assertEquals("http://mock-payment-url.com", result.get("payment_url"));
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        // 清理Redis座位锁
        if (testSeatIds != null && testSessionId != null) {
            for (Long seatId : testSeatIds) {
                RedisCacheUtils.del("seat:lock:" + testSessionId + ":" + seatId);
            }
        }
    }
}