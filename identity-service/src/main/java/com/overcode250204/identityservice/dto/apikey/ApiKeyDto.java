package com.overcode250204.identityservice.dto.apikey;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * ApiKeyDto — Data transfer object representing active API key metadata.
 */
public record ApiKeyDto(
        UUID id,
        String name,
        String prefix,
        List<String> scopes,
        String status,
        OffsetDateTime lastUsedAt,
        OffsetDateTime createdAt) {
}
