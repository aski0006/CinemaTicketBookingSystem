package com.asaki0019.cinematicketbookingsystem.controller;

import com.asaki0019.cinematicketbookingsystem.services.AdminReviewService;
import com.asaki0019.cinematicketbookingsystem.utils.JwtTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/reviews")
public class AdminReviewController {
    @Autowired
    private AdminReviewService adminReviewService;

    private boolean checkJwt(String token) {
        return token != null && JwtTokenUtils.validateToken(token);
    }

    @GetMapping
    public Object getPendingReviews(@RequestHeader("Authorization") String token) {
        if (!checkJwt(token)) {
            return Map.of("error", "未登录或token已过期");
        }
        return adminReviewService.getPendingReviews();
    }

    @PostMapping("/{reviewId}/approve")
    public Object approveReview(@RequestHeader("Authorization") String token,
            @PathVariable Long reviewId) {
        if (!checkJwt(token)) {
            return Map.of("error", "未登录或token已过期");
        }
        adminReviewService.approveReview(reviewId);
        return Map.of("success", true);
    }

    @PostMapping("/{reviewId}/reject")
    public Object rejectReview(@RequestHeader("Authorization") String token,
            @PathVariable Long reviewId,
            @RequestBody(required = false) Map<String, String> req) {
        if (!checkJwt(token)) {
            return Map.of("error", "未登录或token已过期");
        }
        String reason = req != null ? req.getOrDefault("reason", "") : "";
        adminReviewService.rejectReview(reviewId, reason);
        return Map.of("success", true);
    }
}