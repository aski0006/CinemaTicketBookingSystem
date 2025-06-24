package com.asaki0019.cinematicketbookingsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatMapResponse {
    private Long sessionId;
    private List<SeatRow> rows;
}