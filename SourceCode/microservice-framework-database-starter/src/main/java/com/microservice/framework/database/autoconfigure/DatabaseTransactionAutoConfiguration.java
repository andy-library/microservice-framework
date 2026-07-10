package com.microservice.framework.database.autoconfigure;

import com.microservice.framework.database.DatabaseProperties;
import com.microservice.framework.database.api.TransactionTemplateFacade;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 数据库事务管理自动配置
 * <p>
 * 创建 {@link TransactionTemplateFacade} Bean，封装 Spring TransactionTemplate，
 * 提供与 DatabaseProperties 一致的默认事务参数。
 * <p>
 * 激活条件：
 * <ul>
 *   <li>{@code PlatformTransactionManager} 在 classpath 上（spring-boot-starter-jdbc 默认引入）</li>
 * </ul>
 * <p>
 * 用户可通过注册自定义 {@code TransactionTemplateFacade} Bean 覆盖默认实现。
 * <p>
 * 注意：类名为 {@code DatabaseTransactionAutoConfiguration} 以避免与 Spring Boot 内置的
 * {@code org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration}
 * 产生 Bean 定义名称冲突。
 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties(DatabaseProperties.class)
@ConditionalOnClass(PlatformTransactionManager.class)
public class DatabaseTransactionAutoConfiguration {

    /**
     * 创建默认 TransactionTemplateFacade
     * <p>
     * 基于 Spring {@code TransactionTemplate} 实现，将 DatabaseProperties
     * 中的默认超时和隔离级别应用到每次事务操作。
     * <p>
     * {@code @ConditionalOnMissingBean} 保证用户自定义实现不被覆盖。
     *
     * @param transactionManager Spring 事务管理器
     * @param databaseProperties Framework 数据库属性
     * @return 默认 TransactionTemplateFacade 实现
     */
    @Bean
    @ConditionalOnMissingBean(TransactionTemplateFacade.class)
    public TransactionTemplateFacade defaultTransactionTemplateFacade(
            PlatformTransactionManager transactionManager,
            DatabaseProperties databaseProperties) {
        return new DefaultTransactionTemplateFacade(transactionManager, databaseProperties);
    }

    /**
     * TransactionTemplateFacade 默认实现
     * <p>
     * 封装 Spring TransactionTemplate，根据 DatabaseProperties 设置默认参数。
     */
    static class DefaultTransactionTemplateFacade implements TransactionTemplateFacade {

        private final TransactionTemplate transactionTemplate;
        private final DatabaseProperties databaseProperties;

        DefaultTransactionTemplateFacade(PlatformTransactionManager transactionManager,
                                         DatabaseProperties databaseProperties) {
            this.transactionTemplate = new TransactionTemplate(transactionManager);
            this.databaseProperties = databaseProperties;

            // 应用默认事务配置
            DatabaseProperties.TransactionProperties txProps = databaseProperties.getTransaction();
            this.transactionTemplate.setTimeout(txProps.getDefaultTimeout());
            this.transactionTemplate.setIsolationLevel(
                    parseIsolationLevel(txProps.getDefaultIsolation()));
        }

        @Override
        public <T> T execute(TransactionAction<T> action) {
            return transactionTemplate.execute(status -> {
                try {
                    return action.doInTransaction();
                } catch (Exception e) {
                    status.setRollbackOnly();
                    throw new RuntimeException("Transaction execution failed", e);
                }
            });
        }

        @Override
        public <T> T executeWithTimeout(int timeoutSeconds, TransactionAction<T> action) {
            TransactionTemplate template = new TransactionTemplate(transactionTemplate.getTransactionManager());
            template.setTimeout(timeoutSeconds);
            template.setIsolationLevel(transactionTemplate.getIsolationLevel());
            return template.execute(status -> {
                try {
                    return action.doInTransaction();
                } catch (Exception e) {
                    status.setRollbackOnly();
                    throw new RuntimeException("Transaction execution failed", e);
                }
            });
        }

        @Override
        public <T> T executeWithIsolation(IsolationLevel isolation, TransactionAction<T> action) {
            TransactionTemplate template = new TransactionTemplate(transactionTemplate.getTransactionManager());
            template.setTimeout(transactionTemplate.getTimeout());
            template.setIsolationLevel(isolation.getValue());
            return template.execute(status -> {
                try {
                    return action.doInTransaction();
                } catch (Exception e) {
                    status.setRollbackOnly();
                    throw new RuntimeException("Transaction execution failed", e);
                }
            });
        }

        @Override
        public <T> T executeReadOnly(TransactionAction<T> action) {
            TransactionTemplate template = new TransactionTemplate(transactionTemplate.getTransactionManager());
            template.setTimeout(transactionTemplate.getTimeout());
            template.setIsolationLevel(transactionTemplate.getIsolationLevel());
            template.setReadOnly(true);
            return template.execute(status -> {
                try {
                    return action.doInTransaction();
                } catch (Exception e) {
                    status.setRollbackOnly();
                    throw new RuntimeException("Transaction execution failed", e);
                }
            });
        }

        private int parseIsolationLevel(String isolationName) {
            for (IsolationLevel level : IsolationLevel.values()) {
                if (level.name().equals(isolationName)) {
                    return level.getValue();
                }
            }
            return TransactionDefinition.ISOLATION_DEFAULT;
        }
    }
}
