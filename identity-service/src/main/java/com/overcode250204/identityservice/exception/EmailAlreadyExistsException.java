package com.overcode250204.identityservice.exception;

import com.overcode250204.common.exception.DuplicateResourceException;

/** Thrown when a user with the same email is already registered. */
public class EmailAlreadyExistsException extends DuplicateResourceException {
    public EmailAlreadyExistsException(String email) {
        super(IdentityErrorCode.EMAIL_ALREADY_EXISTS,
                "Email is already registered: " + email);
    }
}
