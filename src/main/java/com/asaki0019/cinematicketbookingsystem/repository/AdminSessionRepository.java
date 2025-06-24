package com.asaki0019.cinematicketbookingsystem.repository;

import com.asaki0019.cinematicketbookingsystem.entities.Session;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminSessionRepository extends JpaRepository<Session, Long> {
}