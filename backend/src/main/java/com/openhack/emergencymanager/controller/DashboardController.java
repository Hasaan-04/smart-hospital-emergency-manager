package com.openhack.emergencymanager.controller;

import com.openhack.emergencymanager.service.DashboardService;
import com.openhack.emergencymanager.service.DashboardService.DashboardSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only dashboard API for the Session 7 prototype. */
@RestController
@RequestMapping("/api/hospitals/{hospitalId}/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    public DashboardSnapshot getDashboard(@PathVariable Long hospitalId) {
        return service.snapshot(hospitalId);
    }
}
