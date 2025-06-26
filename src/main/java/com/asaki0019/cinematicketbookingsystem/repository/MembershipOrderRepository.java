package com.asaki0019.cinematicketbookingsystem.repository;

import com.asaki0019.cinematicketbookingsystem.entities.MembershipOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipOrderRepository extends JpaRepository<MembershipOrder, Long> {
    MembershipOrder findByOrderNo(String orderNo);

    Page<MembershipOrder> findByUserId(Long userId, Pageable pageable);

    MembershipOrder findTopByUserIdAndMembershipTypeAndStatusOrderByPaymentTimeDesc(Long userId, String membershipType,
            String status);
}