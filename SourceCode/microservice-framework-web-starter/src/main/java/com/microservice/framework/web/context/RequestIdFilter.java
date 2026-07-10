package com.microservice.framework.web.context;

import com.microservice.framework.web.WebProperties;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public RequestIdFilter(WebProperties.RequestIdProperties requestIdProperties) {
        this.requestIdProperties = requestIdProperties;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String requestId = resolveRequestId(httpRequest);
        RequestIdContext.set(requestId);

        // Also store in the framework context for cross-layer propagation
        try {
            chain.doFilter(request, response);
        } finally {
            RequestIdContext.remove();
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
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
