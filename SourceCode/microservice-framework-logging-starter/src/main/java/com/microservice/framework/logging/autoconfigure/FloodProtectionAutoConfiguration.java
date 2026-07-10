package com.microservice.framework.logging.autoconfigure;

import ch.qos.logback.classic.LoggerContext;
import com.microservice.framework.logging.core.filter.RateLimitingTurboFilter;
import com.microservice.framework.logging.properties.LoggingProperties;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 日志风暴防护自动配置
 * 
 * <h3>功能概述</h3>
 * <p>
 * 自动配置日志限流功能，防止日志风暴导致的资源耗尽：
 * <ul>
 * <li>基于令牌桶算法实现平滑限流</li>
 * <li>可配置每秒日志数量和突发容量</li>
 * <li>可选的 Spring Cloud 动态刷新支持</li>
 * </ul>
 * 
 * <h3>配置层次</h3>
 * <p>
 * 采用双层配置策略，自动选择最优方案：
 * <ol>
 * <li><b>增强配置</b>：当 Spring Cloud Context 可用时，支持 {@code @RefreshScope} 动态刷新。
 * 运维人员可通过 {@code POST /actuator/refresh} 即时调整限流参数。</li>
 * <li><b>基础配置</b>：无 Spring Cloud 环境下的降级配置。功能完整，但需重启应用才能更新配置。</li>
 * </ol>
 * 
 * <h3>条件激活</h3>
 * <p>
 * 此配置类在以下条件满足时激活：
 * <ul>
 * <li>{@code framework.logging.flood-protection.enabled=true}（默认为 true）</li>
 * </ul>
 * 
 * <h3>配置示例</h3>
 * 
 * <pre>
 * framework:
 *   logging:
 *     flood-protection:
 *       enabled: true          # 启用限流
 *       rate: 10               # 每秒允许的日志数量
 *       burst-capacity: 100    # 突发容量
 * </pre>
 * 
 * <h3>为什么需要拆分配置</h3>
 * <p>
 * 直接使用 {@code @RefreshScope} 在无 Spring Cloud 环境下会抛出
 * {@code ClassNotFoundException}。
 * 通过 {@code @ConditionalOnClass} 条件化，确保在任何环境下都能正常工作。
 * 
 * @author Andy Yang
 * @see RateLimitingTurboFilter
 * @see LoggingProperties.FloodProtectionProperties
 */
@AutoConfiguration
@EnableConfigurationProperties(LoggingProperties.class)
@ConditionalOnProperty(prefix = "framework.logging.flood-protection", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FloodProtectionAutoConfiguration {

    /**
     * 增强配置（支持 Spring Cloud 动态刷新）
     * 
     * <p>
     * 条件：当 {@code org.springframework.cloud.context.config.annotation.RefreshScope}
     * 类存在于类路径时生效。
     * 
     * <p>
     * 由于此配置类在 {@link BaseFloodProtectionConfiguration} 之前声明，
     * 且使用 {@code @ConditionalOnClass} 而非 {@code @ConditionalOnMissingBean}，
     * 因此在 Spring Cloud 环境下优先生效。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.cloud.context.config.annotation.RefreshScope")
    static class RefreshableFloodProtectionConfiguration {

        @Bean
        @org.springframework.cloud.context.config.annotation.RefreshScope
        @ConditionalOnMissingBean(RateLimitingTurboFilter.class)
        public RateLimitingTurboFilter rateLimitingTurboFilter(LoggingProperties properties) {
            return createFilter(properties);
        }
    }

    /**
     * 基础配置（无 Spring Cloud）
     * 
     * <p>
     * 条件：当 {@link RateLimitingTurboFilter} Bean 不存在时生效。
     * 由于增强配置优先级更高（先声明），此配置仅在 Spring Cloud 不可用时激活。
     * 
     * <p>
     * 功能与增强配置完全相同，仅缺少动态刷新能力。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.cloud.context.config.annotation.RefreshScope")
    @ConditionalOnMissingBean(RateLimitingTurboFilter.class)
    static class BaseFloodProtectionConfiguration {

        @Bean
        public RateLimitingTurboFilter rateLimitingTurboFilter(LoggingProperties properties) {
            return createFilter(properties);
        }
    }

    /**
     * 创建限流过滤器的工厂方法
     * 
     * <p>
     * 统一的创建逻辑，确保基础配置和增强配置行为一致。
     * 
     * <p>
     * 职责：
     * <ol>
     * <li>从配置属性中读取限流参数</li>
     * <li>创建并配置 {@link RateLimitingTurboFilter}</li>
     * <li>将过滤器注册到 Logback 的 {@link LoggerContext}</li>
     * </ol>
     * 
     * @param properties 日志配置属性
     * @return 配置完成的限流过滤器
     */
    private static RateLimitingTurboFilter createFilter(LoggingProperties properties) {
        LoggingProperties.FloodProtectionProperties floodProtection = properties.getFloodProtection();

        // 创建并配置过滤器
        RateLimitingTurboFilter filter = new RateLimitingTurboFilter();
        filter.setRate(floodProtection.getRate());
        filter.setBurstCapacity(floodProtection.getBurstCapacity());
        filter.setEnabled(floodProtection.isEnabled());

        // 将过滤器注册到 Logback TurboFilter 链
        // TurboFilter 在日志事件创建前执行，性能开销最小
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        filter.setContext(loggerContext);
        filter.start();
        loggerContext.addTurboFilter(filter);

        return filter;
    }
}
