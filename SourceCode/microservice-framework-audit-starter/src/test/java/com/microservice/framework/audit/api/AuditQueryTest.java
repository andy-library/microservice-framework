package com.microservice.framework.audit.api;

import com.microservice.framework.common.page.PageRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuditQuery 测试
 * <p>
 * 验证查询条件的创建、过滤条件判断和分页参数。
 *
 * @author Andy Yang
 */
class AuditQueryTest {

    @Test
    @DisplayName("defaults 工厂方法应创建无过滤的默认查询")
    void defaultsFactoryShouldCreateNoFilterQuery() {
        AuditQuery query = AuditQuery.defaults();

        assertThat(query.getStartTime()).isEmpty();
        assertThat(query.getEndTime()).isEmpty();
        assertThat(query.getOperatorId()).isEmpty();
        assertThat(query.getEventType()).isEmpty();
        assertThat(query.getAction()).isEmpty();
        assertThat(query.getPageRequest()).isNotNull();
        assertThat(query.getPageRequest().getPageNumber()).isEqualTo(1);
        assertThat(query.getPageRequest().getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("创建带所有过滤条件的查询")
    void queryWithAllFiltersShouldBeCreated() {
        Instant start = Instant.parse("2025-01-01T00:00:00Z");
        Instant end = Instant.parse("2025-12-31T23:59:59Z");
        PageRequest pageRequest = PageRequest.builder().pageNumber(2).pageSize(50).build();

        AuditQuery query = new AuditQuery(start, end, "user-001", "LOGIN", "login", pageRequest);

        assertThat(query.getStartTime()).contains(start);
        assertThat(query.getEndTime()).contains(end);
        assertThat(query.getOperatorId()).contains("user-001");
        assertThat(query.getEventType()).contains("LOGIN");
        assertThat(query.getAction()).contains("login");
        assertThat(query.getPageRequest().getPageNumber()).isEqualTo(2);
        assertThat(query.getPageRequest().getPageSize()).isEqualTo(50);
    }

    @Test
    @DisplayName("hasTimeRange 应正确判断时间范围过滤")
    void hasTimeRangeShouldReturnCorrectly() {
        Instant start = Instant.parse("2025-01-01T00:00:00Z");
        Instant end = Instant.parse("2025-12-31T23:59:59Z");

        AuditQuery withRange = new AuditQuery(start, end, null, null, null, null);
        assertThat(withRange.hasTimeRange()).isTrue();

        AuditQuery partialRange = new AuditQuery(start, null, null, null, null, null);
        assertThat(partialRange.hasTimeRange()).isFalse();

        AuditQuery noRange = AuditQuery.defaults();
        assertThat(noRange.hasTimeRange()).isFalse();
    }

    @Test
    @DisplayName("hasOperatorFilter 应正确判断操作者过滤")
    void hasOperatorFilterShouldReturnCorrectly() {
        AuditQuery withOperator = new AuditQuery(null, null, "user-001", null, null, null);
        assertThat(withOperator.hasOperatorFilter()).isTrue();

        AuditQuery noOperator = AuditQuery.defaults();
        assertThat(noOperator.hasOperatorFilter()).isFalse();
    }

    @Test
    @DisplayName("hasEventTypeFilter 应正确判断事件类型过滤")
    void hasEventTypeFilterShouldReturnCorrectly() {
        AuditQuery withType = new AuditQuery(null, null, null, "LOGIN", null, null);
        assertThat(withType.hasEventTypeFilter()).isTrue();

        AuditQuery noType = AuditQuery.defaults();
        assertThat(noType.hasEventTypeFilter()).isFalse();
    }

    @Test
    @DisplayName("null pageRequest 应使用默认分页")
    void nullPageRequestShouldUseDefaultPagination() {
        AuditQuery query = new AuditQuery(null, null, null, null, null, null);
        assertThat(query.getPageRequest()).isNotNull();
        assertThat(query.getPageRequest().getPageNumber()).isEqualTo(1);
        assertThat(query.getPageRequest().getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("AuditQuery equals 和 hashCode 应正确工作")
    void auditQueryEqualsAndHashCodeShouldWork() {
        Instant start = Instant.parse("2025-01-01T00:00:00Z");
        Instant end = Instant.parse("2025-12-31T23:59:59Z");
        PageRequest pageRequest = PageRequest.builder().pageNumber(1).pageSize(20).build();

        AuditQuery query1 = new AuditQuery(start, end, "user-001", "LOGIN", "login", pageRequest);
        AuditQuery query2 = new AuditQuery(start, end, "user-001", "LOGIN", "login", pageRequest);

        assertThat(query1).isEqualTo(query2);
        assertThat(query1.hashCode()).isEqualTo(query2.hashCode());
    }
}
