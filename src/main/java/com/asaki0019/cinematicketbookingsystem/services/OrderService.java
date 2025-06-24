package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Order;
import com.asaki0019.cinematicketbookingsystem.entities.OrderRequest;

import java.util.List;
import java.util.Map;

public interface OrderService {

    Map<String, Object> createOrder(OrderRequest orderRequest);

    List<Order> getUserOrders(Long userId, String status);

    Map<String, Object> cancelOrder(Long orderId);

    Map<String, Object> getOrderPaymentStatus(Long orderId);

    void handlePaymentCallback(String orderNo, String status, String transactionId, double amount);
}