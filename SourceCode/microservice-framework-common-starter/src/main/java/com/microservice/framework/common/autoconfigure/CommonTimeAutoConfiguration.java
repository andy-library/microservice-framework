package com.microservice.framework.common.autoconfigure;

import com.microservice.framework.common.CommonProperties;
import com.microservice.framework.common.time.FrameworkClock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 时间/时钟自动配置
 * <p>
 * 根据 {@code framework.common.time.enabled} 属性决定是否激活，
 * 默认启用。注册 {@link FrameworkClock} Bean，所有日期时间操作
 * 使用配置的时区，禁止依赖操作系统默认时区。
 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties(CommonProperties.class)
@ConditionalOnProperty(prefix = "framework.common.time", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CommonTimeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FrameworkClock frameworkClock(CommonProperties properties) {
        return FrameworkClock.of(properties.getTime().getTimeZone());
    }
}
