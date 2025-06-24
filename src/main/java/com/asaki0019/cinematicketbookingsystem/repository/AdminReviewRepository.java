package com.asaki0019.cinematicketbookingsystem.repository;

import com.asaki0019.cinematicketbookingsystem.entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminReviewRepository extends JpaRepository<Review, Long> {
}