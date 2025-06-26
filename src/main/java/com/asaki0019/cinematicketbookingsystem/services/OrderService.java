package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Order;
import com.asaki0019.cinematicketbookingsystem.dto.UserOrderDTO;
import com.asaki0019.cinematicketbookingsystem.entities.OrderRequest;
import com.asaki0019.cinematicketbookingsystem.entities.PaymentCallbackRequest;
import com.asaki0019.cinematicketbookingsystem.dto.PaymentStatusDTO;

import java.util.List;
import java.util.Map;

public interface OrderService {

    Map<String, Object> createOrder(OrderRequest orderRequest);

    List<UserOrderDTO> getUserOrders(Long userId, String status);

    Map<String, Object> cancelOrder(Long orderId);

    PaymentStatusDTO getPaymentStatus(Long orderId);

    void handlePaymentCallback(String orderNo, String status, String transactionId, double amount);

    void processPaymentCallback(PaymentCallbackRequest callbackRequest);

    Map<String, Object> getOrderStatusByOrderNo(String orderNo);
}