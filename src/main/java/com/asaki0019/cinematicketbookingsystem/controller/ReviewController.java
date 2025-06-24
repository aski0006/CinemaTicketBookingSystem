package com.asaki0019.cinematicketbookingsystem.controller;

import com.asaki0019.cinematicketbookingsystem.entities.Review;
import com.asaki0019.cinematicketbookingsystem.services.ReviewService;
import com.asaki0019.cinematicketbookingsystem.utils.JwtTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    @Autowired
    private ReviewService reviewService;

    /**
     * 订单评价（电影评价）
     * 需要登录校验
     */
    @PostMapping
    public Map<String, Object> addReview(@RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Review review) {
        Map<String, Object> resp = new HashMap<>();
        if (token == null || !JwtTokenUtils.validateToken(token)) {
            resp.put("error", "未登录或token已过期");
            return resp;
        }
        // 设置评价时间
        review.setCreateTime(LocalDateTime.now());
        // 从token中获取userId
        review.setUserId(JwtTokenUtils.getUserFromToken(token).getId());
        Review saved = reviewService.addReview(review);
        resp.put("id", saved.getId());
        resp.put("create_time", saved.getCreateTime());
        resp.put("images", saved.getImages());
        return resp;
    }
}