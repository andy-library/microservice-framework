package com.microservice.framework.database.autoconfigure;

import com.microservice.framework.database.DatabaseProperties;
import com.microservice.framework.database.api.OutboxPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * OutboxAutoConfiguration tests.
 */
class OutboxAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
                    org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration.class,
                    OutboxAutoConfiguration.class))
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:outboxdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=");

    @Nested
    @DisplayName("激活条件")
    class Conditions {

        @Test
        @DisplayName("默认不注册 OutboxPublisher")
        void shouldNotRegisterByDefault() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(OutboxPublisher.class);
            });
        }

        @Test
        @DisplayName("启用 outbox 后注册 OutboxPublisher")
        void shouldRegisterWhenEnabled() {
            contextRunner.withPropertyValues(
                            "framework.database.outbox.enabled=true",
                            "framework.database.outbox.auto-create-table=true")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasSingleBean(OutboxPublisher.class);
                    });
        }
    }

    @Nested
    @DisplayName("JDBC Outbox 行为")
    class JdbcOutboxBehavior {

        @Test
        @DisplayName("应支持 publish、findUnpublished 和 markPublished")
        void shouldPublishFindAndMarkPublished() {
            contextRunner.withPropertyValues(
                            "framework.database.outbox.enabled=true",
                            "framework.database.outbox.auto-create-table=true",
                            "framework.database.outbox.table-name=test_outbox_event",
                            "framework.database.outbox.max-fetch-size=10")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        OutboxPublisher publisher = context.getBean(OutboxPublisher.class);

                        publisher.publish("Order", "ORDER-1", "CREATED", "{\"orderNo\":\"ORDER-1\"}");

                        List<OutboxPublisher.OutboxEvent> unpublished = publisher.findUnpublished();
                        assertThat(unpublished).hasSize(1);
                        OutboxPublisher.OutboxEvent event = unpublished.get(0);
                        assertThat(event.getAggregateType()).isEqualTo("Order");
                        assertThat(event.getAggregateId()).isEqualTo("ORDER-1");
                        assertThat(event.getEventType()).isEqualTo("CREATED");
                        assertThat(event.getPayload()).contains("ORDER-1");
                        assertThat(event.isPublished()).isFalse();
                        assertThat(event.getCreatedAt()).isNotNull();

                        publisher.markPublished(event.getEventId());
                        assertThat(publisher.findUnpublished()).isEmpty();
                    });
        }

        @Test
        @DisplayName("publish 不允许关键字段为空")
        void publishShouldRejectBlankArguments() {
            contextRunner.withPropertyValues(
                            "framework.database.outbox.enabled=true",
                            "framework.database.outbox.auto-create-table=true",
                            "framework.database.outbox.table-name=test_outbox_event_args")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        OutboxPublisher publisher = context.getBean(OutboxPublisher.class);

                        assertThatThrownBy(() -> publisher.publish("", "ORDER-1", "CREATED", "{}"))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("aggregateType must not be blank");
                        assertThat(publisher.findUnpublished()).isEmpty();
                    });
        }

        @Test
        @DisplayName("markPublished 对不存在事件应明确失败")
        void markPublishedShouldFailWhenEventDoesNotExist() {
            contextRunner.withPropertyValues(
                            "framework.database.outbox.enabled=true",
                            "framework.database.outbox.auto-create-table=true",
                            "framework.database.outbox.table-name=test_outbox_event_missing")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        OutboxPublisher publisher = context.getBean(OutboxPublisher.class);

                        assertThatThrownBy(() -> publisher.markPublished("missing-event"))
                                .isInstanceOf(com.microservice.framework.common.error.FrameworkException.class)
                                .hasMessageContaining("DATABASE-OUTBOX-001");
                    });
        }

        @Test
        @DisplayName("非法 outbox 表名应在启动期失败")
        void invalidOutboxTableNameShouldFailStartup() {
            contextRunner.withPropertyValues(
                            "framework.database.outbox.enabled=true",
                            "framework.database.outbox.auto-create-table=true",
                            "framework.database.outbox.table-name=bad-name")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasRootCauseInstanceOf(
                                        org.springframework.boot.context.properties.bind.validation.BindValidationException.class);
                    });
        }

        @Test
        @DisplayName("直接构造 JdbcOutboxPublisher 时也应拒绝非法 SQL 标识符表名")
        void directPublisherShouldRejectInvalidTableName() {
            DatabaseProperties.OutboxProperties properties = new DatabaseProperties.OutboxProperties();
            properties.setAutoCreateTable(true);
            properties.setTableName("1bad_table");
            OutboxAutoConfiguration.JdbcOutboxPublisher publisher =
                    new OutboxAutoConfiguration.JdbcOutboxPublisher(mock(JdbcTemplate.class), properties);

            assertThatThrownBy(publisher::afterPropertiesSet)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid outbox table name");
        }
    }

    @Nested
    @DisplayName("用户覆盖")
    class UserOverride {

        @Test
        @DisplayName("用户自定义 OutboxPublisher 应覆盖默认实现")
        void userProvidedPublisherShouldOverrideDefault() {
            OutboxPublisher customPublisher = new OutboxPublisher() {
                @Override
                public void publish(String aggregateType, String aggregateId, String eventType, String payload) {
                }

                @Override
                public void markPublished(String eventId) {
                }

                @Override
                public List<OutboxEvent> findUnpublished() {
                    return List.of(new OutboxEvent("event-1", "A", "1", "E", "{}", false, Instant.now()));
                }
            };

            contextRunner.withPropertyValues(
                            "framework.database.outbox.enabled=true",
                            "framework.database.outbox.auto-create-table=true")
                    .withBean(OutboxPublisher.class, () -> customPublisher)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasSingleBean(OutboxPublisher.class);
                        assertThat(context.getBean(OutboxPublisher.class)).isSameAs(customPublisher);
                    });
        }
    }
}
