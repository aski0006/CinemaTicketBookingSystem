package com.asaki0019.cinematicketbookingsystem.adminServices;

import com.asaki0019.cinematicketbookingsystem.entities.Review;
import com.asaki0019.cinematicketbookingsystem.repository.AdminReviewRepository;
import com.asaki0019.cinematicketbookingsystem.services.AdminReviewServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AdminReviewServiceImplTest {
    @Mock
    private AdminReviewRepository adminReviewRepository;
    @InjectMocks
    private AdminReviewServiceImpl adminReviewService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetPendingReviews() {
        Review r1 = new Review();
        r1.setStatus("PENDING");
        Review r2 = new Review();
        r2.setStatus("APPROVED");
        when(adminReviewRepository.findAll()).thenReturn(Arrays.asList(r1, r2));
        List<Review> result = adminReviewService.getPendingReviews();
        assertEquals(1, result.size());
        assertEquals("PENDING", result.get(0).getStatus());
    }

    @Test
    void testApproveReview() {
        Review r = new Review();
        r.setStatus("PENDING");
        when(adminReviewRepository.findById(1L)).thenReturn(java.util.Optional.of(r));
        adminReviewService.approveReview(1L);
        assertEquals("APPROVED", r.getStatus());
        verify(adminReviewRepository).save(r);
    }

    @Test
    void testRejectReview() {
        Review r = new Review();
        r.setStatus("PENDING");
        when(adminReviewRepository.findById(2L)).thenReturn(java.util.Optional.of(r));
        adminReviewService.rejectReview(2L, "不合规");
        assertEquals("REJECTED", r.getStatus());
        verify(adminReviewRepository).save(r);
    }
}