package com.asaki0019.cinematicketbookingsystem.service;

import com.asaki0019.cinematicketbookingsystem.entities.Session;
import com.asaki0019.cinematicketbookingsystem.repository.SessionRepository;
import com.asaki0019.cinematicketbookingsystem.services.SessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SessionServiceImplTest {
    @Mock
    private SessionRepository sessionRepository;
    @InjectMocks
    private SessionServiceImpl sessionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        // 清理mock，防止影响其他测试
        reset(sessionRepository);
    }

    @Test
    void testGetSessions_AllNull() {
        when(sessionRepository.findAll()).thenReturn(Collections.emptyList());
        assertEquals(0, sessionService.getSessions(null, null).size());
    }

    @Test
    void testGetSessionById() {
        Session session = new Session();
        session.setId(1L);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        assertEquals(1L, sessionService.getSessionById(1L).getId());
    }
}