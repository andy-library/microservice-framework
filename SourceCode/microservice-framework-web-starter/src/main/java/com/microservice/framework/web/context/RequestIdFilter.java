package com.microservice.framework.web.context;

import com.microservice.framework.common.context.ContextKeys;
import com.microservice.framework.common.context.ThreadLocalContextAdapter;
import com.microservice.framework.web.WebProperties;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that generates or propagates request IDs.
 * <p>
 * Inspects the incoming request for an existing request ID header
 * (configurable via {@code framework.web.request-id.header-name}).
 * If the header is present, its value is used as the request ID.
 * If the header is missing and {@code generateIfMissing} is true,
 * a new UUID-based request ID is generated.
 * <p>
 * The request ID is stored in {@link RequestIdContext} (ThreadLocal)
 * for use by downstream components, and set as a response header
 * so the caller can correlate requests.
 *
 * @author Andy Yang
 */
public class RequestIdFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

    private final WebProperties.RequestIdProperties requestIdProperties;
    private final ThreadLocalContextAdapter contextAdapter;

    public RequestIdFilter(WebProperties.RequestIdProperties requestIdProperties) {
        this(requestIdProperties, new ThreadLocalContextAdapter());
    }

    public RequestIdFilter(WebProperties.RequestIdProperties requestIdProperties,
                           ThreadLocalContextAdapter contextAdapter) {
        this.requestIdProperties = requestIdProperties;
        this.contextAdapter = contextAdapter;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestId = resolveRequestId(httpRequest);
        String previousMdcRequestId = MDC.get(ContextKeys.REQUEST_ID);
        RequestIdContext.set(requestId);
        if (requestId != null) {
            httpResponse.setHeader(requestIdProperties.getHeaderName(), requestId);
            contextAdapter.get().put(ContextKeys.REQUEST_ID, requestId);
            MDC.put(ContextKeys.REQUEST_ID, requestId);
            httpRequest.setAttribute(ContextKeys.REQUEST_ID, requestId);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            RequestIdContext.remove();
            contextAdapter.clear();
            if (previousMdcRequestId == null) {
                MDC.remove(ContextKeys.REQUEST_ID);
            } else {
                MDC.put(ContextKeys.REQUEST_ID, previousMdcRequestId);
            }
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        String headerName = requestIdProperties.getHeaderName();
        String existingId = request.getHeader(headerName);

        if (existingId != null && !existingId.isBlank()) {
            log.debug("Using existing request ID from header {}: {}", headerName, existingId);
            return existingId;
        }

        if (requestIdProperties.isGenerateIfMissing()) {
            String generatedId = generateRequestId();
            log.debug("Generated new request ID: {}", generatedId);
            return generatedId;
        }

        // No existing ID and generation disabled — leave null
        return null;
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
