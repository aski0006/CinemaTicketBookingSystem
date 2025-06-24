package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Seat;
import java.util.List;

public interface SeatService {
    List<Seat> getSeatsByHallId(Long hallId);

    List<Seat> getSeatsByHallIdAndStatus(Long hallId, String status);
}