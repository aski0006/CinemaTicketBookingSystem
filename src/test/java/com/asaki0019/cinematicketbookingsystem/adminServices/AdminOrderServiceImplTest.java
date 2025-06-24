package com.asaki0019.cinematicketbookingsystem.adminServices;

import com.asaki0019.cinematicketbookingsystem.entities.Order;
import com.asaki0019.cinematicketbookingsystem.repository.AdminOrderRepository;
import com.asaki0019.cinematicketbookingsystem.services.AdminOrderServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AdminOrderServiceImplTest {
    @Mock
    private AdminOrderRepository adminOrderRepository;
    @InjectMocks
    private AdminOrderServiceImpl adminOrderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetOrderList() {
        when(adminOrderRepository.findAll()).thenReturn(Collections.emptyList());
        List<Order> result = adminOrderService.getOrderList(null, null, null);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testRefundOrder() {
        Order o = new Order();
        when(adminOrderRepository.findById(1L)).thenReturn(java.util.Optional.of(o));
        when(adminOrderRepository.save(o)).thenReturn(o);
        Order result = adminOrderService.refundOrder(1L, 10.0, "test");
        assertEquals("REFUNDED", result.getStatus());
    }
}