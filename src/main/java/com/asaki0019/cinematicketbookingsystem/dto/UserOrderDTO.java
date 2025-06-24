package com.asaki0019.cinematicketbookingsystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOrderDTO {
    private Long id;
    @JsonProperty("order_no")
    private String orderNo;
    @JsonProperty("movie_title")
    private String movieTitle;
    @JsonProperty("session_time")
    private LocalDateTime sessionTime;
    @JsonProperty("seat_count")
    private int seatCount;
    @JsonProperty("total_amount")
    private Double totalAmount;
    private String status;
}