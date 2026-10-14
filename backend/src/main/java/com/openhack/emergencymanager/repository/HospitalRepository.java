
package com.openhack.emergencymanager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.openhack.emergencymanager.model.Hospital;

public interface HospitalRepository
        extends JpaRepository<Hospital, Long> {

    List<Hospital> findAllByOrderByIdAsc();
}
