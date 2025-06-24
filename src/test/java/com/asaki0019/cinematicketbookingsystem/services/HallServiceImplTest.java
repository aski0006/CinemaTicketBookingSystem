package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Hall;
import com.asaki0019.cinematicketbookingsystem.repository.HallRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HallServiceImplTest {

    @Mock
    private HallRepository hallRepository;

    @InjectMocks
    private HallServiceImpl hallService;

    @Test
    void getAllHalls() {
        when(hallRepository.findAll()).thenReturn(Collections.singletonList(new Hall()));
        List<Hall> result = hallService.getAllHalls();
        assertFalse(result.isEmpty());
        verify(hallRepository).findAll();
    }

    @Test
    void getHallById() {
        Hall hall = new Hall();
        hall.setId(1L);
        when(hallRepository.findById(1L)).thenReturn(Optional.of(hall));
        Hall result = hallService.getHallById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getHallById_NotFound() {
        when(hallRepository.findById(1L)).thenReturn(Optional.empty());
        assertNull(hallService.getHallById(1L));
    }

    @Test
    void getHallById_InvalidId() {
        assertThrows(IllegalArgumentException.class, () -> hallService.getHallById(null));
        assertThrows(IllegalArgumentException.class, () -> hallService.getHallById(0L));
        assertThrows(IllegalArgumentException.class, () -> hallService.getHallById(-1L));
        verify(hallRepository, never()).findById(anyLong());
    }
}