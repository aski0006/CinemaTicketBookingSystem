package com.asaki0019.cinematicketbookingsystem.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MembershipOrderQueryResponseDTO {
    private Long orderId;
    private String orderNo;
    private String membershipType;
    private String duration;
    private Double totalAmount;
    private String status;
    private String paymentUrl;
    private LocalDateTime createTime;
    private LocalDateTime paymentTime;
    private Double refundAmount;
    private String refundStatus;
    private LocalDateTime refundTime;
    private Long id;
    private Long userId;
    private Double amount;
}