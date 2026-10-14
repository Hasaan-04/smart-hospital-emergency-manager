
package com.openhack.emergencymanager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openhack.emergencymanager.model.Room;

public interface RoomRepository
        extends JpaRepository<Room, Long> {

    @Query("""
        SELECT r FROM Room r
        WHERE r.building.hospital.id = :hospitalId
        ORDER BY r.building.name,
                 r.floorNumber,
                 r.number
        """)
    List<Room> findByHospital(
        @Param("hospitalId") Long hospitalId
    );

    boolean existsByBuilding_IdAndFloorNumberAndNumberIgnoreCase(
        Long buildingId,
        int floorNumber,
        String number
    );
}
