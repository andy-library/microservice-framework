package com.microservice.framework.observability.autoconfigure.tracing;

import com.microservice.framework.observability.annotation.Traceable;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

/**
 * Traceable 切面自动配置
 * 自动为 @Traceable、@Scheduled、@KafkaListener 等非 HTTP 入口创建 Trace
 * 
 * @author Andy Yang
 */
@AutoConfiguration
@ConditionalOnClass({ Tracer.class, Aspect.class })
@ConditionalOnProperty(prefix = "framework.observability.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TraceableAspectAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    static class TraceableAspectConfiguration {

        @Bean
        public TraceableAspect traceableAspect(Tracer tracer) {
            return new TraceableAspect(tracer);
        }
    }

    /**
     * Traceable 切面
     */
    @Aspect
    static class TraceableAspect {

        private static final Logger log = LoggerFactory.getLogger(TraceableAspect.class);

        private final Tracer tracer;

        TraceableAspect(Tracer tracer) {
            this.tracer = tracer;
        }

        /**
         * 拦截 @Traceable 注解的方法
         */
        @Around("@annotation(com.microservice.framework.observability.annotation.Traceable) || " +
                "@annotation(org.springframework.scheduling.annotation.Scheduled)")
        public Object traceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
            // 检查是否已存在 Trace 上下文
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                // 已存在 Trace，直接执行
                return joinPoint.proceed();
            }

            // 创建新的根 Span
            String spanName = getSpanName(joinPoint);
            Span newSpan = tracer.nextSpan().name(spanName).start();

            try (Tracer.SpanInScope ws = tracer.withSpan(newSpan)) {
                log.debug("Created new trace for method: {}", spanName);
                return joinPoint.proceed();
            } catch (Throwable ex) {
                newSpan.error(ex);
                throw ex;
            } finally {
                newSpan.end();
            }
        }

        /**
         * 获取 Span 名称
         */
        private String getSpanName(ProceedingJoinPoint joinPoint) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            // 优先使用 @Traceable 注解的 name
            Traceable traceable = method.getAnnotation(Traceable.class);
            if (traceable != null && !traceable.name().isEmpty()) {
                return traceable.name();
            }

            // 检查是否是 @Scheduled
            Scheduled scheduled = method.getAnnotation(Scheduled.class);
            if (scheduled != null) {
                return "scheduled:" + method.getName();
            }

            // 默认使用类名.方法名
            return signature.getDeclaringType().getSimpleName() + "." + method.getName();
        }
    }
}
