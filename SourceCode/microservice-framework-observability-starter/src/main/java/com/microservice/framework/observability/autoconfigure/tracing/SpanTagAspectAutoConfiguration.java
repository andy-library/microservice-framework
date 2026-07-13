package com.microservice.framework.observability.autoconfigure.tracing;

import com.microservice.framework.observability.ObservabilityProperties;
import com.microservice.framework.observability.annotation.SpanTag;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Locale;
import java.util.Set;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SpanTag 切面自动配置
 * 自动将 @SpanTag 标注的参数值注入到当前 Span 的标签中
 * 
 * @author Andy Yang
 */
@AutoConfiguration
@ConditionalOnClass({ Tracer.class, Aspect.class })
@ConditionalOnProperty(prefix = "framework.observability.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ObservabilityProperties.class)
public class SpanTagAspectAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(Tracer.class)
    static class SpanTagAspectConfiguration {

        @Bean
        public SpanTagAspect spanTagAspect(Tracer tracer, ObservabilityProperties properties) {
            return new SpanTagAspect(tracer, properties.getTracing().getSpanTags());
        }
    }

    /**
     * SpanTag 切面
     */
    @Aspect
    static class SpanTagAspect {

        private static final Logger log = LoggerFactory.getLogger(SpanTagAspect.class);

        private final Tracer tracer;
        private final Set<String> sensitiveKeys;
        private final Set<String> highCardinalityKeys;
        private final List<Pattern> sensitiveValuePatterns;
        private final int maxValueLength;
        private final String redactedValue;

        SpanTagAspect(Tracer tracer) {
            this(tracer, new ObservabilityProperties.SpanTagProperties());
        }

        SpanTagAspect(Tracer tracer, ObservabilityProperties.SpanTagProperties properties) {
            this.tracer = tracer;
            ObservabilityProperties.SpanTagProperties effectiveProperties = properties != null
                    ? properties : new ObservabilityProperties.SpanTagProperties();
            this.sensitiveKeys = normalizeKeys(effectiveProperties.getSensitiveKeys());
            this.highCardinalityKeys = normalizeKeys(effectiveProperties.getHighCardinalityKeys());
            this.sensitiveValuePatterns = compilePatterns(effectiveProperties.getSensitiveValuePatterns());
            this.maxValueLength = Math.max(0, effectiveProperties.getMaxValueLength());
            this.redactedValue = hasText(effectiveProperties.getRedactedValue())
                    ? effectiveProperties.getRedactedValue() : "[REDACTED]";
        }

        /**
         * 拦截包含 @SpanTag 参数的方法
         */
        @Around("execution(* *(.., @com.microservice.framework.observability.annotation.SpanTag (*), ..))")
        public Object addSpanTags(ProceedingJoinPoint joinPoint) throws Throwable {
            try {
                Span currentSpan = tracer.currentSpan();
                if (currentSpan != null) {
                    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                    Method method = signature.getMethod();
                    Parameter[] parameters = method.getParameters();
                    Object[] args = joinPoint.getArgs();

                    for (int i = 0; i < parameters.length; i++) {
                        if (i >= args.length) {
                            break;
                        }
                        SpanTag spanTag = parameters[i].getAnnotation(SpanTag.class);
                        if (spanTag != null && args[i] != null) {
                            String tagKey = spanTag.value();
                            String tagValue = sanitize(tagKey, args[i].toString());
                            currentSpan.tag(tagKey, tagValue);
                            log.debug("Added span tag: {}={}", tagKey, tagValue);
                        }
                    }
                }
            } catch (Throwable e) {
                log.warn("Failed to inject span tags", e);
            }

            return joinPoint.proceed();
        }

        private String sanitize(String key, String value) {
            if (isSensitiveKey(key) || isHighCardinalityKey(key) || isSensitiveValue(value)) {
                return redactedValue;
            }
            if (maxValueLength > 0 && value != null && value.length() > maxValueLength) {
                return value.substring(0, maxValueLength);
            }
            return value;
        }

        private boolean isSensitiveKey(String key) {
            if (key == null) {
                return false;
            }
            String normalized = normalizeKey(key);
            if (sensitiveKeys.contains(normalized)) {
                return true;
            }
            return normalized.contains("password")
                    || normalized.contains("secret")
                    || normalized.contains("token")
                    || normalized.contains("credential");
        }

        private boolean isHighCardinalityKey(String key) {
            return key != null && highCardinalityKeys.contains(normalizeKey(key));
        }

        private boolean isSensitiveValue(String value) {
            if (value == null) {
                return false;
            }
            for (Pattern pattern : sensitiveValuePatterns) {
                if (pattern.matcher(value).matches()) {
                    return true;
                }
            }
            return false;
        }

        private static Set<String> normalizeKeys(Set<String> keys) {
            if (keys == null || keys.isEmpty()) {
                return Collections.emptySet();
            }
            Set<String> normalized = new HashSet<>();
            for (String key : keys) {
                if (hasText(key)) {
                    normalized.add(normalizeKey(key));
                }
            }
            return Collections.unmodifiableSet(normalized);
        }

        private static String normalizeKey(String key) {
            return key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        }

        private static List<Pattern> compilePatterns(List<String> patterns) {
            if (patterns == null || patterns.isEmpty()) {
                return Collections.emptyList();
            }
            List<Pattern> compiled = new ArrayList<>();
            for (String pattern : patterns) {
                if (hasText(pattern)) {
                    compiled.add(Pattern.compile(pattern));
                }
            }
            return Collections.unmodifiableList(compiled);
        }

        private static boolean hasText(String value) {
            return value != null && !value.trim().isEmpty();
        }
    }
}
