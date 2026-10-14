package com.openhack.emergencymanager.repository;

import com.openhack.emergencymanager.model.Ambulance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AmbulanceRepository extends JpaRepository<Ambulance, Long> {
    List<Ambulance> findByHospital_IdOrderByIdAsc(Long hospitalId);
    boolean existsByHospital_IdAndVehicleNumberIgnoreCase(Long hospitalId, String number);
}
