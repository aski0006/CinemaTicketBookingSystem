package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Session;
import java.time.LocalDate;
import java.util.List;

public interface SessionService {
    List<Session> getSessions(Long movieId, LocalDate date);

    Session getSessionById(Long sessionId);
}