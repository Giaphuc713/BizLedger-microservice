package com.overcode250204.identityservice.dto.auth;

import com.overcode250204.identityservice.entity.User;

public record Rotation(User user, String refreshToken) {
}