package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Order;
import java.util.List;
import java.time.LocalDate;

public interface AdminOrderService {
    List<Order> getOrderList(String status, LocalDate startDate, LocalDate endDate);

    Order refundOrder(Long orderId, Double refundAmount, String reason);
}