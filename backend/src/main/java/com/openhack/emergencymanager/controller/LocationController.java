
package com.openhack.emergencymanager.controller;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.openhack.emergencymanager.model.Bed;
import com.openhack.emergencymanager.model.Building;
import com.openhack.emergencymanager.model.Department;
import com.openhack.emergencymanager.model.Hospital;
import com.openhack.emergencymanager.model.Room;
import com.openhack.emergencymanager.repository.BedRepository;
import com.openhack.emergencymanager.repository.BuildingRepository;
import com.openhack.emergencymanager.repository.DepartmentRepository;
import com.openhack.emergencymanager.repository.HospitalRepository;
import com.openhack.emergencymanager.repository.RoomRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@RestController
@RequestMapping("/api/hospitals/{hospitalId}")
public class LocationController {

    private final HospitalRepository hospitals;
    private final DepartmentRepository departments;
    private final BuildingRepository buildings;
    private final RoomRepository rooms;
    private final BedRepository beds;
    private final EntityManager entityManager;

    public LocationController(
            HospitalRepository hospitals,
            DepartmentRepository departments,
            BuildingRepository buildings,
            RoomRepository rooms,
            BedRepository beds,
            EntityManager entityManager) {

        this.hospitals = hospitals;
        this.departments = departments;
        this.buildings = buildings;
        this.rooms = rooms;
        this.beds = beds;
        this.entityManager = entityManager;
    }

    // ---------- VALIDATION HELPERS ----------

    private Hospital getHospital(Long id) {
        return hospitals.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Hospital not found"
            ));
    }

    private Building getBuilding(Long id, Long hospitalId) {
        Building b = buildings.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Building not found"
            ));

        if (!b.getHospital().getId().equals(hospitalId)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Building does not belong to this hospital"
            );
        }

        return b;
    }

    private Room getRoom(Long id, Long hospitalId) {
        Room r = rooms.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Room not found"
            ));

        if (!r.getBuilding().getHospital().getId()
                .equals(hospitalId)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Room does not belong to this hospital"
            );
        }

        return r;
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                field + " is required"
            );
        }

        return value.trim();
    }

    // ---------- BUILDINGS ----------

    @GetMapping("/buildings")
    public List<BuildingResponse> getBuildings(
            @PathVariable Long hospitalId) {

        getHospital(hospitalId);

        return buildings
            .findByHospital_IdOrderByNameAsc(hospitalId)
            .stream()
            .map(b -> new BuildingResponse(
                b.getId(), b.getName()
            ))
            .toList();
    }

    @PostMapping("/buildings")
    public ResponseEntity<BuildingResponse> addBuilding(
            @PathVariable Long hospitalId,
            @RequestBody BuildingRequest request) {

        Hospital hospital = getHospital(hospitalId);
        String name = required(request.name(), "Building name");

        if (buildings.existsByHospital_IdAndNameIgnoreCase(
                hospitalId, name)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Building already exists"
            );
        }

        Building saved = buildings.save(
            new Building(name, hospital)
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new BuildingResponse(
                saved.getId(), saved.getName()
            ));
    }

    // ---------- ROOMS ----------

    @GetMapping("/rooms")
    @Transactional(readOnly = true)
    public List<RoomResponse> getRooms(
            @PathVariable Long hospitalId) {

        getHospital(hospitalId);

        return rooms.findByHospital(hospitalId)
            .stream()
            .map(this::toRoomResponse)
            .toList();
    }

    @PostMapping("/rooms")
    public ResponseEntity<RoomResponse> addRoom(
            @PathVariable Long hospitalId,
            @RequestBody RoomRequest request) {

        getHospital(hospitalId);

        if (request.buildingId() == null ||
            request.departmentId() == null ||
            request.floorNumber() == null ||
            request.floorNumber() < 0) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Valid building, department and floor required"
            );
        }

        Building building = getBuilding(
            request.buildingId(), hospitalId
        );

        Department department = departments
            .findById(request.departmentId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Department not found"
            ));

        if (!department.getHospitalId().equals(hospitalId)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Department belongs to another hospital"
            );
        }

        String number = required(
            request.number(), "Room number"
        );

        if (rooms.existsByBuilding_IdAndFloorNumberAndNumberIgnoreCase(
                building.getId(),
                request.floorNumber(),
                number)) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Room already exists on this floor"
            );
        }

        Room saved = rooms.save(
            new Room(
                number,
                request.floorNumber(),
                building,
                department
            )
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(toRoomResponse(saved));
    }

    private RoomResponse toRoomResponse(Room r) {
        return new RoomResponse(
            r.getId(),
            r.getNumber(),
            r.getFloorNumber(),
            r.getBuilding().getId(),
            r.getBuilding().getName(),
            r.getDepartment().getId(),
            r.getDepartment().getName()
        );
    }

    // ---------- BEDS ----------

    @GetMapping("/beds")
    @Transactional(readOnly = true)
    public List<BedResponse> getBeds(
            @PathVariable Long hospitalId) {

        getHospital(hospitalId);

        return beds.findByHospital(hospitalId)
            .stream()
            .map(this::toBedResponse)
            .toList();
    }

    @PostMapping("/beds")
    public ResponseEntity<BedResponse> addBed(
            @PathVariable Long hospitalId,
            @RequestBody BedRequest request) {

        getHospital(hospitalId);

        if (request.roomId() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Room is required"
            );
        }

        Room room = getRoom(
            request.roomId(), hospitalId
        );

        String code = required(
            request.code(), "Bed code"
        );

        Bed.BedType type;

        try {
            type = Bed.BedType.valueOf(
                required(request.type(), "Bed type")
                    .toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid bed type"
            );
        }

        if (beds.existsByRoom_IdAndCodeIgnoreCase(
                room.getId(), code)) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Bed code already exists in this room"
            );
        }

        Bed saved = beds.save(
            new Bed(
                code,
                type,
                room,
                request.oxygenSupport(),
                request.ventilatorCompatible()
            )
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(toBedResponse(saved));
    }

    // Only availability / maintenance toggles for now.
    // Occupied status will be managed by allocations later.

    @PatchMapping("/beds/{bedId}/status")
    @Transactional
    public BedResponse updateBedStatus(
            @PathVariable Long hospitalId,
            @PathVariable Long bedId,
            @RequestBody StatusRequest request) {

        Bed bed = beds.findById(bedId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Bed not found"
            ));

        getRoom(bed.getRoom().getId(), hospitalId);

        // Refresh under the same row lock used by AllocationService.
        // A concurrent confirmation/completion cannot race this status change.
        entityManager.refresh(bed, LockModeType.PESSIMISTIC_WRITE);

        String status = required(
            request.status(), "Bed status"
        );

        if (!status.equals("AVAILABLE") &&
            !status.equals("MAINTENANCE")) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Only AVAILABLE or MAINTENANCE allowed here"
            );
        }

        if (bed.getStatus() == Bed.BedStatus.OCCUPIED) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "An occupied bed cannot be changed manually"
            );
        }

        bed.setStatus(Bed.BedStatus.valueOf(status));

        return toBedResponse(bed);
    }

    private BedResponse toBedResponse(Bed b) {
        Room r = b.getRoom();

        return new BedResponse(
            b.getId(),
            b.getCode(),
            b.getType().name(),
            b.getStatus().name(),
            b.isOxygenSupport(),
            b.isVentilatorCompatible(),
            r.getId(),
            r.getNumber(),
            r.getFloorNumber(),
            r.getBuilding().getName(),
            r.getDepartment().getName()
        );
    }

    // ---------- REQUEST / RESPONSE TYPES ----------

    public record BuildingRequest(String name) {}

    public record BuildingResponse(
        Long id,
        String name
    ) {}

    public record RoomRequest(
        Long buildingId,
        Long departmentId,
        Integer floorNumber,
        String number
    ) {}

    public record RoomResponse(
        Long id,
        String number,
        int floorNumber,
        Long buildingId,
        String buildingName,
        Long departmentId,
        String departmentName
    ) {}

    public record BedRequest(
        Long roomId,
        String code,
        String type,
        boolean oxygenSupport,
        boolean ventilatorCompatible
    ) {}

    public record StatusRequest(String status) {}

    public record BedResponse(
        Long id,
        String code,
        String type,
        String status,
        boolean oxygenSupport,
        boolean ventilatorCompatible,
        Long roomId,
        String roomNumber,
        int floorNumber,
        String buildingName,
        String departmentName
    ) {}
}
