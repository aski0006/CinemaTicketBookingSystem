package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Session;
import com.asaki0019.cinematicketbookingsystem.dto.SessionResponseDTO;
import com.asaki0019.cinematicketbookingsystem.dto.SessionSeatMapDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface SessionService {
    List<SessionResponseDTO> getSessions(Long movieId, String date);

    Session getSessionById(Long sessionId);

    SessionSeatMapDTO getSeatMap(Long sessionId);

    List<SessionResponseDTO> getTodaySessions(Long movieId);

    List<List<Map<String, Object>>> getSessionSeatStatus(Long sessionId);

    List<Map<String, Object>> getTodaySessionsWithSeatStatus(Long movieId);

    Session getSessionByIdWithRedis(Long sessionId);
}