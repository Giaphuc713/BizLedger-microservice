package com.overcode250204.identityservice.service.impl;

import com.overcode250204.identityservice.entity.RefreshToken;
import com.overcode250204.identityservice.entity.User;
import com.overcode250204.identityservice.properties.JwtProperties;
import com.overcode250204.identityservice.repository.RefreshTokenRepository;
import com.overcode250204.identityservice.dto.auth.Rotation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {


    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtProperties jwtProperties
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public String createRefreshToken(User user) {
        String rawToken = generateRawToken();
        OffsetDateTime now = OffsetDateTime.now();

        refreshTokenRepository.save(RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .refreshToken(sha256(rawToken))
                .createdAt(now)
                .expiresAt(now.plusDays(jwtProperties.refreshTokenExpirationDays()))
                .build());

        return rawToken;
    }

    private String generateRawToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    @Transactional
    // Thu hồi token cũ và tạo token mới
    public Rotation rotate(String refreshToken) {
        String tokenHash = sha256(refreshToken);
        RefreshToken current = refreshTokenRepository.findByRefreshTokenForUpdate(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token"));

        if (current.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Refresh token has expired");
        }

        User user = current.getUser();
        if (!user.isActive()) {
            throw new IllegalArgumentException("User is not active");
        }

        // Thu hồi token cũ
        refreshTokenRepository.delete(current);
        String replacementToken = createRefreshToken(user);
        return new Rotation(user, replacementToken);


    }

    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.deleteAllByUser(user);
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.deleteByTokenHash(sha256(rawToken));
    }

}