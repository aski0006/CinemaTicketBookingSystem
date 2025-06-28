package com.asaki0019.cinematicketbookingsystem.repository;

import com.asaki0019.cinematicketbookingsystem.entities.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByMovieId(Long movieId);

    List<Session> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    List<Session> findByMovieIdAndStartTimeBetween(Long movieId, LocalDateTime start, LocalDateTime end);

    List<Session> findByHallIdAndStartTimeBetween(Long hallId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT s FROM Session s WHERE (:movieId IS NULL OR s.movieId = :movieId) AND (:startDate IS NULL OR s.startTime >= :startDate) AND (:endDate IS NULL OR s.startTime < :endDate)")
    List<Session> findSessions(@Param("movieId") Long movieId, @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}