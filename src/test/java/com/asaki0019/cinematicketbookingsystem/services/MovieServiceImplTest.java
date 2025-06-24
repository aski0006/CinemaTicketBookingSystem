package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.dto.MovieSearchResponseDTO;
import com.asaki0019.cinematicketbookingsystem.entities.Movie;
import com.asaki0019.cinematicketbookingsystem.repository.MovieRepository;
import com.asaki0019.cinematicketbookingsystem.utils.LogSystem;
import com.asaki0019.cinematicketbookingsystem.utils.RedisCacheUtils;
import com.asaki0019.cinematicketbookingsystem.utils.ValidationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceImplTest {

    @Mock
    private MovieRepository movieRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private MovieServiceImpl movieService;

    private MockedStatic<RedisCacheUtils> redisCacheUtilsMock;
    private MockedStatic<ValidationUtils> validationUtilsMock;
    private MockedStatic<LogSystem> logSystemMock;

    @BeforeEach
    void setUp() {
        redisCacheUtilsMock = mockStatic(RedisCacheUtils.class);
        validationUtilsMock = mockStatic(ValidationUtils.class);
        logSystemMock = mockStatic(LogSystem.class);
    }

    @AfterEach
    void tearDown() {
        redisCacheUtilsMock.close();
        validationUtilsMock.close();
        logSystemMock.close();
    }

    private Movie createValidMovie() {
        Movie movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Test Movie");
        movie.setReleaseDate(LocalDate.now());
        movie.setStatus("SHOWING");
        movie.setDuration(120);
        return movie;
    }

    @Test
    void getMovieById_CacheHit() throws Exception {
        Movie movie = createValidMovie();
        String movieJson = objectMapper.writeValueAsString(movie);
        redisCacheUtilsMock.when(() -> RedisCacheUtils.get(anyString())).thenReturn(movieJson);

        Movie result = movieService.getMovieById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(movieRepository, never()).findById(anyLong());
        redisCacheUtilsMock.verify(() -> RedisCacheUtils.zincrby(anyString(), eq(1.0), eq("1")));
    }

    @Test
    void getMovieById_CacheMiss() {
        Movie movie = createValidMovie();
        redisCacheUtilsMock.when(() -> RedisCacheUtils.get(anyString())).thenReturn(null);
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));

        Movie result = movieService.getMovieById(1L);

        assertNotNull(result);
        verify(movieRepository).findById(1L);
        redisCacheUtilsMock.verify(() -> RedisCacheUtils.set(anyString(), anyString(), anyLong()));
    }

    @Test
    void getMovieById_NotFound() {
        redisCacheUtilsMock.when(() -> RedisCacheUtils.get(anyString())).thenReturn(null);
        when(movieRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> movieService.getMovieById(1L));
    }

    @Test
    void searchMovies() {
        Page<Movie> moviePage = new PageImpl<>(Collections.singletonList(createValidMovie()));
        when(movieRepository.findByTitleContaining(anyString(), any(Pageable.class))).thenReturn(moviePage);

        MovieSearchResponseDTO result = movieService.searchMovies("Test", Pageable.unpaged());

        assertEquals(1, result.getTotal());
        assertFalse(result.getMovies().isEmpty());
    }

    @Test
    void getRecommendedMovies() {
        redisCacheUtilsMock.when(() -> RedisCacheUtils.zrevrange(anyString(), eq(0L), eq(9L)))
                .thenReturn(List.of("1"));
        when(movieRepository.findById(1L)).thenReturn(Optional.of(createValidMovie()));

        List<Map<String, Object>> result = movieService.getRecommendedMovies();

        assertFalse(result.isEmpty());
        assertEquals(1L, result.get(0).get("id"));
    }

    @Test
    void createMovie() {
        Movie movie = createValidMovie();
        validationUtilsMock.when(() -> ValidationUtils.validateMovie(movie)).thenReturn(true);
        when(movieRepository.save(movie)).thenReturn(movie);

        Movie result = movieService.createMovie(movie);
        assertNotNull(result);
        verify(movieRepository).save(movie);
    }

    @Test
    void createMovie_Invalid() {
        Movie movie = new Movie();
        validationUtilsMock.when(() -> ValidationUtils.validateMovie(movie)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> movieService.createMovie(movie));
    }

    @Test
    void updateMovie() {
        Movie movie = createValidMovie();
        Movie movieDetails = new Movie();
        movieDetails.setTitle("Updated Title");
        movieDetails.setReleaseDate(LocalDate.now());
        movieDetails.setDuration(130);
        movieDetails.setStatus("SHOWING");

        validationUtilsMock.when(() -> ValidationUtils.validateMovie(movieDetails)).thenReturn(true);
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(movieRepository.save(any(Movie.class))).thenAnswer(i -> i.getArguments()[0]);

        Movie result = movieService.updateMovie(1L, movieDetails);
        assertEquals("Updated Title", result.getTitle());
    }
}