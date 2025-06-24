package com.asaki0019.cinematicketbookingsystem.controller;

import com.asaki0019.cinematicketbookingsystem.services.AdminStatisticsService;
import com.asaki0019.cinematicketbookingsystem.utils.JwtTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/statistics")
public class AdminStatisticsController {
    @Autowired
    private AdminStatisticsService adminStatisticsService;

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

    @PostMapping("/reports")
    public Object generateReport(@RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> req) {
        if (!checkJwt(token)) {
            return Map.of("error", "未登录或token已过期");
        }
        String reportType = req.get("reportType") != null ? req.get("reportType").toString() : "PDF";
        Map<String, Object> dateRange = (Map<String, Object>) req.get("dateRange");
        return adminStatisticsService.generateReport(reportType, dateRange);
    }
}