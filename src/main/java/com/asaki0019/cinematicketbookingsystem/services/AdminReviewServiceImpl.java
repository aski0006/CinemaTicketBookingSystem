package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Review;
import com.asaki0019.cinematicketbookingsystem.repository.AdminReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminReviewServiceImpl implements AdminReviewService {

    @Autowired
    private AdminReviewRepository adminReviewRepository;

    @Override
    public List<Review> getPendingReviews() {
        // 只返回待审核的影评
        return adminReviewRepository.findAll().stream()
                .filter(r -> r.getStatus() == null || "PENDING".equals(r.getStatus()))
                .toList();
    }

    @Override
    public void approveReview(Long reviewId) {
        Review review = adminReviewRepository.findById(reviewId).orElse(null);
        if (review != null) {
            review.setStatus("APPROVED");
            adminReviewRepository.save(review);
        }
    }

    @Override
    public void rejectReview(Long reviewId, String reason) {
        Review review = adminReviewRepository.findById(reviewId).orElse(null);
        if (review != null) {
            review.setStatus("REJECTED");
            // 可记录reason到扩展字段或日志
            adminReviewRepository.save(review);
        }
    }
}