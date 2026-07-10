package com.microservice.framework.apollo.autoconfigure;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Nacos Starter 互斥失败分析器
 * <p>
 * 当 {@link NacosStarterConflictException} 导致应用启动失败时，
 * 将异常转换为用户友好的错误报告，明确指出 Apollo 和 Nacos
 * 不能同时使用，并给出解决建议。
 * <p>
 * 此分析器通过 {@code META-INF/spring.factories} 注册，
 * 以确保在自动配置加载前即可生效。
 *
 * @author Andy Yang
 */
public class NacosExclusionFailureAnalyzer extends AbstractFailureAnalyzer<NacosStarterConflictException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, NacosStarterConflictException cause) {
        return new FailureAnalysis(
                cause.getMessage(),
                "Remove the Nacos Starter dependency from your project (e.g., " +
                        "microservice-framework-nacos-starter), or switch to using Nacos Starter " +
                        "instead of Apollo Starter by removing the Apollo dependency.",
                cause
        );
    }
}
