package com.overcode250204.identityservice.service;

import java.util.Map;

/**
 * IHealthService — Service interface defining readiness and liveness contracts.
 * Performs real-time checks across system dependencies dynamically.
 */
public interface IHealthService {

    /**
     * Conducts liveness check for the service.
     *
     * @return map conveying active liveness state properties
     */
    Map<String, Object> checkLiveness();

    /**
     * Performs a deep readiness check across all underlying platform
     * dependencies (PostgreSQL database, Redis cache, Elasticsearch, and Kafka
     * clusters).
     *
     * @return map enclosing component health statuses and connection latencies
     */
    Map<String, Object> checkReadiness();
}
