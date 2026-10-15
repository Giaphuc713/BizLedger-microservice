package com.overcode250204.identityservice.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bizledger.jwt")
public record JwtProperties( String issuer,
                             String secret,
                             long accessTokenExpirationMinutes) {
}
