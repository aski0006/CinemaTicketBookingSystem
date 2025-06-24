package com.asaki0019.cinematicketbookingsystem.userServices;

import com.asaki0019.cinematicketbookingsystem.entities.Movie;
import com.asaki0019.cinematicketbookingsystem.repository.MovieRepository;
import com.asaki0019.cinematicketbookingsystem.services.MovieService;
import com.asaki0019.cinematicketbookingsystem.utils.RedisCacheUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class MovieServiceImplTest {

    @Autowired
    private MovieService movieService;

    @Autowired
    private MovieRepository movieRepository;

    private Movie movie1;
    private static final String RECOMMENDATION_POPULAR_KEY = "movie:recommendations:popular";

    @BeforeEach
    void setUp() {
        movie1 = new Movie();
        movie1.setTitle("Test Movie One");
        movie1.setDirector("Test Director");
        movie1.setReleaseDate(LocalDate.now());
        movie1.setStatus("SHOWING");
        movie1.setDuration(120);
        movie1.setActors("主演A,主演B");
        movie1.setDescription("测试电影简介");
        movie1.setPosterUrl("http://example.com/poster.jpg");
        movie1.setTrailerUrl("http://example.com/trailer.mp4");
        movie1.setRating(8.5);
        movie1 = movieService.createMovie(movie1);

        // Clean up recommendation key before each test
        RedisCacheUtils.del(RECOMMENDATION_POPULAR_KEY);
    }

    @Test
    void testCreateAndGetMovie() {
        Movie foundMovie = movieService.getMovieById(movie1.getId());
        assertNotNull(foundMovie);
        assertEquals("Test Movie One", foundMovie.getTitle());
    }

    @Test
    void testSearchMovies() {
        Page<Movie> results = movieService.searchMovies("One", PageRequest.of(0, 5));
        assertFalse(results.isEmpty());
        assertEquals(1, results.getTotalElements());
        assertEquals("Test Movie One", results.getContent().get(0).getTitle());
    }

    @Test
    void testGetRecommendedMovies() {
        // Simulate movie views
        movieService.getMovieById(movie1.getId());
        movieService.getMovieById(movie1.getId());

        List<Map<String, Object>> recommendations = movieService.getRecommendedMovies();
        assertFalse(recommendations.isEmpty());
        assertEquals(movie1.getId(), recommendations.get(0).get("id"));
    }

    @Test
    void testUpdateMovie() {
        Movie movieDetails = new Movie();
        movieDetails.setTitle("Updated Test Movie");
        movieDetails.setDirector("Updated Director");
        movieDetails.setActors("主演A,主演B");
        movieDetails.setDuration(130);
        movieDetails.setReleaseDate(movie1.getReleaseDate());
        movieDetails.setDescription("更新后的简介");
        movieDetails.setPosterUrl("http://example.com/updated_poster.jpg");
        movieDetails.setTrailerUrl("http://example.com/updated_trailer.mp4");
        movieDetails.setRating(9.0);
        movieDetails.setStatus("SHOWING");
        movieService.updateMovie(movie1.getId(), movieDetails);

        Movie updatedMovie = movieService.getMovieById(movie1.getId());
        assertEquals("Updated Test Movie", updatedMovie.getTitle());
    }

    @Test
    void testDelistMovie() {
        movieService.delistMovie(movie1.getId());
        Movie delistedMovie = movieService.getMovieById(movie1.getId());
        assertEquals("OFF", delistedMovie.getStatus());
    }

    @AfterEach
    void tearDown() {
        movieRepository.deleteAllInBatch();
        RedisCacheUtils.del(RECOMMENDATION_POPULAR_KEY);
    }
}