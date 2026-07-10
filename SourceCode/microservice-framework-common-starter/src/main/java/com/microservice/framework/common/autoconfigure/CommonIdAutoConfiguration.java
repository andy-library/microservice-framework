package com.microservice.framework.common.autoconfigure;

import com.microservice.framework.common.CommonProperties;
import com.microservice.framework.common.error.FrameworkErrorCode;
import com.microservice.framework.common.error.FrameworkException;
import com.microservice.framework.common.id.IdGenerator;
import com.microservice.framework.common.id.SnowflakeIdGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 分布式 ID 自动配置
 * <p>
 * 根据 {@code framework.common.id.enabled} 属性决定是否激活，
 * 默认启用。当 {@code framework.common.id.type} 为 {@code snowflake}
 * （默认值）时注册 {@link SnowflakeIdGenerator} Bean。
 * <p>
 * {@code framework.common.id.worker-id} 必须显式配置（0-1023），
 * 否则启动时抛出 {@link FrameworkException}。
 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties(CommonProperties.class)
@ConditionalOnProperty(prefix = "framework.common.id", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CommonIdAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "framework.common.id", name = "type", havingValue = "snowflake", matchIfMissing = true)
    public IdGenerator snowflakeIdGenerator(CommonProperties properties) {
        Long workerId = properties.getId().getWorkerId();
        if (workerId == null) {
            throw new FrameworkException(
                FrameworkErrorCode.COMMON_ID_WORKER_INVALID,
                "framework.common.id.worker-id must be explicitly configured (0-1023)");
        }
        return SnowflakeIdGenerator.of(workerId, properties.getId().getClockBackwardTolerance());
    }
}
