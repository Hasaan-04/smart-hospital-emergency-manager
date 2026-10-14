
package com.openhack.emergencymanager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openhack.emergencymanager.model.Bed;

public interface BedRepository
        extends JpaRepository<Bed, Long> {

    @Query("""
        SELECT b FROM Bed b
        WHERE b.room.building.hospital.id = :hospitalId
        ORDER BY b.room.building.name,
                 b.room.floorNumber,
                 b.room.number,
                 b.code
        """)
    List<Bed> findByHospital(
        @Param("hospitalId") Long hospitalId
    );

    boolean existsByRoom_IdAndCodeIgnoreCase(
        Long roomId,
        String code
    );
}
