package com.overcode250204.identityservice.service;

import com.overcode250204.identityservice.dto.auth.AuthResponse;
import com.overcode250204.identityservice.dto.auth.SsoLoginRequest;

import java.util.List;
import java.util.Map;

/**
 * ISsoMfaComplianceService — Service interface defining compliance contracts
 * for SSO providers, Multi-Factor Authentication (MFA), GDPR/data exports,
 * policies, and rate limits.
 */
public interface ISsoMfaComplianceService {

    /**
     * Authenticates a user federation request using third-party OIDC ID token.
     *
     * @param request SSO login request payload
     * @return standard verification AuthResponse on success
     */
    AuthResponse ssoLogin(SsoLoginRequest request);

    /**
     * Lists active SSO connection providers configured for the identity realm.
     *
     * @return active SSO providers details
     */
    List<Map<String, Object>> getSsoProviders();

    /**
     * Initiates the enrollment flow of a multi-factor authentication method (e.g.
     * TOTP secrets generation).
     *
     * @return map with MFA enrollment configuration properties (secret Key, QR
     *         barcode URI)
     * @throws com.overcode250204.identityservice.exception.UserNotFoundException if
     *                                                                                current
     *                                                                                auditing
     *                                                                                subject
     *                                                                                is
     *                                                                                missing
     */
    Map<String, Object> enrollMfa();

    /**
     * Verifies the submitted OTP code to finalize MFA activation.
     *
     * @param code verification OTP passcode
     * @return verification state properties map
     * @throws com.overcode250204.identityservice.exception.UserNotFoundException if
     *                                                                                current
     *                                                                                subject
     *                                                                                is
     *                                                                                missing
     * @throws org.springframework.security.authentication.BadCredentialsException    if
     *                                                                                code
     *                                                                                is
     *                                                                                invalid
     */
    Map<String, Object> verifyMfa(String code);

    /**
     * Retrieves API throttling rate limit parameters for a given tenant.
     *
     * @param tenantId unique identifier of the tenant
     * @return rate limit definitions map
     */
    Map<String, Object> getRateLimit(String tenantId);

    /**
     * Registers a GDPR data export request for a user identity.
     *
     * @param subjectId identifier of the subject requesting export
     * @return status details of request processing
     * @throws com.overcode250204.identityservice.exception.UserNotFoundException if
     *                                                                                user
     *                                                                                does
     *                                                                                not
     *                                                                                exist
     */
    Map<String, Object> requestDataExport(String subjectId);

    /**
     * Submits a GDPR right to be forgotten data erasure request.
     *
     * @param subjectId identifier of the subject requesting erasure
     * @return deletion request schedule status details
     * @throws com.overcode250204.identityservice.exception.UserNotFoundException if
     *                                                                                user
     *                                                                                does
     *                                                                                not
     *                                                                                exist
     */
    Map<String, Object> requestErasure(String subjectId);

    /**
     * Exports security logs format to SIEM provider systems.
     *
     * @param format export standard format (e.g., CEF, JSON)
     * @return payload of SIEM audit logs
     */
    Map<String, Object> exportAuditSiem(String format);

    /**
     * Lists active change data capture subscriptions.
     *
     * @return collection of subscription configurations
     */
    List<Map<String, Object>> getCdcSubscriptions();

    /**
     * Registers a new change data capture event queue subscription.
     *
     * @param body subscription parameters mapping details
     * @return details of the created subscription
     */
    Map<String, Object> createCdcSubscription(Map<String, Object> body);
}
