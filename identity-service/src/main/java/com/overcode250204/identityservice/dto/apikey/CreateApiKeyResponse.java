package com.overcode250204.identityservicejava.dto.apikey;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * CreateApiKeyResponse — Outbound transfer object returned on successful API
 * Key generation.
 * Encloses the unhashed full text token (returned only once).
 */
public record CreateApiKeyResponse(
        UUID id,
        String name,
        String prefix,
        List<String> scopes,
        String status,
        OffsetDateTime lastUsedAt,
        OffsetDateTime createdAt,
        String fullKey) {
}
