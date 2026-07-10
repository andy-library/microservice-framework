package com.microservice.framework.common.autoconfigure;

import com.microservice.framework.common.CommonProperties;
import com.microservice.framework.common.context.ThreadLocalContextAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 轻量上下文传播自动配置
 * <p>
 * 根据 {@code framework.common.context.enabled} 属性决定是否激活，
 * 默认启用。注册 {@link ThreadLocalContextAdapter} Bean，
 * 提供 ThreadLocal 级别的上下文绑定与跨线程传播支持。
 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties(CommonProperties.class)
@ConditionalOnProperty(prefix = "framework.common.context", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CommonContextAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ThreadLocalContextAdapter threadLocalContextAdapter() {
        return new ThreadLocalContextAdapter();
    }
}
