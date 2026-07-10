package com.microservice.framework.audit.api;

import com.microservice.framework.common.page.PageRequest;
import com.microservice.framework.common.page.PageResult;

import java.util.List;
import java.util.Optional;

/**
 * 审计记录器接口
 * <p>
 * 提供审计事件的记录和查询能力。
 * 支持单条和批量记录，以及基于条件的查询检索。
 * <p>
 * 应用可通过此接口：
 * - 记录操作审计事件（单条或批量）
 * - 通过 ID 获取审计条目
 * - 基于条件查询审计事件
 *
 * @author Andy Yang
 */
public interface AuditRecorder {

    /**
     * 记录审计事件
     * <p>
     * 将审计条目持久化存储，并根据配置进行防篡改校验。
     *
     * @param entry 审计条目
     */
    void record(AuditEntry entry);

    /**
     * 批量记录审计事件
     * <p>
     * 批量持久化审计条目，提高写入性能。
     *
     * @param entries 审计条目列表
     */
    void recordBatch(List<AuditEntry> entries);

    /**
     * 通过 ID 获取审计条目
     * <p>
     * 根据审计条目唯一标识查询对应的审计记录。
     *
     * @param id 审计条目 ID
     * @return 对应的审计条目，不存在时返回空 Optional
     */
    Optional<AuditEntry> getEntry(String id);

    /**
     * 基于条件查询审计事件
     * <p>
     * 根据查询条件检索审计条目，支持时间范围、操作者、
     * 事件类型和操作类型等过滤条件。
     *
     * @param query 审计查询条件
     * @return 分页审计条目结果
     */
    PageResult<AuditEntry> query(AuditQuery query);
}
