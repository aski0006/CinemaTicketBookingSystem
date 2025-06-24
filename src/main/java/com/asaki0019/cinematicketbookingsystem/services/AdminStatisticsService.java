package com.asaki0019.cinematicketbookingsystem.services;

import java.util.List;
import java.util.Map;

public interface AdminStatisticsService {
    List<Map<String, Object>> getDailyStatistics(String startDate, String endDate);

    List<Map<String, Object>> getMovieStatistics();

    byte[] generateReport(String reportType, Map<String, Object> dateRange);
}