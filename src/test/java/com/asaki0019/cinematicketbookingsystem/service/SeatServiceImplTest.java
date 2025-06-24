package com.asaki0019.cinematicketbookingsystem.service;

import com.asaki0019.cinematicketbookingsystem.entities.Seat;
import com.asaki0019.cinematicketbookingsystem.repository.SeatRepository;
import com.asaki0019.cinematicketbookingsystem.services.SeatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SeatServiceImplTest {
    @Mock
    private SeatRepository seatRepository;
    @InjectMocks
    private SeatServiceImpl seatService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        // 清理mock，防止影响其他测试
        reset(seatRepository);
    }

    @Test
    void testGetSeatsByHallId() {
        when(seatRepository.findByHallId(1L)).thenReturn(Collections.emptyList());
        assertEquals(0, seatService.getSeatsByHallId(1L).size());
    }

    @Test
    void testGetSeatsByHallIdAndStatus() {
        when(seatRepository.findByHallIdAndStatus(1L, "AVAILABLE")).thenReturn(Collections.emptyList());
        assertEquals(0, seatService.getSeatsByHallIdAndStatus(1L, "AVAILABLE").size());
    }
}