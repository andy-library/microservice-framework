package com.microservice.framework.drools.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 规则引擎接口
 * <p>
 * 提供规则执行、验证和查询的核心能力。
 * 支持无状态批量执行和有状态会话执行两种模式。
 * <p>
 * 应用可通过此接口：
 * - 执行规则并获取匹配结果
 * - 插入业务事实对象触发规则
 * - 验证规则文件的有效性
 * - 查询已加载的规则数量和分组信息
 *
 * @author Andy Yang
 */
public interface RuleEngine {

    /**
     * 执行规则，使用给定的事实集合
     * <p>
     * 无状态执行模式，适用于单次规则触发场景。
     *
     * @param facts 业务事实对象集合
     * @return 规则执行结果
     */
    RuleExecutionResult execute(Collection<?> facts);

    /**
     * 使用全局变量和事实对象执行规则
     * <p>
     * 无状态执行模式，支持传入全局变量供规则使用。
     *
     * @param facts  业务事实对象集合
     * @param globals 全局变量映射
     * @return 规则执行结果
     */
    RuleExecutionResult executeWithFacts(Collection<?> facts, Map<String, Object> globals);

    /**
     * 验证规则文件的有效性
     * <p>
     * 检查已加载的规则文件语法和逻辑是否正确，
     * 不实际执行规则。
     *
     * @return 验证是否通过
     */
    boolean validate();

    /**
     * 获取当前已加载的规则总数
     *
     * @return 规则数量
     */
    int getRuleCount();

    /**
     * 获取指定分组的规则名称列表
     * <p>
     * 规则分组用于组织和管理规则，便于按业务域查询。
     *
     * @param group 规则分组名称
     * @return 该分组下的规则名称列表
     */
    List<String> getRuleGroup(String group);
}
