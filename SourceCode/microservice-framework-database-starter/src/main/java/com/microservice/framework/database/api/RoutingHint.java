package com.microservice.framework.database.api;

/**
 * 数据库路由提示
 * <p>
 * 用于指定当前操作的数据库路由目标（主库或从库），
 * 配合读写分离场景下控制读写流向。
 * <p>
 * 使用方式：
 * <ul>
 *   <li>通过 HintManager 设置路由提示（ShardingSphere 场景）</li>
 *   <li>通过 ThreadLocal 传播路由提示（通用场景）</li>
 * </ul>
 *
 * @author Andy Yang
 */
public enum RoutingHint {

    /**
     * 主库路由：所有写操作和需要最新数据的读操作路由到主库
     */
    PRIMARY,

    /**
     * 从库路由：普通读操作路由到从库，减轻主库压力
     */
    READ_REPLICA;

    /**
     * 创建主库路由提示的静态工厂方法
     * <p>
     * 语义明确，比直接使用枚举值更具可读性。
     *
     * @return PRIMARY 路由提示
     */
    public static RoutingHint primary() {
        return PRIMARY;
    }

    /**
     * 创建从库路由提示的静态工厂方法
     * <p>
     * 语义明确，比直接使用枚举值更具可读性。
     *
     * @return READ_REPLICA 路由提示
     */
    public static RoutingHint readReplica() {
        return READ_REPLICA;
    }

    /**
     * 判断是否为主库路由
     *
     * @return 如果路由目标是主库返回 true
     */
    public boolean isPrimary() {
        return this == PRIMARY;
    }

    /**
     * 判断是否为从库路由
     *
     * @return 如果路由目标是从库返回 true
     */
    public boolean isReadReplica() {
        return this == READ_REPLICA;
    }
}
