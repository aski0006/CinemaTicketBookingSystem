package com.asaki0019.cinematicketbookingsystem.controller;

import com.asaki0019.cinematicketbookingsystem.dto.MovieSearchResponseDTO;
import com.asaki0019.cinematicketbookingsystem.entities.Movie;
import com.asaki0019.cinematicketbookingsystem.services.MovieService;
import com.asaki0019.cinematicketbookingsystem.dto.MovieSummaryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import com.asaki0019.cinematicketbookingsystem.utils.RedisCacheUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    @Autowired
    private MovieService movieService;

    @GetMapping("/{movieId}")
    public Movie getMovieById(@PathVariable Long movieId) {
        return movieService.getMovieById(movieId);
    }

    @GetMapping("/search")
    public MovieSearchResponseDTO searchMovies(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return movieService.searchMovies(keyword, pageable);
    }

    @GetMapping("/recommendations")
    public List<Map<String, Object>> getRecommendations() {
        return movieService.getRecommendedMovies();
    }

    @GetMapping("/today")
    public MovieSearchResponseDTO getTodayMovies(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        String val = RedisCacheUtils.get("today_movies");
        List<Long> ids = new ArrayList<>();
        if (val != null && !val.isEmpty()) {
            val = val.replaceAll("[\\[\\] ]", "");
            for (String s : val.split(",")) {
                try {
                    ids.add(Long.parseLong(s));
                } catch (Exception ignored) {
                }
            }
        }
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(ids.size(), from + size);
        List<MovieSummaryDTO> movieSummaries = new ArrayList<>();
        if (from < to) {
            for (Long id : ids.subList(from, to)) {
                try {
                    Movie m = movieService.getMovieById(id);
                    if (m != null) {
                        movieSummaries.add(new MovieSummaryDTO(m.getId(), m.getTitle(), m.getPosterUrl(), m.getRating(),
                                m.getGenre(), m.getStatus()));
                    }
                } catch (Exception e) {
                    // 跳过不存在的电影
                }
            }
        }
        return new MovieSearchResponseDTO(ids.size(), movieSummaries, null);
    }
}