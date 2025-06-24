package com.asaki0019.cinematicketbookingsystem.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "statistics")
public class Statistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stat_date")
    private LocalDate statDate;

    @Column(name = "movie_id")
    private Long movieId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "ticket_sales")
    private Integer ticketSales;

    @Column(name = "revenue")
    private Double revenue;
}