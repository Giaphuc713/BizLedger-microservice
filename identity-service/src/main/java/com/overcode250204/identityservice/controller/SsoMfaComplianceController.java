package com.overcode250204.identityservice.controller;

import com.overcode250204.common.web.ApiResponse;
import com.overcode250204.identityservice.dto.auth.AuthResponse;
import com.overcode250204.identityservice.dto.auth.SsoLoginRequest;
import com.overcode250204.identityservice.service.ISsoMfaComplianceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SsoMfaComplianceController {
    private final ISsoMfaComplianceService complianceService;

    @PostMapping("/auth/sso/login")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<AuthResponse>> ssoLogin(@Valid @RequestBody SsoLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(complianceService.ssoLogin(request)));
    }

    @GetMapping("/auth/sso/providers")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listSsoProviders() {
        return ResponseEntity.ok(ApiResponse.success(complianceService.getSsoProviders()));
    }

    @PostMapping("/mfa/enroll")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> enrollMfa() {
        return ResponseEntity.ok(ApiResponse.success(complianceService.enrollMfa()));
    }

    @PostMapping("/mfa/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyMfa(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        return ResponseEntity.ok(ApiResponse.success(complianceService.verifyMfa(code)));
    }

    @GetMapping("/rate-limit")
    @PreAuthorize("hasAuthority('compliance:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRateLimit(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        return ResponseEntity.ok(ApiResponse.success(complianceService.getRateLimit(tenantId)));
    }

    @PostMapping("/compliance/data-export")
    @PreAuthorize("hasAuthority('compliance:write')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestDataExport(@RequestParam String subjectId) {
        return ResponseEntity.ok(ApiResponse.success(complianceService.requestDataExport(subjectId)));
    }

    @PostMapping("/compliance/erasure")
    @PreAuthorize("hasAuthority('compliance:write')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestErasure(@RequestParam String subjectId) {
        return ResponseEntity.ok(ApiResponse.success(complianceService.requestErasure(subjectId)));
    }

    @PostMapping("/compliance/audit-siem")
    @PreAuthorize("hasAuthority('compliance:write')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exportAuditSiem(@RequestParam String format) {
        return ResponseEntity.ok(ApiResponse.success(complianceService.exportAuditSiem(format)));
    }

    @GetMapping("/compliance/cdc")
    @PreAuthorize("hasAuthority('compliance:read')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listCdcSubscriptions() {
        return ResponseEntity.ok(ApiResponse.success(complianceService.getCdcSubscriptions()));
    }

    @PostMapping("/compliance/cdc")
    @PreAuthorize("hasAuthority('compliance:write')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createCdcSubscription(
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.success(complianceService.createCdcSubscription(body)));
    }


}
