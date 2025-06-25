package com.asaki0019.cinematicketbookingsystem.controller;

import com.asaki0019.cinematicketbookingsystem.dto.MembershipPurchaseRequestDTO;
import com.asaki0019.cinematicketbookingsystem.dto.MembershipPurchaseResponseDTO;
import com.asaki0019.cinematicketbookingsystem.dto.MembershipOrderQueryResponseDTO;
import com.asaki0019.cinematicketbookingsystem.services.MembershipOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/memberships")
public class MembershipOrderController {
    @Autowired
    private MembershipOrderService membershipOrderService;

    @PostMapping("/purchase")
    public MembershipPurchaseResponseDTO purchaseMembership(@RequestBody MembershipPurchaseRequestDTO requestDTO) {
        return membershipOrderService.purchaseMembership(requestDTO);
    }

    @GetMapping("/orders")
    public Page<MembershipOrderQueryResponseDTO> queryOrders(@RequestParam Long userId, Pageable pageable) {
        return membershipOrderService.queryMembershipOrders(userId, pageable);
    }

    @PostMapping("/refund/{orderId}")
    public void requestRefund(@PathVariable Long orderId, @RequestParam Double refundAmount,
            @RequestParam(required = false) String reason) {
        membershipOrderService.requestRefund(orderId, refundAmount, reason);
    }

    @PostMapping("/refund/process/{orderId}")
    public void processRefund(@PathVariable Long orderId, @RequestParam boolean approve,
            @RequestParam(required = false) String reason) {
        membershipOrderService.processRefund(orderId, approve, reason);
    }
}