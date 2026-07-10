package com.microservice.framework.web.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PageResponse pagination field tests.
 *
 * @author Andy Yang
 */
class PageResponseTest {

    // ======================================================================
    // Factory method tests
    // ======================================================================

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("of() 应返回分页数据及正确的 hasNext")
        void successPagination() {
            List<String> items = List.of("a", "b", "c");
            PageResponse<String> response = PageResponse.of(items, 30, 1, 10);
            assertThat(response.getCode()).isEqualTo(0);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isEqualTo(items);
            assertThat(response.getTotal()).isEqualTo(30);
            assertThat(response.getPage()).isEqualTo(1);
            assertThat(response.getPageSize()).isEqualTo(10);
            assertThat(response.getHasNext()).isTrue();
            assertThat(response.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("最后一页时 hasNext 应为 false")
        void lastPageNoNext() {
            List<String> items = List.of("a", "b");
            PageResponse<String> response = PageResponse.of(items, 12, 2, 10);
            assertThat(response.getHasNext()).isFalse();
        }

        @Test
        @DisplayName("空分页应返回 total=0, hasNext=false")
        void emptyPagination() {
            PageResponse<String> response = PageResponse.empty(1, 10);
            assertThat(response.getCode()).isEqualTo(0);
            assertThat(response.getData()).isEmpty();
            assertThat(response.getTotal()).isEqualTo(0);
            assertThat(response.getPage()).isEqualTo(1);
            assertThat(response.getPageSize()).isEqualTo(10);
            assertThat(response.getHasNext()).isFalse();
        }

        @Test
        @DisplayName("自定义消息的分页成功响应")
        void successWithCustomMessage() {
            List<Integer> items = List.of(1, 2);
            PageResponse<Integer> response = PageResponse.of("查询成功", items, 5, 1, 10);
            assertThat(response.getMessage()).isEqualTo("查询成功");
            assertThat(response.getData()).isEqualTo(items);
        }

        @Test
        @DisplayName("pageError() 应返回错误码分页响应")
        void errorPagination() {
            PageResponse<Void> response = PageResponse.pageError(-1, "query failed");
            assertThat(response.getCode()).isEqualTo(-1);
            assertThat(response.getMessage()).isEqualTo("query failed");
            assertThat(response.getData()).isNull();
            assertThat(response.getTotal()).isEqualTo(0);
            assertThat(response.getHasNext()).isFalse();
        }

        @Test
        @DisplayName("full() 应设置所有字段")
        void ofAllFields() {
            long ts = 1700000000000L;
            List<String> items = List.of("a");
            PageResponse<String> response = PageResponse.full(
                    0, "ok", items, ts, "req-1",
                    100, 1, 10, true);
            assertThat(response.getCode()).isEqualTo(0);
            assertThat(response.getMessage()).isEqualTo("ok");
            assertThat(response.getData()).isEqualTo(items);
            assertThat(response.getTimestamp()).isEqualTo(ts);
            assertThat(response.getRequestId()).isEqualTo("req-1");
            assertThat(response.getTotal()).isEqualTo(100);
            assertThat(response.getPage()).isEqualTo(1);
            assertThat(response.getPageSize()).isEqualTo(10);
            assertThat(response.getHasNext()).isTrue();
        }
    }

    // ======================================================================
    // withXxx method tests
    // ======================================================================

    @Nested
    @DisplayName("withXxx 方法")
    class WithMethods {

        @Test
        @DisplayName("withRequestId 应注入 request ID")
        void withRequestId() {
            List<String> items = List.of("a");
            PageResponse<String> original = PageResponse.of(items, 10, 1, 5);
            PageResponse<String> modified = original.withRequestId("req-789");
            assertThat(modified.getRequestId()).isEqualTo("req-789");
            assertThat(modified.getTotal()).isEqualTo(10);
            assertThat(modified.getData()).isEqualTo(items);
        }

        @Test
        @DisplayName("withoutTimestamp 应移除 timestamp")
        void withoutTimestamp() {
            List<String> items = List.of("a");
            PageResponse<String> original = PageResponse.of(items, 10, 1, 5);
            assertThat(original.getTimestamp()).isNotNull();
            PageResponse<String> modified = original.withoutTimestamp();
            assertThat(modified.getTimestamp()).isNull();
            assertThat(modified.getTotal()).isEqualTo(10);
        }

        @Test
        @DisplayName("withoutRequestId 应移除 requestId")
        void withoutRequestId() {
            List<String> items = List.of("a");
            PageResponse<String> original = PageResponse.of(items, 10, 1, 5)
                    .withRequestId("req-1");
            assertThat(original.getRequestId()).isEqualTo("req-1");
            PageResponse<String> modified = original.withoutRequestId();
            assertThat(modified.getRequestId()).isNull();
        }
    }

    // ======================================================================
    // Equality tests
    // ======================================================================

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("相同字段值的 PageResponse 应相等")
        void equalResponses() {
            long ts = 1700000000000L;
            List<String> items = List.of("a");
            PageResponse<String> r1 = PageResponse.full(0, "success", items, ts, "req-1", 10, 1, 5, true);
            PageResponse<String> r2 = PageResponse.full(0, "success", items, ts, "req-1", 10, 1, 5, true);
            assertThat(r1).isEqualTo(r2);
            assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        }

        @Test
        @DisplayName("不同 total 的 PageResponse 应不相等")
        void differentTotalNotEqual() {
            List<String> items = List.of("a");
            PageResponse<String> r1 = PageResponse.of(items, 10, 1, 5);
            PageResponse<String> r2 = PageResponse.of(items, 20, 1, 5);
            assertThat(r1).isNotEqualTo(r2);
        }

        @Test
        @DisplayName("不同 hasNext 的 PageResponse 应不相等")
        void differentHasNextNotEqual() {
            List<String> items = List.of("a");
            PageResponse<String> r1 = PageResponse.of(items, 10, 1, 5);
            PageResponse<String> r2 = PageResponse.of(items, 5, 1, 5);
            assertThat(r1.getHasNext()).isTrue();
            assertThat(r2.getHasNext()).isFalse();
            assertThat(r1).isNotEqualTo(r2);
        }

        @Test
        @DisplayName("与 ApiResponse 比较应返回 false")
        void comparedToApiResponse() {
            List<String> items = List.of("a");
            PageResponse<String> pageResponse = PageResponse.of(items, 10, 1, 5);
            ApiResponse<List<String>> apiResponse = ApiResponse.success(items);
            assertThat(pageResponse).isNotEqualTo(apiResponse);
        }
    }

    // ======================================================================
    // toString test
    // ======================================================================

    @Test
    @DisplayName("toString 应包含分页字段")
    void toStringShouldContainPaginationFields() {
        List<String> items = List.of("a");
        PageResponse<String> r = PageResponse.of(items, 10, 1, 5)
                .withRequestId("req-1");
        String str = r.toString();
        assertThat(str).contains("PageResponse");
        assertThat(str).contains("total=10");
        assertThat(str).contains("page=1");
        assertThat(str).contains("pageSize=5");
        assertThat(str).contains("hasNext=true");
        assertThat(str).contains("requestId='req-1'");
    }
}
