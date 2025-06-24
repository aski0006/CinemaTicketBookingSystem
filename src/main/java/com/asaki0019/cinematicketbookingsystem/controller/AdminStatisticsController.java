package com.asaki0019.cinematicketbookingsystem.controller;

import com.asaki0019.cinematicketbookingsystem.entities.Statistics;
import com.asaki0019.cinematicketbookingsystem.repository.AdminStatisticsRepository;
import com.asaki0019.cinematicketbookingsystem.services.AdminStatisticsService;
import com.asaki0019.cinematicketbookingsystem.services.ReportService;
import com.asaki0019.cinematicketbookingsystem.utils.JwtTokenUtils;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/statistics")
public class AdminStatisticsController {
    @Autowired
    private AdminStatisticsService adminStatisticsService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private AdminStatisticsRepository statisticsRepository;

    private boolean checkJwt(String token) {
        return token != null && JwtTokenUtils.validateToken(token);
    }

    @GetMapping("/daily")
    public Object getDailyStatistics(@RequestHeader("Authorization") String token,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        if (!checkJwt(token)) {
            return Map.of("error", "未登录或token已过期");
        }
        return adminStatisticsService.getDailyStatistics(startDate, endDate);
    }

    @GetMapping("/movies")
    public Object getMovieStatistics(@RequestHeader("Authorization") String token) {
        if (!checkJwt(token)) {
            return Map.of("error", "未登录或token已过期");
        }
        return adminStatisticsService.getMovieStatistics();
    }

    @Data
    static class ReportRequest {
        private String reportType;
        private DateRange dateRange;

        @Data
        static class DateRange {
            private LocalDate start;
            private LocalDate end;
        }
    }

    @PostMapping("/reports")
    public ResponseEntity<InputStreamResource> generateReport(@RequestBody ReportRequest request) {
        String reportType = request.getReportType();
        LocalDate startDate = request.getDateRange().getStart();
        LocalDate endDate = request.getDateRange().getEnd();

        List<Statistics> stats = statisticsRepository.findByDateRange(startDate, endDate);

        if ("EXCEL".equalsIgnoreCase(reportType)) {
            ByteArrayInputStream bis = reportService.generateExcelReport(stats);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=statistics.xlsx");
            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(bis));
        }
        // Fallback for other types or invalid type
        return ResponseEntity.badRequest().build();
    }
}