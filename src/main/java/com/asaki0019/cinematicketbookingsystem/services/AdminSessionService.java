package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.dto.SeatMapResponse;
import com.asaki0019.cinematicketbookingsystem.entities.Session;
import com.asaki0019.cinematicketbookingsystem.dto.SessionResponseDTO;
import java.util.List;

public interface AdminSessionService {
    Session createSession(Session session);

    Session updateSession(Long sessionId, Session session);

    void deleteSession(Long sessionId);

    List<Session> getSessionSeats(Long sessionId);

    SeatMapResponse monitorSessionSeats(Long sessionId);

    SeatMapResponse getSessionSeatStatus(Long sessionId);

    int autoArrangeSessions(List<Long> todayMovieIds);

    List<SessionResponseDTO> getTodaySessions();
}