package com.openhack.emergencymanager.controller;

import com.openhack.emergencymanager.model.*;
import com.openhack.emergencymanager.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/hospitals/{hospitalId}/cases")
public class EmergencyCaseController {

    private final HospitalRepository hospitals;
    private final AmbulanceRepository ambulances;
    private final EmergencyCaseRepository cases;

    public EmergencyCaseController(HospitalRepository hospitals,
                                   AmbulanceRepository ambulances,
                                   EmergencyCaseRepository cases) {
        this.hospitals = hospitals;
        this.ambulances = ambulances;
        this.cases = cases;
    }

    private Hospital hospital(Long hospitalId) {
        return hospitals.findById(hospitalId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospital not found"));
    }

    private EmergencyCase getCase(Long hospitalId, Long caseId) {
        EmergencyCase found = cases.findById(caseId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));
        if (!found.getHospital().getId().equals(hospitalId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found in this hospital");
        }
        return found;
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private String required(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) throw bad(field + " is required");
        String text = value.trim();
        if (text.length() > maxLength) throw bad(field + " is too long");
        return text;
    }

    private String optional(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String text = value.trim();
        if (text.length() > maxLength) throw bad(field + " is too long");
        return text;
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, String raw, String label) {
        String normalized = required(raw, label, 40).toUpperCase(Locale.ROOT);
        try {
            return Enum.valueOf(type, normalized);
        } catch (IllegalArgumentException ex) {
            throw bad("Invalid " + label);
        }
    }

    private CaseResponse toResponse(EmergencyCase c) {
        Ambulance ambulance = c.getAmbulance();
        return new CaseResponse(
            c.getId(), c.getPatientName(), c.getAge(), c.getChiefComplaint(),
            c.getUrgency().name(), c.getArrivalStatus().name(), c.getCaseStatus().name(),
            c.getRegisteredAt(), c.getArrivedAt(), c.getEstimatedArrivalAt(),
            ambulance == null ? null : ambulance.getId(),
            ambulance == null ? null : ambulance.getVehicleNumber(),
            c.getRequestedBedType() == null ? null : c.getRequestedBedType().name(),
            c.getRequestedSpecialization(), c.isOxygenNeeded(), c.isVentilatorNeeded()
        );
    }

    // List every case, newest registration first.
    @GetMapping
    @Transactional(readOnly = true)
    public List<CaseResponse> listAll(@PathVariable Long hospitalId) {
        hospital(hospitalId);
        return cases.findByHospital_IdOrderByRegisteredAtDesc(hospitalId)
            .stream().map(this::toResponse).toList();
    }

    // Arrived patients who are WAITING: clinical urgency first, then arrival time.
    @GetMapping("/queue")
    @Transactional(readOnly = true)
    public List<CaseResponse> waitingQueue(@PathVariable Long hospitalId) {
        hospital(hospitalId);
        return cases.findByHospital_IdOrderByRegisteredAtDesc(hospitalId).stream()
            .filter(c -> c.getCaseStatus() == EmergencyCase.CaseStatus.WAITING)
            .filter(c -> c.getArrivalStatus() == EmergencyCase.ArrivalStatus.ARRIVED)
            .sorted(Comparator
                .comparingInt((EmergencyCase c) -> c.getUrgency().ordinal())
                .thenComparing(EmergencyCase::getArrivedAt)
                .thenComparing(EmergencyCase::getId))
            .map(this::toResponse).toList();
    }

    // Incoming cases are shown separately; they have NOT started waiting at the hospital.
    @GetMapping("/incoming")
    @Transactional(readOnly = true)
    public List<CaseResponse> incoming(@PathVariable Long hospitalId) {
        hospital(hospitalId);
        return cases.findByHospital_IdOrderByRegisteredAtDesc(hospitalId).stream()
            .filter(c -> c.getCaseStatus() == EmergencyCase.CaseStatus.WAITING)
            .filter(c -> c.getArrivalStatus() == EmergencyCase.ArrivalStatus.PRE_ARRIVAL)
            .sorted(Comparator
                .comparingInt((EmergencyCase c) -> c.getUrgency().ordinal())
                .thenComparing(EmergencyCase::getEstimatedArrivalAt)
                .thenComparing(EmergencyCase::getId))
            .map(this::toResponse).toList();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<CaseResponse> register(@PathVariable Long hospitalId,
                                                  @RequestBody RegisterCaseRequest req) {
        Hospital hospital = hospital(hospitalId);
        String name = required(req.patientName(), "Patient name", 120);
        String complaint = required(req.chiefComplaint(), "Chief complaint", 500);
        if (req.age() == null || req.age() < 0 || req.age() > 125) {
            throw bad("Age must be between 0 and 125");
        }
        EmergencyCase.Urgency urgency = enumValue(
            EmergencyCase.Urgency.class, req.urgency(), "urgency");
        EmergencyCase.ArrivalStatus arrival = enumValue(
            EmergencyCase.ArrivalStatus.class, req.arrivalStatus(), "arrival status");

        Instant eta = null;
        if (arrival == EmergencyCase.ArrivalStatus.PRE_ARRIVAL) {
            try {
                eta = Instant.parse(required(req.estimatedArrivalAt(), "ETA", 60));
            } catch (DateTimeParseException ex) {
                throw bad("ETA must be a valid ISO timestamp");
            }
        } else if (req.estimatedArrivalAt() != null && !req.estimatedArrivalAt().isBlank()) {
            throw bad("ETA should only be provided for pre-arrival cases");
        }

        Ambulance ambulance = null;
        if (req.ambulanceId() != null) {
            ambulance = ambulances.findById(req.ambulanceId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Ambulance not found"));
            if (!ambulance.getHospital().getId().equals(hospitalId)) {
                throw bad("Ambulance belongs to another hospital");
            }
            if (ambulance.getStatus() == Ambulance.Status.MAINTENANCE) {
                throw bad("Cannot link an ambulance under maintenance");
            }
        }

        Bed.BedType bedType = (req.requestedBedType() == null ||
            req.requestedBedType().isBlank()) ? null :
            enumValue(Bed.BedType.class, req.requestedBedType(), "bed type");

        EmergencyCase created = cases.save(new EmergencyCase(
            hospital, ambulance, name, req.age(), complaint, urgency, arrival,
            eta, bedType, optional(req.requestedSpecialization(), "Specialization", 100),
            Boolean.TRUE.equals(req.oxygenNeeded()),
            Boolean.TRUE.equals(req.ventilatorNeeded())
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    // In this prototype, an authenticated, clinically authorized staff member will
    // eventually be required to use this endpoint. Auth is NOT implemented yet.
    @PatchMapping("/{caseId}/priority")
    @Transactional
    public CaseResponse changePriority(@PathVariable Long hospitalId,
                                       @PathVariable Long caseId,
                                       @RequestBody PriorityRequest req) {
        EmergencyCase c = getCase(hospitalId, caseId);
        if (c.getCaseStatus() == EmergencyCase.CaseStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot change completed case");
        }
        c.changeUrgency(enumValue(EmergencyCase.Urgency.class, req.urgency(), "urgency"));
        return toResponse(c);
    }

    @PatchMapping("/{caseId}/arrive")
    @Transactional
    public CaseResponse confirmArrival(@PathVariable Long hospitalId,
                                       @PathVariable Long caseId) {
        EmergencyCase c = getCase(hospitalId, caseId);
        if (c.getArrivalStatus() != EmergencyCase.ArrivalStatus.PRE_ARRIVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Patient already arrived");
        }
        if (c.getCaseStatus() != EmergencyCase.CaseStatus.WAITING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Case is not waiting");
        }
        c.confirmArrival();
        // Ambulance dispatch / return is a separate staff-confirmed action.
        return toResponse(c);
    }

    public record RegisterCaseRequest(
        String patientName, Integer age, String chiefComplaint, String urgency,
        String arrivalStatus, String estimatedArrivalAt, Long ambulanceId,
        String requestedBedType, String requestedSpecialization,
        Boolean oxygenNeeded, Boolean ventilatorNeeded
    ) {}

    public record PriorityRequest(String urgency) {}

    public record CaseResponse(
        Long id, String patientName, Integer age, String chiefComplaint,
        String urgency, String arrivalStatus, String caseStatus,
        Instant registeredAt, Instant arrivedAt, Instant estimatedArrivalAt,
        Long ambulanceId, String ambulanceNumber,
        String requestedBedType, String requestedSpecialization,
        boolean oxygenNeeded, boolean ventilatorNeeded
    ) {}
}
