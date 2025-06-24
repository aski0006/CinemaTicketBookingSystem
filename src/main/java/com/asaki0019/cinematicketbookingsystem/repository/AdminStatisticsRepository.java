package com.asaki0019.cinematicketbookingsystem.repository;

import com.asaki0019.cinematicketbookingsystem.entities.Statistics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminStatisticsRepository extends JpaRepository<Statistics, Long> {
}