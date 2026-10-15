package com.overcode250204.inventoryservice.controller;

import com.overcode250204.common.web.ApiResponse;
import com.overcode250204.inventoryservice.entity.InventoryItem;
import com.overcode250204.inventoryservice.entity.StockReservation;
import com.overcode250204.inventoryservice.service.IInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final IInventoryService inventoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    public ResponseEntity<ApiResponse<List<InventoryItem>>> getInventory(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getInventory(tenantId)));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAuthority('inventory:read')")
    public ResponseEntity<ApiResponse<List<InventoryItem>>> getLowStock(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getLowStock(tenantId)));
    }

    @GetMapping("/reservations")
    @PreAuthorize("hasAuthority('inventory:read')")
    public ResponseEntity<ApiResponse<List<StockReservation>>> getReservations(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getReservations(tenantId)));
    }

    @GetMapping("/adjustments")
    @PreAuthorize("hasAuthority('inventory:read')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAdjustments(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAdjustments(tenantId)));
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAuthority('inventory:write')")
    public ResponseEntity<ApiResponse<InventoryItem>> adjust(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam UUID productId,
            @RequestParam int delta,
            @RequestParam(required = false, defaultValue = "Manual Adjustment") String reason) {
        return ResponseEntity.ok(ApiResponse.success("Stock adjusted successfully",
                inventoryService.adjust(tenantId, productId, delta, reason)));
    }

    @PostMapping("/reserve")
    @PreAuthorize("hasAuthority('inventory:write')")
    public ResponseEntity<ApiResponse<StockReservation>> reserve(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam UUID productId,
            @RequestParam int qty,
            @RequestParam int ttlSeconds) {
        return ResponseEntity.ok(ApiResponse.success("Stock reserved successfully",
                inventoryService.reserve(tenantId, productId, qty)));
    }

    @PostMapping("/reservations/{reservationId}/release")
    @PreAuthorize("hasAuthority('inventory:write')")
    public ResponseEntity<ApiResponse<StockReservation>> release(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID reservationId) {
        return ResponseEntity.ok(ApiResponse.success("Stock reservation released successfully",
                inventoryService.releaseReservation(tenantId, reservationId)));
    }
}
