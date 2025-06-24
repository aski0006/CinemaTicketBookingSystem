package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Order;
import com.asaki0019.cinematicketbookingsystem.entities.OrderSeat;
import com.asaki0019.cinematicketbookingsystem.entities.Seat;
import com.asaki0019.cinematicketbookingsystem.repository.AdminOrderRepository;
import com.asaki0019.cinematicketbookingsystem.repository.OrderSeatRepository;
import com.asaki0019.cinematicketbookingsystem.repository.SeatRepository;
import com.asaki0019.cinematicketbookingsystem.utils.PaymentGatewayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class AdminOrderServiceImpl implements AdminOrderService {
    @Autowired
    private AdminOrderRepository adminOrderRepository;

    @Autowired
    private OrderSeatRepository orderSeatRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Override
    public List<Order> searchOrders(String status, String startDateStr, String endDateStr) {
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        if (startDateStr != null && !startDateStr.isEmpty()) {
            startDate = LocalDate.parse(startDateStr).atStartOfDay();
        }
        if (endDateStr != null && !endDateStr.isEmpty()) {
            endDate = LocalDate.parse(endDateStr).atTime(LocalTime.MAX);
        }

        return adminOrderRepository.findWithFilters(status, startDate, endDate);
    }

    @Override
    public Order getOrderDetails(Long orderId) {
        return adminOrderRepository.findById(orderId).orElse(null);
    }

    @Override
    @Transactional
    public Order refundOrder(Long orderId, Double refundAmount, String reason) {
        Order order = adminOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!"COMPLETED".equals(order.getStatus())) {
            throw new IllegalStateException("只有已完成的订单才能退款");
        }

        PaymentGatewayUtils.PayType payType;
        try {
            payType = PaymentGatewayUtils.PayType.valueOf(order.getPaymentMethod().toUpperCase());
        } catch (Exception e) {
            throw new IllegalStateException("不支持的支付方式，无法退款: " + order.getPaymentMethod());
        }

        PaymentGatewayUtils.refund(
                payType,
                order.getOrderNo(),
                refundAmount,
                reason);

        List<OrderSeat> orderSeats = orderSeatRepository.findByOrderId(order.getId());
        List<Long> seatIds = orderSeats.stream().map(OrderSeat::getSeatId).toList();
        List<Seat> seats = seatRepository.findAllById(seatIds);
        seats.forEach(seat -> seat.setStatus("AVAILABLE"));
        seatRepository.saveAll(seats);

        order.setStatus("REFUNDED");
        adminOrderRepository.save(order);

        return order;
    }
}