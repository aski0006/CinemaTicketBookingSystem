package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Hall;
import com.asaki0019.cinematicketbookingsystem.repository.HallRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HallServiceImpl implements HallService {
    @Autowired
    private HallRepository hallRepository;

    @Override
    public List<Hall> getAllHalls() {
        return hallRepository.findAll();
    }

    @Override
    public Hall getHallById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("影厅ID参数不合法");
        }
        return hallRepository.findById(id).orElse(null);
    }

    @Override
    public Hall updateHall(Hall hall) {
        System.out.println(hall);
        if (hall == null || hall.getId() == null || hall.getId() <= 0) {
            throw new IllegalArgumentException("影厅ID参数不合法");
        }
        Hall existing = hallRepository.findById(hall.getId()).orElse(null);
        if (existing == null) {
            throw new IllegalArgumentException("影厅不存在");
        }
        existing.setName(hall.getName());
        existing.setType(hall.getType());
        existing.setCapacity(hall.getCapacity());
        existing.setSeatLayout(hall.getSeatLayout());
        existing.setScreenType(hall.getScreenType());
        return hallRepository.save(existing);
    }
}