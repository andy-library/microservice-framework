package com.microservice.framework.logging.autoconfigure;

import ch.qos.logback.classic.LoggerContext;
import com.microservice.framework.logging.core.masking.PatternMaskingConverter;
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
 * 数据脱敏自动配置
 * 
 * <h3>功能概述</h3>
 * <p>
 * 自动配置日志数据脱敏功能，支持以下特性：
 * <ul>
 * <li>可配置的脱敏规则（手机号、身份证、邮箱、银行卡号等）</li>
 * <li>可选的 Spring Cloud 动态刷新支持</li>
 * <li>无 Spring Cloud 环境下的优雅降级</li>
 * </ul>
 * 
 * <h3>配置层次</h3>
 * <p>
 * 配置分为两层，自动选择最优策略：
 * <ol>
 * <li><b>增强配置</b>：当 Spring Cloud Context 可用时，支持 {@code @RefreshScope} 动态刷新</li>
 * <li><b>基础配置</b>：无 Spring Cloud 环境下的降级配置，功能完整但不支持动态刷新</li>
 * </ol>
 * 
 * <h3>配置示例</h3>
 * 
 * <pre>
 * framework:
 *   logging:
 *     masking:
 *       enabled: true                    # 启用脱敏
 *       enabled-default-rules:           # 启用的规则
 *         - MOBILE_PHONE
 *         - ID_CARD
 * </pre>
 * 
 * @author Andy Yang
 * @see PatternMaskingConverter
 * @see LoggingProperties.MaskingProperties
 */
@AutoConfiguration
@EnableConfigurationProperties(LoggingProperties.class)
@ConditionalOnProperty(prefix = "framework.logging.masking", name = "enabled", havingValue = "true")
public class MaskingLoggingAutoConfiguration {

    /**
     * 增强配置（支持 Spring Cloud 动态刷新）
     * 
     * <p>
     * 条件：当 {@code org.springframework.cloud.context.config.annotation.RefreshScope}
     * 类存在于类路径时生效。
     * 
     * <p>
     * 特性：
     * <ul>
     * <li>支持通过 {@code POST /actuator/refresh} 动态更新脱敏规则</li>
     * <li>配置变更无需重启应用</li>
     * </ul>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.cloud.context.config.annotation.RefreshScope")
    static class RefreshableMaskingConfiguration {

        @Bean
        @org.springframework.cloud.context.config.annotation.RefreshScope
        public PatternMaskingConverter patternMaskingConverter(LoggingProperties properties) {
            return createConverter(properties);
        }
    }

    /**
     * 基础配置（无 Spring Cloud）
     * 
     * <p>
     * 条件：当 {@link PatternMaskingConverter} Bean 不存在时生效。
     * 由于增强配置优先级更高（先声明），此配置仅在 Spring Cloud 不可用时激活。
     * 
     * <p>
     * 功能与增强配置完全相同，仅缺少动态刷新能力。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.cloud.context.config.annotation.RefreshScope")
    @ConditionalOnMissingBean(PatternMaskingConverter.class)
    static class BaseMaskingConfiguration {

        @Bean
        public PatternMaskingConverter patternMaskingConverter(LoggingProperties properties) {
            return createConverter(properties);
        }
    }

    /**
     * 创建脱敏转换器的工厂方法
     * 
     * <p>
     * 统一的创建逻辑，确保基础配置和增强配置行为一致。
     * 
     * @param properties 日志配置属性
     * @return 配置完成的脱敏转换器
     */
    private static PatternMaskingConverter createConverter(LoggingProperties properties) {
        LoggingProperties.MaskingProperties masking = properties.getMasking();

        // 创建并配置转换器
        PatternMaskingConverter converter = new PatternMaskingConverter();
        converter.setEnabled(masking.isEnabled());

        // 设置启用的脱敏规则（支持按需启用部分规则）
        converter.setEnabledRules(masking.getEnabledDefaultRules());

        // 将转换器注册到 Logback 上下文
        // 这样 logback-spring.xml 中可以通过 %maskedMessage 引用此转换器
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        converter.setContext(loggerContext);
        converter.start();

        // 以名称键存储转换器，便于运行时查找和调试
        loggerContext.putObject("patternMaskingConverter", converter);

        return converter;
    }
}
