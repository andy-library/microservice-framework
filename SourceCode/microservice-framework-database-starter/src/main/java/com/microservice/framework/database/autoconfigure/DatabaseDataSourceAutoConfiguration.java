package com.microservice.framework.database.autoconfigure;

import com.microservice.framework.database.DatabaseProperties;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.ObjectProvider;

import javax.sql.DataSource;

/**
 * 数据源自动配置
 * <p>
 * 当 {@code HikariDataSource} 在 classpath 上时，通过 BeanPostProcessor
 * 将 DatabaseProperties 中的连接池参数应用到 Spring Boot 自动创建的 HikariDataSource。
 * <p>
 * 激活条件：
 * <ul>
 *   <li>{@code com.zaxxer.hikari.HikariDataSource} 在 classpath 上（spring-boot-starter-jdbc 默认引入）</li>
 * </ul>
 * <p>
 * 设计说明：
 * <ul>
 *   <li>不重复创建 DataSource Bean，而是通过 BeanPostProcessor 增强 Spring Boot 自动创建的 DataSource</li>
 *   <li>Framework 配置作为默认值，用户通过 {@code spring.datasource.hikari.*} 属性可覆盖</li>
 *   <li>用户也可通过注册自定义 {@code DataSource} Bean 完全替换默认数据源</li>
 * </ul>
 * <p>
 * 优先级规则：
 * <ul>
 *   <li>Spring Boot {@code spring.datasource.hikari.*} 属性优先级最高，在 Bean 创建时已应用</li>
 *   <li>Framework {@code framework.database.data-source.*} 属性仅在 HikariCP 属性未被显式设置时生效</li>
 *   <li>未被显式设置的属性保留为 HikariCP 内部默认值（maximumPoolSize=-1 表示使用默认值 10，
 *       connectionTimeout=30000 为构造器默认值）</li>
 * </ul>
 * <p>
 * 注意：类名为 {@code DatabaseDataSourceAutoConfiguration} 以避免与 Spring Boot 内置的
 * {@code org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration}
 * 产生 Bean 定义名称冲突。
 * <p>
 * BeanPostProcessor 注册策略：
 * <ul>
 *   <li>HikariDataSourceCustomizer（BeanPostProcessor）声明在独立的轻量内部配置类中，
 *       使用 {@code proxyBeanMethods = false} 避免触发外层配置类的早期实例化</li>
 *   <li>BeanPostProcessor 通过 {@code ObjectProvider} 延迟获取 DatabaseProperties，
 *       避免 BeanPostProcessor 的直接依赖导致 DatabaseProperties 早期实例化</li>
 *   <li>这消除了 Spring 关于 BeanPostProcessor 早期实例化的警告</li>
 * </ul>
 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties(DatabaseProperties.class)
@ConditionalOnClass(HikariDataSource.class)
@Import(HikariDataSourceCustomizerConfiguration.class)
public class DatabaseDataSourceAutoConfiguration {

}

/**
 * HikariDataSource 定制器配置
 * <p>
 * 独立的轻量配置类，声明 HikariDataSourceCustomizer BeanPostProcessor。
 * 使用 {@code proxyBeanMethods = false} 确保 BeanPostProcessor 注册
 * 不触发外层 {@code DatabaseDataSourceAutoConfiguration} 的早期实例化。
 * <p>
 * BeanPostProcessor 通过 {@code ObjectProvider<DatabaseProperties>} 延迟获取配置属性，
 * 避免 DatabaseProperties 被早期实例化而跳过其他 BeanPostProcessor 的处理，
 * 从而彻底消除 Spring 的 BeanPostProcessor 早期实例化警告。
 * <p>
 * 此配置类通过 {@code @Import} 被引入，不依赖外层配置类的实例化。
 */
@Configuration(proxyBeanMethods = false)
class HikariDataSourceCustomizerConfiguration {

    /**
     * 创建 HikariDataSource 定制 BeanPostProcessor
     * <p>
     * 在 Spring Boot 自动创建 HikariDataSource Bean 初始化前，
     * 应用 Framework 连接池参数作为默认值。
     * <p>
     * 工作原理：
     * <ul>
     *   <li>Spring Boot DataSourceAutoConfiguration 创建 HikariDataSource 时，
     *       会将 {@code spring.datasource.hikari.*} 属性通过 setter 方法设置到 Bean 上</li>
     *   <li>用户未配置的 HikariCP 属性保留为 HikariCP 内部默认值</li>
     *   <li>BeanPostProcessor 在 Bean 初始化前运行，将 HikariCP 默认值替换为 Framework 配置值</li>
     *   <li>这样 Framework 配置作为第二优先级默认值，仅当 Spring Boot 属性未设置时生效</li>
     * </ul>
     * <p>
     * 使用 {@code ObjectProvider} 延迟获取 DatabaseProperties，
     * 使 BeanPostProcessor 注册不触发 DatabaseProperties 的早期实例化。
     *
     * @param databasePropertiesProvider Framework 数据库属性的延迟提供者
     * @return HikariDataSource 定制器
     */
    @Bean
    static HikariDataSourceCustomizer hikariDataSourceCustomizer(
            ObjectProvider<DatabaseProperties> databasePropertiesProvider) {
        return new HikariDataSourceCustomizer(databasePropertiesProvider);
    }

    /**
     * HikariDataSource 定制器
     * <p>
     * BeanPostProcessor 实现，在 HikariDataSource Bean 初始化前应用 Framework 默认连接池参数。
     * 仅对 HikariDataSource 类型生效，不影响其他 DataSource 实现。
     * <p>
     * HikariCP 各属性的默认值标记方式不一致：
     * <ul>
     *   <li>{@code maximumPoolSize}：未显式设置时为 -1，池初始化时解析为 10</li>
     *   <li>{@code connectionTimeout}：构造器默认值为 30000（非标记值）</li>
     * </ul>
     * 因此需要分别判断：maximumPoolSize 检查 -1，connectionTimeout 检查 30000。
     * <p>
     * 通过 {@code ObjectProvider} 延迟获取 DatabaseProperties，
     * 避免作为 BeanPostProcessor 直接依赖导致的早期实例化问题。
     */
    static class HikariDataSourceCustomizer implements BeanPostProcessor {

        /** HikariCP 内部默认 maximumPoolSize 值 */
        private static final int HIKARI_DEFAULT_MAX_POOL_SIZE = -1;

        /** HikariCP 构造器默认 connectionTimeout 值 */
        private static final long HIKARI_DEFAULT_CONNECTION_TIMEOUT = 30000L;

        private final ObjectProvider<DatabaseProperties> databasePropertiesProvider;

        HikariDataSourceCustomizer(ObjectProvider<DatabaseProperties> databasePropertiesProvider) {
            this.databasePropertiesProvider = databasePropertiesProvider;
        }

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
            if (bean instanceof HikariDataSource hikari) {
                DatabaseProperties databaseProperties = databasePropertiesProvider.getObject();
                DatabaseProperties.DataSourceProperties dsProps = databaseProperties.getDataSource();

                // maximumPoolSize: HikariCP 使用 -1 标记"使用内部默认值 10"
                // 如果值为 -1，说明用户未显式配置，应用 Framework 默认值
                if (hikari.getMaximumPoolSize() == HIKARI_DEFAULT_MAX_POOL_SIZE) {
                    hikari.setMaximumPoolSize(dsProps.getPoolSize());
                }

                // connectionTimeout: HikariCP 构造器默认值为 30000（非标记值）
                // 如果值仍为 30000，说明用户未显式配置（假设用户不会恰好设置 30000）
                // 应用 Framework 默认值（如果 Framework 配置了不同值）
                if (hikari.getConnectionTimeout() == HIKARI_DEFAULT_CONNECTION_TIMEOUT
                        && dsProps.getTimeout() != HIKARI_DEFAULT_CONNECTION_TIMEOUT) {
                    hikari.setConnectionTimeout(dsProps.getTimeout());
                }
            }
            return bean;
        }
    }
}
