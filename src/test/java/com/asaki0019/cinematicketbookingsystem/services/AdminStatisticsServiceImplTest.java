package com.asaki0019.cinematicketbookingsystem.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AdminStatisticsServiceImplTest {

    @InjectMocks
    private AdminStatisticsServiceImpl adminStatisticsService;

    @Test
    void getDailyStatistics() {
        List<Map<String, Object>> result = adminStatisticsService.getDailyStatistics("2023-01-01", "2023-01-31");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getDailyStatistics_InvalidDate() {
        assertThrows(IllegalArgumentException.class,
                () -> adminStatisticsService.getDailyStatistics(null, "2023-01-31"));
        assertThrows(IllegalArgumentException.class,
                () -> adminStatisticsService.getDailyStatistics("2023-01-01", null));
        assertThrows(IllegalArgumentException.class, () -> adminStatisticsService.getDailyStatistics("", "2023-01-31"));
        assertThrows(IllegalArgumentException.class, () -> adminStatisticsService.getDailyStatistics("2023-01-01", ""));
    }

    @Test
    void getMovieStatistics() {
        List<Map<String, Object>> result = adminStatisticsService.getMovieStatistics();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void generateReport() {
        Map<String, Object> dateRange = new HashMap<>();
        dateRange.put("start", "2023-01-01");
        dateRange.put("end", "2023-01-31");
        byte[] report = adminStatisticsService.generateReport("PDF", dateRange);
        assertNotNull(report);
        assertTrue(report.length > 0);
        assertTrue(new String(report).contains("PDF"));
    }

    @Test
    void generateReport_InvalidParams() {
        assertThrows(IllegalArgumentException.class,
                () -> adminStatisticsService.generateReport(null, Collections.emptyMap()));
        assertThrows(IllegalArgumentException.class,
                () -> adminStatisticsService.generateReport("", Collections.emptyMap()));
        assertThrows(IllegalArgumentException.class, () -> adminStatisticsService.generateReport("PDF", null));
    }
}