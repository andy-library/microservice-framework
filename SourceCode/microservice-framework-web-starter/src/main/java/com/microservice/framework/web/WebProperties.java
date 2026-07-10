package com.microservice.framework.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Web Starter configuration properties.
 * <p>
 * Aggregates response formatting, exception handling, request ID, and
 * OpenAPI configuration groups. All properties are prefixed with
 * {@code framework.web}.
 *
 * @author Andy Yang
 */
@ConfigurationProperties(prefix = "framework.web")
public class WebProperties {

    @NestedConfigurationProperty
    private ResponseProperties response = new ResponseProperties();

    @NestedConfigurationProperty
    private ExceptionProperties exception = new ExceptionProperties();

    @NestedConfigurationProperty
    private RequestIdProperties requestId = new RequestIdProperties();

    @NestedConfigurationProperty
    private OpenApiProperties openApi = new OpenApiProperties();

    // ======================================================================
    // Getters and Setters
    // ======================================================================

    public ResponseProperties getResponse() { return response; }
    public void setResponse(ResponseProperties response) { this.response = response; }

    public ExceptionProperties getException() { return exception; }
    public void setException(ExceptionProperties exception) { this.exception = exception; }

    public RequestIdProperties getRequestId() { return requestId; }
    public void setRequestId(RequestIdProperties requestId) { this.requestId = requestId; }

    public OpenApiProperties getOpenApi() { return openApi; }
    public void setOpenApi(OpenApiProperties openApi) { this.openApi = openApi; }

    // ======================================================================
    // Nested configuration classes
    // ======================================================================

    /**
     * Response formatting properties.
     * <p>
     * Controls the default success/error codes and whether timestamp
     * and request ID are included in API responses.
     */
    public static class ResponseProperties {

        /** Default success code in API responses. */
        private int successCode = 0;

        /** Default error code in API responses. */
        private int errorCode = -1;

        /** Whether to include timestamp in API responses. */
        private boolean includeTimestamp = true;

        /** Whether to include request ID in API responses. */
        private boolean includeRequestId = true;

        public int getSuccessCode() { return successCode; }
        public void setSuccessCode(int successCode) { this.successCode = successCode; }

        public int getErrorCode() { return errorCode; }
        public void setErrorCode(int errorCode) { this.errorCode = errorCode; }

        public boolean isIncludeTimestamp() { return includeTimestamp; }
        public void setIncludeTimestamp(boolean includeTimestamp) { this.includeTimestamp = includeTimestamp; }

        public boolean isIncludeRequestId() { return includeRequestId; }
        public void setIncludeRequestId(boolean includeRequestId) { this.includeRequestId = includeRequestId; }
    }

    /**
     * Exception handling properties.
     * <p>
     * Controls whether global exception handling is enabled and
     * whether stack traces are included in error responses.
     */
    public static class ExceptionProperties {

        /** Whether to enable global exception handling. */
        private boolean enabled = true;

        /** Whether to include stack traces in error responses (for development only). */
        private boolean includeStackTrace = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public boolean isIncludeStackTrace() { return includeStackTrace; }
        public void setIncludeStackTrace(boolean includeStackTrace) { this.includeStackTrace = includeStackTrace; }
    }

    /**
     * Request ID propagation properties.
     * <p>
     * Controls request ID filter behavior: header name, whether
     * to generate IDs when missing, and whether the filter is enabled.
     */
    public static class RequestIdProperties {

        /** Whether to enable the request ID filter. */
        private boolean enabled = true;

        /** Header name for request ID propagation. */
        private String headerName = "X-Request-ID";

        /** Whether to generate a new request ID if the header is missing. */
        private boolean generateIfMissing = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getHeaderName() { return headerName; }
        public void setHeaderName(String headerName) { this.headerName = headerName; }

        public boolean isGenerateIfMissing() { return generateIfMissing; }
        public void setGenerateIfMissing(boolean generateIfMissing) { this.generateIfMissing = generateIfMissing; }
    }

    /**
     * OpenAPI (Swagger) properties.
     * <p>
     * Controls whether OpenAPI documentation is generated and
     * the basic metadata (title, version).
     */
    public static class OpenApiProperties {

        /** Whether to enable OpenAPI documentation generation. */
        private boolean enabled = false;

        /** API title for OpenAPI documentation. */
        private String title;

        /** API version for OpenAPI documentation. */
        private String version;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
    }
}
