package com.openhack.emergencymanager.controller;

import com.openhack.emergencymanager.service.AllocationService;
import com.openhack.emergencymanager.service.AllocationService.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Session 6 revised: staff-assisted manual allocation, no new holds. */
@RestController
@RequestMapping("/api/hospitals/{hospitalId}/allocations")
public class AllocationController {
    private final AllocationService service;

    public AllocationController(AllocationService service) {
        this.service = service;
    }

    @GetMapping("/cases/{caseId}/resources")
    public ResourceCatalog resources(@PathVariable Long hospitalId,
                                     @PathVariable Long caseId) {
        return service.catalog(hospitalId, caseId);
    }

    @GetMapping
    public List<AllocationView> list(@PathVariable Long hospitalId) {
        return service.list(hospitalId);
    }

    @PostMapping("/confirm")
    public ResponseEntity<AllocationView> confirm(@PathVariable Long hospitalId,
                                                  @RequestBody ConfirmRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.confirm(hospitalId, request));
    }

    @PostMapping("/{allocationId}/complete")
    public AllocationView complete(@PathVariable Long hospitalId,
                                   @PathVariable Long allocationId) {
        return service.complete(hospitalId, allocationId);
    }

    // Only for old HELD records created before switching to manual allocation.
    @PostMapping("/{allocationId}/cancel")
    public AllocationView cancel(@PathVariable Long hospitalId,
                                 @PathVariable Long allocationId) {
        return service.cancel(hospitalId, allocationId);
    }
}
