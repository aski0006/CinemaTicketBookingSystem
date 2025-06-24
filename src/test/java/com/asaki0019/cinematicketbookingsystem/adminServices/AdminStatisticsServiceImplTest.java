package com.asaki0019.cinematicketbookingsystem.adminServices;

import org.junit.jupiter.api.Test;

import com.asaki0019.cinematicketbookingsystem.services.AdminStatisticsServiceImpl;

import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class AdminStatisticsServiceImplTest {
    private final AdminStatisticsServiceImpl service = new AdminStatisticsServiceImpl();

    @Test
    void testGenerateReport() {
        Map<String, Object> dateRange = new HashMap<>();
        dateRange.put("start", "2024-01-01");
        dateRange.put("end", "2024-01-31");
        byte[] result = service.generateReport("PDF", dateRange);
        assertNotNull(result);
        assertTrue(new String(result).contains("PDF"));
    }

    @Test
    void testGenerateReportInvalid() {
        assertThrows(IllegalArgumentException.class, () -> service.generateReport(null, null));
    }

    @Test
    void testGetDailyStatisticsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> service.getDailyStatistics(null, "2024-01-01"));
    }
}