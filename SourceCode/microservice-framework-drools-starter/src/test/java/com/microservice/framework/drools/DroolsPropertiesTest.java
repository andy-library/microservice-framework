package com.microservice.framework.drools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DroolsProperties 绑定和默认值测试
 * <p>
 * 验证各嵌套配置组的默认值符合设计规格，
 * 并验证属性绑定后值能正确覆盖默认值。
 *
 * @author Andy Yang
 */
class DroolsPropertiesTest {

    @Test
    @DisplayName("默认规则配置应与设计规格一致")
    void defaultRulePropertiesShouldMatchSpecification() {
        DroolsProperties properties = new DroolsProperties();
        DroolsProperties.RuleProperties rule = properties.getRule();

        assertThat(rule.getEnabled()).isTrue();
        assertThat(rule.getRuleFiles()).isEmpty();
        assertThat(rule.getGroups()).isEmpty();
        assertThat(rule.getValidateOnStartup()).isTrue();
    }

    @Test
    @DisplayName("默认会话配置应与设计规格一致")
    void defaultSessionPropertiesShouldMatchSpecification() {
        DroolsProperties properties = new DroolsProperties();
        DroolsProperties.SessionProperties session = properties.getSession();

        assertThat(session.getMaxSessions()).isEqualTo(100);
        assertThat(session.getSessionTimeout()).isEqualTo(30000L);
    }

    @Test
    @DisplayName("默认运行治理配置应与设计规格一致")
    void defaultGovernancePropertiesShouldMatchSpecification() {
        DroolsProperties properties = new DroolsProperties();
        DroolsProperties.GovernanceProperties governance = properties.getGovernance();

        assertThat(governance.getAuditEnabled()).isTrue();
        assertThat(governance.getMaxExecutionTimeMs()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("自定义属性值应能覆盖默认值")
    void customPropertyValuesShouldOverrideDefaults() {
        DroolsProperties properties = new DroolsProperties();

        properties.getRule().setEnabled(false);
        properties.getRule().setRuleFiles(Arrays.asList("rules/discount.drl", "rules/risk.drl"));
        properties.getRule().setGroups(Arrays.asList("discount", "risk"));
        properties.getRule().setValidateOnStartup(false);

        assertThat(properties.getRule().getEnabled()).isFalse();
        assertThat(properties.getRule().getRuleFiles()).containsExactly("rules/discount.drl", "rules/risk.drl");
        assertThat(properties.getRule().getGroups()).containsExactly("discount", "risk");
        assertThat(properties.getRule().getValidateOnStartup()).isFalse();

        properties.getSession().setMaxSessions(50);
        properties.getSession().setSessionTimeout(60000L);
        assertThat(properties.getSession().getMaxSessions()).isEqualTo(50);
        assertThat(properties.getSession().getSessionTimeout()).isEqualTo(60000L);

        properties.getGovernance().setAuditEnabled(false);
        properties.getGovernance().setMaxExecutionTimeMs(10000L);
        assertThat(properties.getGovernance().getAuditEnabled()).isFalse();
        assertThat(properties.getGovernance().getMaxExecutionTimeMs()).isEqualTo(10000L);
    }
}
