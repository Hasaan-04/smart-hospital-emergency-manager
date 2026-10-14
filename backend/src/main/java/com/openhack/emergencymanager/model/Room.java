
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
    name = "hospital_rooms",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {
            "building_id",
            "floor_number",
            "number"
        }
    )
)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String number;

    @Column(name = "floor_number", nullable = false)
    private int floorNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    public Room() {
    }

    public Room(
            String number,
            int floorNumber,
            Building building,
            Department department) {

        this.number = number;
        this.floorNumber = floorNumber;
        this.building = building;
        this.department = department;
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public Building getBuilding() {
        return building;
    }

    public Department getDepartment() {
        return department;
    }
}
