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
}