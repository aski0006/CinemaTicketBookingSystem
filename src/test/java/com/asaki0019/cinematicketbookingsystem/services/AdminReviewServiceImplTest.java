package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Review;
import com.asaki0019.cinematicketbookingsystem.repository.AdminReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminReviewServiceImplTest {

    @Mock
    private AdminReviewRepository adminReviewRepository;

    @InjectMocks
    private AdminReviewServiceImpl adminReviewService;

    @Test
    void getPendingReviews() {
        Review pendingReview = new Review();
        pendingReview.setStatus("PENDING");
        Review nullStatusReview = new Review();
        nullStatusReview.setStatus(null);
        Review approvedReview = new Review();
        approvedReview.setStatus("APPROVED");

        when(adminReviewRepository.findAll())
                .thenReturn(Arrays.asList(pendingReview, nullStatusReview, approvedReview));

        List<Review> result = adminReviewService.getPendingReviews();

        assertEquals(2, result.size());
        assertTrue(result.contains(pendingReview));
        assertTrue(result.contains(nullStatusReview));
    }

    @Test
    void approveReview() {
        Review review = new Review();
        review.setStatus("PENDING");
        when(adminReviewRepository.findById(1L)).thenReturn(Optional.of(review));

        adminReviewService.approveReview(1L);

        assertEquals("APPROVED", review.getStatus());
        verify(adminReviewRepository).save(review);
    }

    @Test
    void approveReview_NotFound() {
        when(adminReviewRepository.findById(1L)).thenReturn(Optional.empty());
        adminReviewService.approveReview(1L);
        verify(adminReviewRepository, never()).save(any());
    }

    @Test
    void rejectReview() {
        Review review = new Review();
        review.setStatus("PENDING");
        when(adminReviewRepository.findById(1L)).thenReturn(Optional.of(review));

        adminReviewService.rejectReview(1L, "Inappropriate content");

        assertEquals("REJECTED", review.getStatus());
        verify(adminReviewRepository).save(review);
    }

    @Test
    void rejectReview_NotFound() {
        when(adminReviewRepository.findById(1L)).thenReturn(Optional.empty());
        adminReviewService.rejectReview(1L, "Inappropriate content");
        verify(adminReviewRepository, never()).save(any());
    }
}