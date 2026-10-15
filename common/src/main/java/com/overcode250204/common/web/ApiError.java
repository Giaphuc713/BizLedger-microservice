package com.overcode250204.common.web;

import java.util.Map;

/**
 * ApiError — Structured error body inside ApiResponse.
 *
 * Usage:
 * ApiError.of("USER_NOT_FOUND", "No user with that ID")
 * ApiError.validation(fieldErrors)
 */
public record ApiError(
        String code,
        String detail,
        Map<String, String> validationErrors) {
    public static ApiError of(String code, String detail) {
        return new ApiError(code, detail, null);
    }

    public static ApiError validation(Map<String, String> validationErrors) {
        return new ApiError("VALIDATION_ERROR", "Invalid request body", validationErrors);
    }
}
