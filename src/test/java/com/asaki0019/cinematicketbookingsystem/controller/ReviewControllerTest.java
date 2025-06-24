package com.asaki0019.cinematicketbookingsystem.controller;

import com.asaki0019.cinematicketbookingsystem.entities.Review;
import com.asaki0019.cinematicketbookingsystem.repository.ReviewRepository;
import com.asaki0019.cinematicketbookingsystem.services.ReviewService;
import com.asaki0019.cinematicketbookingsystem.utils.JwtTokenUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
public class ReviewControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ReviewService reviewService;
    @MockBean
    private ReviewRepository reviewRepository;

    private MockedStatic<JwtTokenUtils> jwtTokenUtilsMock;
    private Review testReview;

    @BeforeEach
    void setUp() {
        // mock JWT校验
        jwtTokenUtilsMock = mockStatic(JwtTokenUtils.class);
        jwtTokenUtilsMock.when(() -> JwtTokenUtils.validateToken(any())).thenReturn(true);
        jwtTokenUtilsMock.when(() -> JwtTokenUtils.getUserFromToken(any())).thenReturn(
                new com.asaki0019.cinematicketbookingsystem.entities.User(
                        1L, "test", null, null, null, null, 0, "ACTIVE", null));
        // 构造测试评价
        testReview = new Review();
        testReview.setId(100L);
        testReview.setUserId(1L);
        testReview.setMovieId(1L);
        testReview.setRating(5);
        testReview.setContent("很好");
        testReview.setImages("[]");
        testReview.setCreateTime(LocalDateTime.now());
        when(reviewService.addReview(any(Review.class))).thenReturn(testReview);
    }

    @AfterEach
    void tearDown() {
        // 清理mock
        if (jwtTokenUtilsMock != null) {
            jwtTokenUtilsMock.close();
        }
    }

    @Test
    void testAddReview_Success() throws Exception {
        String json = "{" +
                "\"movieId\":1," +
                "\"rating\":5," +
                "\"content\":\"很好\", " +
                "\"images\":\"[]\"}";
        mockMvc.perform(post("/api/reviews")
                .header("Authorization", "Bearer testtoken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.create_time").exists())
                .andExpect(jsonPath("$.images").isArray());
    }

    @Test
    void testAddReview_Unauthorized() throws Exception {
        // mock token无效
        jwtTokenUtilsMock.when(() -> JwtTokenUtils.validateToken(any())).thenReturn(false);
        String json = "{" +
                "\"movieId\":1," +
                "\"rating\":5," +
                "\"content\":\"很好\", " +
                "\"images\":\"[]\"}";
        mockMvc.perform(post("/api/reviews")
                .header("Authorization", "Bearer badtoken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value("未登录或token已过期"));
    }
}