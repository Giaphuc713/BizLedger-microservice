package com.overcode250204.identityservice.controller;

import com.overcode250204.common.web.ApiResponse;
import com.overcode250204.identityservice.service.IHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * HealthController — Controller exposing liveness and readiness health probe
 * API endpoints.
 */
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final IHealthService healthService;

    @GetMapping("/api/v1/health/liveness")
    public ResponseEntity<ApiResponse<Map<String, Object>>> liveness() {
        return ResponseEntity.ok(ApiResponse.success(healthService.checkLiveness()));
    }

    @GetMapping("/api/v1/health/readiness")
    public ResponseEntity<ApiResponse<Map<String, Object>>> readiness() {
        return ResponseEntity.ok(ApiResponse.success(healthService.checkReadiness()));
    }

    @GetMapping("/health/ready")
    public Map<String, Object> ready() {
        return Map.of(
                "service", "identity-service-java",
                "status", "UP");
    }
}
