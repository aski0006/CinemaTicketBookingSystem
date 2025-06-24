package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Session;
import com.asaki0019.cinematicketbookingsystem.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SessionServiceImpl implements SessionService {
    @Autowired
    private SessionRepository sessionRepository;

    @Override
    public List<Session> getSessions(Long movieId, LocalDate date) {
        LocalDateTime start = date != null ? date.atStartOfDay() : null;
        LocalDateTime end = date != null ? date.atTime(23, 59, 59) : null;
        if (movieId != null && date != null) {
            return sessionRepository.findByMovieIdAndStartTimeBetween(movieId, start, end);
        } else if (movieId != null) {
            return sessionRepository.findByMovieId(movieId);
        } else if (date != null) {
            return sessionRepository.findByStartTimeBetween(start, end);
        } else {
            return sessionRepository.findAll();
        }
    }

    @Override
    public Session getSessionById(Long sessionId) {
        return sessionRepository.findById(sessionId).orElse(null);
    }
}