package com.asaki0019.cinematicketbookingsystem.service;

import com.asaki0019.cinematicketbookingsystem.entities.Hall;
import com.asaki0019.cinematicketbookingsystem.repository.HallRepository;
import com.asaki0019.cinematicketbookingsystem.services.HallServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class HallServiceImplTest {
    @Mock
    private HallRepository hallRepository;
    @InjectMocks
    private HallServiceImpl hallService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        // 清理mock，防止影响其他测试
        reset(hallRepository);
    }

    @Test
    void testGetAllHalls() {
        when(hallRepository.findAll()).thenReturn(Collections.emptyList());
        assertEquals(0, hallService.getAllHalls().size());
    }

    @Test
    void testGetHallById() {
        Hall hall = new Hall();
        hall.setId(1L);
        when(hallRepository.findById(1L)).thenReturn(Optional.of(hall));
        assertEquals(1L, hallService.getHallById(1L).getId());
    }
}