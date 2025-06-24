package com.asaki0019.cinematicketbookingsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HallInfoDTO {
    private Long id;
    private String name;
    private String type;
}