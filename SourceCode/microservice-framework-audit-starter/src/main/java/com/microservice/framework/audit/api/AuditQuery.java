package com.microservice.framework.audit.api;

import com.microservice.framework.common.page.PageRequest;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * 审计查询条件
 * <p>
 * 不可变对象，封装审计事件的查询过滤条件。
 * 支持时间范围、操作者、事件类型和操作类型等过滤维度，
 * 并内嵌分页请求参数。
 *
 * @author Andy Yang
 */
public final class AuditQuery {

    private final Instant startTime;
    private final Instant endTime;
    private final String operatorId;
    private final String eventType;
    private final String action;
    private final PageRequest pageRequest;

    /**
     * 创建审计查询条件
     *
     * @param startTime    查询起始时间（可为 null，表示不限）
     * @param endTime      查询截止时间（可为 null，表示不限）
     * @param operatorId   操作者 ID（可为 null，表示不限）
     * @param eventType    事件类型（可为 null，表示不限）
     * @param action       操作类型（可为 null，表示不限）
     * @param pageRequest  分页请求（可为 null，使用默认分页）
     */
    public AuditQuery(Instant startTime, Instant endTime, String operatorId,
                      String eventType, String action, PageRequest pageRequest) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.operatorId = operatorId;
        this.eventType = eventType;
        this.action = action;
        this.pageRequest = pageRequest != null ? pageRequest : PageRequest.builder().pageNumber(1).pageSize(20).build();
    }

    /**
     * 创建默认查询条件（无过滤，默认分页）
     *
     * @return 默认查询条件
     */
    public static AuditQuery defaults() {
        return new AuditQuery(null, null, null, null, null, PageRequest.builder().pageNumber(1).pageSize(20).build());
    }

    /**
     * 获取查询起始时间
     *
     * @return 起始时间，不限时返回空 Optional
     */
    public Optional<Instant> getStartTime() {
        return Optional.ofNullable(startTime);
    }

    /**
     * 获取查询截止时间
     *
     * @return 截止时间，不限时返回空 Optional
     */
    public Optional<Instant> getEndTime() {
        return Optional.ofNullable(endTime);
    }

    /**
     * 获取操作者 ID
     *
     * @return 操作者 ID，不限时返回空 Optional
     */
    public Optional<String> getOperatorId() {
        return Optional.ofNullable(operatorId);
    }

    /**
     * 获取事件类型
     *
     * @return 事件类型，不限时返回空 Optional
     */
    public Optional<String> getEventType() {
        return Optional.ofNullable(eventType);
    }

    /**
     * 获取操作类型
     *
     * @return 操作类型，不限时返回空 Optional
     */
    public Optional<String> getAction() {
        return Optional.ofNullable(action);
    }

    /**
     * 获取分页请求参数
     *
     * @return 分页请求
     */
    public PageRequest getPageRequest() {
        return pageRequest;
    }

    /**
     * 是否有时间范围过滤
     *
     * @return 是否设置了时间范围
     */
    public boolean hasTimeRange() {
        return startTime != null && endTime != null;
    }

    /**
     * 是否有操作者过滤
     *
     * @return 是否设置了操作者条件
     */
    public boolean hasOperatorFilter() {
        return operatorId != null;
    }

    /**
     * 是否有事件类型过滤
     *
     * @return 是否设置了事件类型条件
     */
    public boolean hasEventTypeFilter() {
        return eventType != null;
    }

    @Override
    public String toString() {
        return "AuditQuery{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", operatorId='" + operatorId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", action='" + action + '\'' +
                ", pageRequest=" + pageRequest +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditQuery)) {
            return false;
        }
        AuditQuery that = (AuditQuery) o;
        return Objects.equals(startTime, that.startTime)
                && Objects.equals(endTime, that.endTime)
                && Objects.equals(operatorId, that.operatorId)
                && Objects.equals(eventType, that.eventType)
                && Objects.equals(action, that.action)
                && Objects.equals(pageRequest, that.pageRequest);
    }

    @Override
    public int hashCode() {
        int result = startTime != null ? startTime.hashCode() : 0;
        result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
        result = 31 * result + (operatorId != null ? operatorId.hashCode() : 0);
        result = 31 * result + (eventType != null ? eventType.hashCode() : 0);
        result = 31 * result + (action != null ? action.hashCode() : 0);
        result = 31 * result + pageRequest.hashCode();
        return result;
    }
}
