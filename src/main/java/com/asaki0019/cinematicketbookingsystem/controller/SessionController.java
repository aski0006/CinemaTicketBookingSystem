package com.asaki0019.cinematicketbookingsystem.controller;

import com.asaki0019.cinematicketbookingsystem.aop.LogAspect.NotLogInAOP;
import com.asaki0019.cinematicketbookingsystem.dto.SessionResponseDTO;
import com.asaki0019.cinematicketbookingsystem.dto.SessionSeatMapDTO;
import com.asaki0019.cinematicketbookingsystem.entities.Session;
import com.asaki0019.cinematicketbookingsystem.entities.Seat;
import com.asaki0019.cinematicketbookingsystem.services.SessionService;
import com.asaki0019.cinematicketbookingsystem.services.SeatService;
import com.asaki0019.cinematicketbookingsystem.utils.JwtTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {
    @Autowired
    private SessionService sessionService;
    @Autowired
    private SeatService seatService;

    /**
     * 查询电影场次
     * /api/sessions?movieId=&date=
     */
    @GetMapping("")
    public List<SessionResponseDTO> getSessions(@RequestParam(required = false) Long movieId,
            @RequestParam(required = false) String date) {
        return sessionService.getSessions(movieId, date);
    }

    /**
     * 获取场次座位状态（支持Redis场次）
     */
    @GetMapping("/{sessionId}/seats")
    public List<List<Map<String, Object>>> getSessionSeats(@PathVariable Long sessionId) {
        return sessionService.getSessionSeatStatus(sessionId);
    }

    @GetMapping("/{sessionId}/seat-map")
    public SessionSeatMapDTO getSeatMap(@PathVariable Long sessionId) {
        return sessionService.getSeatMap(sessionId);
    }

    /**
     * 获取指定影片的场次信息
     * /api/sessions/movie/{movieId}
     */
    @GetMapping("/movie/{movieId}")
    public List<Map<String, Object>> getMovieSessions(@PathVariable Long movieId) {
        List<SessionResponseDTO> sessions = sessionService.getSessions(movieId, null);
        return sessions.stream().map(session -> {
            Map<String, Object> sessionMap = new HashMap<>();
            sessionMap.put("id", session.getId());
            sessionMap.put("startTime", session.getStartTime());
            sessionMap.put("endTime", session.getEndTime());
            sessionMap.put("price", session.getPrice());
            sessionMap.put("hallName", session.getHall() != null ? session.getHall().getName() : "未知影厅");
            sessionMap.put("movieId", session.getMovieId());

            // 计算场次状态：根据座位占用情况
            try {
                SessionSeatMapDTO seatMap = sessionService.getSeatMap(session.getId());
                long totalSeats = seatMap.getSeats().size();
                long occupiedSeats = seatMap.getSeats().stream()
                        .filter(seat -> "OCCUPIED".equals(seat.getStatus()))
                        .count();

                String status;
                if (occupiedSeats == 0) {
                    status = "AVAILABLE";
                } else if (occupiedSeats >= totalSeats) {
                    status = "FULL";
                } else {
                    status = "AVAILABLE";
                }
                sessionMap.put("status", status);
                sessionMap.put("availableSeats", totalSeats - occupiedSeats);
                sessionMap.put("totalSeats", totalSeats);
            } catch (Exception e) {
                sessionMap.put("status", "AVAILABLE");
                sessionMap.put("availableSeats", 0);
                sessionMap.put("totalSeats", 0);
            }

            return sessionMap;
        }).toList();
    }

    /**
     * 获取今日所有场次（优先查Redis）
     */
    @GetMapping("/today")
    @NotLogInAOP
    public List<Map<String, Object>> getTodaySessions(@RequestParam(required = false) Long movieId) {
        return sessionService.getTodaySessionsWithSeatStatus(movieId);
    }

    /**
     * 锁定座位（下单时用）
     * body: [{row: 1, col: 2}, ...]
     */
    @PostMapping("/{sessionId}/lock")
    public Map<String, Object> lockSeats(@PathVariable Long sessionId, @RequestBody List<Map<String, Integer>> seats) {
        String redisKey = "session_seats:" + sessionId;
        try {
            String json = com.asaki0019.cinematicketbookingsystem.utils.RedisCacheUtils.get(redisKey);
            if (json == null || json.isEmpty())
                return Map.of("error", "场次不存在");
            ObjectMapper objectMapper = new ObjectMapper();
            List<List<Map<String, Object>>> seatStatus = objectMapper.readValue(json, List.class);
            // 检查并锁定
            for (Map<String, Integer> seat : seats) {
                int row = seat.get("row");
                int col = seat.get("col");
                Map<String, Object> cell = seatStatus.get(row).get(col);
                if (cell == null || !"AVAILABLE".equals(cell.get("status"))) {
                    return Map.of("error", "座位已被占用");
                }
            }
            // 标记为LOCKED
            for (Map<String, Integer> seat : seats) {
                int row = seat.get("row");
                int col = seat.get("col");
                seatStatus.get(row).get(col).put("status", "LOCKED");
            }
            com.asaki0019.cinematicketbookingsystem.utils.RedisCacheUtils.set(redisKey,
                    objectMapper.writeValueAsString(seatStatus));
            return Map.of("success", true);
        } catch (Exception e) {
            return Map.of("error", "锁定失败");
        }
    }
}