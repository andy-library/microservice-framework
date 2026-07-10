package com.microservice.framework.drools.api;

import java.util.Collection;

/**
 * 有状态规则会话接口
 * <p>
 * 提供有状态的规则执行模式，适用于需要逐步累积事实对象、
 * 多次触发规则并保持中间状态的场景。
 * <p>
 * 使用流程：
 * 1. 创建会话
 * 2. 插入事实对象
 * 3. 执行规则
 * 4. 获取执行结果
 * 5. 释放会话资源（dispose）
 * <p>
 * 重要：会话使用完毕后必须调用 {@link #dispose()} 释放资源，
 * 避免内存泄漏。
 *
 * @author Andy Yang
 */
public interface RuleSession {

    /**
     * 向会话中插入事实对象
     * <p>
     * 插入的对象将参与后续的规则匹配和执行。
     *
     * @param fact 业务事实对象
     * @return 事实对象在会话中的句柄
     */
    Object insert(Object fact);

    /**
     * 批量插入事实对象
     * <p>
     * 将多个事实对象一次性插入会话中。
     *
     * @param facts 业务事实对象集合
     */
    void insertAll(Collection<?> facts);

    /**
     * 执行当前会话中已加载的规则
     * <p>
     * 触发所有激活规则对已插入事实对象的匹配和执行。
     *
     * @return 规则执行结果
     */
    RuleExecutionResult execute();

    /**
     * 获取最后一次规则执行的结果
     * <p>
     * 返回最近一次 {@link #execute()} 调用的结果，
     * 如果尚未执行过规则则返回空结果。
     *
     * @return 规则执行结果
     */
    RuleExecutionResult getResult();

    /**
     * 释放会话资源
     * <p>
     * 会话使用完毕后必须调用此方法释放底层资源，
     * 防止内存泄漏和资源耗尽。
     */
    void dispose();
}
