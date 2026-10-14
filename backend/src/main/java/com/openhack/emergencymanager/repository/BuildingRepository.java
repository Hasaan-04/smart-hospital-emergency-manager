
package com.openhack.emergencymanager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.openhack.emergencymanager.model.Building;

public interface BuildingRepository
        extends JpaRepository<Building, Long> {

    List<Building> findByHospital_IdOrderByNameAsc(
        Long hospitalId
    );

    boolean existsByHospital_IdAndNameIgnoreCase(
        Long hospitalId,
        String name
    );
}
