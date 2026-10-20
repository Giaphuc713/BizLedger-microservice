package com.overcode250204.identityservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "forgot_password")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPassword {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String email;

    // Store the hash of the reset token here, not the raw token sent by email.
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

}
