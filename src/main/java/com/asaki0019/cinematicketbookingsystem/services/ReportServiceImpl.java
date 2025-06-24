package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Statistics;
import com.asaki0019.cinematicketbookingsystem.repository.AdminStatisticsRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private AdminStatisticsRepository statisticsRepository;

    @Override
    public ByteArrayInputStream generateDailyReport(String reportType, LocalDate startDate, LocalDate endDate) {
        List<Statistics> dailyStats = statisticsRepository.findByStatDateBetween(startDate, endDate);

        switch (reportType.toUpperCase()) {
            case "EXCEL":
                return generateExcelReport(dailyStats);
            case "CSV":
                return generateCsvReport(dailyStats);
            default:
                throw new IllegalArgumentException("Unsupported report type: " + reportType);
        }
    }

    @Override
    public ByteArrayInputStream generateExcelReport(List<Statistics> statistics) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Statistics");

            // Header
            Row headerRow = sheet.createRow(0);
            String[] headers = { "ID", "Date", "Movie ID", "Session ID", "Tickets Sold", "Revenue" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Data
            int rowIdx = 1;
            for (Statistics stat : statistics) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(stat.getId());
                row.createCell(1).setCellValue(stat.getStatDate().toString());
                row.createCell(2).setCellValue(stat.getMovieId());
                row.createCell(3).setCellValue(stat.getSessionId());
                row.createCell(4).setCellValue(stat.getTicketSales());
                row.createCell(5).setCellValue(stat.getRevenue());
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report: " + e.getMessage());
        }
    }

    private ByteArrayInputStream generateCsvReport(List<Statistics> dailyStats) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter pw = new PrintWriter(out)) {
            // Header
            pw.println("Date,Ticket Sales,Revenue");

            // Data
            for (Statistics stat : dailyStats) {
                pw.printf("%s,%d,%.2f\n",
                        stat.getStatDate().toString(),
                        stat.getTicketSales(),
                        stat.getRevenue());
            }
            pw.flush();
        }
        return new ByteArrayInputStream(out.toByteArray());
    }
}