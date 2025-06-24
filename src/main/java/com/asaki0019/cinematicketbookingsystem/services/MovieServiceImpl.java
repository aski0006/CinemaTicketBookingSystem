package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Movie;
import com.asaki0019.cinematicketbookingsystem.repository.MovieRepository;
import com.asaki0019.cinematicketbookingsystem.utils.LogSystem;
import com.asaki0019.cinematicketbookingsystem.utils.RedisCacheUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MovieServiceImpl implements MovieService {

    @Autowired
    private MovieRepository movieRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final String MOVIE_CACHE_KEY_PREFIX = "movie:details:";
    private static final String RECOMMENDATION_POPULAR_KEY = "movie:recommendations:popular";
    private static final long CACHE_EXPIRATION_SECONDS = 2 * 60 * 60; // 2 hours

    @Override
    public Movie getMovieById(Long movieId) {
        String cacheKey = MOVIE_CACHE_KEY_PREFIX + movieId;

        // 1. Try to get from cache
        try {
            String cachedMovieJson = RedisCacheUtils.get(cacheKey);
            if (cachedMovieJson != null) {
                // Cache hit, deserialize and return
                RedisCacheUtils.zincrby(RECOMMENDATION_POPULAR_KEY, 1, String.valueOf(movieId));
                return objectMapper.readValue(cachedMovieJson, Movie.class);
            }
        } catch (IOException e) {
            // Log error but proceed to fetch from DB
        }

        // 2. Get from DB if not in cache
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found"));

        // 3. Put into cache
        try {
            String movieJson = objectMapper.writeValueAsString(movie);
            RedisCacheUtils.set(cacheKey, movieJson, CACHE_EXPIRATION_SECONDS);
            RedisCacheUtils.zincrby(RECOMMENDATION_POPULAR_KEY, 1, String.valueOf(movieId));
        } catch (JsonProcessingException e) {
            LogSystem.error(cacheKey);
        }

        return movie;
    }

    @Override
    public Page<Movie> searchMovies(String keyword, Pageable pageable) {
        if (keyword == null) {
            keyword = "";
        }
        return movieRepository.findByTitleContaining(keyword, pageable);
    }

    @Override
    public List<Map<String, Object>> getRecommendedMovies() {
        // Get top 10 popular movie IDs from Redis sorted set
        List<String> movieIds = RedisCacheUtils.zrevrange(RECOMMENDATION_POPULAR_KEY, 0, 9);
        if (movieIds == null || movieIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> ids = movieIds.stream().map(Long::valueOf).collect(Collectors.toList());
        // 保证顺序和存在性
        return ids.stream()
                .map(id -> movieRepository.findById(id).orElse(null))
                .filter(movie -> movie != null)
                .map(movie -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", movie.getId());
                    map.put("title", movie.getTitle());
                    map.put("poster_url", movie.getPosterUrl());
                    map.put("match_score", 1.0);
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Movie createMovie(Movie movie) {
        // To be implemented for admin
        return movieRepository.save(movie);
    }

    @Override
    public Movie updateMovie(Long movieId, Movie movieDetails) {
        // To be implemented for admin
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found"));

        movie.setTitle(movieDetails.getTitle());
        movie.setDirector(movieDetails.getDirector());
        movie.setActors(movieDetails.getActors());
        movie.setDuration(movieDetails.getDuration());
        movie.setReleaseDate(movieDetails.getReleaseDate());
        movie.setDescription(movieDetails.getDescription());
        movie.setPosterUrl(movieDetails.getPosterUrl());
        movie.setTrailerUrl(movieDetails.getTrailerUrl());
        movie.setRating(movieDetails.getRating());
        movie.setStatus(movieDetails.getStatus());

        return movieRepository.save(movie);
    }

    @Override
    public Movie delistMovie(Long movieId) {
        // To be implemented for admin
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found"));
        movie.setStatus("OFF");
        return movieRepository.save(movie);
    }
}