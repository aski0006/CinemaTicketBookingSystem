package com.asaki0019.cinematicketbookingsystem.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_seat")
public class OrderSeat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "seat_id")
    private Long seatId;

    @Column(name = "final_price")
    private Double finalPrice;

    @Column(name = "row_no")
    private Integer rowNo; // 座位行号（从Redis二维数组获取）

    @Column(name = "col_no")
    private Integer colNo; // 座位列号（从Redis二维数组获取）

    @Column(name = "type")
    private String type; // 座位类型（如普通、情侣、VIP等）

    @Column(name = "price_factor")
    private Double priceFactor; // 价格系数（如1.0、1.5等）

    @Column(name = "status")
    private String status; // 座位订单状态，如FINISHI、FAILED等
}