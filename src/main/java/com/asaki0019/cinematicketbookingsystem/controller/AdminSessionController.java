package com.asaki0019.cinematicketbookingsystem.controller;

import com.asaki0019.cinematicketbookingsystem.dto.SeatMapResponse;
import com.asaki0019.cinematicketbookingsystem.entities.Session;
import com.asaki0019.cinematicketbookingsystem.services.AdminSessionService;
import com.asaki0019.cinematicketbookingsystem.utils.JwtTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/sessions")
public class AdminSessionController {
    @Autowired
    private AdminSessionService adminSessionService;

    private boolean checkJwt(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return false;
        }
        return JwtTokenUtils.validateToken(token.substring(7));
    }

    @PostMapping
    public Object createSession(@RequestHeader("Authorization") String token,
            @RequestBody Session session) {
        if (!checkJwt(token)) {
            return Map.of("error", "未登录或token已过期");
        }
        return adminSessionService.createSession(session);
    }

    @PutMapping("/{sessionId}")
    public Object updateSession(@RequestHeader("Authorization") String token,
            @PathVariable Long sessionId,
            @RequestBody Session session) {
        if (!checkJwt(token)) {
            return Map.of("error", "未登录或token已过期");
        }
        return adminSessionService.updateSession(sessionId, session);
    }

    @DeleteMapping("/{sessionId}")
    public Object deleteSession(@RequestHeader("Authorization") String token,
            @PathVariable Long sessionId) {
        if (!checkJwt(token)) {
            return Map.of("error", "未登录或token已过期");
        }
        adminSessionService.deleteSession(sessionId);
        return Map.of("success", true);
    }

    @GetMapping("/{sessionId}/seats")
    public ResponseEntity<SeatMapResponse> getSessionSeatStatus(@PathVariable Long sessionId) {
        SeatMapResponse seatMap = adminSessionService.getSessionSeatStatus(sessionId);
        return ResponseEntity.ok(seatMap);
    }
}