package com.openhack.emergencymanager.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.openhack.emergencymanager.model.AllocationHold;
import com.openhack.emergencymanager.model.Ambulance;
import com.openhack.emergencymanager.model.Bed;
import com.openhack.emergencymanager.model.Doctor;
import com.openhack.emergencymanager.model.EmergencyCase;
import com.openhack.emergencymanager.model.Equipment;
import com.openhack.emergencymanager.model.Hospital;
import com.openhack.emergencymanager.repository.AllocationHoldRepository;
import com.openhack.emergencymanager.repository.AmbulanceRepository;
import com.openhack.emergencymanager.repository.BedRepository;
import com.openhack.emergencymanager.repository.DoctorRepository;
import com.openhack.emergencymanager.repository.EmergencyCaseRepository;
import com.openhack.emergencymanager.repository.EquipmentRepository;
import com.openhack.emergencymanager.repository.HospitalRepository;
import com.openhack.emergencymanager.service.AllocationService.ResourceCatalog;
import com.openhack.emergencymanager.service.AllocationService.ResourceOption;

/**
 * Session 7: Read-only operational dashboard, based on the working manual
 * allocation rules from Session 6. No new database tables or schema changes.
 * All data is fictional for development/demo purposes.
 */
@Service
public class DashboardService {

    private static final ZoneId HOSPITAL_ZONE = ZoneId.of("Asia/Karachi");

    private final HospitalRepository hospitals;
    private final EmergencyCaseRepository cases;
    private final BedRepository beds;
    private final DoctorRepository doctors;
    private final AmbulanceRepository ambulances;
    private final EquipmentRepository equipment;
    private final AllocationHoldRepository holds;
    private final AllocationService allocationService;

    public DashboardService(HospitalRepository hospitals,
                            EmergencyCaseRepository cases,
                            BedRepository beds,
                            DoctorRepository doctors,
                            AmbulanceRepository ambulances,
                            EquipmentRepository equipment,
                            AllocationHoldRepository holds,
                            AllocationService allocationService) {
        this.hospitals = hospitals;
        this.cases = cases;
        this.beds = beds;
        this.doctors = doctors;
        this.ambulances = ambulances;
        this.equipment = equipment;
        this.holds = holds;
        this.allocationService = allocationService;
    }

    @Transactional(readOnly = true)
    public DashboardSnapshot snapshot(Long hospitalId) {
        Hospital hospital = hospitals.findById(hospitalId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospital not found"));

        Instant now = Instant.now();
        List<EmergencyCase> allCases = cases.findByHospital_IdOrderByRegisteredAtDesc(hospitalId);
        List<Bed> allBeds = beds.findByHospital(hospitalId);
        List<Doctor> allDoctors = doctors.findByHospital_IdOrderByIdAsc(hospitalId);
        List<Ambulance> allAmbulances = ambulances.findByHospital_IdOrderByIdAsc(hospitalId);
        List<Equipment> allEquipment = equipment.findByHospital_IdOrderByIdAsc(hospitalId);
        List<AllocationHold> allAllocations = holds.findByEmergencyCase_Hospital_IdOrderByIdDesc(hospitalId);

        // Legacy HELD records are still respected until their expiration time.
        List<AllocationHold> activeLegacyHolds = allAllocations.stream()
            .filter(a -> a.getStatus() == AllocationHold.Status.HELD)
            .filter(a -> a.getExpiresAt() != null && a.getExpiresAt().isAfter(now))
            .toList();
        Set<Long> heldBedIds = activeLegacyHolds.stream().filter(a -> a.getBed() != null)
            .map(a -> a.getBed().getId()).collect(Collectors.toSet());
        Set<Long> heldDoctorIds = activeLegacyHolds.stream().filter(a -> a.getDoctor() != null)
            .map(a -> a.getDoctor().getId()).collect(Collectors.toSet());
        Set<Long> heldEquipmentIds = activeLegacyHolds.stream().filter(a -> a.getEquipment() != null)
            .map(a -> a.getEquipment().getId()).collect(Collectors.toSet());

        ResourceStats bedStats = summarizeBeds(allBeds, heldBedIds);
        ResourceStats doctorStats = summarizeDoctors(allDoctors, heldDoctorIds);
        ResourceStats ambulanceStats = summarizeAmbulances(allAmbulances);
        ResourceStats equipmentStats = summarizeEquipment(allEquipment, heldEquipmentIds);

        Comparator<EmergencyCase> byUrgency = Comparator
            .comparingInt((EmergencyCase c) -> c.getUrgency().ordinal());
        List<EmergencyCase> waiting = allCases.stream()
            .filter(c -> c.getCaseStatus() == EmergencyCase.CaseStatus.WAITING)
            .filter(c -> c.getArrivalStatus() == EmergencyCase.ArrivalStatus.ARRIVED)
            .sorted(byUrgency.thenComparing(EmergencyCase::getArrivedAt,
                Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(EmergencyCase::getId))
            .toList();
        List<EmergencyCase> incoming = allCases.stream()
            .filter(c -> c.getCaseStatus() == EmergencyCase.CaseStatus.WAITING)
            .filter(c -> c.getArrivalStatus() == EmergencyCase.ArrivalStatus.PRE_ARRIVAL)
            .sorted(byUrgency.thenComparing(EmergencyCase::getEstimatedArrivalAt,
                Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(EmergencyCase::getId))
            .toList();

        List<AlertCard> alerts = new ArrayList<>();
        int shortageCases = 0;
        // The system never assigns urgency; it only displays staff-entered levels.
        for (EmergencyCase c : waiting) {
            if (c.getUrgency() == EmergencyCase.Urgency.CRITICAL) {
                alerts.add(new AlertCard("CRITICAL", "CRITICAL", c.getId(), c.getPatientName(),
                    "Critical-priority patient is waiting for attention."));
            }
        }
        for (EmergencyCase c : incoming) {
            if (c.getUrgency() == EmergencyCase.Urgency.CRITICAL) {
                alerts.add(new AlertCard("INCOMING", "WARNING", c.getId(), c.getPatientName(),
                    "Critical-priority pre-arrival case: prepare for the incoming patient."));
            }
        }
        for (EmergencyCase c : allCases) {
            if (c.getCaseStatus() != EmergencyCase.CaseStatus.WAITING) continue;
            // Reuses Session 6's same eligibility criteria rather than inventing
            // a second set of clinical/operational matching rules.
            ResourceCatalog catalog = allocationService.catalog(hospitalId, c.getId());
            List<String> gaps = resourceGaps(catalog);
            if (!gaps.isEmpty()) {
                shortageCases++;
                alerts.add(new AlertCard("SHORTAGE",
                    c.getUrgency() == EmergencyCase.Urgency.CRITICAL ? "CRITICAL" : "WARNING",
                    c.getId(), c.getPatientName(), String.join("; ", gaps)));
            }
        }

        long treating = allCases.stream()
            .filter(c -> c.getCaseStatus() == EmergencyCase.CaseStatus.UNDER_TREATMENT).count();
        long completed = allCases.stream()
            .filter(c -> c.getCaseStatus() == EmergencyCase.CaseStatus.COMPLETED).count();
        long active = allAllocations.stream()
            .filter(a -> a.getStatus() == AllocationHold.Status.APPROVED).count();
        long criticalWaiting = waiting.stream()
            .filter(c -> c.getUrgency() == EmergencyCase.Urgency.CRITICAL).count();

        Summary summary = new Summary(allCases.size(), waiting.size(), incoming.size(),
            (int) treating, (int) completed, (int) criticalWaiting, (int) active, shortageCases);

        List<AllocationCard> recent = allAllocations.stream().limit(8)
            .map(this::toAllocationCard).toList();
        List<AllocationCard> activeAllocations = allAllocations.stream()
            .filter(a -> a.getStatus() == AllocationHold.Status.APPROVED)
            .map(this::toAllocationCard).toList();

        return new DashboardSnapshot(now, hospital.getName(), summary,
            new Resources(bedStats, doctorStats, ambulanceStats, equipmentStats),
            waiting.stream().map(c -> toCaseCard(c, now)).toList(),
            incoming.stream().map(c -> toCaseCard(c, now)).toList(),
            activeAllocations, recent, alerts);
    }

    private List<String> resourceGaps(ResourceCatalog catalog) {
        List<String> gaps = new ArrayList<>();
        List<ResourceOption> eligibleBeds = catalog.beds().stream()
            .filter(ResourceOption::eligible).toList();
        List<ResourceOption> eligibleDoctors = catalog.doctors().stream()
            .filter(ResourceOption::eligible).toList();
        List<ResourceOption> eligibleEquipment = catalog.equipment().stream()
            .filter(ResourceOption::eligible).toList();

        if (catalog.needsBed() && eligibleBeds.isEmpty())
            gaps.add("No eligible bed currently meets the specified requirements");
        if (catalog.needsDoctor() && eligibleDoctors.isEmpty())
            gaps.add("No available, on-shift doctor matches the requested specialization");
        if (catalog.needsVentilator()) {
            Set<Long> eligibleBedDepartments = eligibleBeds.stream()
                .map(ResourceOption::departmentId).collect(Collectors.toSet());
            boolean matchingVentilator = eligibleEquipment.stream()
                .anyMatch(e -> eligibleBedDepartments.contains(e.departmentId()));
            if (!matchingVentilator)
                gaps.add("No eligible ventilator is available in an eligible bed's department");
        }
        return gaps;
    }

    private boolean withinShift(Doctor d) {
        LocalTime now = LocalTime.now(HOSPITAL_ZONE);
        LocalTime start = d.getShiftStart();
        LocalTime end = d.getShiftEnd();
        if (start == null || end == null) return false;
        if (start.equals(end)) return true; // Session 6 demo convention.
        if (start.isBefore(end)) return !now.isBefore(start) && now.isBefore(end);
        return !now.isBefore(start) || now.isBefore(end);
    }

    private ResourceStats summarizeBeds(List<Bed> items, Set<Long> heldIds) {
        int available = 0, occupied = 0, unavailable = 0, held = 0;
        for (Bed b : items) {
            if (b.getStatus() == Bed.BedStatus.OCCUPIED) occupied++;
            else if (b.getStatus() == Bed.BedStatus.MAINTENANCE) unavailable++;
            else if (heldIds.contains(b.getId())) held++;
            else available++;
        }
        return new ResourceStats(items.size(), available, occupied, unavailable, held, 0);
    }

    private ResourceStats summarizeDoctors(List<Doctor> items, Set<Long> heldIds) {
        int available = 0, occupied = 0, unavailable = 0, held = 0, offShift = 0;
        for (Doctor d : items) {
            if (d.getStatus() == Doctor.Status.BUSY) occupied++;
            else if (d.getStatus() == Doctor.Status.OFF_DUTY) unavailable++;
            else if (heldIds.contains(d.getId())) held++;
            else if (!withinShift(d)) offShift++;
            else available++;
        }
        return new ResourceStats(items.size(), available, occupied, unavailable, held, offShift);
    }

    private ResourceStats summarizeAmbulances(List<Ambulance> items) {
        int available = 0, occupied = 0, unavailable = 0;
        for (Ambulance a : items) {
            if (a.getStatus() == Ambulance.Status.DISPATCHED) occupied++;
            else if (a.getStatus() == Ambulance.Status.MAINTENANCE) unavailable++;
            else available++;
        }
        return new ResourceStats(items.size(), available, occupied, unavailable, 0, 0);
    }

    private ResourceStats summarizeEquipment(List<Equipment> items, Set<Long> heldIds) {
        int available = 0, occupied = 0, unavailable = 0, held = 0;
        for (Equipment e : items) {
            if (e.getStatus() == Equipment.Status.IN_USE) occupied++;
            else if (e.getStatus() == Equipment.Status.MAINTENANCE) unavailable++;
            else if (heldIds.contains(e.getId())) held++;
            else available++;
        }
        return new ResourceStats(items.size(), available, occupied, unavailable, held, 0);
    }

    private CaseCard toCaseCard(EmergencyCase c, Instant now) {
        Long minutes = c.getArrivedAt() == null ? null :
            Math.max(0, Duration.between(c.getArrivedAt(), now).toMinutes());
        return new CaseCard(c.getId(), c.getPatientName(), c.getUrgency().name(),
            c.getArrivalStatus().name(), c.getCaseStatus().name(), c.getChiefComplaint(),
            c.getArrivedAt(), c.getEstimatedArrivalAt(), minutes,
            c.getRequestedBedType() == null ? null : c.getRequestedBedType().name(),
            c.getRequestedSpecialization(), c.isOxygenNeeded(), c.isVentilatorNeeded());
    }

    private AllocationCard toAllocationCard(AllocationHold a) {
        return new AllocationCard(a.getId(), a.getEmergencyCase().getId(),
            a.getEmergencyCase().getPatientName(), a.getStatus().name(),
            a.getBed() == null ? null : a.getBed().getCode(),
            a.getDoctor() == null ? null : a.getDoctor().getFullName(),
            a.getEquipment() == null ? null : a.getEquipment().getAssetCode(),
            a.getCreatedAt());
    }

    public record Summary(int totalCases, int waiting, int incoming, int underTreatment,
                          int completed, int criticalWaiting, int activeAllocations,
                          int shortageCases) {}
    /** 'available' means allocatable now, excluding held and off-shift resources. */
    public record ResourceStats(int total, int available, int engaged, int unavailable,
                                int held, int offShift) {}
    public record Resources(ResourceStats beds, ResourceStats doctors,
                            ResourceStats ambulances, ResourceStats equipment) {}
    public record CaseCard(Long id, String patientName, String urgency, String arrivalStatus,
                           String caseStatus, String chiefComplaint, Instant arrivedAt,
                           Instant estimatedArrivalAt, Long waitMinutes, String requestedBedType,
                           String requestedSpecialization, boolean oxygenNeeded,
                           boolean ventilatorNeeded) {}
    public record AllocationCard(Long id, Long caseId, String patientName, String status,
                                 String bedCode, String doctorName, String equipmentCode,
                                 Instant createdAt) {}
    public record AlertCard(String type, String severity, Long caseId,
                            String patientName, String message) {}
    public record DashboardSnapshot(Instant generatedAt, String hospitalName,
                                    Summary summary, Resources resources,
                                    List<CaseCard> waitingQueue, List<CaseCard> incomingCases,
                                    List<AllocationCard> activeAllocations,
                                    List<AllocationCard> recentActivity,
                                    List<AlertCard> alerts) {}
}
