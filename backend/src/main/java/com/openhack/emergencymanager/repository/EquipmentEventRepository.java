package com.openhack.emergencymanager.repository;

import com.openhack.emergencymanager.model.EquipmentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EquipmentEventRepository extends JpaRepository<EquipmentEvent, Long> {
    List<EquipmentEvent> findByEquipment_IdOrderByOccurredAtDescIdDesc(Long equipmentId);
}
