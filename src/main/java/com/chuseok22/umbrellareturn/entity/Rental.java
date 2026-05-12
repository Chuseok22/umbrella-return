package com.chuseok22.umbrellareturn.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental")
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "umbrella_id", nullable = false)
    private Umbrella umbrella;

    @Column(nullable = false, length = 50)
    private String borrowerName;

    @Column(nullable = false, length = 20)
    private String borrowerPhone;

    @Column(nullable = false, length = 20)
    private String borrowerStudentId;

    @Column(nullable = false)
    private LocalDateTime rentedAt;

    private LocalDateTime returnedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RentalStatus status;

    protected Rental() {}

    public Rental(Umbrella umbrella, String borrowerName, String borrowerPhone, String borrowerStudentId) {
        this.umbrella = umbrella;
        this.borrowerName = borrowerName;
        this.borrowerPhone = borrowerPhone;
        this.borrowerStudentId = borrowerStudentId;
        this.rentedAt = LocalDateTime.now();
        this.status = RentalStatus.RENTED;
    }

    public Long getId() { return id; }
    public Umbrella getUmbrella() { return umbrella; }
    public String getBorrowerName() { return borrowerName; }
    public String getBorrowerPhone() { return borrowerPhone; }
    public String getBorrowerStudentId() { return borrowerStudentId; }
    public LocalDateTime getRentedAt() { return rentedAt; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
    public RentalStatus getStatus() { return status; }

    public void markReturned() {
        this.returnedAt = LocalDateTime.now();
        this.status = RentalStatus.RETURNED;
    }

    public boolean isActive() { return this.status == RentalStatus.RENTED; }
}
