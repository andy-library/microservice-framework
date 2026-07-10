package com.microservice.framework.database.api;

/**
 * 事务模板门面
 * <p>
 * 封装 Spring {@code TransactionTemplate}，提供更简洁的事务操作接口，
 * 支持超时时间、隔离级别和只读事务的声明式配置。
 * <p>
 * 设计意图：
 * <ul>
 *   <li>避免业务代码直接操作 TransactionTemplate 的底层细节</li>
 *   <li>提供与 DatabaseProperties 默认值一致的事务配置</li>
 *   <li>支持编程式事务边界控制，弥补 @Transactional 无法覆盖的场景</li>
 * </ul>
 *
 * @author Andy Yang
 */
public interface TransactionTemplateFacade {

    /**
     * 在默认事务配置下执行操作
     * <p>
     * 使用 DatabaseProperties 中定义的默认超时和隔离级别。
     *
     * @param action 事务内执行的操作
     * @param <T>    返回值类型
     * @return 操作返回值
     * @throws com.microservice.framework.common.error.FrameworkException 事务执行失败时抛出
     */
    <T> T execute(TransactionAction<T> action);

    /**
     * 在指定超时时间内执行事务操作
     *
     * @param timeoutSeconds 超时时间（秒），0 表示无超时
     * @param action         事务内执行的操作
     * @param <T>            返回值类型
     * @return 操作返回值
     * @throws com.microservice.framework.common.error.FrameworkException 事务执行失败时抛出
     */
    <T> T executeWithTimeout(int timeoutSeconds, TransactionAction<T> action);

    /**
     * 在指定隔离级别下执行事务操作
     *
     * @param isolation 隔离级别
     * @param action    事务内执行的操作
     * @param <T>       返回值类型
     * @return 操作返回值
     * @throws com.microservice.framework.common.error.FrameworkException 事务执行失败时抛出
     */
    <T> T executeWithIsolation(IsolationLevel isolation, TransactionAction<T> action);

    /**
     * 在只读事务下执行操作
     * <p>
     * 只读事务适用于查询场景，数据库可优化执行路径。
     *
     * @param action 事务内执行的操作
     * @param <T>    返回值类型
     * @return 操作返回值
     * @throws com.microservice.framework.common.error.FrameworkException 事务执行失败时抛出
     */
    <T> T executeReadOnly(TransactionAction<T> action);

    /**
     * 事务操作回调
     *
     * @param <T> 返回值类型
     */
    @FunctionalInterface
    interface TransactionAction<T> {

        /**
         * 在事务内执行的操作
         *
         * @return 操作返回值
         * @throws Exception 业务异常
         */
        T doInTransaction() throws Exception;
    }

    /**
     * 事务隔离级别枚举
     * <p>
     * 与 Spring {@code TransactionDefinition} 隔离级别对应，
     * 提供更语义化的枚举名称。
     */
    enum IsolationLevel {

        /**
         * 使用数据库默认隔离级别
         */
        DEFAULT(-1),

        /**
         * 读未提交：最低隔离级别，可能读到未提交数据
         */
        READ_UNCOMMITTED(1),

        /**
         * 读已提交：大多数数据库默认级别，防止脏读
         */
        READ_COMMITTED(2),

        /**
         * 可重复读：防止脏读和不可重复读
         */
        REPEATABLE_READ(3),

        /**
         * 串行化：最高隔离级别，防止脏读、不可重复读和幻读
         */
        SERIALIZABLE(4);

        private final int value;

        IsolationLevel(int value) {
            this.value = value;
        }

        /**
         * 获取对应的 Spring TransactionDefinition 隔离级别值
         *
         * @return 隔离级别整数常量
         */
        public int getValue() {
            return value;
        }
    }
}
