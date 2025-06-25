package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.dto.MembershipPurchaseRequestDTO;
import com.asaki0019.cinematicketbookingsystem.dto.MembershipPurchaseResponseDTO;
import com.asaki0019.cinematicketbookingsystem.dto.MembershipOrderQueryResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MembershipOrderService {
    MembershipPurchaseResponseDTO purchaseMembership(MembershipPurchaseRequestDTO requestDTO);

    void handlePaymentCallback(String orderNo, String status, String transactionId, double amount);

    Page<MembershipOrderQueryResponseDTO> queryMembershipOrders(Long userId, Pageable pageable);

    void requestRefund(Long orderId, Double refundAmount, String reason);

    void processRefund(Long orderId, boolean approve, String reason);
    // 可扩展：查询会员订单、退款等
}