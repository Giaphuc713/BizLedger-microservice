package com.overcode250204.orderservice.service.impl;


import com.overcode250204.common.annotation.AuditLog;
import com.overcode250204.orderservice.dto.CreateOrderRequest;
import com.overcode250204.orderservice.dto.OrderItemDto;
import com.overcode250204.orderservice.dto.OrderResponse;
import com.overcode250204.orderservice.entity.Order;
import com.overcode250204.orderservice.entity.OrderItem;
import com.overcode250204.orderservice.entity.OrderStatusHistory;
import com.overcode250204.orderservice.exception.InvalidOrderStatusException;
import com.overcode250204.orderservice.exception.OrderNotFoundException;
import com.overcode250204.orderservice.outbox.OutboxHelper;
import com.overcode250204.orderservice.repository.OrderRepository;
import com.overcode250204.orderservice.repository.OrderStatusHistoryRepository;
import com.overcode250204.orderservice.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import net.devh.boot.grpc.client.inject.GrpcClient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService implements IOrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OutboxHelper outboxHelper;

    @GrpcClient("catalog")
    private com.overcode250204.common.grpc.ProductGrpcServiceGrpc.ProductGrpcServiceBlockingStub productGrpcServiceStub;

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(UUID tenantId, Pageable pageable) {
        return orderRepository.findByTenantId(tenantId, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID tenantId, UUID id) {
        return orderRepository.findByIdAndTenantId(id, tenantId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Override
    @Transactional
    @AuditLog(action = "ORDER_CREATE", resource = "order")
    public OrderResponse createOrder(UUID tenantId, CreateOrderRequest request) {
        BigDecimal total = BigDecimal.ZERO;

        Order order = Order.builder()
                .tenantId(tenantId)
                .customerId(request.customerId())
                .status("DRAFT")
                .build();

        for (OrderItemDto itemDto : request.items()) {
            log.info("gRPC Call - Fetching catalog price for product: {}", itemDto.productId());
            com.overcode250204.common.grpc.ProductResponse grpcProductResponse = productGrpcServiceStub
                    .getProductInfo(
                            com.overcode250204.common.grpc.ProductRequest.newBuilder()
                                    .setTenantId(tenantId.toString())
                                    .setProductId(itemDto.productId().toString())
                                    .build());

            if (!grpcProductResponse.getActive()) {
                throw new RuntimeException(
                        "Product is inactive or unavailable: " + itemDto.productId());
            }

            BigDecimal verifiedPrice = BigDecimal.valueOf(grpcProductResponse.getPrice());
            log.info("gRPC Verified - Product: {}, Price: {}", grpcProductResponse.getName(),
                    verifiedPrice);

            order.addItem(OrderItem.builder()
                    .productId(itemDto.productId())
                    .sku(grpcProductResponse.getSku())
                    .name(grpcProductResponse.getName())
                    .quantity(itemDto.quantity())
                    .unitPrice(verifiedPrice)
                    .build());

            total = total.add(verifiedPrice.multiply(BigDecimal.valueOf(itemDto.quantity())));
        }

        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);
        recordHistory(saved.getId(), null, "DRAFT", "Order created as draft (verified via gRPC)");
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    @AuditLog(action = "ORDER_SUBMIT", resource = "order")
    public OrderResponse submitOrder(UUID tenantId, UUID orderId) {
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!"DRAFT".equals(order.getStatus())) {
            throw new InvalidOrderStatusException(orderId, order.getStatus(), "DRAFT");
        }

        String oldStatus = order.getStatus();
        order.setStatus("PENDING_APPROVAL");
        Order saved = orderRepository.save(order);
        recordHistory(orderId, oldStatus, "PENDING_APPROVAL", "Order submitted for stock reservation");

        List<Map<String, Object>> itemPayloads = order.getItems().stream()
                .map(i -> Map.<String, Object>of(
                        "productId", i.getProductId().toString(),
                        "quantity", i.getQuantity()))
                .collect(Collectors.toList());

        publishOutbox(tenantId, "order.approval_requested", orderId, Map.of(
                "orderId", orderId.toString(),
                "items", itemPayloads,
                "tenantId", tenantId.toString()));

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void handleInventoryReserved(UUID tenantId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!"PENDING_APPROVAL".equals(order.getStatus())) {
            log.warn("Ignore reservation: order={} status={}", orderId, order.getStatus());
            return;
        }

        String oldStatus = order.getStatus();
        order.setStatus("APPROVED");
        orderRepository.save(order);

        recordHistory(orderId, oldStatus, "APPROVED", "Stock successfully reserved. Pending payment.");

        publishOutbox(tenantId, "order.approved", orderId, Map.of(
                "orderId", orderId.toString(),
                "totalAmount", order.getTotalAmount().doubleValue(),
                "currency", order.getCurrency(),
                "tenantId", tenantId.toString()));
    }

    @Override
    @Transactional
    public void handleInventoryFailed(UUID tenantId, UUID orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!"PENDING_APPROVAL".equals(order.getStatus())) {
            log.warn("Ignore reservation failure: order={} status={}", orderId, order.getStatus());
            return;
        }

        String oldStatus = order.getStatus();
        order.setStatus("APPROVAL_FAILED");
        orderRepository.save(order);

        recordHistory(orderId, oldStatus, "APPROVAL_FAILED", "Stock reservation failing: " + reason);

        publishOutbox(tenantId, "order.cancelled", orderId, Map.of(
                "orderId", orderId.toString(),
                "reason", reason,
                "tenantId", tenantId.toString()));
    }

    @Override
    @Transactional
    public void handlePaymentSucceeded(UUID tenantId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!"APPROVED".equals(order.getStatus())) {
            log.warn("Ignore payment success: order={} status={}", orderId, order.getStatus());
            return;
        }

        String oldStatus = order.getStatus();
        order.setStatus("PAID");
        orderRepository.save(order);

        recordHistory(orderId, oldStatus, "PAID", "Order fully paid and completed");
    }

    @Override
    @Transactional
    public OrderResponse approveOrder(UUID tenantId, UUID orderId) {
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        handleInventoryReserved(tenantId, orderId);
        return mapToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID tenantId, UUID orderId) {
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        String oldStatus = order.getStatus();
        order.setStatus("CANCELLED");
        orderRepository.save(order);
        recordHistory(orderId, oldStatus, "CANCELLED", "Order cancelled by user request");
        publishOutbox(tenantId, "order.cancelled", orderId, Map.of(
                "orderId", orderId.toString(),
                "reason", "User cancelled",
                "tenantId", tenantId.toString()));
        return mapToResponse(order);
    }

    private void recordHistory(UUID orderId, String fromStatus, String toStatus, String reason) {
        OrderStatusHistory hist = OrderStatusHistory.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .reason(reason)
                .changedAt(OffsetDateTime.now())
                .build();
        historyRepository.save(hist);
    }

    private void publishOutbox(UUID tenantId, String eventType, UUID orderId, Map<String, Object> data) {
        outboxHelper.saveEvent("order.events", eventType, "order-service", tenantId, data);
    }

    private OrderResponse mapToResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getTenantId(),
                order.getCustomerId(),
                order.getCurrency(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getItems().stream()
                        .map(i -> new OrderItemDto(i.getProductId(), i.getSku(), i.getName(),
                                i.getQuantity(),
                                i.getUnitPrice()))
                        .collect(Collectors.toList()),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
