package com.microservice.framework.nacos.autoconfigure;

import com.microservice.framework.nacos.ConfigGovernanceProperties;
import com.microservice.framework.nacos.NacosProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Nacos 配置中心自动配置
 * <p>
 * 根据 {@code framework.nacos.enabled} 属性决定是否激活 Nacos 配置中心集成，
 * 默认启用。真实 Nacos ConfigData 加载由 {@link NacosConfigDataEnvironmentPostProcessor}
 * 在配置数据解析前完成。
 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties({NacosProperties.class, ConfigGovernanceProperties.class})
@ConditionalOnProperty(prefix = "framework.nacos", name = "enabled", havingValue = "true")
public class NacosConfigAutoConfiguration {
}
