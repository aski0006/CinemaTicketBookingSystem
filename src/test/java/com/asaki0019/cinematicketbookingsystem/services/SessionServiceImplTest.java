package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.dto.SessionResponseDTO;
import com.asaki0019.cinematicketbookingsystem.dto.SessionSeatMapDTO;
import com.asaki0019.cinematicketbookingsystem.entities.*;
import com.asaki0019.cinematicketbookingsystem.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private HallRepository hallRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderSeatRepository orderSeatRepository;

    @InjectMocks
    private SessionServiceImpl sessionService;

    @Test
    void getSessions() {
        Session session = new Session();
        session.setHallId(1L);
        Hall hall = new Hall();
        hall.setId(1L);
        hall.setName("Hall 1");

        when(sessionRepository.findSessions(any(), any(), any())).thenReturn(Collections.singletonList(session));
        when(hallRepository.findById(1L)).thenReturn(Optional.of(hall));

        List<SessionResponseDTO> result = sessionService.getSessions(1L, "2023-01-01");

        assertFalse(result.isEmpty());
        assertEquals("Hall 1", result.get(0).getHall().getName());
    }

    @Test
    void getSessionById() {
        Session session = new Session();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        assertNotNull(sessionService.getSessionById(1L));
    }

    @Test
    void getSessionById_NotFound() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.empty());
        assertNull(sessionService.getSessionById(1L));
    }

    @Test
    void getSeatMap() {
        Session session = new Session();
        session.setHallId(1L);
        Seat availableSeat = new Seat();
        availableSeat.setId(1L);
        Seat occupiedSeat = new Seat();
        occupiedSeat.setId(2L);
        Order order = new Order();
        order.setId(10L);
        OrderSeat orderSeat = new OrderSeat();
        orderSeat.setSeatId(2L);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(seatRepository.findByHallId(1L)).thenReturn(List.of(availableSeat, occupiedSeat));
        when(orderRepository.findBySessionIdAndStatusIn(anyLong(), anyList())).thenReturn(List.of(order));
        when(orderSeatRepository.findByOrderIdIn(anyList())).thenReturn(List.of(orderSeat));

        SessionSeatMapDTO result = sessionService.getSeatMap(1L);

        assertEquals(2, result.getSeats().size());
        assertEquals("AVAILABLE", result.getSeats().get(0).getStatus());
        assertEquals("OCCUPIED", result.getSeats().get(1).getStatus());
    }

    @Test
    void getSeatMap_SessionNotFound() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> sessionService.getSeatMap(1L));
    }
}