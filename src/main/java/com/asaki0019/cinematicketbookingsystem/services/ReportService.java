package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Statistics;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    ByteArrayInputStream generateDailyReport(String reportType, LocalDate startDate, LocalDate endDate);

    ByteArrayInputStream generateExcelReport(List<Statistics> statistics);
    // Can add methods for PDF, CSV etc.
}