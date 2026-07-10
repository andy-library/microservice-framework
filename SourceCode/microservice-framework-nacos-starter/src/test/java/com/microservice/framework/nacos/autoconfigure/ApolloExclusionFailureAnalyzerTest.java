package com.microservice.framework.nacos.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ApolloExclusionFailureAnalyzer 测试
 * <p>
 * 验证 Apollo Starter 互斥失败分析器对冲突异常的分析能力。
 *
 * @author Andy Yang
 */
class ApolloExclusionFailureAnalyzerTest {

    private final ApolloExclusionFailureAnalyzer analyzer = new ApolloExclusionFailureAnalyzer();

    @Test
    @DisplayName("分析 ApolloStarterConflictException 应生成用户友好的错误报告")
    void shouldAnalyzeConflictException() {
        ApolloStarterConflictException exception = new ApolloStarterConflictException();

        FailureAnalysis analysis = analyzer.analyze(exception);

        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription()).contains("Nacos Starter and Apollo Starter cannot coexist");
        assertThat(analysis.getAction()).contains("Remove the Apollo Starter dependency");
        assertThat(analysis.getAction()).contains("microservice-framework-apollo-starter");
        assertThat(analysis.getCause()).isEqualTo(exception);
    }

    @Test
    @DisplayName("自定义消息的异常应正确分析")
    void shouldAnalyzeCustomMessageException() {
        ApolloStarterConflictException exception =
                new ApolloStarterConflictException("Custom conflict message");

        FailureAnalysis analysis = analyzer.analyze(exception);

        assertThat(analysis.getDescription()).isEqualTo("Custom conflict message");
        assertThat(analysis.getAction()).isNotEmpty();
    }

    @Test
    @DisplayName("ApolloStarterConflictException 默认消息应包含关键信息")
    void defaultExceptionMessageShouldContainKeyInfo() {
        ApolloStarterConflictException exception = new ApolloStarterConflictException();

        assertThat(exception.getMessage())
                .contains("Nacos Starter")
                .contains("Apollo Starter")
                .contains("cannot coexist");
    }
}
