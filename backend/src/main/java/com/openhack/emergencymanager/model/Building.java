
package com.openhack.emergencymanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "hospital_buildings",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"hospital_id", "name"}
    )
)
public class Building {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    public Building() {
    }

    public Building(String name, Hospital hospital) {
        this.name = name;
        this.hospital = hospital;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Hospital getHospital() {
        return hospital;
    }
}
