package com.asaki0019.cinematicketbookingsystem.entities;

import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {
    private Long userId;
    private Long sessionId;
    private List<Long> seatIds;
    private String paymentMethod;
    private List<List<Integer>> seatPositions;
}