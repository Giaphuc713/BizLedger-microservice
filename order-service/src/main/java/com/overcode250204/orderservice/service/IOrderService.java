package com.overcode250204.orderservice.service;

import com.overcode250204.orderservice.dto.CreateOrderRequest;
import com.overcode250204.orderservice.dto.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * IOrderService — Service interface defining business contracts for order
 * creation, submission, and processing flows.
 */
public interface IOrderService {

    /**
     * Retrieves a paginated list of catalog orders for a given tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @param pageable pagination and sorting parameters
     * @return a page containing matching order response objects
     */
    Page<OrderResponse> getOrders(UUID tenantId, Pageable pageable);

    /**
     * Looks up details of a specific order by its ID and tenant ID.
     *
     * @param tenantId the unique identifier of the tenant owning the order
     * @param id       the unique identifier of the order
     * @return the details of the order
     * @throws com.overcode250204.orderservice.exception.OrderNotFoundException if
     *                                                                          the
     *                                                                          order
     *                                                                          does
     *                                                                          not
     *                                                                          exist
     */
    OrderResponse getOrderById(UUID tenantId, UUID id);

    /**
     * Creates a new draft order.
     *
     * @param tenantId the unique identifier of the tenant owning the order
     * @param request  the parameters to build the new order
     * @return the details of the newly created order
     */
    OrderResponse createOrder(UUID tenantId, CreateOrderRequest request);

    /**
     * Submits a draft order for validation, reserving inventory and creating
     * payment invoices.
     *
     * @param tenantId the unique identifier of the tenant
     * @param orderId  the unique identifier of the order
     * @return the updated order details
     * @throws com.overcode250204.orderservice.exception.OrderNotFoundException      if
     *                                                                               the
     *                                                                               order
     *                                                                               does
     *                                                                               not
     *                                                                               exist
     * @throws com.overcode250204.orderservice.exception.InvalidOrderStatusException if
     *                                                                               the
     *                                                                               order
     *                                                                               status
     *                                                                               is
     *                                                                               not
     *                                                                               DRAFT
     */
    OrderResponse submitOrder(UUID tenantId, UUID orderId);

    /**
     * Event listener callback triggered when stock reservation succeeds.
     *
     * @param tenantId the unique identifier of the tenant
     * @param orderId  the unique identifier of the order
     */
    void handleInventoryReserved(UUID tenantId, UUID orderId);

    /**
     * Event listener callback triggered when stock reservation fails.
     *
     * @param tenantId the unique identifier of the tenant
     * @param orderId  the unique identifier of the order
     * @param reason   comment text describing the inventory shortage
     */
    void handleInventoryFailed(UUID tenantId, UUID orderId, String reason);

    /**
     * Event listener callback triggered when payment transaction completes.
     *
     * @param tenantId the unique identifier of the tenant
     * @param orderId  the unique identifier of the order
     */
    void handlePaymentSucceeded(UUID tenantId, UUID orderId);

    /**
     * Approves a processed order, preparing it for fulfilment.
     *
     * @param tenantId the unique identifier of the tenant
     * @param orderId  the unique identifier of the order
     * @return the updated order details
     * @throws com.overcode250204.orderservice.exception.OrderNotFoundException      if
     *                                                                               the
     *                                                                               order
     *                                                                               does
     *                                                                               not
     *                                                                               exist
     * @throws com.overcode250204.orderservice.exception.InvalidOrderStatusException if
     *                                                                               the
     *                                                                               order
     *                                                                               cannot
     *                                                                               be
     *                                                                               approved
     */
    OrderResponse approveOrder(UUID tenantId, UUID orderId);

    /**
     * Cancels an order, releasing any active stock allocations.
     *
     * @param tenantId the unique identifier of the tenant
     * @param orderId  the unique identifier of the order
     * @return the updated order details
     * @throws com.overcode250204.orderservice.exception.OrderNotFoundException      if
     *                                                                               the
     *                                                                               order
     *                                                                               does
     *                                                                               not
     *                                                                               exist
     * @throws com.overcode250204.orderservice.exception.InvalidOrderStatusException if
     *                                                                               the
     *                                                                               order
     *                                                                               status
     *                                                                               cannot
     *                                                                               be
     *                                                                               cancelled
     */
    OrderResponse cancelOrder(UUID tenantId, UUID orderId);
}
