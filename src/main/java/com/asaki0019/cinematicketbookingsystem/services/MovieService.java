package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface MovieService {
    Movie getMovieById(Long movieId);

    Page<Movie> searchMovies(String keyword, Pageable pageable);

    List<Map<String, Object>> getRecommendedMovies();

    Movie createMovie(Movie movie);

    Movie updateMovie(Long movieId, Movie movie);

    Movie delistMovie(Long movieId);
}