package com.microservice.framework.web.exception;

import com.microservice.framework.common.error.FrameworkErrorCode;
import com.microservice.framework.common.error.FrameworkException;

/**
 * Business-level exception for the Web module.
 * <p>
 * Thrown when a business rule is violated (e.g., insufficient balance,
 * duplicate order). Carries a structured {@link FrameworkErrorCode}
 * following the module error code convention.
 * <p>
 * Error code format: {@code WEB-BIZ-NNN}
 *
 * @author Andy Yang
 */
public class BusinessException extends FrameworkException {

    /** Generic business rule violation. */
    public static final FrameworkErrorCode WEB_BIZ_GENERAL = FrameworkErrorCode.of("WEB", "BIZ", 1);

    /** Resource not found. */
    public static final FrameworkErrorCode WEB_BIZ_NOT_FOUND = FrameworkErrorCode.of("WEB", "BIZ", 2);

    /** Operation not permitted. */
    public static final FrameworkErrorCode WEB_BIZ_FORBIDDEN = FrameworkErrorCode.of("WEB", "BIZ", 3);

    /** Duplicate operation. */
    public static final FrameworkErrorCode WEB_BIZ_DUPLICATE = FrameworkErrorCode.of("WEB", "BIZ", 4);

    public BusinessException(FrameworkErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessException(FrameworkErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessException(FrameworkErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public BusinessException(FrameworkErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
