package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.dto.PaymentStatusDTO;
import com.asaki0019.cinematicketbookingsystem.dto.UserOrderDTO;
import com.asaki0019.cinematicketbookingsystem.entities.*;
import com.asaki0019.cinematicketbookingsystem.repository.*;
import com.asaki0019.cinematicketbookingsystem.utils.PaymentGatewayUtils;
import com.asaki0019.cinematicketbookingsystem.utils.RedisCacheUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderSeatRepository orderSeatRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private MovieRepository movieRepository;
    @Mock
    private RedisCacheUtils redisCacheUtils;
    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private MockedStatic<PaymentGatewayUtils> paymentGatewayUtilsMock;
    private MockedStatic<RedisCacheUtils> redisCacheUtilsMock;

    @BeforeEach
    void setUp() {
        paymentGatewayUtilsMock = mockStatic(PaymentGatewayUtils.class);
        redisCacheUtilsMock = mockStatic(RedisCacheUtils.class);
    }

    @AfterEach
    void tearDown() {
        paymentGatewayUtilsMock.close();
        redisCacheUtilsMock.close();
    }

    private OrderRequest createValidOrderRequest() {
        OrderRequest request = new OrderRequest();
        request.setUserId(1L);
        request.setSessionId(1L);
        request.setSeatIds(List.of(1L, 2L));
        request.setPaymentMethod("ALIPAY");
        return request;
    }

    @Test
    void createOrder_Success() throws JsonProcessingException {
        OrderRequest request = createValidOrderRequest();
        Order savedOrder = new Order();
        savedOrder.setOrderNo("ORD123");

        redisCacheUtilsMock.when(() -> RedisCacheUtils.exists(anyString())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(objectMapper.writeValueAsString(any(Order.class))).thenReturn("{}");
        paymentGatewayUtilsMock
                .when(() -> PaymentGatewayUtils.unifiedOrder(any(), anyString(), anyDouble(), anyString(), anyString(),
                        anyString(), anyMap()))
                .thenReturn(new HashMap<>());

        var result = orderService.createOrder(request);
        assertNotNull(result);
        assertEquals("ORD123", result.get("order_no"));
    }

    @Test
    void createOrder_SeatLocked() {
        OrderRequest request = createValidOrderRequest();
        redisCacheUtilsMock.when(() -> RedisCacheUtils.exists(startsWith("seat_lock:"))).thenReturn(true);
        assertThrows(RuntimeException.class, () -> orderService.createOrder(request));
    }

    @Test
    void getUserOrders() {
        Order order = new Order();
        order.setSessionId(1L);
        Session session = new Session();
        session.setMovieId(1L);

        when(orderRepository.findByUserId(1L)).thenReturn(Collections.singletonList(order));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(movieRepository.findById(1L)).thenReturn(Optional.of(new Movie()));
        when(orderSeatRepository.findByOrderId(any())).thenReturn(Collections.emptyList());

        List<UserOrderDTO> result = orderService.getUserOrders(1L, null);
        assertFalse(result.isEmpty());
    }

    @Test
    void cancelOrder_Success() {
        Order order = new Order();
        order.setStatus("PENDING_PAYMENT");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        var result = orderService.cancelOrder(1L);
        assertEquals("CANCELLED", result.get("new_status"));
        assertEquals("CANCELLED", order.getStatus());
    }

    @Test
    void handlePaymentCallback_Success() {
        Order order = new Order();
        when(orderRepository.findByOrderNo("ORD123")).thenReturn(order);
        orderService.handlePaymentCallback("ORD123", "SUCCESS", "TXN123", 100.0);
        assertEquals("COMPLETED", order.getStatus());
    }
}