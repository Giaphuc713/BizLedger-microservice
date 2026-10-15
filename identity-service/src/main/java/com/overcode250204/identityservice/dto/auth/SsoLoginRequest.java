package com.overcode250204.identityservice.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record SsoLoginRequest(
        @NotBlank(message = "SSO provider name must not be blank") String provider,

        @NotBlank(message = "OIDC ID token must not be blank") String idToken) {
}
