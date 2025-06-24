package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.dto.ReviewResponseDTO;
import com.asaki0019.cinematicketbookingsystem.entities.Review;
import com.asaki0019.cinematicketbookingsystem.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class ReviewServiceImpl implements ReviewService {
    @Autowired
    private ReviewRepository reviewRepository;

    @Override
    public ReviewResponseDTO createReview(Review review) {
        review.setCreateTime(LocalDateTime.now());
        Review savedReview = reviewRepository.save(review);
        // 假设图片在实体中以逗号分隔的字符串形式存储
        List<String> imageList = (savedReview.getImages() != null && !savedReview.getImages().isEmpty())
                ? Arrays.asList(savedReview.getImages().split(","))
                : List.of();
        return new ReviewResponseDTO(savedReview.getId(), savedReview.getCreateTime(), imageList);
    }

    @Override
    public List<Review> getReviewsByMovieId(Long movieId) {
        return reviewRepository.findByMovieId(movieId);
    }
}