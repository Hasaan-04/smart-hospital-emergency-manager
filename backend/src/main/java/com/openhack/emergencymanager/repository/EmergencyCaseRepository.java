package com.openhack.emergencymanager.repository;

import com.openhack.emergencymanager.model.EmergencyCase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmergencyCaseRepository extends JpaRepository<EmergencyCase, Long> {
    List<EmergencyCase> findByHospital_IdOrderByRegisteredAtDesc(Long hospitalId);
}
