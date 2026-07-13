package com.microservice.framework.audit.api;

/**
 * Supplies the secret key used to sign audit tamper evidence.
 * <p>
 * Production applications should provide this bean from an external secret
 * manager or equivalent secure configuration source.
 */
@FunctionalInterface
public interface AuditTamperEvidenceKeyProvider {

    /**
     * Return the current signing key bytes.
     *
     * @return non-empty key material
     */
    byte[] currentKey();
}
