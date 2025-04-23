package com.rxclinic.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "appointments")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @ManyToOne
    @JoinColumn(name = "doctor_id")
    private User doctor;

    @Column(nullable = false)
    private String status; // PENDING, CONFIRMED, CANCELLED

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}