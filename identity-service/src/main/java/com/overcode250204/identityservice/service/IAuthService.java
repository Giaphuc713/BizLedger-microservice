package com.overcode250204.identityservice.service;

import com.overcode250204.identityservice.dto.auth.AuthResponse;
import com.overcode250204.identityservice.dto.auth.LoginRequest;
import com.overcode250204.identityservice.dto.auth.MeResponse;
import com.overcode250204.identityservice.dto.auth.RegisterTenantRequest;
import com.overcode250204.identityservice.entity.AuthenticatedUser;

/**
 * IAuthService — contract for authentication and tenant registration.
 */
public interface IAuthService {

    /**
     * Registers a new tenant and creates the owner account.
     *
     * @throws com.overcode250204.identityservice.exception.TenantCodeExistsException   if
     *                                                                                      the
     *                                                                                      tenant
     *                                                                                      code
     *                                                                                      is
     *                                                                                      already
     *                                                                                      taken
     * @throws com.overcode250204.identityservice.exception.EmailAlreadyExistsException if
     *                                                                                      the
     *                                                                                      owner
     *                                                                                      email
     *                                                                                      is
     *                                                                                      already
     *                                                                                      registered
     */
    AuthResponse registerTenant(RegisterTenantRequest request);

    /**
     * Authenticates a user and returns an access token.
     *
     * @throws com.overcode250204.identityservice.exception.UserInactiveException if
     *                                                                                the
     *                                                                                user
     *                                                                                account
     *                                                                                is
     *                                                                                locked/inactive
     * @throws org.springframework.security.authentication.BadCredentialsException    if
     *                                                                                credentials
     *                                                                                are
     *                                                                                wrong
     */
    AuthResponse login(LoginRequest request);

    /**
     * Returns profile info for the currently authenticated user from the JWT
     * principal.
     */
    MeResponse me(AuthenticatedUser user);
}
