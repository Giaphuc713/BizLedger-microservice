package com.overcode250204.identityservice.exception;

import com.overcode250204.common.exception.ForbiddenOperationException;

/** Thrown when an inactive or locked user attempts to authenticate. */
public class UserInactiveException extends ForbiddenOperationException {
    public UserInactiveException(String email) {
        super(IdentityErrorCode.USER_INACTIVE,
                "User account is inactive or locked: " + email);
    }
}
