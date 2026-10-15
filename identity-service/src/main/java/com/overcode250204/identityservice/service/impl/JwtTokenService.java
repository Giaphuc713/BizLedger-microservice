package com.overcode250204.identityservice.service.impl;

import com.overcode250204.identityservice.entity.AuthenticatedUser;
import com.overcode250204.identityservice.properties.JwtProperties;
import com.overcode250204.identityservice.service.IJwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtTokenService implements IJwtTokenService {
    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(
            UUID userId,
            UUID tenantId,
            String email,
            List<String> roles,
            List<String> permissions
    ) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.accessTokenExpirationMinutes() * 60);

        return Jwts.builder()
                .issuer(jwtProperties.issuer())
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("userId", userId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .signWith(secretKey)
                .compact();
    }

    public AuthenticatedUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(jwtProperties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID userId = UUID.fromString(claims.get("userId", String.class));
        UUID tenantId = UUID.fromString(claims.get("tenantId", String.class));
        String email = claims.get("email", String.class);

        List<String> roles = claims.get("roles", List.class);
        List<String> permissions = claims.get("permissions", List.class);

        return new AuthenticatedUser(userId, tenantId, email, roles, permissions);
    }
}
