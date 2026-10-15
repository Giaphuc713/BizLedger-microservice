package com.overcode250204.common.web;

import java.time.OffsetDateTime;

/**
 * ApiResponse — Uniform HTTP response wrapper for ALL BizLedger services.
 *
 * Usage pattern (in every service controller):
 * return ApiResponse.success(data);
 * return ApiResponse.failure("message", ApiError.of("CODE", "detail"));
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        ApiError error,
        OffsetDateTime timestamp) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data, null, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> failure(String message, ApiError error) {
        return new ApiResponse<>(false, message, null, error, OffsetDateTime.now());
    }

    public static ApiResponse<Void> noContent(String message) {
        return new ApiResponse<>(true, message, null, null, OffsetDateTime.now());
    }
}
