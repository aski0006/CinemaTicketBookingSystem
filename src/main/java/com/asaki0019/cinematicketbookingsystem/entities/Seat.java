package com.asaki0019.cinematicketbookingsystem.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "seat")
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hall_id")
    private Long hallId;

    @Column(name = "row_no")
    private String rowNo;

    @Column(name = "col_no")
    private Integer colNo;

    @Column(name = "type")
    private String type;

    @Column(name = "price_factor")
    private Double priceFactor;

    @Column(name = "status")
    private String status;
}