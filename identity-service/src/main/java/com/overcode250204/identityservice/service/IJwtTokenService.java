package com.overcode250204.identityservice.service;

import com.overcode250204.identityservice.entity.AuthenticatedUser;
import java.util.List;
import java.util.UUID;

/**
 * IJwtTokenService — Service interface defining contracts for generating and
 * parsing identity bearer tokens (JWT).
 */
public interface IJwtTokenService {

    /**
     * Generates a signed access token containing tenant, role, and permission
     * claims.
     *
     * @param userId      the unique identifier of the user
     * @param tenantId    the unique identifier of the tenant context
     * @param email       the user's email address
     * @param roles       list of roles assigned to the user
     * @param permissions list of permissions assigned to the user
     * @return the serialized JWT string
     */
    String generateAccessToken(
            UUID userId,
            UUID tenantId,
            String email,
            List<String> roles,
            List<String> permissions);

    /**
     * Parses and cryptographically validates a raw JWT token, returning the
     * authenticated subject principal details.
     *
     * @param token standard Bearer JWT credential string
     * @return the deserialized, validated AuthenticatedUser object
     */
    AuthenticatedUser parseToken(String token);
}
