package com.microservice.framework.logging.autoconfigure;

import ch.qos.logback.classic.LoggerContext;
import com.microservice.framework.logging.core.filter.TraceSampledFilter;
import com.microservice.framework.logging.properties.LoggingProperties;
import io.micrometer.tracing.Tracer;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Trace 关联采样自动配置
 * 
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties(LoggingProperties.class)
@ConditionalOnBean(Tracer.class)
@ConditionalOnProperty(prefix = "framework.logging.trace-sampling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TraceSamplingAutoConfiguration {

    @Bean
    public TraceSampledFilter traceSampledFilter(Tracer tracer, LoggingProperties properties) {
        LoggingProperties.TraceSamplingProperties traceSampling = properties.getTraceSampling();

        TraceSampledFilter filter = new TraceSampledFilter(tracer);
        filter.setLevelForUnsampled(traceSampling.getLevelForUnsampled());
        filter.setEnabled(traceSampling.isEnabled());

        // 注册到 Logback
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        filter.setContext(loggerContext);
        filter.start();
        loggerContext.addTurboFilter(filter);

        return filter;
    }
}
