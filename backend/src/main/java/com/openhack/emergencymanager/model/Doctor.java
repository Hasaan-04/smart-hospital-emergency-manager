package com.openhack.emergencymanager.model;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "doctors", uniqueConstraints =
    @UniqueConstraint(columnNames = {"hospital_id", "employee_code"}))
public class Doctor {
    public enum Status { AVAILABLE, BUSY, OFF_DUTY }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "employee_code", nullable = false)
    private String employeeCode;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String specialization;

    @Column(nullable = false)
    private LocalTime shiftStart;

    @Column(nullable = false)
    private LocalTime shiftEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.AVAILABLE;

    protected Doctor() {}

    public Doctor(Hospital hospital, Department department,
                  String employeeCode, String fullName, String specialization,
                  LocalTime shiftStart, LocalTime shiftEnd) {
        this.hospital = hospital;
        this.department = department;
        this.employeeCode = employeeCode;
        this.fullName = fullName;
        this.specialization = specialization;
        this.shiftStart = shiftStart;
        this.shiftEnd = shiftEnd;
    }

    public Long getId() { return id; }
    public Hospital getHospital() { return hospital; }
    public Department getDepartment() { return department; }
    public String getEmployeeCode() { return employeeCode; }
    public String getFullName() { return fullName; }
    public String getSpecialization() { return specialization; }
    public LocalTime getShiftStart() { return shiftStart; }
    public LocalTime getShiftEnd() { return shiftEnd; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
