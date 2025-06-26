package com.asaki0019.cinematicketbookingsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieSearchResponseDTO {
    private long total;
    private List<MovieSummaryDTO> movies;
    private List<String> genres;
}