package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.dto.ReviewResponseDTO;
import com.asaki0019.cinematicketbookingsystem.entities.Review;
import com.asaki0019.cinematicketbookingsystem.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Test
    void createReview() {
        Review reviewToSave = new Review();
        reviewToSave.setImages("url1,url2");

        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review saved = invocation.getArgument(0);
            saved.setId(1L); // Simulate saving and getting an ID
            return saved;
        });

        ReviewResponseDTO result = reviewService.createReview(reviewToSave);

        assertNotNull(result);
        assertNotNull(result.getCreateTime());
        assertEquals(2, result.getImages().size());
    }

    @Test
    void createReview_NoImages() {
        Review reviewToSave = new Review();
        reviewToSave.setImages(null);

        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ReviewResponseDTO result = reviewService.createReview(reviewToSave);

        assertNotNull(result);
        assertTrue(result.getImages().isEmpty());
    }

    @Test
    void getReviewsByMovieId() {
        when(reviewRepository.findByMovieId(1L)).thenReturn(Collections.singletonList(new Review()));
        List<Review> result = reviewService.getReviewsByMovieId(1L);
        assertFalse(result.isEmpty());
    }
}