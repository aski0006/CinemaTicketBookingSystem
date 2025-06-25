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
@Table(name = "membership_order")
public class MembershipOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no")
    private String orderNo;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "membership_type")
    private String membershipType;

    @Column(name = "duration")
    private String duration;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "status")
    private String status;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "payment_time")
    private LocalDateTime paymentTime;

    @Column(name = "payment_url")
    private String paymentUrl;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "refund_amount")
    private Double refundAmount;

    @Column(name = "refund_status")
    private String refundStatus; // NONE, REQUESTED, REFUNDED, REJECTED

    @Column(name = "refund_time")
    private LocalDateTime refundTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}