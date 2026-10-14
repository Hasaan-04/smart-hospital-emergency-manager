package com.openhack.emergencymanager.repository;

import com.openhack.emergencymanager.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    List<Equipment> findByHospital_IdOrderByIdAsc(Long hospitalId);
    boolean existsByHospital_IdAndAssetCodeIgnoreCase(Long hospitalId, String code);
}
