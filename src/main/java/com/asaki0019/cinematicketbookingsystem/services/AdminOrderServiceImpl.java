package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Order;
import com.asaki0019.cinematicketbookingsystem.repository.AdminOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AdminOrderServiceImpl implements AdminOrderService {
    @Autowired
    private AdminOrderRepository adminOrderRepository;

    @Override
    public List<Order> getOrderList(String status, LocalDate startDate, LocalDate endDate) {
        // 简单实现：不区分status和日期，实际可用Specification等动态查询
        return adminOrderRepository.findAll();
    }

    @Override
    public Order refundOrder(Long orderId, Double refundAmount, String reason) {
        Order order = adminOrderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setStatus("REFUNDED");
            // 可记录refundAmount和reason到日志或扩展字段
            adminOrderRepository.save(order);
        }
        return order;
    }
}