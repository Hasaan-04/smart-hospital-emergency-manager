package com.openhack.emergencymanager.repository;

import com.openhack.emergencymanager.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findByHospital_IdOrderByIdAsc(Long hospitalId);
    boolean existsByHospital_IdAndEmployeeCodeIgnoreCase(Long hospitalId, String code);
}
