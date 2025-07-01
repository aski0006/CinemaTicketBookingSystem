package com.asaki0019.cinematicketbookingsystem.controller;

import com.asaki0019.cinematicketbookingsystem.entities.Movie;
import com.asaki0019.cinematicketbookingsystem.services.MovieService;
import com.asaki0019.cinematicketbookingsystem.utils.RedisCacheUtils;
import com.asaki0019.cinematicketbookingsystem.dto.MovieSearchResponseDTO;
import com.asaki0019.cinematicketbookingsystem.dto.AdminMoviesManagerResponseDTO;
import com.asaki0019.cinematicketbookingsystem.utils.LogSystem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin/movies")
public class AdminMovieController {

    @Autowired
    private MovieService movieService;

    private static final String TODAY_MOVIES_KEY = "today_movies";
    private static final String RECOMMENDATION_POPULAR_KEY = "movie:recommendations:popular";

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Movie createMovie(@RequestBody Movie movie) {
        return movieService.createMovie(movie);
    }

    @PutMapping("/{movieId}")
    public Movie updateMovie(@PathVariable Long movieId, @RequestBody Movie movie) {
        return movieService.updateMovie(movieId, movie);
    }

    @DeleteMapping("/{movieId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMovie(@PathVariable Long movieId) {
        movieService.deleteMovie(movieId);
    }

    @PostMapping("/today")
    public Map<String, Object> setTodayMovies(@RequestBody List<Long> movieIds) {
        if (movieIds == null || movieIds.isEmpty()) {
            return Collections.singletonMap("error", "未选择任何电影");
        }
        List<Movie> movies = new ArrayList<>();
        List<Long> invalidIds = new ArrayList<>();
        for (Long id : movieIds) {
            Movie m = movieService.getMovieById(id);
            if (m == null || !"SHOWING".equalsIgnoreCase(m.getStatus())) {
                invalidIds.add(id);
            } else {
                movies.add(m);
            }
        }
        if (!invalidIds.isEmpty()) {
            return Map.of(
                    "error", "仅允许设置状态为'热映中'的电影为今日播映，非法ID:",
                    "invalidIds", invalidIds);
        }
        List<Long> validIds = movies.stream().map(Movie::getId).toList();
        RedisCacheUtils.set(TODAY_MOVIES_KEY, validIds.toString());
        return Collections.singletonMap("msg", "ok");
    }

    @GetMapping("/today")
    public List<Long> getTodayMovies() {
        String val = RedisCacheUtils.get(TODAY_MOVIES_KEY);
        if (val == null || val.isEmpty())
            return new ArrayList<>();
        val = val.replaceAll("[\\[\\] ]", "");
        if (val.isEmpty())
            return new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (String s : val.split(",")) {
            try {
                ids.add(Long.parseLong(s));
            } catch (Exception ignored) {
            }
        }
        return ids;
    }

    @DeleteMapping("/today/{movieId}")
    public Map<String, Object> removeTodayMovie(@PathVariable Long movieId) {
        String val = RedisCacheUtils.get(TODAY_MOVIES_KEY);
        if (val == null || val.isEmpty()) {
            return Collections.singletonMap("error", "今日无影片");
        }
        val = val.replaceAll("[\\[\\] ]", "");
        List<Long> ids = new ArrayList<>();
        for (String s : val.split(",")) {
            try {
                ids.add(Long.parseLong(s));
            } catch (Exception ignored) {
            }
        }
        boolean removed = ids.remove(movieId);
        RedisCacheUtils.set(TODAY_MOVIES_KEY, ids.toString());
        if (removed) {
            return Collections.singletonMap("msg", "已移除");
        } else {
            return Collections.singletonMap("error", "未找到该影片");
        }
    }

    @GetMapping("")
    public Map<String, Object> getMovies(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Movie> moviePage = movieService.getMoviePage(keyword, pageable);
        List<AdminMoviesManagerResponseDTO> list = moviePage.getContent().stream()
                .map(m -> new AdminMoviesManagerResponseDTO(
                        m.getId(),
                        m.getTitle(),
                        m.getDirector(),
                        m.getActors(),
                        m.getDuration(),
                        m.getReleaseDate() != null ? m.getReleaseDate().toString() : null,
                        m.getStatus(),
                        m.getPosterUrl(),
                        m.getDescription()))
                .toList();
        Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("movies", list);
        resp.put("total", moviePage.getTotalElements());
        return resp;
    }

    /**
     * 获取影片热度排行（点击量）
     * /admin/movies/hot
     */
    @GetMapping("/hot")
    public List<Map<String, Object>> getHotMovies() {
        List<String> movieIds = RedisCacheUtils.zrevrange(RECOMMENDATION_POPULAR_KEY, 0, 9); // 取前10
        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (String movieIdStr : movieIds) {
            try {
                Long id = Long.valueOf(movieIdStr);
                Movie m = movieService.getMovieById(id);
                double views = RedisCacheUtils.zscore(RECOMMENDATION_POPULAR_KEY, String.valueOf(id));
                Map<String, Object> map = new HashMap<>();
                map.put("id", id);
                map.put("title", m != null ? m.getTitle() : "未知");
                map.put("views", (long) views);
                map.put("rank", rank++);
                result.add(map);
            } catch (Exception e) {
                // 查不到电影，自动从热度榜移除
                RedisCacheUtils.zrem(RECOMMENDATION_POPULAR_KEY, movieIdStr);
                LogSystem.error("热度排行解析失败并已移除:" + movieIdStr);
            }
        }
        return result;
    }
}