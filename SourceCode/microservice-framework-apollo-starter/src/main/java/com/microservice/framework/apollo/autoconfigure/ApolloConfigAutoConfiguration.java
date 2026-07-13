package com.microservice.framework.apollo.autoconfigure;

import com.microservice.framework.apollo.ApolloProperties;
import com.microservice.framework.apollo.ConfigGovernanceProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Apollo 配置中心自动配置
 * <p>
 * 根据 {@code framework.apollo.enabled} 属性决定是否激活 Apollo 配置中心集成，
 * 默认启用。真实 Apollo ConfigData 加载由 {@link ApolloConfigDataEnvironmentPostProcessor}
 * 在配置数据解析前完成。
 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties({ApolloProperties.class, ConfigGovernanceProperties.class})
@ConditionalOnProperty(prefix = "framework.apollo", name = "enabled", havingValue = "true")
public class ApolloConfigAutoConfiguration {
}
