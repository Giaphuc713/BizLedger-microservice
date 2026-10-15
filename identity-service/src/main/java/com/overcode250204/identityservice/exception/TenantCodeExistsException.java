package com.overcode250204.identityservice.exception;

import com.overcode250204.common.exception.DuplicateResourceException;

/** Thrown when a tenant with the same code is already registered. */
public class TenantCodeExistsException extends DuplicateResourceException {
    public TenantCodeExistsException(String tenantCode) {
        super(IdentityErrorCode.TENANT_CODE_ALREADY_EXISTS,
                "Tenant code is already registered: " + tenantCode);
    }
}
