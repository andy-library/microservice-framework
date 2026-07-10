package com.microservice.framework.fieldencryption.api;

/**
 * Provides raw key material for field encryption.
 * <p>
 * Implementations must keep key material confidential. The returned bytes are
 * used only inside the encryption component and must never be logged, exposed in
 * API responses, or embedded into ciphertext.
 */
public interface KeyMaterialProvider {

    /**
     * Returns AES key material for the specified key id.
     *
     * @param keyId key identifier stored in ciphertext metadata
     * @return raw AES key material
     */
    byte[] getKeyMaterial(String keyId);
}
