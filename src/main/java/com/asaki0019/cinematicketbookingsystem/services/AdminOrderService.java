package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Order;
import java.util.List;

public interface AdminOrderService {
    Order refundOrder(Long orderId, Double refundAmount, String reason);

    List<Order> searchOrders(String status, String startDate, String endDate);

    Order getOrderDetails(Long orderId);
}