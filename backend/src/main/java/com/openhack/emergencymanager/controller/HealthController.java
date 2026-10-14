
package com.openhack.emergencymanager.controller;

import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public Map<String, String> checkHealth() {

        jdbcTemplate.queryForObject(
            "SELECT 1",
            Integer.class
        );

        return Map.of(
            "status", "connected",
            "backend", "Spring Boot",
            "database", "MySQL"
        );
    }
}
