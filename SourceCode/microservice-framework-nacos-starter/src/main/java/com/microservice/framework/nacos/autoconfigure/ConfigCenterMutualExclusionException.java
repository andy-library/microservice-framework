package com.microservice.framework.nacos.autoconfigure;

/**
 * Raised when more than one framework configuration center is enabled.
 *
 * @author Andy Yang
 */
public class ConfigCenterMutualExclusionException extends RuntimeException {

    public ConfigCenterMutualExclusionException(String message) {
        super(message);
    }
}
