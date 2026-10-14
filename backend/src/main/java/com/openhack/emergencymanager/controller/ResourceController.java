package com.openhack.emergencymanager.controller;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
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

import com.openhack.emergencymanager.model.Ambulance;
import com.openhack.emergencymanager.model.Department;
import com.openhack.emergencymanager.model.Doctor;
import com.openhack.emergencymanager.model.Equipment;
import com.openhack.emergencymanager.model.EquipmentEvent;
import com.openhack.emergencymanager.model.Hospital;
import com.openhack.emergencymanager.model.Room;
import com.openhack.emergencymanager.repository.AmbulanceRepository;
import com.openhack.emergencymanager.repository.DepartmentRepository;
import com.openhack.emergencymanager.repository.DoctorRepository;
import com.openhack.emergencymanager.repository.EquipmentEventRepository;
import com.openhack.emergencymanager.repository.EquipmentRepository;
import com.openhack.emergencymanager.repository.HospitalRepository;
import com.openhack.emergencymanager.repository.RoomRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@RestController
@RequestMapping("/api/hospitals/{hospitalId}/resources")
public class ResourceController {
    private final HospitalRepository hospitals;
    private final DepartmentRepository departments;
    private final RoomRepository rooms;
    private final DoctorRepository doctors;
    private final AmbulanceRepository ambulances;
    private final EquipmentRepository equipment;
    private final EquipmentEventRepository events;
    private final EntityManager entityManager;

    public ResourceController(HospitalRepository hospitals,
                              DepartmentRepository departments,
                              RoomRepository rooms,
                              DoctorRepository doctors,
                              AmbulanceRepository ambulances,
                              EquipmentRepository equipment,
                              EquipmentEventRepository events,
                              EntityManager entityManager) {
        this.hospitals = hospitals;
        this.departments = departments;
        this.rooms = rooms;
        this.doctors = doctors;
        this.ambulances = ambulances;
        this.equipment = equipment;
        this.events = events;
        this.entityManager = entityManager;
    }

    // ------- Helpers: validation and hospital isolation -------
    private Hospital hospital(Long hospitalId) {
        return hospitals.findById(hospitalId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospital not found"));
    }

    private Department department(Long hospitalId, Long departmentId) {
        if (departmentId == null) throw bad("Department is required");
        Department d = departments.findById(departmentId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
        if (!d.getHospitalId().equals(hospitalId)) throw bad("Wrong hospital for department");
        return d;
    }

    private Room room(Long hospitalId, Long roomId, Department expectedDepartment) {
        if (roomId == null) return null;
        Room r = rooms.findById(roomId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        if (!r.getBuilding().getHospital().getId().equals(hospitalId))
            throw bad("Wrong hospital for room");
        if (!r.getDepartment().getId().equals(expectedDepartment.getId()))
            throw bad("The room must belong to the selected department");
        return r;
    }

    private Doctor doctor(Long hospitalId, Long id) {
        Doctor d = doctors.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));
        if (!d.getHospital().getId().equals(hospitalId)) throw bad("Wrong hospital");
        return d;
    }

    private Ambulance ambulance(Long hospitalId, Long id) {
        Ambulance a = ambulances.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Ambulance not found"));
        if (!a.getHospital().getId().equals(hospitalId)) throw bad("Wrong hospital");
        return a;
    }

    private Equipment item(Long hospitalId, Long id) {
        Equipment e = equipment.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found"));
        if (!e.getHospital().getId().equals(hospitalId)) throw bad("Wrong hospital");
        return e;
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) throw bad(field + " is required");
        return value.trim();
    }

    private LocalTime parseTime(String value) {
        try { return LocalTime.parse(required(value, "Shift time")); }
        catch (DateTimeParseException ex) { throw bad("Shift time must be HH:mm"); }
    }

    private String status(String value) {
        return required(value, "Status").toUpperCase(Locale.ROOT);
    }

    // ------- Doctors -------
    @GetMapping("/doctors")
    @Transactional(readOnly = true)
    public List<DoctorView> listDoctors(@PathVariable Long hospitalId) {
        hospital(hospitalId);
        return doctors.findByHospital_IdOrderByIdAsc(hospitalId)
            .stream().map(this::doctorView).toList();
    }

    @PostMapping("/doctors")
    @Transactional
    public ResponseEntity<DoctorView> addDoctor(@PathVariable Long hospitalId,
                                                @RequestBody DoctorRequest req) {
        Hospital h = hospital(hospitalId);
        Department d = department(hospitalId, req.departmentId());
        String code = required(req.employeeCode(), "Employee code");
        if (doctors.existsByHospital_IdAndEmployeeCodeIgnoreCase(hospitalId, code))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee code already exists");
        Doctor doctor = doctors.save(new Doctor(h, d, code,
            required(req.fullName(), "Name"), required(req.specialization(), "Specialization"),
            parseTime(req.shiftStart()), parseTime(req.shiftEnd())));
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorView(doctor));
    }

    @PatchMapping("/doctors/{id}/status")
    @Transactional
    public DoctorView doctorStatus(@PathVariable Long hospitalId, @PathVariable Long id,
                                   @RequestBody StatusRequest req) {
        Doctor d = doctor(hospitalId, id);
        entityManager.refresh(d, LockModeType.PESSIMISTIC_WRITE);
        if (d.getStatus() == Doctor.Status.BUSY) throw new ResponseStatusException(
            HttpStatus.CONFLICT, "A busy doctor cannot be changed here");
        String value = status(req.status());
        if (!value.equals("AVAILABLE") && !value.equals("OFF_DUTY"))
            throw bad("Allowed statuses: AVAILABLE, OFF_DUTY");
        d.setStatus(Doctor.Status.valueOf(value));
        return doctorView(d);
    }

    private DoctorView doctorView(Doctor d) {
        return new DoctorView(d.getId(), d.getEmployeeCode(), d.getFullName(),
            d.getSpecialization(), d.getDepartment().getName(),
            d.getShiftStart().toString(), d.getShiftEnd().toString(), d.getStatus().name());
    }

    // ------- Ambulances -------
    @GetMapping("/ambulances")
    @Transactional(readOnly = true)
    public List<AmbulanceView> listAmbulances(@PathVariable Long hospitalId) {
        hospital(hospitalId);
        return ambulances.findByHospital_IdOrderByIdAsc(hospitalId)
            .stream().map(this::ambulanceView).toList();
    }

    @PostMapping("/ambulances")
    @Transactional
    public ResponseEntity<AmbulanceView> addAmbulance(@PathVariable Long hospitalId,
                                                      @RequestBody AmbulanceRequest req) {
        Hospital h = hospital(hospitalId);
        String number = required(req.vehicleNumber(), "Vehicle number");
        if (ambulances.existsByHospital_IdAndVehicleNumberIgnoreCase(hospitalId, number))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vehicle already registered");
        Ambulance saved = ambulances.save(new Ambulance(h, number,
            required(req.driverName(), "Driver name")));
        return ResponseEntity.status(HttpStatus.CREATED).body(ambulanceView(saved));
    }

    @PatchMapping("/ambulances/{id}/status")
    @Transactional
    public AmbulanceView ambulanceStatus(@PathVariable Long hospitalId, @PathVariable Long id,
                                         @RequestBody StatusRequest req) {
        Ambulance a = ambulance(hospitalId, id);
        String value = status(req.status());
        try { a.setStatus(Ambulance.Status.valueOf(value)); }
        catch (IllegalArgumentException ex) { throw bad("Invalid ambulance status"); }
        return ambulanceView(a);
    }

    private AmbulanceView ambulanceView(Ambulance a) {
        return new AmbulanceView(a.getId(), a.getVehicleNumber(), a.getDriverName(),
            a.getStatus().name());
    }

    // ------- Equipment -------
    @GetMapping("/equipment")
    @Transactional(readOnly = true)
    public List<EquipmentView> listEquipment(@PathVariable Long hospitalId) {
        hospital(hospitalId);
        return equipment.findByHospital_IdOrderByIdAsc(hospitalId)
            .stream().map(this::equipmentView).toList();
    }

    @PostMapping("/equipment")
    @Transactional
    public ResponseEntity<EquipmentView> addEquipment(@PathVariable Long hospitalId,
                                                      @RequestBody EquipmentRequest req) {
        Hospital h = hospital(hospitalId);
        Department d = department(hospitalId, req.departmentId());
        Room r = room(hospitalId, req.roomId(), d);
        String code = required(req.assetCode(), "Asset code");
        if (equipment.existsByHospital_IdAndAssetCodeIgnoreCase(hospitalId, code))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Asset code already exists");
        Equipment saved = equipment.save(new Equipment(h, d, r, code,
            required(req.name(), "Equipment name"), required(req.category(), "Category")));
        events.save(new EquipmentEvent(saved, "REGISTERED", "Added to " + d.getName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(equipmentView(saved));
    }

    @PatchMapping("/equipment/{id}/status")
    @Transactional
    public EquipmentView equipmentStatus(@PathVariable Long hospitalId, @PathVariable Long id,
                                         @RequestBody StatusRequest req) {
        Equipment e = item(hospitalId, id);
        entityManager.refresh(e, LockModeType.PESSIMISTIC_WRITE);
        if (e.getStatus() == Equipment.Status.IN_USE) throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Equipment in use cannot be changed manually");
        String value = status(req.status());
        if (!value.equals("AVAILABLE") && !value.equals("MAINTENANCE"))
            throw bad("Allowed statuses: AVAILABLE, MAINTENANCE");
        String old = e.getStatus().name();
        e.setStatus(Equipment.Status.valueOf(value));
        if (!old.equals(value))
            events.save(new EquipmentEvent(e, "STATUS_CHANGE", old + " -> " + value));
        return equipmentView(e);
    }

    @PatchMapping("/equipment/{id}/move")
    @Transactional
    public EquipmentView moveEquipment(@PathVariable Long hospitalId, @PathVariable Long id,
                                       @RequestBody MoveRequest req) {
        Equipment e = item(hospitalId, id);
        entityManager.refresh(e, LockModeType.PESSIMISTIC_WRITE);
        if (e.getStatus() == Equipment.Status.IN_USE) throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Cannot move equipment while in use");
        Department d = department(hospitalId, req.departmentId());
        Room r = room(hospitalId, req.roomId(), d);
        String previous = e.getDepartment().getName() +
            (e.getRoom() == null ? "" : "/" + e.getRoom().getNumber());
        e.moveTo(d, r);
        String next = d.getName() + (r == null ? "" : "/" + r.getNumber());
        if (!previous.equals(next)) {
            String note = req.note() == null ? "" : req.note().trim();
            if (note.length() > 150) throw bad("Note must be 150 characters or fewer");
            events.save(new EquipmentEvent(e, "MOVED", previous + " -> " + next +
                (note.isEmpty() ? "" : " (" + note + ")")));
        }
        return equipmentView(e);
    }

    @GetMapping("/equipment/{id}/history")
    @Transactional(readOnly = true)
    public List<EventView> history(@PathVariable Long hospitalId, @PathVariable Long id) {
        item(hospitalId, id);
        return events.findByEquipment_IdOrderByOccurredAtDescIdDesc(id)
            .stream().map(ev -> new EventView(ev.getId(), ev.getEventType(),
                ev.getDescription(), ev.getOccurredAt().toString())).toList();
    }

    private EquipmentView equipmentView(Equipment e) {
        return new EquipmentView(e.getId(), e.getAssetCode(), e.getName(),
            e.getCategory(), e.getStatus().name(), e.getDepartment().getId(),
            e.getDepartment().getName(),
            e.getRoom() == null ? null : e.getRoom().getId(),
            e.getRoom() == null ? null : e.getRoom().getNumber());
    }

    public record StatusRequest(String status) {}
    public record DoctorRequest(String employeeCode, String fullName, String specialization,
                                Long departmentId, String shiftStart, String shiftEnd) {}
    public record DoctorView(Long id, String employeeCode, String fullName,
                             String specialization, String departmentName,
                             String shiftStart, String shiftEnd, String status) {}
    public record AmbulanceRequest(String vehicleNumber, String driverName) {}
    public record AmbulanceView(Long id, String vehicleNumber, String driverName, String status) {}
    public record EquipmentRequest(String assetCode, String name, String category,
                                   Long departmentId, Long roomId) {}
    public record EquipmentView(Long id, String assetCode, String name, String category,
                                String status, Long departmentId, String departmentName,
                                Long roomId, String roomNumber) {}
    public record MoveRequest(Long departmentId, Long roomId, String note) {}
    public record EventView(Long id, String eventType, String description, String occurredAt) {}
}
