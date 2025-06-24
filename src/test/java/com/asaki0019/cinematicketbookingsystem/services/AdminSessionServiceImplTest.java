package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.dto.SeatMapResponse;
import com.asaki0019.cinematicketbookingsystem.entities.Order;
import com.asaki0019.cinematicketbookingsystem.entities.OrderSeat;
import com.asaki0019.cinematicketbookingsystem.entities.Seat;
import com.asaki0019.cinematicketbookingsystem.entities.Session;
import com.asaki0019.cinematicketbookingsystem.repository.*;
import com.asaki0019.cinematicketbookingsystem.utils.ValidationUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminSessionServiceImplTest {

    @Mock
    private AdminSessionRepository adminSessionRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderSeatRepository orderSeatRepository;
    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private AdminSessionServiceImpl adminSessionService;

    private MockedStatic<ValidationUtils> validationUtilsMock;

    @BeforeEach
    void setUp() {
        validationUtilsMock = mockStatic(ValidationUtils.class);
    }

    @AfterEach
    void tearDown() {
        validationUtilsMock.close();
    }

    private Session createValidSession() {
        Session session = new Session();
        session.setMovieId(1L);
        session.setHallId(1L);
        session.setStartTime(LocalDateTime.now().plusDays(1));
        session.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        session.setPrice(100.0);
        return session;
    }

    @Test
    void createSession() {
        Session session = createValidSession();
        validationUtilsMock.when(() -> ValidationUtils.validateSession(session)).thenReturn(true);
        when(adminSessionRepository.save(session)).thenReturn(session);

        Session result = adminSessionService.createSession(session);
        assertNotNull(result);
        verify(adminSessionRepository).save(session);
    }

    @Test
    void createSession_Invalid() {
        Session session = new Session();
        validationUtilsMock.when(() -> ValidationUtils.validateSession(session)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> adminSessionService.createSession(session));
    }

    @Test
    void updateSession() {
        Session session = createValidSession();
        validationUtilsMock.when(() -> ValidationUtils.validateSession(session)).thenReturn(true);
        when(adminSessionRepository.save(session)).thenReturn(session);

        Session result = adminSessionService.updateSession(1L, session);
        assertEquals(1L, result.getId());
        verify(adminSessionRepository).save(session);
    }

    @Test
    void deleteSession() {
        adminSessionService.deleteSession(1L);
        verify(adminSessionRepository).deleteById(1L);
    }

    @Test
    void monitorSessionSeats() {
        Session session = createValidSession();
        Seat testSeat = new Seat();
        testSeat.setRowNo("A");
        testSeat.setColNo(1);
        when(adminSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(seatRepository.findByHallId(anyLong())).thenReturn(List.of(testSeat));
        when(orderRepository.findBySessionIdAndStatusIn(anyLong(), anyList())).thenReturn(List.of(new Order()));
        when(orderSeatRepository.findByOrderIdIn(anyList())).thenReturn(List.of(new OrderSeat()));

        SeatMapResponse response = adminSessionService.monitorSessionSeats(1L);

        assertNotNull(response);
        assertEquals(1L, response.getSessionId());
    }

    @Test
    void monitorSessionSeats_SessionNotFound() {
        when(adminSessionRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> adminSessionService.monitorSessionSeats(1L));
    }
}