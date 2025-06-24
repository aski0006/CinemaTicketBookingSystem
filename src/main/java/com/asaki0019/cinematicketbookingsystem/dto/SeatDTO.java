package com.asaki0019.cinematicketbookingsystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatDTO {
    private Long id;
    @JsonProperty("row_no")
    private String rowNo;
    @JsonProperty("col_no")
    private Integer colNo;
    private String type;
    @JsonProperty("price_factor")
    private Double priceFactor;
    private String status;
}