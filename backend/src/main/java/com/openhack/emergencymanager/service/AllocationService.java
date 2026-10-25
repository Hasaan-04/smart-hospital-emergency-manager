package com.openhack.emergencymanager.service;

import com.openhack.emergencymanager.model.*;
import com.openhack.emergencymanager.repository.*;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Session 6 (revised): staff choose from ALL resources; server validates the
 * complete set before immediately confirming. No new temporary holds are made.
 * Never use with real patient data before authentication and clinical review.
 */
@Service
public class AllocationService {

    private static final ZoneId HOSPITAL_ZONE = ZoneId.of("Asia/Karachi");

    private final EntityManager em;
    private final HospitalRepository hospitals;
    private final EmergencyCaseRepository cases;
    private final BedRepository beds;
    private final DoctorRepository doctors;
    private final EquipmentRepository equipment;
    private final AllocationHoldRepository holds;
    private final EquipmentEventRepository events;

    public AllocationService(EntityManager em,
                             HospitalRepository hospitals,
                             EmergencyCaseRepository cases,
                             BedRepository beds,
                             DoctorRepository doctors,
                             EquipmentRepository equipment,
                             AllocationHoldRepository holds,
                             EquipmentEventRepository events) {
        this.em = em;
        this.hospitals = hospitals;
        this.cases = cases;
        this.beds = beds;
        this.doctors = doctors;
        this.equipment = equipment;
        this.holds = holds;
        this.events = events;
    }

    private ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
    private ResponseStatusException conflict(String msg) {
        return new ResponseStatusException(HttpStatus.CONFLICT, msg);
    }

    /**
     * TiDB supports SELECT ... FOR UPDATE but not Hibernate's generated
     * FOR UPDATE OF <table_alias>. Lock by primary key with native SQL, then
     * refresh the managed entity to read its state after the lock is acquired.
     * Call only from a write transaction (@Transactional).
     */
    private <T> T findWithRowLock(Class<T> type, Long id) {
        if (id == null) return null;

        // Hard-coded whitelist: never interpolate a table name from user input.
        String table;
        if (type == EmergencyCase.class) table = "emergency_cases";
        else if (type == Bed.class) table = "hospital_beds";
        else if (type == Doctor.class) table = "doctors";
        else if (type == Equipment.class) table = "medical_equipment";
        else if (type == AllocationHold.class) table = "allocation_holds";
        else throw new IllegalArgumentException("Unsupported lock entity: " + type.getName());

        List<?> rows = em.createNativeQuery(
                "SELECT id FROM " + table + " WHERE id = :id FOR UPDATE")
            .setParameter("id", id)
            .getResultList();
        if (rows.isEmpty()) return null;

        T entity = em.find(type, id);
        if (entity != null) em.refresh(entity);
        return entity;
    }

    private EmergencyCase caseForHospital(Long hospitalId, Long caseId, boolean lock) {
        if (!hospitals.existsById(hospitalId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospital not found");
        EmergencyCase c = lock
            ? findWithRowLock(EmergencyCase.class, caseId)
            : cases.findById(caseId).orElse(null);
        if (c == null || !c.getHospital().getId().equals(hospitalId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found in this hospital");
        return c;
    }

    private <T> T lockResource(Class<T> type, Long id) {
        if (id == null) return null;
        T value = findWithRowLock(type, id);
        if (value == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Selected resource not found");
        return value;
    }

    private boolean needsBed(EmergencyCase c) {
        return c.getRequestedBedType() != null || c.isOxygenNeeded() || c.isVentilatorNeeded();
    }
    private boolean needsDoctor(EmergencyCase c) {
        return c.getRequestedSpecialization() != null && !c.getRequestedSpecialization().isBlank();
    }

    /**
     * Only well-known synonyms, not fuzzy text/substring matching.
     * Unrecognized specialties must match after punctuation/spacing normalization.
     */
    private String normalize(String input) {
        return input == null ? "" : input.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }
    private String specialty(String input) {
        String n = normalize(input);
        return switch (n) {
            case "cardiology", "cardiologist" -> "cardiology";
            case "pulmonology", "pulmonologist" -> "pulmonology";
            case "neurology", "neurologist" -> "neurology";
            case "orthopedics", "orthopaedics", "orthopedic surgeon", "orthopaedic surgeon" -> "orthopedics";
            case "pediatrics", "paediatrics", "pediatrician", "paediatrician" -> "pediatrics";
            case "anesthesiology", "anaesthesiology", "anesthesiologist", "anaesthesiologist" -> "anesthesiology";
            case "emergency medicine", "emergency physician" -> "emergency medicine";
            case "internal medicine", "general medicine", "internist", "general physician" -> "internal medicine";
            default -> n;
        };
    }
    private boolean withinShift(Doctor d) {
        LocalTime now = LocalTime.now(HOSPITAL_ZONE);
        LocalTime start = d.getShiftStart();
        LocalTime end = d.getShiftEnd();
        if (start.equals(end)) return true; // Existing prototype convention: 24-hour shift.
        if (start.isBefore(end)) return !now.isBefore(start) && now.isBefore(end);
        return !now.isBefore(start) || now.isBefore(end); // Overnight.
    }
    private boolean ventilator(Equipment e) {
        String name = normalize(e.getCategory());
        return name.matches(".*\\bventilators?\\b.*");
    }

    private List<AllocationHold> liveLegacyHolds() {
        return holds.findByStatusAndExpiresAtAfter(AllocationHold.Status.HELD, Instant.now());
    }
    private boolean heldBed(Long id, List<AllocationHold> active) {
        return active.stream().anyMatch(h -> h.getBed() != null && id.equals(h.getBed().getId()));
    }
    private boolean heldDoctor(Long id, List<AllocationHold> active) {
        return active.stream().anyMatch(h -> h.getDoctor() != null && id.equals(h.getDoctor().getId()));
    }
    private boolean heldEquipment(Long id, List<AllocationHold> active) {
        return active.stream().anyMatch(h -> h.getEquipment() != null && id.equals(h.getEquipment().getId()));
    }

    /** All known resources are returned, with eligibility and human-readable reasons. */
    @Transactional(readOnly = true)
    public ResourceCatalog catalog(Long hospitalId, Long caseId) {
        EmergencyCase c = caseForHospital(hospitalId, caseId, false);
        if (c.getCaseStatus() != EmergencyCase.CaseStatus.WAITING)
            throw conflict("Only WAITING cases may receive new allocations");

        List<AllocationHold> active = liveLegacyHolds();
        List<ResourceOption> bedOptions = new ArrayList<>();
        for (Bed b : beds.findByHospital(hospitalId)) {
            List<String> reasons = new ArrayList<>();
            if (b.getStatus() != Bed.BedStatus.AVAILABLE)
                reasons.add("Bed status: " + b.getStatus());
            if (heldBed(b.getId(), active)) reasons.add("Reserved by an earlier hold");
            if (c.getRequestedBedType() != null && b.getType() != c.getRequestedBedType())
                reasons.add("Needs " + c.getRequestedBedType() + " bed, not " + b.getType());
            if (c.isOxygenNeeded() && !b.isOxygenSupport())
                reasons.add("Required oxygen connection is missing");
            if (c.isVentilatorNeeded() && !b.isVentilatorCompatible())
                reasons.add("Bed is not ventilator-compatible");
            String label = b.getCode() + " — " + b.getType() + " / "
                + b.getRoom().getDepartment().getName() + " / " + b.getRoom().getNumber();
            bedOptions.add(new ResourceOption(b.getId(), label, reasons.isEmpty(), reasons,
                b.getRoom().getDepartment().getId()));
        }

        List<ResourceOption> doctorOptions = new ArrayList<>();
        for (Doctor d : doctors.findByHospital_IdOrderByIdAsc(hospitalId)) {
            List<String> reasons = new ArrayList<>();
            if (d.getStatus() != Doctor.Status.AVAILABLE)
                reasons.add("Doctor status: " + d.getStatus());
            if (heldDoctor(d.getId(), active)) reasons.add("Reserved by an earlier hold");
            if (needsDoctor(c) && !specialty(d.getSpecialization()).equals(
                    specialty(c.getRequestedSpecialization())))
                reasons.add("Specialty mismatch: needs " + c.getRequestedSpecialization());
            if (!withinShift(d)) reasons.add("Outside recorded working shift (Pakistan time)");
            String label = d.getFullName() + " — " + d.getSpecialization()
                + " / " + d.getDepartment().getName();
            doctorOptions.add(new ResourceOption(d.getId(), label, reasons.isEmpty(), reasons,
                d.getDepartment().getId()));
        }

        List<ResourceOption> equipmentOptions = new ArrayList<>();
        for (Equipment e : equipment.findByHospital_IdOrderByIdAsc(hospitalId)) {
            List<String> reasons = new ArrayList<>();
            if (e.getStatus() != Equipment.Status.AVAILABLE)
                reasons.add("Equipment status: " + e.getStatus());
            if (heldEquipment(e.getId(), active)) reasons.add("Reserved by an earlier hold");
            if (!ventilator(e)) reasons.add("This equipment is not a ventilator");
            String label = e.getAssetCode() + " — " + e.getName() + " / " + e.getDepartment().getName();
            equipmentOptions.add(new ResourceOption(e.getId(), label, reasons.isEmpty(), reasons,
                e.getDepartment().getId()));
        }

        return new ResourceCatalog(c.getId(), c.getPatientName(), c.getUrgency().name(),
            c.getArrivalStatus().name(), c.getRequestedBedType() == null ? null : c.getRequestedBedType().name(),
            c.getRequestedSpecialization(), c.isOxygenNeeded(), c.isVentilatorNeeded(),
            needsBed(c), needsDoctor(c), c.isVentilatorNeeded(),
            c.getArrivalStatus() == EmergencyCase.ArrivalStatus.ARRIVED,
            bedOptions, doctorOptions, equipmentOptions);
    }

    private void validateBed(Bed b, EmergencyCase c) {
        if (b.getStatus() != Bed.BedStatus.AVAILABLE) throw conflict("Bed is not available");
        if (c.getRequestedBedType() != null && b.getType() != c.getRequestedBedType())
            throw conflict("Incorrect bed type for this case");
        if (c.isOxygenNeeded() && !b.isOxygenSupport())
            throw conflict("Selected bed lacks required oxygen connection");
        if (c.isVentilatorNeeded() && !b.isVentilatorCompatible())
            throw conflict("Selected bed is not ventilator-compatible");
    }
    private void validateDoctor(Doctor d, EmergencyCase c) {
        if (d.getStatus() != Doctor.Status.AVAILABLE) throw conflict("Doctor is not available");
        if (!withinShift(d)) throw conflict("Doctor is outside their scheduled shift");
        if (needsDoctor(c) && !specialty(d.getSpecialization()).equals(specialty(c.getRequestedSpecialization())))
            throw conflict("Doctor specialization does not match the requested specialty");
    }
    private void validateEquipment(Equipment e, Bed b) {
        if (e.getStatus() != Equipment.Status.AVAILABLE) throw conflict("Equipment is not available");
        if (!ventilator(e)) throw conflict("Selected equipment is not a ventilator");
        if (b == null || !e.getDepartment().getId().equals(b.getRoom().getDepartment().getId()))
            throw conflict("Ventilator must be in the selected bed's department; move it through Equipment Management first");
    }

    /** Direct staff confirmation: the entire operation is one database transaction. */
    @Transactional
    public AllocationView confirm(Long hospitalId, ConfirmRequest request) {
        if (request == null || request.caseId() == null)
            throw bad("Select an emergency case");
        EmergencyCase c = caseForHospital(hospitalId, request.caseId(), true);
        if (c.getCaseStatus() != EmergencyCase.CaseStatus.WAITING)
            throw conflict("Case is no longer waiting");
        if (c.getArrivalStatus() != EmergencyCase.ArrivalStatus.ARRIVED)
            throw conflict("Patient must arrive before allocation can be confirmed");

        boolean bedRequired = needsBed(c);
        boolean doctorRequired = needsDoctor(c);
        boolean equipmentRequired = c.isVentilatorNeeded();
        if (!bedRequired && !doctorRequired && !equipmentRequired)
            throw bad("This case has no resource requirements. Update or register its requirements before allocation.");
        if (bedRequired != (request.bedId() != null))
            throw bad(bedRequired ? "Select a bed" : "This case does not request a bed");
        if (doctorRequired != (request.doctorId() != null))
            throw bad(doctorRequired ? "Select a doctor" : "This case does not request a doctor");
        if (equipmentRequired != (request.equipmentId() != null))
            throw bad(equipmentRequired ? "Select a ventilator" : "This case does not request equipment");

        // Lock the same entity order on every confirmation: case -> bed -> doctor -> equipment.
        // This makes concurrent claims of one resource wait and recheck its current status.
        Bed b = lockResource(Bed.class, request.bedId());
        Doctor d = lockResource(Doctor.class, request.doctorId());
        Equipment e = lockResource(Equipment.class, request.equipmentId());

        if (b != null && !b.getRoom().getBuilding().getHospital().getId().equals(hospitalId))
            throw bad("Bed belongs to another hospital");
        if (d != null && !d.getHospital().getId().equals(hospitalId))
            throw bad("Doctor belongs to another hospital");
        if (e != null && !e.getHospital().getId().equals(hospitalId))
            throw bad("Equipment belongs to another hospital");

        // Legacy two-minute holds from the previous Session 6 are respected until expiry.
        List<AllocationHold> active = liveLegacyHolds();
        if (b != null && heldBed(b.getId(), active)) throw conflict("Bed is temporarily held");
        if (d != null && heldDoctor(d.getId(), active)) throw conflict("Doctor is temporarily held");
        if (e != null && heldEquipment(e.getId(), active)) throw conflict("Equipment is temporarily held");
        if (active.stream().anyMatch(h -> h.getEmergencyCase().getId().equals(c.getId())))
            throw conflict("Cancel the patient's previous temporary hold first");

        if (b != null) validateBed(b, c);
        if (d != null) validateDoctor(d, c);
        if (e != null) validateEquipment(e, b);

        if (b != null) b.setStatus(Bed.BedStatus.OCCUPIED);
        if (d != null) d.setStatus(Doctor.Status.BUSY);
        if (e != null) {
            e.setStatus(Equipment.Status.IN_USE);
            events.save(new EquipmentEvent(e, "ALLOCATED", "Assigned to case #" + c.getId()));
        }
        c.startTreatment();

        // Reuse the existing table and history rather than migrate or drop any data.
        // expires_at is still mandatory in the legacy table, but no hold is created.
        AllocationHold allocation = new AllocationHold(c, b, d, e, Instant.now());
        allocation.approve();
        holds.save(allocation);
        return toView(allocation);
    }

    @Transactional(readOnly = true)
    public List<AllocationView> list(Long hospitalId) {
        if (!hospitals.existsById(hospitalId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospital not found");
        return holds.findByEmergencyCase_Hospital_IdOrderByIdDesc(hospitalId)
            .stream().map(this::toView).toList();
    }

    private AllocationHold lockedAllocation(Long hospitalId, Long allocationId) {
        // First discover the case, then lock case -> allocation, consistent with confirm().
        AllocationHold first = holds.findById(allocationId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Allocation not found"));
        EmergencyCase c = caseForHospital(hospitalId, first.getEmergencyCase().getId(), true);
        AllocationHold locked = findWithRowLock(AllocationHold.class, allocationId);
        if (locked == null || !locked.getEmergencyCase().getId().equals(c.getId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Allocation not found in this hospital");
        return locked;
    }

    @Transactional
    public AllocationView complete(Long hospitalId, Long allocationId) {
        AllocationHold a = lockedAllocation(hospitalId, allocationId);
        if (a.getStatus() != AllocationHold.Status.APPROVED)
            throw conflict("Only confirmed active allocations can be completed");
        if (a.getEmergencyCase().getCaseStatus() != EmergencyCase.CaseStatus.UNDER_TREATMENT)
            throw conflict("Case is not under treatment");
        Bed b = a.getBed() == null ? null : lockResource(Bed.class, a.getBed().getId());
        Doctor d = a.getDoctor() == null ? null : lockResource(Doctor.class, a.getDoctor().getId());
        Equipment e = a.getEquipment() == null ? null : lockResource(Equipment.class, a.getEquipment().getId());

        if (b != null && b.getStatus() != Bed.BedStatus.OCCUPIED)
            throw conflict("Bed status changed unexpectedly; review before releasing");
        if (d != null && d.getStatus() != Doctor.Status.BUSY)
            throw conflict("Doctor status changed unexpectedly; review before releasing");
        if (e != null && e.getStatus() != Equipment.Status.IN_USE)
            throw conflict("Equipment status changed unexpectedly; review before releasing");
        if (b != null) b.setStatus(Bed.BedStatus.AVAILABLE);
        if (d != null) d.setStatus(Doctor.Status.AVAILABLE);
        if (e != null) {
            e.setStatus(Equipment.Status.AVAILABLE);
            events.save(new EquipmentEvent(e, "RELEASED", "Released from case #" + a.getEmergencyCase().getId()));
        }
        a.getEmergencyCase().completeTreatment();
        a.release();
        return toView(a);
    }

    /** Cancels ONLY legacy unapproved holds left from the earlier Session 6. */
    @Transactional
    public AllocationView cancel(Long hospitalId, Long allocationId) {
        AllocationHold a = lockedAllocation(hospitalId, allocationId);
        if (a.getStatus() != AllocationHold.Status.HELD)
            throw conflict("Only unapproved legacy holds can be cancelled");
        if (!a.getExpiresAt().isAfter(Instant.now())) a.expire();
        else a.cancel();
        return toView(a);
    }

    /** Kept for compatibility with the existing 15-second AllocationScheduler. */
    @Transactional
    public void expireOldHolds() {
        List<AllocationHold> stale = holds.findByStatusAndExpiresAtLessThanEqual(
            AllocationHold.Status.HELD, Instant.now());
        stale.sort(Comparator.comparing(h -> h.getEmergencyCase().getId()));
        for (AllocationHold candidate : stale) {
            AllocationHold a = lockedAllocation(
                candidate.getEmergencyCase().getHospital().getId(), candidate.getId());
            if (a.getStatus() == AllocationHold.Status.HELD &&
                !a.getExpiresAt().isAfter(Instant.now())) a.expire();
        }
    }

    private AllocationView toView(AllocationHold a) {
        return new AllocationView(a.getId(), a.getEmergencyCase().getId(),
            a.getEmergencyCase().getPatientName(), a.getEmergencyCase().getCaseStatus().name(),
            a.getStatus().name(),
            a.getBed() == null ? null : a.getBed().getCode(),
            a.getDoctor() == null ? null : a.getDoctor().getFullName(),
            a.getEquipment() == null ? null : a.getEquipment().getAssetCode(),
            a.getCreatedAt(), a.getApprovedAt(), a.getFinishedAt(), a.getExpiresAt());
    }

    public record ResourceOption(Long id, String label, boolean eligible,
                                 List<String> reasons, Long departmentId) {}
    public record ResourceCatalog(Long caseId, String patientName, String urgency,
                                  String arrivalStatus, String requestedBedType,
                                  String requestedSpecialization, boolean oxygenNeeded,
                                  boolean ventilatorNeeded, boolean needsBed,
                                  boolean needsDoctor, boolean needsVentilator,
                                  boolean canConfirm, List<ResourceOption> beds,
                                  List<ResourceOption> doctors, List<ResourceOption> equipment) {}
    public record ConfirmRequest(Long caseId, Long bedId, Long doctorId, Long equipmentId) {}
    public record AllocationView(Long id, Long caseId, String patientName,
                                 String caseStatus, String status, String bedCode,
                                 String doctorName, String equipmentCode,
                                 Instant createdAt, Instant approvedAt,
                                 Instant finishedAt, Instant expiresAt) {}
}
