package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Seat;
import com.asaki0019.cinematicketbookingsystem.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeatServiceImpl implements SeatService {
    @Autowired
    private SeatRepository seatRepository;

    @Override
    public List<Seat> getSeatsByHallId(Long hallId) {
        if (hallId == null || hallId <= 0) {
            throw new IllegalArgumentException("影厅ID参数不合法");
        }
        return seatRepository.findByHallId(hallId);
    }

    @Override
    public List<Seat> getSeatsByHallIdAndStatus(Long hallId, String status) {
        if (hallId == null || hallId <= 0 || status == null || status.isEmpty()) {
            throw new IllegalArgumentException("参数不合法");
        }
        return seatRepository.findByHallIdAndStatus(hallId, status);
    }
}