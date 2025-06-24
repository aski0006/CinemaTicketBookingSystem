package com.asaki0019.cinematicketbookingsystem.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PaymentCallbackRequest {
    @JsonProperty("order_no")
    private String orderNo;

    @JsonProperty("payment_status")
    private String paymentStatus;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("amount")
    private Double amount;
}