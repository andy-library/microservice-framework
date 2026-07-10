package com.microservice.framework.nacos.autoconfigure;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Apollo Starter 互斥失败分析器
 * <p>
 * 当 {@link ApolloStarterConflictException} 导致应用启动失败时，
 * 将异常转换为用户友好的错误报告，明确指出 Nacos 和 Apollo
 * 不能同时使用，并给出解决建议。
 * <p>
 * 此分析器通过 {@code META-INF/spring.factories} 注册，
 * 以确保在自动配置加载前即可生效。
 *
 * @author Andy Yang
 */
public class ApolloExclusionFailureAnalyzer extends AbstractFailureAnalyzer<ApolloStarterConflictException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, ApolloStarterConflictException cause) {
        return new FailureAnalysis(
                cause.getMessage(),
                "Remove the Apollo Starter dependency from your project (e.g., " +
                        "microservice-framework-apollo-starter), or switch to using Apollo Starter " +
                        "instead of Nacos Starter by removing the Nacos dependency.",
                cause
        );
    }
}
