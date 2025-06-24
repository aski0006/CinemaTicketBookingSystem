package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Statistics;
import com.asaki0019.cinematicketbookingsystem.repository.AdminStatisticsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private AdminStatisticsRepository statisticsRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private List<Statistics> createMockStatistics() {
        Statistics stats = new Statistics();
        stats.setId(1L);
        stats.setStatDate(LocalDate.now());
        stats.setMovieId(1L);
        stats.setSessionId(1L);
        stats.setTicketSales(100);
        stats.setRevenue(5000.0);
        return Collections.singletonList(stats);
    }

    @Test
    void generateDailyReport_Excel() throws IOException {
        when(statisticsRepository.findByStatDateBetween(any(), any())).thenReturn(createMockStatistics());

        ByteArrayInputStream result = reportService.generateDailyReport("EXCEL", LocalDate.now(), LocalDate.now());

        assertNotNull(result);
        assertTrue(result.available() > 0);
        // A simple check to see if it's a valid XLSX file (starts with PK zip header)
        byte[] bytes = new byte[4];
        result.read(bytes);
        assertArrayEquals(new byte[] { 0x50, 0x4B, 0x03, 0x04 }, bytes);
    }

    @Test
    void generateDailyReport_Csv() throws IOException {
        when(statisticsRepository.findByStatDateBetween(any(), any())).thenReturn(createMockStatistics());

        ByteArrayInputStream result = reportService.generateDailyReport("CSV", LocalDate.now(), LocalDate.now());

        assertNotNull(result);
        assertTrue(result.available() > 0);
        String content = new String(result.readAllBytes());
        assertTrue(content.contains("Date,Ticket Sales,Revenue"));
        assertTrue(content.contains("100,5000.00"));
    }

    @Test
    void generateDailyReport_UnsupportedType() {
        when(statisticsRepository.findByStatDateBetween(any(), any())).thenReturn(Collections.emptyList());

        assertThrows(IllegalArgumentException.class, () -> {
            reportService.generateDailyReport("PDF", LocalDate.now(), LocalDate.now());
        });
    }

    @Test
    void generateExcelReport_EmptyList() throws IOException {
        ByteArrayInputStream result = reportService.generateExcelReport(Collections.emptyList());
        assertNotNull(result);
        assertTrue(result.available() > 0);
    }
}