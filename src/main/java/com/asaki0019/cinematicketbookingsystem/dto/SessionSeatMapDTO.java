package com.asaki0019.cinematicketbookingsystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionSeatMapDTO {
    @JsonProperty("session_id")
    private Long sessionId;
    private List<SeatDTO> seats;
}