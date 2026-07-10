package com.microservice.framework.json.api;

import com.microservice.framework.common.error.FrameworkErrorCode;
import com.microservice.framework.common.error.FrameworkException;

/**
 * JSON 编解码异常
 * <p>
 * 所有 JSON 序列化/反序列化错误统一使用此异常，包括但不限于：
 * <ul>
 *   <li>输入/输出参数为 null</li>
 *   <li>JSON 格式错误（如语法错误、不完整 JSON）</li>
 *   <li>类型不匹配（如期望对象但收到数组）</li>
 *   <li>安全限制触发（如多态反序列化被阻断、超长 JSON、超深嵌套）</li>
 *   <li>底层实现抛出的未知异常</li>
 * </ul>
 *
 * @author Andy Yang
 */
public class JsonCodecException extends FrameworkException {

    /** 输入参数为 null */
    public static final FrameworkErrorCode JSON_PARAM_NULL =
            FrameworkErrorCode.of("JSON", "PARAM", 1);

    /** JSON 格式错误 */
    public static final FrameworkErrorCode JSON_FORMAT_ERROR =
            FrameworkErrorCode.of("JSON", "FORMAT", 1);

    /** 类型不匹配 */
    public static final FrameworkErrorCode JSON_TYPE_MISMATCH =
            FrameworkErrorCode.of("JSON", "TYPE", 1);

    /** 多态反序列化被安全策略阻断 */
    public static final FrameworkErrorCode JSON_POLYMORPHIC_BLOCKED =
            FrameworkErrorCode.of("JSON", "SECURITY", 1);

    /** JSON 嵌套深度超过安全限制 */
    public static final FrameworkErrorCode JSON_DEPTH_EXCEEDED =
            FrameworkErrorCode.of("JSON", "SECURITY", 2);

    /** JSON 数据长度超过安全限制 */
    public static final FrameworkErrorCode JSON_PAYLOAD_EXCEEDED =
            FrameworkErrorCode.of("JSON", "SECURITY", 3);

    /** 反序列化遇到未知属性 */
    public static final FrameworkErrorCode JSON_UNKNOWN_PROPERTY =
            FrameworkErrorCode.of("JSON", "DESER", 1);

    /** 通用序列化/反序列化内部错误 */
    public static final FrameworkErrorCode JSON_INTERNAL_ERROR =
            FrameworkErrorCode.of("JSON", "INTERNAL", 1);

    /**
     * 创建仅包含错误码的异常
     *
     * @param errorCode 错误码
     */
    public JsonCodecException(FrameworkErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 创建包含错误码和详细信息的异常
     *
     * @param errorCode 错误码
     * @param message   详细描述信息
     */
    public JsonCodecException(FrameworkErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 创建包含错误码、详细信息和原始异常的异常
     *
     * @param errorCode 错误码
     * @param message   详细描述信息
     * @param cause     原始异常
     */
    public JsonCodecException(FrameworkErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 创建包含错误码和原始异常的异常
     *
     * @param errorCode 错误码
     * @param cause     原始异常
     */
    public JsonCodecException(FrameworkErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
