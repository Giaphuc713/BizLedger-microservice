package com.overcode250204.orderservice.controller;

import com.overcode250204.common.web.ApiResponse;
import com.overcode250204.orderservice.dto.CreateOrderRequest;
import com.overcode250204.orderservice.dto.OrderResponse;
import com.overcode250204.orderservice.service.IOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;

    @GetMapping
    @PreAuthorize("hasAuthority('order:read')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrders(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            Pageable pageable) {
        Page<OrderResponse> orders = orderService.getOrders(tenantId, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('order:read')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        OrderResponse order = orderService.getOrderById(tenantId, id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('order:write')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = orderService.createOrder(tenantId, request);
        return ResponseEntity.ok(ApiResponse.success("Order created as draft successfully", created));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('order:write')")
    public ResponseEntity<ApiResponse<OrderResponse>> submitOrder(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        OrderResponse submitted = orderService.submitOrder(tenantId, id);
        return ResponseEntity.ok(ApiResponse.success("Order submitted successfully", submitted));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('order:write')")
    public ResponseEntity<ApiResponse<OrderResponse>> approveOrder(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        OrderResponse approved = orderService.approveOrder(tenantId, id);
        return ResponseEntity.ok(ApiResponse.success("Order approved successfully", approved));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('order:write')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        OrderResponse cancelled = orderService.cancelOrder(tenantId, id);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", cancelled));
    }
}
