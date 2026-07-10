package com.microservice.framework.observability.annotation;

import java.lang.annotation.*;

/**
 * Span 标签注解
 * 用于方法参数，自动将参数值注入到当前 Span 的标签中
 * 
 * @author Andy Yang
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SpanTag {

    /**
     * 标签键
     * 
     * @return 标签键
     */
    String value();
}
