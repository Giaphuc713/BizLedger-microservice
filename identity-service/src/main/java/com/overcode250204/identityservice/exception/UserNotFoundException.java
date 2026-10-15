package com.overcode250204.identityservice.exception;




import com.overcode250204.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(UUID userId) {
        super(IdentityErrorCode.USER_NOT_FOUND, "User not found: " + userId);
    }

    public UserNotFoundException(String email) {
        super(IdentityErrorCode.USER_NOT_FOUND, "User not found with email: " + email);
    }
}
