package com.asaki0019.cinematicketbookingsystem.controller;

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
@RequestMapping("/api")
public class SessionController {
    @Autowired
    private SessionService sessionService;
    @Autowired
    private SeatService seatService;

    /**
     * 查询电影场次
     * /api/sessions?movieId=&date=
     */
    @GetMapping("/sessions")
    public List<Session> getSessions(@RequestParam(required = false) Long movieId,
            @RequestParam(required = false) String date) {
        LocalDate localDate = null;
        if (date != null && !date.isEmpty()) {
            localDate = LocalDate.parse(date);
        }
        return sessionService.getSessions(movieId, localDate);
    }

    /**
     * 获取场次座位图
     * /api/sessions/{sessionId}/seats
     * 需要登录校验
     */
    @GetMapping("/sessions/{sessionId}/seats")
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
        List<Seat> seats = seatService.getSeatsByHallId(session.getHallId());
        Map<String, Object> result = new HashMap<>();
        result.put("session_id", sessionId);
        result.put("seats", seats);
        return result;
    }
}