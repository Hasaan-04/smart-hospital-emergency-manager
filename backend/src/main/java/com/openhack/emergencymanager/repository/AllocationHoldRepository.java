package com.openhack.emergencymanager.repository;

import com.openhack.emergencymanager.model.AllocationHold;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface AllocationHoldRepository extends JpaRepository<AllocationHold, Long> {
    List<AllocationHold> findByStatusAndExpiresAtAfter(AllocationHold.Status status, Instant now);
    List<AllocationHold> findByStatusAndExpiresAtLessThanEqual(AllocationHold.Status status, Instant now);
    List<AllocationHold> findByEmergencyCase_Hospital_IdOrderByIdDesc(Long hospitalId);
}
