package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.dto.ReviewResponseDTO;
import com.asaki0019.cinematicketbookingsystem.entities.Review;

import java.util.List;

public interface ReviewService {
    ReviewResponseDTO createReview(Review review);

    List<Review> getReviewsByMovieId(Long movieId);
}