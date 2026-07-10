package com.microservice.framework.xxljob;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XxlJobProperties 绑定和默认值测试
 * <p>
 * 验证各嵌套配置组的默认值符合设计规格，
 * 并验证属性绑定后值能正确覆盖默认值。
 *
 * @author Andy Yang
 */
class XxlJobPropertiesTest {

    @Test
    @DisplayName("默认 enabled 配置应为 true")
    void defaultEnabledShouldBeTrue() {
        XxlJobProperties properties = new XxlJobProperties();

        assertThat(properties.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("默认 Admin 配置属性应为 null（需显式配置）")
    void defaultAdminPropertiesShouldBeNull() {
        XxlJobProperties properties = new XxlJobProperties();
        XxlJobProperties.AdminProperties admin = properties.getAdmin();

        assertThat(admin.getAddresses()).isNull();
        assertThat(admin.getAppName()).isNull();
        assertThat(admin.getAccessToken()).isNull();
    }

    @Test
    @DisplayName("默认 Executor 配置应与设计规格一致")
    void defaultExecutorPropertiesShouldMatchSpecification() {
        XxlJobProperties properties = new XxlJobProperties();
        XxlJobProperties.ExecutorProperties executor = properties.getExecutor();

        assertThat(executor.getAppName()).isNull();
        assertThat(executor.getAddress()).isNull();
        assertThat(executor.getPort()).isEqualTo(9999);
        assertThat(executor.getLogPath()).isEqualTo("/data/applogs/xxl-job/jobhandler");
        assertThat(executor.getLogRetentionDays()).isEqualTo(30);
    }

    @Test
    @DisplayName("默认幂等配置应与设计规格一致")
    void defaultIdempotencyPropertiesShouldMatchSpecification() {
        XxlJobProperties properties = new XxlJobProperties();
        XxlJobProperties.IdempotencyProperties idempotency = properties.getIdempotency();

        assertThat(idempotency.getEnabled()).isTrue();
        assertThat(idempotency.getStoreType()).isEqualTo(XxlJobProperties.StoreType.MEMORY);
    }

    @Test
    @DisplayName("默认超时配置应与设计规格一致")
    void defaultTimeoutPropertiesShouldMatchSpecification() {
        XxlJobProperties properties = new XxlJobProperties();
        XxlJobProperties.TimeoutProperties timeout = properties.getTimeout();

        assertThat(timeout.getDefaultTimeout()).isEqualTo(300);
        assertThat(timeout.getTimeoutHandler()).isEqualTo(XxlJobProperties.TimeoutHandler.FAIL);
    }

    @Test
    @DisplayName("自定义属性值应能覆盖默认值")
    void customPropertyValuesShouldOverrideDefaults() {
        XxlJobProperties properties = new XxlJobProperties();

        properties.setEnabled(false);
        assertThat(properties.getEnabled()).isFalse();

        properties.getAdmin().setAddresses("http://127.0.0.1:8080/xxl-job-admin");
        properties.getAdmin().setAppName("my-app");
        properties.getAdmin().setAccessToken("secret-token");
        assertThat(properties.getAdmin().getAddresses()).isEqualTo("http://127.0.0.1:8080/xxl-job-admin");
        assertThat(properties.getAdmin().getAppName()).isEqualTo("my-app");
        assertThat(properties.getAdmin().getAccessToken()).isEqualTo("secret-token");

        properties.getExecutor().setAppName("my-executor");
        properties.getExecutor().setAddress("http://192.168.1.100:9999");
        properties.getExecutor().setPort(8888);
        properties.getExecutor().setLogPath("/var/log/xxl-job");
        properties.getExecutor().setLogRetentionDays(60);
        assertThat(properties.getExecutor().getAppName()).isEqualTo("my-executor");
        assertThat(properties.getExecutor().getAddress()).isEqualTo("http://192.168.1.100:9999");
        assertThat(properties.getExecutor().getPort()).isEqualTo(8888);
        assertThat(properties.getExecutor().getLogPath()).isEqualTo("/var/log/xxl-job");
        assertThat(properties.getExecutor().getLogRetentionDays()).isEqualTo(60);

        properties.getIdempotency().setEnabled(false);
        assertThat(properties.getIdempotency().getEnabled()).isFalse();

        properties.getTimeout().setDefaultTimeout(600);
        properties.getTimeout().setTimeoutHandler(XxlJobProperties.TimeoutHandler.TIMEOUT);
        assertThat(properties.getTimeout().getDefaultTimeout()).isEqualTo(600);
        assertThat(properties.getTimeout().getTimeoutHandler()).isEqualTo(XxlJobProperties.TimeoutHandler.TIMEOUT);
    }

    @Test
    @DisplayName("StoreType 枚举应包含所有定义的存储类型")
    void storeTypeEnumShouldContainAllDefinedTypes() {
        XxlJobProperties.StoreType[] types = XxlJobProperties.StoreType.values();

        assertThat(types).containsExactlyInAnyOrder(XxlJobProperties.StoreType.MEMORY);
    }

    @Test
    @DisplayName("TimeoutHandler 枚举应包含所有定义的超时处理策略")
    void timeoutHandlerEnumShouldContainAllDefinedStrategies() {
        XxlJobProperties.TimeoutHandler[] handlers = XxlJobProperties.TimeoutHandler.values();

        assertThat(handlers).containsExactlyInAnyOrder(
                XxlJobProperties.TimeoutHandler.FAIL,
                XxlJobProperties.TimeoutHandler.TIMEOUT);
    }
}
