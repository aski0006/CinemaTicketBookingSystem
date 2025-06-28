package com.asaki0019.cinematicketbookingsystem.controller;

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
     * 获取场次座位图
     * /api/sessions/{sessionId}/seats
     * 需要登录校验
     */
    @GetMapping("/{sessionId}/seats")
    public Map<String, Object> getSessionSeats(@PathVariable Long sessionId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        // 校验JWT
        if (token == null || !JwtTokenUtils.validateToken(token)) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("error", "未登录或token已过期");
            return resp;
        }
        Session session = sessionService.getSessionById(sessionId);
        if (session == null) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("error", "场次不存在");
            return resp;
        }
        List<com.asaki0019.cinematicketbookingsystem.entities.Seat> seats = seatService
                .getSeatsByHallId(session.getHallId());
        // 构造分组结构
        Map<String, List<Map<String, Object>>> rows = new HashMap<>();
        for (com.asaki0019.cinematicketbookingsystem.entities.Seat seat : seats) {
            Map<String, Object> seatInfo = new HashMap<>();
            seatInfo.put("colNo", seat.getColNo());
            seatInfo.put("status", seat.getStatus());
            rows.computeIfAbsent(seat.getRowNo(), k -> new java.util.ArrayList<>()).add(seatInfo);
        }
        List<Map<String, Object>> rowList = new java.util.ArrayList<>();
        for (String rowNo : rows.keySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("rowNo", rowNo);
            row.put("seats", rows.get(rowNo));
            rowList.add(row);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("rows", rowList);
        return result;
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
    public List<SessionResponseDTO> getTodaySessions(@RequestParam(required = false) Long movieId) {
        return sessionService.getTodaySessions(movieId);
    }
}