
package com.openhack.emergencymanager.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.openhack.emergencymanager.model.Department;
import com.openhack.emergencymanager.model.Hospital;
import com.openhack.emergencymanager.repository.DepartmentRepository;
import com.openhack.emergencymanager.repository.HospitalRepository;

@RestController
@RequestMapping("/api/hospitals/{hospitalId}/departments")
public class DepartmentController {

    private final DepartmentRepository departments;
    private final HospitalRepository hospitals;

    public DepartmentController(
            DepartmentRepository departments,
            HospitalRepository hospitals) {

        this.departments = departments;
        this.hospitals = hospitals;
    }

    @GetMapping
    public List<DepartmentResponse> getDepartments(
            @PathVariable Long hospitalId) {

        getHospital(hospitalId);

        return departments
                .findByHospital_IdOrderByNameAsc(hospitalId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    public ResponseEntity<DepartmentResponse> addDepartment(
            @PathVariable Long hospitalId,
            @RequestBody DepartmentRequest request) {

        Hospital hospital = getHospital(hospitalId);

        if (request.name() == null ||
            request.name().isBlank()) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Department name is required"
            );
        }

        String name = request.name().trim();

        if (departments.existsByHospital_IdAndNameIgnoreCase(
                hospitalId, name)) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Department already exists"
            );
        }

        Department saved = departments.save(
            new Department(name, hospital)
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(saved));
    }

    private Hospital getHospital(Long id) {
        return hospitals.findById(id)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Hospital not found"
                    )
                );
    }

    private DepartmentResponse toResponse(Department d) {
        return new DepartmentResponse(
            d.getId(),
            d.getName(),
            d.getHospitalId()
        );
    }

    public record DepartmentRequest(String name) {}

    public record DepartmentResponse(
        Long id,
        String name,
        Long hospitalId
    ) {}
}
