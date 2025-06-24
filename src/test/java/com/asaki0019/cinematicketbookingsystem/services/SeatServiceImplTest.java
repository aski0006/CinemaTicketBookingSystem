package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Seat;
import com.asaki0019.cinematicketbookingsystem.repository.SeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatServiceImplTest {

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private SeatServiceImpl seatService;

    @Test
    void getSeatsByHallId() {
        when(seatRepository.findByHallId(1L)).thenReturn(Collections.singletonList(new Seat()));
        List<Seat> result = seatService.getSeatsByHallId(1L);
        assertFalse(result.isEmpty());
        verify(seatRepository).findByHallId(1L);
    }

    @Test
    void getSeatsByHallId_InvalidId() {
        assertThrows(IllegalArgumentException.class, () -> seatService.getSeatsByHallId(null));
        assertThrows(IllegalArgumentException.class, () -> seatService.getSeatsByHallId(0L));
        verify(seatRepository, never()).findByHallId(anyLong());
    }

    @Test
    void getSeatsByHallIdAndStatus() {
        when(seatRepository.findByHallIdAndStatus(1L, "AVAILABLE")).thenReturn(Collections.singletonList(new Seat()));
        List<Seat> result = seatService.getSeatsByHallIdAndStatus(1L, "AVAILABLE");
        assertFalse(result.isEmpty());
        verify(seatRepository).findByHallIdAndStatus(1L, "AVAILABLE");
    }

    @Test
    void getSeatsByHallIdAndStatus_InvalidParams() {
        assertThrows(IllegalArgumentException.class, () -> seatService.getSeatsByHallIdAndStatus(null, "AVAILABLE"));
        assertThrows(IllegalArgumentException.class, () -> seatService.getSeatsByHallIdAndStatus(1L, null));
        assertThrows(IllegalArgumentException.class, () -> seatService.getSeatsByHallIdAndStatus(1L, ""));
        verify(seatRepository, never()).findByHallIdAndStatus(anyLong(), anyString());
    }
}