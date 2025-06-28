package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Hall;
import java.util.List;

public interface HallService {
    List<Hall> getAllHalls();

    Hall getHallById(Long id);

    Hall updateHall(Hall hall);
}