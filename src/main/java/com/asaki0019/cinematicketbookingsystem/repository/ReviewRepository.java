package com.asaki0019.cinematicketbookingsystem.repository;

import com.asaki0019.cinematicketbookingsystem.entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByMovieId(Long movieId);
}