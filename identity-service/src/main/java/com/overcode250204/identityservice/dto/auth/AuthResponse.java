package com.overcode250204.identityservice.dto.auth;

import java.util.List;
import java.util.UUID;

public record AuthResponse(String accessToken,
                           String tokenType,
                           UUID userId,
                           UUID tenantId,
                           String email,
                           String fullName,
                           List<String> roles,
                           List<String> permissions) {
}
