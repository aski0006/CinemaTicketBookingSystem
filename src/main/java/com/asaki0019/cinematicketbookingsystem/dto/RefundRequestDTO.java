package com.asaki0019.cinematicketbookingsystem.dto;

import lombok.Data;

@Data
public class RefundRequestDTO {
    private Double refundAmount;
    private String reason;
}