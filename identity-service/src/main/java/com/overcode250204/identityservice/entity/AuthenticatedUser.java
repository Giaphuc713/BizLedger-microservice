package com.overcode250204.identityservice.entity;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(UUID userId,
                                UUID tenantId,
                                String email,
                                List<String> roles,
                                List<String> permissions) {
}
