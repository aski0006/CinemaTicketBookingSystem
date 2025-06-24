package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Order;
import com.asaki0019.cinematicketbookingsystem.entities.OrderSeat;
import com.asaki0019.cinematicketbookingsystem.entities.Seat;
import com.asaki0019.cinematicketbookingsystem.repository.AdminOrderRepository;
import com.asaki0019.cinematicketbookingsystem.repository.OrderSeatRepository;
import com.asaki0019.cinematicketbookingsystem.repository.SeatRepository;
import com.asaki0019.cinematicketbookingsystem.utils.PaymentGatewayUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceImplTest {

    @Mock
    private AdminOrderRepository adminOrderRepository;

    @Mock
    private OrderSeatRepository orderSeatRepository;

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private AdminOrderServiceImpl adminOrderService;

    private MockedStatic<PaymentGatewayUtils> paymentGatewayUtilsMock;

    @BeforeEach
    void setUp() {
        paymentGatewayUtilsMock = mockStatic(PaymentGatewayUtils.class);
    }

    @AfterEach
    void tearDown() {
        paymentGatewayUtilsMock.close();
    }

    @Test
    void searchOrders() {
        when(adminOrderRepository.findWithFilters(any(), any(), any())).thenReturn(Collections.emptyList());
        List<Order> result = adminOrderService.searchOrders("COMPLETED", "2023-01-01", "2023-01-31");
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(adminOrderRepository).findWithFilters(eq("COMPLETED"), any(LocalDateTime.class),
                any(LocalDateTime.class));
    }

    @Test
    void getOrderDetails() {
        Order order = new Order();
        order.setId(1L);
        when(adminOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        Order result = adminOrderService.getOrderDetails(1L);
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getOrderDetails_NotFound() {
        when(adminOrderRepository.findById(1L)).thenReturn(Optional.empty());
        Order result = adminOrderService.getOrderDetails(1L);
        assertNull(result);
    }

    @Test
    void refundOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus("COMPLETED");
        order.setPaymentMethod("ALIPAY");
        order.setOrderNo("ORD123");

        OrderSeat orderSeat = new OrderSeat();
        orderSeat.setSeatId(10L);
        Seat seat = new Seat();
        seat.setId(10L);
        seat.setStatus("OCCUPIED");

        when(adminOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderSeatRepository.findByOrderId(1L)).thenReturn(List.of(orderSeat));
        when(seatRepository.findAllById(anyList())).thenReturn(List.of(seat));
        paymentGatewayUtilsMock.when(() -> PaymentGatewayUtils.refund(any(), anyString(), anyDouble(), anyString()))
                .then(invocation -> null);

        Order result = adminOrderService.refundOrder(1L, 50.0, "Customer request");

        assertEquals("REFUNDED", result.getStatus());
        assertEquals("AVAILABLE", seat.getStatus());
        verify(seatRepository).saveAll(anyList());
        verify(adminOrderRepository).save(order);
    }

    @Test
    void refundOrder_OrderNotFound() {
        when(adminOrderRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> adminOrderService.refundOrder(1L, 50.0, "reason"));
    }

    @Test
    void refundOrder_InvalidStatus() {
        Order order = new Order();
        order.setStatus("PENDING");
        when(adminOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        assertThrows(IllegalStateException.class, () -> adminOrderService.refundOrder(1L, 50.0, "reason"));
    }

    @Test
    void refundOrder_UnsupportedPaymentMethod() {
        Order order = new Order();
        order.setStatus("COMPLETED");
        order.setPaymentMethod("CASH");
        when(adminOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        assertThrows(IllegalStateException.class, () -> adminOrderService.refundOrder(1L, 50.0, "reason"));
    }
}