package com.microservice.framework.observability.annotation;

import java.lang.annotation.*;

/**
 * 可追踪注解
 * 用于标记需要自动创建 Trace 的方法（如定时任务、消息监听器等）
 * 
 * @author Andy Yang
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Traceable {

    /**
     * Span 名称
     * 如果不指定，则使用方法名
     * 
     * @return Span 名称
     */
    String name() default "";

    /**
     * Span 类型
     * 
     * @return Span 类型
     */
    String kind() default "INTERNAL";
}
