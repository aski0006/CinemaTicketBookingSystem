package com.asaki0019.cinematicketbookingsystem.repository;

import com.asaki0019.cinematicketbookingsystem.entities.Statistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AdminStatisticsRepository extends JpaRepository<Statistics, Long> {
    List<Statistics> findByStatDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT s FROM Statistics s WHERE s.statDate BETWEEN :startDate AND :endDate")
    List<Statistics> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}