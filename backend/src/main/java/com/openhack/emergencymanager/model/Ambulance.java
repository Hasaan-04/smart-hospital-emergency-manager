package com.openhack.emergencymanager.model;

import jakarta.persistence.*;

@Entity
@Table(name = "ambulances", uniqueConstraints =
    @UniqueConstraint(columnNames = {"hospital_id", "vehicle_number"}))
public class Ambulance {
    public enum Status { AVAILABLE, DISPATCHED, MAINTENANCE }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Column(name = "vehicle_number", nullable = false)
    private String vehicleNumber;

    @Column(nullable = false)
    private String driverName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.AVAILABLE;

    protected Ambulance() {}

    public Ambulance(Hospital hospital, String vehicleNumber, String driverName) {
        this.hospital = hospital;
        this.vehicleNumber = vehicleNumber;
        this.driverName = driverName;
    }

    public Long getId() { return id; }
    public Hospital getHospital() { return hospital; }
    public String getVehicleNumber() { return vehicleNumber; }
    public String getDriverName() { return driverName; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
