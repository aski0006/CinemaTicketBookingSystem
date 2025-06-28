package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Movie;
import com.asaki0019.cinematicketbookingsystem.dto.MovieSearchResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface MovieService {
    Movie getMovieById(Long movieId);

    MovieSearchResponseDTO searchMovies(String keyword, Pageable pageable);

    List<Map<String, Object>> getRecommendedMovies();

    List<Map<String, Object>> getRecommendedMovies(String preferredGenre);

    Movie createMovie(Movie movie);

    Movie updateMovie(Long movieId, Movie movie);

    void deleteMovie(Long movieId);

    Page<Movie> getTodayMovies(Pageable pageable);

    Page<Movie> getMoviePage(String keyword, Pageable pageable);
}