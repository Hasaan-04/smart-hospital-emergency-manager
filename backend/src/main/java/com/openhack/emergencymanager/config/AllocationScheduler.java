package com.openhack.emergencymanager.config;

import com.openhack.emergencymanager.service.AllocationService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class AllocationScheduler {
    private final AllocationService allocationService;

    public AllocationScheduler(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    // The server, not React, owns expiration. Runs every 15 seconds.
    @Scheduled(fixedDelay = 15000)
    public void expireOldReservations() {
        allocationService.expireOldHolds();
    }
}
