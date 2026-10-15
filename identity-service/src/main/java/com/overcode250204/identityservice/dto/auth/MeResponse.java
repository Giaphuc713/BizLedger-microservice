package com.overcode250204.identityservice.dto.auth;

import java.util.List;
import java.util.UUID;

public record MeResponse(
        UUID userId,
        UUID tenantId,
        String email,
        List<String> roles,
        List<String> permissions
) {
}
