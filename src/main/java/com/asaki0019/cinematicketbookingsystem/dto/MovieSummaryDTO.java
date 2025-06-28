package com.asaki0019.cinematicketbookingsystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieSummaryDTO {
    private Long id;
    private String title;
    @JsonProperty("poster_url")
    private String posterUrl;
    private Double rating;
    private String genres; // 类型字符串，如 "剧情 动作 惊悚"
    private String status; // 上映状态，如 SHOWING/UPCOMING
}