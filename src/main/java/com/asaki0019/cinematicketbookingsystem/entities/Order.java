package com.asaki0019.cinematicketbookingsystem.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders") // 注意：改为orders因为order是SQL关键字
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no")
    private String orderNo;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "status")
    private String status;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "payment_time")
    private LocalDateTime paymentTime;

    @Column(name = "e_ticket_url")
    private String eTicketUrl;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}