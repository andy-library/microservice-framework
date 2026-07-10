package com.microservice.framework.database.autoconfigure;

import com.microservice.framework.common.error.FrameworkErrorCode;
import com.microservice.framework.common.error.FrameworkException;
import com.microservice.framework.database.DatabaseProperties;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Database production safety auto-configuration.
 * <p>
 * Centralizes fail-fast checks for settings that are acceptable in local or
 * demo environments but unsafe for production microservices.
 */
@AutoConfiguration
@EnableConfigurationProperties(DatabaseProperties.class)
public class DatabaseProductionSafetyAutoConfiguration {

    private static final String MODULE = "DATABASE";
    private static final long PROD_MAX_CONNECTION_TIMEOUT_MS = 5_000L;
    private static final long PROD_MAX_SLOW_SQL_THRESHOLD_MS = 5_000L;
    private static final int PROD_MAX_TRANSACTION_TIMEOUT_SECONDS = 60;
    private static final int PROD_MAX_OUTBOX_FETCH_SIZE = 1_000;

    /**
     * Creates a startup validator for production database settings.
     *
     * @param properties Database properties
     * @param environment Spring environment
     * @return production safety validator
     */
    @Bean
    public SmartInitializingSingleton databaseProductionSafetyValidator(
            DatabaseProperties properties,
            Environment environment) {
        return () -> {
            if (!environment.acceptsProfiles(Profiles.of("prod"))) {
                return;
            }
            validateDataSource(properties.getDataSource());
            validateTransaction(properties.getTransaction());
            validateReadWriteSplitting(properties.getRw());
            validateOutbox(properties.getOutbox());
        };
    }

    private static void validateDataSource(DatabaseProperties.DataSourceProperties dataSource) {
        if (dataSource.getTimeout() > PROD_MAX_CONNECTION_TIMEOUT_MS) {
            throw new FrameworkException(
                    FrameworkErrorCode.of(MODULE, "GOVERNANCE", 1),
                    "framework.database.data-source.timeout must not exceed 5000ms in prod profile");
        }
        if (dataSource.getSlowSqlThreshold() <= 0) {
            throw new FrameworkException(
                    FrameworkErrorCode.of(MODULE, "GOVERNANCE", 2),
                    "framework.database.data-source.slow-sql-threshold must be enabled in prod profile");
        }
        if (dataSource.getSlowSqlThreshold() > PROD_MAX_SLOW_SQL_THRESHOLD_MS) {
            throw new FrameworkException(
                    FrameworkErrorCode.of(MODULE, "GOVERNANCE", 3),
                    "framework.database.data-source.slow-sql-threshold must not exceed 5000ms in prod profile");
        }
    }

    private static void validateTransaction(DatabaseProperties.TransactionProperties transaction) {
        if (transaction.getDefaultTimeout() > PROD_MAX_TRANSACTION_TIMEOUT_SECONDS) {
            throw new FrameworkException(
                    FrameworkErrorCode.of(MODULE, "GOVERNANCE", 4),
                    "framework.database.transaction.default-timeout must not exceed 60s in prod profile");
        }
    }

    private static void validateReadWriteSplitting(DatabaseProperties.RwProperties rw) {
        if (rw.isEnabled() && !rw.isReadAfterWriteRoutePrimary()) {
            throw new FrameworkException(
                    FrameworkErrorCode.of(MODULE, "GOVERNANCE", 5),
                    "framework.database.rw.read-after-write-route-primary cannot be disabled in prod profile");
        }
    }

    private static void validateOutbox(DatabaseProperties.OutboxProperties outbox) {
        if (outbox.isEnabled() && outbox.isAutoCreateTable()) {
            throw new FrameworkException(
                    FrameworkErrorCode.of(MODULE, "GOVERNANCE", 6),
                    "framework.database.outbox.auto-create-table cannot be enabled in prod profile");
        }
        if (outbox.getMaxFetchSize() > PROD_MAX_OUTBOX_FETCH_SIZE) {
            throw new FrameworkException(
                    FrameworkErrorCode.of(MODULE, "GOVERNANCE", 7),
                    "framework.database.outbox.max-fetch-size must not exceed 1000 in prod profile");
        }
    }
}
