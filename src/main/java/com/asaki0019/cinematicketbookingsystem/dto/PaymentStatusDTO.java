package com.asaki0019.cinematicketbookingsystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusDTO {
    @JsonProperty("order_no")
    private String orderNo;
    private String status;
    @JsonProperty("last_update")
    private LocalDateTime lastUpdate;
}