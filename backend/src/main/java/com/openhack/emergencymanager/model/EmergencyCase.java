package com.openhack.emergencymanager.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "emergency_cases", indexes = {
    @Index(name = "idx_case_hospital", columnList = "hospital_id"),
    @Index(name = "idx_case_priority", columnList = "hospital_id, case_status, arrival_status")
})
public class EmergencyCase {

    // Enum order also defines the prototype's queue priority: 0 = most urgent.
    public enum Urgency { CRITICAL, HIGH, MEDIUM, LOW }
    public enum ArrivalStatus { ARRIVED, PRE_ARRIVAL }
    public enum CaseStatus { WAITING, UNDER_TREATMENT, COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ambulance_id")
    private Ambulance ambulance;

    @Column(nullable = false, length = 120)
    private String patientName;

    @Column(nullable = false)
    private Integer age;

    @Column(nullable = false, length = 500)
    private String chiefComplaint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Urgency urgency;

    @Enumerated(EnumType.STRING)
    @Column(name = "arrival_status", nullable = false)
    private ArrivalStatus arrivalStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_status", nullable = false)
    private CaseStatus caseStatus = CaseStatus.WAITING;

    @Column(nullable = false, updatable = false)
    private Instant registeredAt;

    private Instant arrivedAt;
    private Instant estimatedArrivalAt;

    // Staff-entered requirements. These are input for the later allocation engine.
    @Enumerated(EnumType.STRING)
    private Bed.BedType requestedBedType;

    @Column(length = 100)
    private String requestedSpecialization;

    @Column(nullable = false)
    private boolean oxygenNeeded;

    @Column(nullable = false)
    private boolean ventilatorNeeded;

    protected EmergencyCase() {}

    public EmergencyCase(Hospital hospital, Ambulance ambulance, String patientName,
                         int age, String chiefComplaint, Urgency urgency,
                         ArrivalStatus arrivalStatus, Instant estimatedArrivalAt,
                         Bed.BedType requestedBedType, String requestedSpecialization,
                         boolean oxygenNeeded, boolean ventilatorNeeded) {
        this.hospital = hospital;
        this.ambulance = ambulance;
        this.patientName = patientName;
        this.age = age;
        this.chiefComplaint = chiefComplaint;
        this.urgency = urgency;
        this.arrivalStatus = arrivalStatus;
        this.estimatedArrivalAt = estimatedArrivalAt;
        this.requestedBedType = requestedBedType;
        this.requestedSpecialization = requestedSpecialization;
        this.oxygenNeeded = oxygenNeeded;
        this.ventilatorNeeded = ventilatorNeeded;
        this.registeredAt = Instant.now();
        if (arrivalStatus == ArrivalStatus.ARRIVED) this.arrivedAt = this.registeredAt;
    }

    public Long getId() { return id; }
    public Hospital getHospital() { return hospital; }
    public Ambulance getAmbulance() { return ambulance; }
    public String getPatientName() { return patientName; }
    public Integer getAge() { return age; }
    public String getChiefComplaint() { return chiefComplaint; }
    public Urgency getUrgency() { return urgency; }
    public ArrivalStatus getArrivalStatus() { return arrivalStatus; }
    public CaseStatus getCaseStatus() { return caseStatus; }
    public Instant getRegisteredAt() { return registeredAt; }
    public Instant getArrivedAt() { return arrivedAt; }
    public Instant getEstimatedArrivalAt() { return estimatedArrivalAt; }
    public Bed.BedType getRequestedBedType() { return requestedBedType; }
    public String getRequestedSpecialization() { return requestedSpecialization; }
    public boolean isOxygenNeeded() { return oxygenNeeded; }
    public boolean isVentilatorNeeded() { return ventilatorNeeded; }

    public void changeUrgency(Urgency urgency) { this.urgency = urgency; }

    public void confirmArrival() {
        this.arrivalStatus = ArrivalStatus.ARRIVED;
        this.arrivedAt = Instant.now();
        this.estimatedArrivalAt = null;
    }
    
    // Start treatment after staff approve allocation
    public void startTreatment() {
        if (caseStatus != CaseStatus.WAITING ||
            arrivalStatus != ArrivalStatus.ARRIVED) {
            throw new IllegalStateException(
                "Case must be arrived and waiting"
            );
        }

        this.caseStatus = CaseStatus.UNDER_TREATMENT;
    }

    // Complete treatment after staff release resources
    public void completeTreatment() {
        if (caseStatus != CaseStatus.UNDER_TREATMENT) {
            throw new IllegalStateException(
                "Case is not under treatment"
            );
        }

        this.caseStatus = CaseStatus.COMPLETED;
    }

}
