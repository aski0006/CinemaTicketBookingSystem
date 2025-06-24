package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Session;
import java.util.List;

public interface AdminSessionService {
    Session createSession(Session session);

    Session updateSession(Long sessionId, Session session);

    void deleteSession(Long sessionId);

    List<Session> getSessionSeats(Long sessionId);
}