package com.asaki0019.cinematicketbookingsystem.adminServices;

import com.asaki0019.cinematicketbookingsystem.entities.Session;
import com.asaki0019.cinematicketbookingsystem.repository.AdminSessionRepository;
import com.asaki0019.cinematicketbookingsystem.services.AdminSessionServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AdminSessionServiceImplTest {
    @Mock
    private AdminSessionRepository adminSessionRepository;
    @InjectMocks
    private AdminSessionServiceImpl adminSessionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateSession() {
        Session s = new Session();
        s.setMovieId(1L);
        s.setHallId(1L);
        s.setStartTime(java.time.LocalDateTime.now().plusDays(1));
        s.setEndTime(java.time.LocalDateTime.now().plusDays(1).plusHours(2));
        s.setPrice(50.0);
        when(adminSessionRepository.save(s)).thenReturn(s);
        assertEquals(s, adminSessionService.createSession(s));
    }

    @Test
    void testUpdateSession() {
        Session s = new Session();
        s.setMovieId(1L);
        s.setHallId(1L);
        s.setStartTime(java.time.LocalDateTime.now().plusDays(1));
        s.setEndTime(java.time.LocalDateTime.now().plusDays(1).plusHours(2));
        s.setPrice(50.0);
        when(adminSessionRepository.save(s)).thenReturn(s);
        assertEquals(s, adminSessionService.updateSession(1L, s));
    }

    @Test
    void testDeleteSession() {
        adminSessionService.deleteSession(1L);
        verify(adminSessionRepository).deleteById(1L);
    }
}