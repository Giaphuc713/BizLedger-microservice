package com.overcode250204.inventoryservice.service.impl;

import com.overcode250204.inventoryservice.exception.InventoryItemNotFoundException;
import com.overcode250204.inventoryservice.exception.InsufficientStockException;
import com.overcode250204.inventoryservice.entity.InventoryItem;
import com.overcode250204.inventoryservice.entity.StockMovement;
import com.overcode250204.inventoryservice.entity.StockReservation;
import com.overcode250204.inventoryservice.repository.InventoryItemRepository;
import com.overcode250204.inventoryservice.outbox.OutboxHelper;
import com.overcode250204.inventoryservice.repository.StockMovementRepository;
import com.overcode250204.inventoryservice.repository.StockReservationRepository;
import com.overcode250204.inventoryservice.service.IInventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements IInventoryService {

    private final InventoryItemRepository inventoryRepository;
    private final StockReservationRepository reservationRepository;
    private final StockMovementRepository stockMovementRepository;
    private final OutboxHelper outboxHelper;

    @Override
    @Transactional
    public void reserveStock(UUID tenantId, UUID orderId, List<Map<String, Object>> items, String traceId) {
        log.info("[Saga Participant] Reserving stock for order={} tenant={}", orderId, tenantId);

        try {
            for (Map<String, Object> item : items) {
                UUID productId = UUID.fromString((String) item.get("productId"));
                int qty = ((Number) item.get("quantity")).intValue();

                InventoryItem invItem = inventoryRepository.findByTenantIdAndProductId(tenantId, productId)
                        .orElseThrow(() -> new InventoryItemNotFoundException(productId));

                if (invItem.getAvailableQty() < qty) {
                    throw new InsufficientStockException(productId, invItem.getSku(),
                            invItem.getAvailableQty(), qty);
                }

                invItem.setAvailableQty(invItem.getAvailableQty() - qty);
                invItem.setReservedQty(invItem.getReservedQty() + qty);
                inventoryRepository.save(invItem);

                StockMovement movement = StockMovement.builder()
                        .id(UUID.randomUUID())
                        .tenantId(tenantId)
                        .productId(productId)
                        .movementType("RESERVE")
                        .quantity(qty)
                        .referenceId(orderId.toString())
                        .createdAt(OffsetDateTime.now())
                        .build();

                StockReservation reservation = StockReservation.builder()
                        .tenantId(tenantId)
                        .orderId(orderId)
                        .productId(productId)
                        .quantity(qty)
                        .status("RESERVED")
                        .build();
                reservationRepository.save(reservation);
            }

            publishOutboxEvent(tenantId, "inventory.reserved", orderId, traceId, Map.of(
                    "orderId", orderId.toString(),
                    "tenantId", tenantId.toString()));

        } catch (Exception ex) {
            log.error("[Saga Participant] Stock reservation failed for order={}", orderId, ex);

            publishOutboxEvent(tenantId, "inventory.reserve_failed", orderId, traceId, Map.of(
                    "orderId", orderId.toString(),
                    "reason", ex.getMessage(),
                    "tenantId", tenantId.toString()));

            throw ex;
        }
    }

    @Override
    @Transactional
    public void releaseStock(UUID tenantId, UUID orderId, String traceId) {
        log.info("[Saga Participant] Releasing stock reservations for order={} tenant={}", orderId, tenantId);

        List<StockReservation> reservations = reservationRepository.findByOrderId(orderId);
        for (StockReservation res : reservations) {
            if ("RELEASED".equals(res.getStatus()))
                continue;

            InventoryItem invItem = inventoryRepository.findByTenantIdAndProductId(tenantId, res.getProductId())
                    .orElseThrow(() -> new InventoryItemNotFoundException(res.getProductId()));

            invItem.setAvailableQty(invItem.getAvailableQty() + res.getQuantity());
            invItem.setReservedQty(Math.max(0, invItem.getReservedQty() - res.getQuantity()));
            inventoryRepository.save(invItem);

            res.setStatus("RELEASED");
            reservationRepository.save(res);
        }

        publishOutboxEvent(tenantId, "inventory.released", orderId, traceId, Map.of(
                "orderId", orderId.toString(),
                "tenantId", tenantId.toString()));
    }

    @Override
    @Transactional
    public void commitStock(UUID tenantId, UUID orderId) {
        log.info("[Saga Participant] Committing stock reservations for order={} tenant={}", orderId, tenantId);

        List<StockReservation> reservations = reservationRepository.findByOrderId(orderId);
        for (StockReservation res : reservations) {
            if ("COMPLETED".equals(res.getStatus()))
                continue;

            InventoryItem invItem = inventoryRepository.findByTenantIdAndProductId(tenantId, res.getProductId())
                    .orElseThrow(() -> new InventoryItemNotFoundException(res.getProductId()));

            invItem.setReservedQty(Math.max(0, invItem.getReservedQty() - res.getQuantity()));
            inventoryRepository.save(invItem);

            res.setStatus("COMPLETED");
            reservationRepository.save(res);
        }
    }

    @Override
    @Transactional
    public void adjustStock(UUID tenantId, UUID productId, String sku, int qty) {
        InventoryItem item = inventoryRepository.findByTenantIdAndProductId(tenantId, productId)
                .orElse(InventoryItem.builder()
                        .tenantId(tenantId)
                        .productId(productId)
                        .sku(sku)
                        .availableQty(0)
                        .reservedQty(0)
                        .build());

        item.setAvailableQty(item.getAvailableQty() + qty);
        inventoryRepository.save(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getInventory(UUID tenantId) {
        return inventoryRepository.findByTenantId(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getLowStock(UUID tenantId) {
        return inventoryRepository.findByTenantId(tenantId).stream()
                .filter(i -> i.getAvailableQty() <= 10)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockReservation> getReservations(UUID tenantId) {
        return reservationRepository.findByTenantId(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAdjustments(UUID tenantId) {
        return stockMovementRepository.findByTenantId(tenantId).stream()
                .filter(m -> "ADJUST".equalsIgnoreCase(m.getMovementType()))
                .map(m -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", m.getId().toString());
                    map.put("productId", m.getProductId().toString());
                    map.put("delta", m.getQuantity());
                    map.put("reason", m.getReferenceId() == null ? "Manual Correction" : m.getReferenceId());
                    map.put("createdAt", m.getCreatedAt().toString());
                    map.put("actor", "Minh Tran");
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InventoryItem adjust(UUID tenantId, UUID productId, int delta, String reason) {
        InventoryItem item = inventoryRepository.findByTenantIdAndProductId(tenantId, productId)
                .orElse(InventoryItem.builder()
                        .id(UUID.randomUUID())
                        .tenantId(tenantId)
                        .productId(productId)
                        .sku("SKU-" + productId.toString().substring(0, 8).toUpperCase())
                        .availableQty(0)
                        .reservedQty(0)
                        .build());

        item.setAvailableQty(item.getAvailableQty() + delta);
        inventoryRepository.save(item);

        StockMovement movement = StockMovement.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .productId(productId)
                .movementType("ADJUST")
                .quantity(delta)
                .referenceId(reason)
                .createdAt(OffsetDateTime.now())
                .build();
        stockMovementRepository.save(movement);

        return item;
    }

    @Override
    @Transactional
    public StockReservation reserve(UUID tenantId, UUID productId, int qty) {
        InventoryItem item = inventoryRepository.findByTenantIdAndProductId(tenantId, productId)
                .orElseThrow(() -> new InventoryItemNotFoundException(productId));

        if (item.getAvailableQty() < qty) {
            throw new InsufficientStockException(productId, item.getSku(), item.getAvailableQty(), qty);
        }

        item.setAvailableQty(item.getAvailableQty() - qty);
        item.setReservedQty(item.getReservedQty() + qty);
        inventoryRepository.save(item);

        StockReservation reservation = StockReservation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .orderId(UUID.randomUUID())
                .productId(productId)
                .quantity(qty)
                .status("RESERVED")
                .createdAt(OffsetDateTime.now())
                .build();
        reservationRepository.save(reservation);

        StockMovement movement = StockMovement.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .productId(productId)
                .movementType("RESERVE")
                .quantity(qty)
                .referenceId(reservation.getId().toString())
                .createdAt(OffsetDateTime.now())
                .build();
        stockMovementRepository.save(movement);

        return reservation;
    }

    @Override
    @Transactional
    public StockReservation releaseReservation(UUID tenantId, UUID reservationId) {
        StockReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new com.overcode250204.inventoryservice.exception.StockReservationNotFoundException(
                        reservationId));

        if (!reservation.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Access denied for tenant");
        }

        if ("RELEASED".equalsIgnoreCase(reservation.getStatus())) {
            return reservation;
        }

        InventoryItem item = inventoryRepository.findByTenantIdAndProductId(tenantId, reservation.getProductId())
                .orElseThrow(() -> new InventoryItemNotFoundException(reservation.getProductId()));

        item.setAvailableQty(item.getAvailableQty() + reservation.getQuantity());
        item.setReservedQty(Math.max(0, item.getReservedQty() - reservation.getQuantity()));
        inventoryRepository.save(item);

        reservation.setStatus("RELEASED");
        reservation.setUpdatedAt(OffsetDateTime.now());
        reservationRepository.save(reservation);

        StockMovement movement = StockMovement.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .productId(reservation.getProductId())
                .movementType("RELEASE")
                .quantity(reservation.getQuantity())
                .referenceId(reservation.getId().toString())
                .createdAt(OffsetDateTime.now())
                .build();
        stockMovementRepository.save(movement);

        return reservation;
    }

    private void publishOutboxEvent(UUID tenantId, String eventType, UUID orderId, String traceId,
                                    Map<String, Object> data) {
        outboxHelper.saveEvent("inventory.events", eventType, "inventory-service-java", tenantId, traceId, data);
    }
}
