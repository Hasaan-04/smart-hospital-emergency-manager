package com.openhack.emergencymanager.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "allocation_holds", indexes = {
    @Index(name = "idx_hold_status_expiry", columnList = "status, expires_at"),
    @Index(name = "idx_hold_case", columnList = "emergency_case_id")
})
public class AllocationHold {
    public enum Status { HELD, APPROVED, CANCELLED, EXPIRED, RELEASED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emergency_case_id", nullable = false)
    private EmergencyCase emergencyCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bed_id")
    private Bed bed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id")
    private Equipment equipment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.HELD;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    private Instant approvedAt;
    private Instant finishedAt;

    protected AllocationHold() {}

    public AllocationHold(EmergencyCase emergencyCase, Bed bed, Doctor doctor,
                          Equipment equipment, Instant expiresAt) {
        this.emergencyCase = emergencyCase;
        this.bed = bed;
        this.doctor = doctor;
        this.equipment = equipment;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
        this.status = Status.HELD;
    }

    public Long getId() { return id; }
    public EmergencyCase getEmergencyCase() { return emergencyCase; }
    public Bed getBed() { return bed; }
    public Doctor getDoctor() { return doctor; }
    public Equipment getEquipment() { return equipment; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getApprovedAt() { return approvedAt; }
    public Instant getFinishedAt() { return finishedAt; }

    public void expire() { this.status = Status.EXPIRED; this.finishedAt = Instant.now(); }
    public void cancel() { this.status = Status.CANCELLED; this.finishedAt = Instant.now(); }
    public void approve() { this.status = Status.APPROVED; this.approvedAt = Instant.now(); }
    public void release() { this.status = Status.RELEASED; this.finishedAt = Instant.now(); }
}
