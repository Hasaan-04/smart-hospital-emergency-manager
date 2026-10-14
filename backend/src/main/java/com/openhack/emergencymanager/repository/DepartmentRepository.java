
package com.openhack.emergencymanager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.openhack.emergencymanager.model.Department;

public interface DepartmentRepository
        extends JpaRepository<Department, Long> {

    List<Department> findByHospital_IdOrderByNameAsc(
        Long hospitalId
    );

    boolean existsByHospital_IdAndNameIgnoreCase(
        Long hospitalId,
        String name
    );
}
