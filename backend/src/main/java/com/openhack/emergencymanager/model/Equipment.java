package com.openhack.emergencymanager.model;

import jakarta.persistence.*;

@Entity
@Table(name = "medical_equipment", uniqueConstraints =
    @UniqueConstraint(columnNames = {"hospital_id", "asset_code"}))
public class Equipment {
    public enum Status { AVAILABLE, IN_USE, MAINTENANCE }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "asset_code", nullable = false)
    private String assetCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.AVAILABLE;

    protected Equipment() {}

    public Equipment(Hospital hospital, Department department, Room room,
                     String assetCode, String name, String category) {
        this.hospital = hospital;
        this.department = department;
        this.room = room;
        this.assetCode = assetCode;
        this.name = name;
        this.category = category;
    }

    public Long getId() { return id; }
    public Hospital getHospital() { return hospital; }
    public Department getDepartment() { return department; }
    public Room getRoom() { return room; }
    public String getAssetCode() { return assetCode; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public void moveTo(Department department, Room room) {
        this.department = department;
        this.room = room;
    }
}
