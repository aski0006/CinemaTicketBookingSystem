package com.asaki0019.cinematicketbookingsystem.dto;

import lombok.Data;

@Data
public class MembershipPurchaseResponseDTO {
    private Long orderId;
    private String orderNo;
    private String membershipType;
    private String duration;
    private Double totalAmount;
    private String status;
    private String paymentUrl;
}