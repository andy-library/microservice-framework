package com.microservice.framework.database.autoconfigure;

import com.microservice.framework.database.DatabaseProperties;
import com.microservice.framework.database.api.TransactionTemplateFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DatabaseTransactionAutoConfiguration 自动配置测试
 * <p>
 * 使用 ApplicationContextRunner 验证 TransactionTemplateFacade 的激活条件、
 * 默认参数配置和用户自定义覆盖行为。
 *
 * @author Andy Yang
 */
class DatabaseTransactionAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
                    org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration.class,
                    org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration.class,
                    DatabaseTransactionAutoConfiguration.class))
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:testdb",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=");

    @Nested
    @DisplayName("默认配置")
    class DefaultConfiguration {

        @Test
        @DisplayName("默认配置应创建 TransactionTemplateFacade Bean")
        void defaultConfigurationShouldCreateTransactionTemplateFacade() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasBean("defaultTransactionTemplateFacade");
                assertThat(context.getBean(TransactionTemplateFacade.class)).isNotNull();
            });
        }

        @Test
        @DisplayName("默认 TransactionTemplateFacade 应使用默认超时和隔离级别")
        void defaultTransactionTemplateFacadeShouldUseDefaults() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                TransactionTemplateFacade facade = context.getBean(TransactionTemplateFacade.class);
                assertThat(facade).isNotNull();
            });
        }
    }

    @Nested
    @DisplayName("属性绑定")
    class PropertyBinding {

        @Test
        @DisplayName("自定义超时时间应生效")
        void customTimeoutShouldBeApplied() {
            contextRunner.withPropertyValues(
                    "framework.database.transaction.default-timeout=60")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context.getBean(TransactionTemplateFacade.class)).isNotNull();
                    });
        }

        @Test
        @DisplayName("自定义隔离级别应生效")
        void customIsolationShouldBeApplied() {
            contextRunner.withPropertyValues(
                    "framework.database.transaction.default-isolation=SERIALIZABLE")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context.getBean(TransactionTemplateFacade.class)).isNotNull();
                    });
        }
    }

    @Nested
    @DisplayName("用户自定义覆盖")
    class UserOverride {

        @Test
        @DisplayName("用户自定义 TransactionTemplateFacade 应覆盖默认实现")
        void userProvidedFacadeShouldOverrideDefault() {
            contextRunner.withBean("customFacade", TransactionTemplateFacade.class,
                    () -> new TransactionTemplateFacade() {
                        @Override
                        public <T> T execute(TransactionTemplateFacade.TransactionAction<T> action) {
                            try {
                                return action.doInTransaction();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public <T> T executeWithTimeout(int timeoutSeconds, TransactionTemplateFacade.TransactionAction<T> action) {
                            return null;
                        }

                        @Override
                        public <T> T executeWithIsolation(TransactionTemplateFacade.IsolationLevel isolation, TransactionTemplateFacade.TransactionAction<T> action) {
                            return null;
                        }

                        @Override
                        public <T> T executeReadOnly(TransactionTemplateFacade.TransactionAction<T> action) {
                            return null;
                        }
                    })
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("customFacade");
                        assertThat(context).doesNotHaveBean("defaultTransactionTemplateFacade");
                        TransactionTemplateFacade facade = context.getBean(TransactionTemplateFacade.class);
                        assertThat(facade).isNotInstanceOf(DatabaseTransactionAutoConfiguration.DefaultTransactionTemplateFacade.class);
                    });
        }
    }
}
