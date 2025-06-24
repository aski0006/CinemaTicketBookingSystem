package com.asaki0019.cinematicketbookingsystem.repository;

import com.asaki0019.cinematicketbookingsystem.entities.OrderSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderSeatRepository extends JpaRepository<OrderSeat, Long> {
    List<OrderSeat> findByOrderId(Long orderId);

    List<OrderSeat> findByOrderIdIn(List<Long> orderIds);
}