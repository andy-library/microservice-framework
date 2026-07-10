package com.microservice.framework.apollo.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NacosExclusionFailureAnalyzer 测试
 * <p>
 * 验证 Nacos Starter 互斥失败分析器对冲突异常的分析能力。
 *
 * @author Andy Yang
 */
class NacosExclusionFailureAnalyzerTest {

    private final NacosExclusionFailureAnalyzer analyzer = new NacosExclusionFailureAnalyzer();

    @Test
    @DisplayName("分析 NacosStarterConflictException 应生成用户友好的错误报告")
    void shouldAnalyzeConflictException() {
        NacosStarterConflictException exception = new NacosStarterConflictException();

        FailureAnalysis analysis = analyzer.analyze(exception);

        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription()).contains("Apollo Starter and Nacos Starter cannot coexist");
        assertThat(analysis.getAction()).contains("Remove the Nacos Starter dependency");
        assertThat(analysis.getAction()).contains("microservice-framework-nacos-starter");
        assertThat(analysis.getCause()).isEqualTo(exception);
    }

    @Test
    @DisplayName("自定义消息的异常应正确分析")
    void shouldAnalyzeCustomMessageException() {
        NacosStarterConflictException exception =
                new NacosStarterConflictException("Custom conflict message");

        FailureAnalysis analysis = analyzer.analyze(exception);

        assertThat(analysis.getDescription()).isEqualTo("Custom conflict message");
        assertThat(analysis.getAction()).isNotEmpty();
    }

    @Test
    @DisplayName("NacosStarterConflictException 默认消息应包含关键信息")
    void defaultExceptionMessageShouldContainKeyInfo() {
        NacosStarterConflictException exception = new NacosStarterConflictException();

        assertThat(exception.getMessage())
                .contains("Apollo Starter")
                .contains("Nacos Starter")
                .contains("cannot coexist");
    }
}
