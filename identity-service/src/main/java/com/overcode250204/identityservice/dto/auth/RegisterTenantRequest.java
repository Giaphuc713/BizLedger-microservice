package com.overcode250204.identityservice.dto.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterTenantRequest(
        @JsonAlias("businessName") @NotBlank(message = "Tenant name is required") String tenantName,

        @JsonAlias("tenantCode") String tenantCode,

        @JsonAlias({
                "ownerEmail",
                "email" }) @NotBlank(message = "Owner email is required") @Email(message = "Owner email is invalid") String ownerEmail,

        @JsonAlias({ "ownerPassword",
                "password" }) @NotBlank(message = "Owner password is required") @Size(min = 8, message = "Owner password must have at least 8 characters") String ownerPassword,

        @JsonAlias({ "ownerFullName", "fullName" }) String ownerFullName) {
}
