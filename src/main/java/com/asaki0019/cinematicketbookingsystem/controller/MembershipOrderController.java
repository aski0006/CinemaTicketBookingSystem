package com.asaki0019.cinematicketbookingsystem.controller;

import com.asaki0019.cinematicketbookingsystem.dto.MembershipPurchaseRequestDTO;
import com.asaki0019.cinematicketbookingsystem.dto.MembershipPurchaseResponseDTO;
import com.asaki0019.cinematicketbookingsystem.dto.MembershipOrderQueryResponseDTO;
import com.asaki0019.cinematicketbookingsystem.services.MembershipOrderService;
import com.asaki0019.cinematicketbookingsystem.repository.MembershipOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Map;

@RestController
@RequestMapping("/api/memberships")
public class MembershipOrderController {
    @Autowired
    private MembershipOrderService membershipOrderService;

    @Autowired
    private MembershipOrderRepository membershipOrderRepository;

    @PostMapping("/purchase")
    public MembershipPurchaseResponseDTO purchaseMembership(@RequestHeader("Authorization") String token,
            @RequestBody MembershipPurchaseRequestDTO requestDTO) {
        if (token == null || !token.startsWith("Bearer ")
                || !com.asaki0019.cinematicketbookingsystem.utils.JwtTokenUtils.validateToken(token.substring(7))) {
            throw new RuntimeException("未登录或token已过期");
        }
        // 直接返回service生成的DTO，DTO中已包含支付URL
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

    @PostMapping("/upgrade-price")
    public Map<String, Object> getUpgradePrice(@RequestBody Map<String, String> req) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication
                .getPrincipal() instanceof com.asaki0019.cinematicketbookingsystem.entities.User user)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "未登录");
        }
        String targetType = req.get("targetType");
        String targetDuration = req.get("targetDuration");
        double targetPrice = membershipOrderService.calculateMembershipPrice(targetType, targetDuration);
        double currentValue = 0;
        int leftDays = 0;
        // 只处理VIP升级SVIP
        if (user.getMemberLevel() == 1 && "SVIP".equals(targetType)) {
            // 查找当前有效VIP订单
            com.asaki0019.cinematicketbookingsystem.entities.MembershipOrder vipOrder = membershipOrderRepository
                    .findTopByUserIdAndMembershipTypeAndStatusOrderByPaymentTimeDesc(user.getId(), "VIP", "COMPLETED");
            if (vipOrder != null && vipOrder.getPaymentTime() != null) {
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.time.LocalDateTime end = vipOrder.getPaymentTime();
                int totalDays = getTotalDays(vipOrder.getDuration());
                leftDays = (int) java.time.temporal.ChronoUnit.DAYS.between(now, end);
                leftDays = Math.max(leftDays, 0);
                double vipPrice = membershipOrderService.calculateMembershipPrice("VIP", vipOrder.getDuration());
                currentValue = vipPrice * leftDays / totalDays;
            }
        }
        double diff = Math.max(targetPrice - currentValue, 0);
        Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("upgradePrice", diff);
        resp.put("targetType", targetType);
        resp.put("targetDuration", targetDuration);
        resp.put("leftDays", leftDays);
        resp.put("deduct", currentValue);
        return resp;
    }

    private int getTotalDays(String duration) {
        switch (duration) {
            case "monthly":
                return 30;
            case "quarterly":
                return 90;
            case "yearly":
                return 365;
            default:
                return 30;
        }
    }
}