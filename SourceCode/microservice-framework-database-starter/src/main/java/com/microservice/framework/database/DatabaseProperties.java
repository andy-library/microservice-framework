package com.microservice.framework.database;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * Database Starter 配置属性
 * <p>
 * 聚合数据源、读写分离、分库分表、事务管理和数据迁移等配置组，
 * 所有属性前缀为 {@code framework.database}。
 *
 * @author Andy Yang
 */
@ConfigurationProperties(prefix = "framework.database")
@Validated
public class DatabaseProperties {

    /**
     * 数据源配置
     */
    @NestedConfigurationProperty
    private DataSourceProperties dataSource = new DataSourceProperties();

    /**
     * 分库分表配置
     */
    @NestedConfigurationProperty
    private ShardingProperties sharding = new ShardingProperties();

    /**
     * 读写分离配置
     */
    @NestedConfigurationProperty
    private RwProperties rw = new RwProperties();

    /**
     * 事务管理配置
     */
    @NestedConfigurationProperty
    private TransactionProperties transaction = new TransactionProperties();

    /**
     * 数据迁移配置
     */
    @NestedConfigurationProperty
    private MigrationProperties migration = new MigrationProperties();

    /**
     * 事务性 Outbox 配置
     */
    @NestedConfigurationProperty
    private OutboxProperties outbox = new OutboxProperties();

    // Getters and Setters

    public DataSourceProperties getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSourceProperties dataSource) {
        this.dataSource = dataSource;
    }

    public ShardingProperties getSharding() {
        return sharding;
    }

    public void setSharding(ShardingProperties sharding) {
        this.sharding = sharding;
    }

    public RwProperties getRw() {
        return rw;
    }

    public void setRw(RwProperties rw) {
        this.rw = rw;
    }

    public TransactionProperties getTransaction() {
        return transaction;
    }

    public void setTransaction(TransactionProperties transaction) {
        this.transaction = transaction;
    }

    public MigrationProperties getMigration() {
        return migration;
    }

    public void setMigration(MigrationProperties migration) {
        this.migration = migration;
    }

    public OutboxProperties getOutbox() {
        return outbox;
    }

    public void setOutbox(OutboxProperties outbox) {
        this.outbox = outbox;
    }

    /**
     * 数据源配置
     * <p>
     * 配置 HikariCP 连接池参数和慢 SQL 检测阈值。
     */
    public static class DataSourceProperties {

        /**
         * 连接池大小，默认 10
         * <p>
         * HikariCP 默认连接池大小，可根据数据库实例规格和并发量调整。
         */
        @Min(1)
        private int poolSize = 10;

        /**
         * 连接超时时间（毫秒），默认 30000
         * <p>
         * 客户端等待获取连接的最大毫秒数，超时将抛出异常。
         */
        @Min(1000)
        private long timeout = 30000L;

        /**
         * 慢 SQL 检测阈值（毫秒），默认 1000
         * <p>
         * 执行时间超过此阈值的 SQL 将被标记为慢查询并记录日志。
         * 设为 0 表示不检测慢 SQL。
         */
        @Min(0)
        private long slowSqlThreshold = 1000L;

        // Getters and Setters

        public int getPoolSize() {
            return poolSize;
        }

        public void setPoolSize(int poolSize) {
            this.poolSize = poolSize;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        public long getSlowSqlThreshold() {
            return slowSqlThreshold;
        }

        public void setSlowSqlThreshold(long slowSqlThreshold) {
            this.slowSqlThreshold = slowSqlThreshold;
        }
    }

    /**
     * 分库分表配置
     * <p>
     * 控制 ShardingSphere-JDBC 分库分表功能的开启与规则定义。
     * 默认关闭分库分表，仅启用读写分离。
     */
    public static class ShardingProperties {

        /**
         * 是否启用分库分表，默认 false
         * <p>
         * 关闭时仅启用读写分离（RwProperties.enabled=true），
         * 开启后将按照 ShardingSphere 规则进行分库分表路由。
         */
        private boolean enabled = false;

        /**
         * ShardingSphere 规则配置
         * <p>
         * 当 {@code enabled=true} 时，此处定义的分片规则将被加载。
         * 规则格式遵循 ShardingSphere 5.x YAML 配置规范。
         */
        @NotNull
        private String rules = "";

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getRules() {
            return rules;
        }

        public void setRules(String rules) {
            this.rules = rules;
        }
    }

    /**
     * 读写分离配置
     * <p>
     * 控制 ShardingSphere-JDBC 读写分离功能的开启与行为。
     */
    public static class RwProperties {

        /**
         * 是否启用读写分离，默认 true
         * <p>
         * 启用后，读操作路由到从库，写操作路由到主库。
         * 关闭后所有操作均路由到主库。
         */
        private boolean enabled = true;

        /**
         * 写操作后是否路由到主库读，默认 true
         * <p>
         * 防止写后读一致性问题：在主库执行写操作后，
         * 紧接着的读操作仍路由到主库，确保读到最新数据。
         */
        private boolean readAfterWriteRoutePrimary = true;

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isReadAfterWriteRoutePrimary() {
            return readAfterWriteRoutePrimary;
        }

        public void setReadAfterWriteRoutePrimary(boolean readAfterWriteRoutePrimary) {
            this.readAfterWriteRoutePrimary = readAfterWriteRoutePrimary;
        }
    }

    /**
     * 事务管理配置
     * <p>
     * 配置 TransactionTemplateFacade 的默认事务参数。
     */
    public static class TransactionProperties {

        /**
         * 默认事务超时时间（秒），默认 30
         * <p>
         * 超过此时间的事务将自动回滚，防止长事务占用数据库资源。
         */
        @Min(1)
        private int defaultTimeout = 30;

        /**
         * 默认事务隔离级别，默认 READ_COMMITTED
         * <p>
         * 与 {@link com.microservice.framework.database.api.TransactionTemplateFacade.IsolationLevel} 对应。
         */
        @NotNull
        private String defaultIsolation = "READ_COMMITTED";

        // Getters and Setters

        public int getDefaultTimeout() {
            return defaultTimeout;
        }

        public void setDefaultTimeout(int defaultTimeout) {
            this.defaultTimeout = defaultTimeout;
        }

        public String getDefaultIsolation() {
            return defaultIsolation;
        }

        public void setDefaultIsolation(String defaultIsolation) {
            this.defaultIsolation = defaultIsolation;
        }
    }

    /**
     * 数据迁移配置
     * <p>
     * 控制 Flyway 数据库迁移功能的开启与脚本路径。
     */
    public static class MigrationProperties {

        /**
         * 是否启用 Flyway 数据迁移，默认 false
         * <p>
         * 需要 flyway-core 在 classpath 上才可启用。
         * 启用后将在应用启动时自动执行未应用的迁移脚本。
         */
        private boolean enabled = false;

        /**
         * 迁移脚本位置，默认 classpath:db/migration
         * <p>
         * 支持 Spring 资源路径格式，如 classpath:、file: 等。
         */
        @NotNull
        private String locations = "classpath:db/migration";

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getLocations() {
            return locations;
        }

        public void setLocations(String locations) {
            this.locations = locations;
        }
    }

    /**
     * 事务性 Outbox 配置。
     */
    public static class OutboxProperties {

        /**
         * 是否启用 Outbox，默认 false。
         */
        private boolean enabled = false;

        /**
         * 是否自动创建 Outbox 表，默认 false。
         */
        private boolean autoCreateTable = false;

        /**
         * Outbox 表名。
         */
        @NotNull
        @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{0,63}",
                message = "tableName must start with a letter and contain only letters, digits, and underscores")
        private String tableName = "framework_outbox_event";

        /**
         * 单次拉取未发布事件最大数量。
         */
        @Min(1)
        private int maxFetchSize = 100;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isAutoCreateTable() {
            return autoCreateTable;
        }

        public void setAutoCreateTable(boolean autoCreateTable) {
            this.autoCreateTable = autoCreateTable;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public int getMaxFetchSize() {
            return maxFetchSize;
        }

        public void setMaxFetchSize(int maxFetchSize) {
            this.maxFetchSize = maxFetchSize;
        }
    }
}
