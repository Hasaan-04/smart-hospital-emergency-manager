
package com.openhack.emergencymanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "hospital_beds",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"room_id", "code"}
    )
)
public class Bed {

    public enum BedType {
        GENERAL,
        EMERGENCY,
        ICU
    }

    public enum BedStatus {
        AVAILABLE,
        OCCUPIED,
        MAINTENANCE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BedType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BedStatus status = BedStatus.AVAILABLE;

    @Column(nullable = false)
    private boolean oxygenSupport;

    @Column(nullable = false)
    private boolean ventilatorCompatible;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    public Bed() {
    }

    public Bed(
            String code,
            BedType type,
            Room room,
            boolean oxygenSupport,
            boolean ventilatorCompatible) {

        this.code = code;
        this.type = type;
        this.room = room;
        this.oxygenSupport = oxygenSupport;
        this.ventilatorCompatible = ventilatorCompatible;
        this.status = BedStatus.AVAILABLE;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public BedType getType() {
        return type;
    }

    public BedStatus getStatus() {
        return status;
    }

    public Room getRoom() {
        return room;
    }

    public boolean isOxygenSupport() {
        return oxygenSupport;
    }

    public boolean isVentilatorCompatible() {
        return ventilatorCompatible;
    }

    public void setStatus(BedStatus status) {
        this.status = status;
    }
}
