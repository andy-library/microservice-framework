package com.microservice.framework.observability.common.constants;

/**
 * MDC 键常量定义
 * 
 * @author Andy Yang
 */
public final class MdcKeys {

    /**
     * 追踪ID
     */
    public static final String TRACE_ID = "traceId";

    /**
     * Span ID
     */
    public static final String SPAN_ID = "spanId";

    /**
     * 请求关联 ID。
     */
    public static final String REQUEST_ID = "requestId";

    /**
     * 租户ID
     */
    public static final String TENANT_ID = "tenantId";

    /**
     * 用户组
     */
    public static final String USER_GROUP = "userGroup";

    private MdcKeys() {
        throw new UnsupportedOperationException("Utility class");
    }
}
