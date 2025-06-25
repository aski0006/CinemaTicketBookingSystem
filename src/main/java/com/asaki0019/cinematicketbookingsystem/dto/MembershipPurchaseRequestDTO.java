package com.asaki0019.cinematicketbookingsystem.dto;

import lombok.Data;

@Data
public class MembershipPurchaseRequestDTO {
    private Long userId;
    private String membershipType; // VIP, SVIP
    private String duration; // monthly, quarterly, yearly
    private String paymentMethod; // ALIPAY, WECHAT, CREDIT_CARD
}