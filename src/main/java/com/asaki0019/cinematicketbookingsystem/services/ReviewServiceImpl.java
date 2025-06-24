package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Review;
import com.asaki0019.cinematicketbookingsystem.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReviewServiceImpl implements ReviewService {
    @Autowired
    private ReviewRepository reviewRepository;

    @Override
    public Review addReview(Review review) {
        if (!com.asaki0019.cinematicketbookingsystem.utils.ValidationUtils.validateReview(review)) {
            throw new IllegalArgumentException("影评参数不合法");
        }
        return reviewRepository.save(review);
    }
}