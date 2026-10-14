
package com.openhack.emergencymanager.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.openhack.emergencymanager.model.Hospital;
import com.openhack.emergencymanager.repository.HospitalRepository;

@RestController
@RequestMapping("/api/hospitals")
public class HospitalController {

    private final HospitalRepository repository;

    public HospitalController(HospitalRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Hospital> getHospitals() {
        return repository.findAllByOrderByIdAsc();
    }

    @PostMapping
    public ResponseEntity<Hospital> addHospital(
            @RequestBody HospitalRequest request) {

        if (request.name() == null ||
            request.name().isBlank() ||
            request.city() == null ||
            request.city().isBlank() ||
            request.address() == null ||
            request.address().isBlank()) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "All hospital fields are required"
            );
        }

        Hospital hospital = new Hospital(
            request.name().trim(),
            request.city().trim(),
            request.address().trim()
        );

        Hospital saved = repository.save(hospital);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(saved);
    }

    public record HospitalRequest(
        String name,
        String city,
        String address
    ) {}
}
