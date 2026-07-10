package com.microservice.framework.observability.annotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Traceable 和 @SpanTag 注解单元测试
 */
class TracingAnnotationsTest {

    @Test
    @DisplayName("@Traceable: 注解可正常解析")
    void testTraceable_AnnotationExists() throws NoSuchMethodException {
        Method method = AnnotatedService.class.getMethod("tracedMethod");
        Traceable annotation = method.getAnnotation(Traceable.class);

        assertNotNull(annotation);
        assertEquals("custom-span-name", annotation.value());
    }

    @Test
    @DisplayName("@Traceable: 默认值为空")
    void testTraceable_DefaultValue() throws NoSuchMethodException {
        Method method = AnnotatedService.class.getMethod("defaultTracedMethod");
        Traceable annotation = method.getAnnotation(Traceable.class);

        assertNotNull(annotation);
        assertEquals("", annotation.value());
    }

    @Test
    @DisplayName("@SpanTag: 注解可正常解析")
    void testSpanTag_AnnotationExists() throws NoSuchMethodException {
        Method method = AnnotatedService.class.getMethod("taggedMethod", String.class);
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        assertTrue(parameterAnnotations.length > 0);
        assertTrue(parameterAnnotations[0].length > 0);
        assertTrue(parameterAnnotations[0][0] instanceof SpanTag);

        SpanTag spanTag = (SpanTag) parameterAnnotations[0][0];
        assertEquals("orderId", spanTag.value());
    }

    @Test
    @DisplayName("@SpanTag: 自定义 key (已废弃，改为统一使用 value)")
    void testSpanTag_CustomKey() throws NoSuchMethodException {
        Method method = AnnotatedService.class.getMethod("customKeyMethod", String.class);
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        SpanTag spanTag = (SpanTag) parameterAnnotations[0][0];
        assertEquals("custom.key", spanTag.value());
    }

    @Test
    @DisplayName("@Traceable + @SpanTag: 组合使用")
    void testCombinedAnnotations() throws NoSuchMethodException {
        Method method = AnnotatedService.class.getMethod("combinedMethod", String.class);

        Traceable traceable = method.getAnnotation(Traceable.class);
        assertNotNull(traceable);

        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        assertTrue(parameterAnnotations[0][0] instanceof SpanTag);
    }

    // 测试用辅助类
    static class AnnotatedService {

        @Traceable("custom-span-name")
        public void tracedMethod() {
        }

        @Traceable
        public void defaultTracedMethod() {
        }

        public void taggedMethod(@SpanTag("orderId") String id) {
        }

        public void customKeyMethod(@SpanTag("custom.key") String value) {
        }

        @Traceable("combined-operation")
        public void combinedMethod(@SpanTag("userId") String userId) {
        }
    }
}
