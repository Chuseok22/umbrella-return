package com.chuseok22.umbrellareturn.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sms_log")
public class SmsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id", nullable = false)
    private Rental rental;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private boolean success;

    @Column(columnDefinition = "TEXT")
    private String response;

    protected SmsLog() {}

    public SmsLog(Rental rental, String phone, String message, boolean success, String response) {
        this.rental = rental;
        this.phone = phone;
        this.message = message;
        this.sentAt = LocalDateTime.now();
        this.success = success;
        this.response = response;
    }

    public Long getId() { return id; }
    public Rental getRental() { return rental; }
    public String getPhone() { return phone; }
    public String getMessage() { return message; }
    public LocalDateTime getSentAt() { return sentAt; }
    public boolean isSuccess() { return success; }
    public String getResponse() { return response; }
}
