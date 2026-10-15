package com.overcode250204.identityservice.dto.apikey;

import java.util.List;

/**
 * CreateApiKeyRequest — Request body standard input class for generating a new
 * Token Key.
 */
public record CreateApiKeyRequest(
        String name,
        List<String> scopes) {
}
