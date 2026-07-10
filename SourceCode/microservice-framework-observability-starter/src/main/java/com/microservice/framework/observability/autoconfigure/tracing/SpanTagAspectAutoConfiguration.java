package com.microservice.framework.observability.autoconfigure.tracing;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * SpanTag 切面自动配置
 * 自动将 @SpanTag 标注的参数值注入到当前 Span 的标签中
 * 
 * @author Andy Yang
 */
@AutoConfiguration
@ConditionalOnClass({ Tracer.class, Aspect.class })
@ConditionalOnProperty(prefix = "framework.observability.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SpanTagAspectAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(Tracer.class)
    static class SpanTagAspectConfiguration {

        @Bean
        public SpanTagAspect spanTagAspect(Tracer tracer) {
            return new SpanTagAspect(tracer);
        }
    }

    /**
     * SpanTag 切面
     */
    @Aspect
    static class SpanTagAspect {

        private static final Logger log = LoggerFactory.getLogger(SpanTagAspect.class);

        private final Tracer tracer;

        SpanTagAspect(Tracer tracer) {
            this.tracer = tracer;
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
                            String tagValue = args[i].toString();
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
    }
}
