package com.overcode250204.identityservice;

import com.overcode250204.identityservice.entity.RefreshToken;
import com.overcode250204.identityservice.entity.User;
import com.overcode250204.identityservice.properties.JwtProperties;
import com.overcode250204.identityservice.repository.RefreshTokenRepository;
import com.overcode250204.identityservice.service.impl.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void createRefreshTokenStoresOnlyHashAndUsesConfiguredExpiration() throws Exception {
        RefreshTokenService service = new RefreshTokenService(
                refreshTokenRepository,
                new JwtProperties("issuer", "secret", 15, 7));
        User user = new User();

        String rawToken = service.createRefreshToken(user);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken storedToken = tokenCaptor.getValue();

        String expectedHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        assertNotEquals(rawToken, storedToken.getRefreshToken());
        assertEquals(expectedHash, storedToken.getRefreshToken());
        assertEquals(user, storedToken.getUser());
        assertTrue(storedToken.getExpiresAt().isAfter(OffsetDateTime.now().plusDays(6)));
        assertTrue(storedToken.getExpiresAt().isBefore(OffsetDateTime.now().plusDays(8)));
    }

    @Test
    void revokeAllForUserDeletesOnlyTokensBelongingToThatUser() {
        RefreshTokenService service = new RefreshTokenService(
                refreshTokenRepository,
                new JwtProperties("issuer", "secret", 15, 7));
        User user = new User();
        user.setId(UUID.randomUUID());

        service.revokeAllForUser(user);

        verify(refreshTokenRepository).deleteAllByUser(user);
    }
}
