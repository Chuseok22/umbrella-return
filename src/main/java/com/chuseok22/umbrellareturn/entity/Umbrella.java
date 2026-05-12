package com.chuseok22.umbrellareturn.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "umbrella")
public class Umbrella {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UmbrellaStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Umbrella() {}

    public Umbrella(String number) {
        this.number = number;
        this.status = UmbrellaStatus.AVAILABLE;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getNumber() { return number; }
    public UmbrellaStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void markRented() { this.status = UmbrellaStatus.RENTED; }
    public void markAvailable() { this.status = UmbrellaStatus.AVAILABLE; }
    public boolean isAvailable() { return this.status == UmbrellaStatus.AVAILABLE; }
}
