package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Review;
import java.util.List;

public interface AdminReviewService {
    List<Review> getPendingReviews();

    void approveReview(Long reviewId);

    void rejectReview(Long reviewId, String reason);
}