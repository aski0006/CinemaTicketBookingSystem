package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Session;
import com.asaki0019.cinematicketbookingsystem.repository.AdminSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminSessionServiceImpl implements AdminSessionService {
    @Autowired
    private AdminSessionRepository adminSessionRepository;

    @Override
    public Session createSession(Session session) {
        if (!com.asaki0019.cinematicketbookingsystem.utils.ValidationUtils.validateSession(session)) {
            throw new IllegalArgumentException("场次参数不合法");
        }
        return adminSessionRepository.save(session);
    }

    @Override
    public Session updateSession(Long sessionId, Session session) {
        if (!com.asaki0019.cinematicketbookingsystem.utils.ValidationUtils.validateSession(session)) {
            throw new IllegalArgumentException("场次参数不合法");
        }
        session.setId(sessionId);
        return adminSessionRepository.save(session);
    }

    @Override
    public void deleteSession(Long sessionId) {
        adminSessionRepository.deleteById(sessionId);
    }

    @Override
    public List<Session> getSessionSeats(Long sessionId) {
        // 实际应返回座位状态，这里仅返回Session
        return List.of(adminSessionRepository.findById(sessionId).orElse(null));
    }
}