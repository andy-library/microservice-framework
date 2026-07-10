package com.microservice.framework.nacos.autoconfigure;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Failure analyzer for configuration center mutual exclusion violations.
 *
 * @author Andy Yang
 */
public class ConfigCenterMutualExclusionFailureAnalyzer
        extends AbstractFailureAnalyzer<ConfigCenterMutualExclusionException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, ConfigCenterMutualExclusionException cause) {
        return new FailureAnalysis(
                cause.getMessage(),
                "Choose exactly one config starter for the application and set the other one to disabled.",
                cause
        );
    }
}
