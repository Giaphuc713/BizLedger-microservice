package com.overcode250204.inventoryservice.service;

import com.overcode250204.inventoryservice.entity.InventoryItem;
import com.overcode250204.inventoryservice.entity.StockReservation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * IInventoryService — Service interface defining business contracts for stock
 * and reservation management.
 */
public interface IInventoryService {

    /**
     * Reserves stock items for a given order draft.
     *
     * @param tenantId the unique identifier of the tenant
     * @param orderId  the unique identifier of the order
     * @param items    list of maps denoting product UUID and qty requested
     * @param traceId  tracing context for correlation
     * @throws com.overcode250204.inventoryservice.exception.InsufficientStockException     if
     *                                                                                      the
     *                                                                                      stock
     *                                                                                      is
     *                                                                                      insufficient
     * @throws com.overcode250204.inventoryservice.exception.InventoryItemNotFoundException if
     *                                                                                      a
     *                                                                                      product
     *                                                                                      has
     *                                                                                      no
     *                                                                                      inventory
     *                                                                                      record
     */
    void reserveStock(UUID tenantId, UUID orderId, List<Map<String, Object>> items, String traceId);

    /**
     * Releases active stock reservation for a pending/cancelled order.
     *
     * @param tenantId the unique identifier of the tenant
     * @param orderId  the unique identifier of the order
     * @param traceId  tracing context
     * @throws com.overcode250204.inventoryservice.exception.StockReservationNotFoundException if
     *                                                                                         reservation
     *                                                                                         does
     *                                                                                         not
     *                                                                                         exist
     */
    void releaseStock(UUID tenantId, UUID orderId, String traceId);

    /**
     * Commits stock reservation, turning pending reservations into permanent
     * inventory deduplication.
     *
     * @param tenantId the unique identifier of the tenant
     * @param orderId  the unique identifier of the order
     * @throws com.overcode250204.inventoryservice.exception.StockReservationNotFoundException if
     *                                                                                         reservation
     *                                                                                         does
     *                                                                                         not
     *                                                                                         exist
     */
    void commitStock(UUID tenantId, UUID orderId);

    /**
     * Adjusts system stock count directly.
     *
     * @param tenantId  the unique identifier of the tenant
     * @param productId the unique identifier of the product
     * @param sku       product SKU code
     * @param qty       adjustment delta value
     * @throws com.overcode250204.inventoryservice.exception.InventoryItemNotFoundException if
     *                                                                                      the
     *                                                                                      item
     *                                                                                      does
     *                                                                                      not
     *                                                                                      exist
     */
    void adjustStock(UUID tenantId, UUID productId, String sku, int qty);

    /**
     * Lists current inventory records for a tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return list of matching inventory items
     */
    List<InventoryItem> getInventory(UUID tenantId);

    /**
     * Identifies items whose stock count is beneath their configured reorder alert
     * threshold.
     *
     * @param tenantId the unique identifier of the tenant
     * @return list of critical or low-stock inventory items
     */
    List<InventoryItem> getLowStock(UUID tenantId);

    /**
     * Lists outbox stock reservations for a tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return list of active stock reservations
     */
    List<StockReservation> getReservations(UUID tenantId);

    /**
     * Gathers historical stock adjustment records.
     *
     * @param tenantId the unique identifier of the tenant
     * @return list of adjustment events
     */
    List<Map<String, Object>> getAdjustments(UUID tenantId);

    /**
     * Helper to modify stock count and register reason details.
     *
     * @param tenantId  the unique identifier of the tenant
     * @param productId the identifier of the product
     * @param delta     modification delta
     * @param reason    comment text
     * @return the adjusted inventory item state
     * @throws com.overcode250204.inventoryservice.exception.InventoryItemNotFoundException if
     *                                                                                      the
     *                                                                                      item
     *                                                                                      does
     *                                                                                      not
     *                                                                                      exist
     */
    InventoryItem adjust(UUID tenantId, UUID productId, int delta, String reason);

    /**
     * Legacy inline reservation helper.
     *
     * @param tenantId  the unique identifier of the tenant
     * @param productId the unique identifier of the product
     * @param qty       reservation quantity
     * @return the active reservation record
     * @throws com.overcode250204.inventoryservice.exception.InsufficientStockException if
     *                                                                                  the
     *                                                                                  stock
     *                                                                                  is
     *                                                                                  insufficient
     */
    StockReservation reserve(UUID tenantId, UUID productId, int qty);

    /**
     * Legacy inline reservation release helper.
     *
     * @param tenantId      the unique identifier of the tenant
     * @param reservationId reservation unique identifier
     * @return the released reservation details
     * @throws com.overcode250204.inventoryservice.exception.StockReservationNotFoundException if
     *                                                                                         reservation
     *                                                                                         does
     *                                                                                         not
     *                                                                                         exist
     */
    StockReservation releaseReservation(UUID tenantId, UUID reservationId);
}
