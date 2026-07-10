package com.microservice.framework.objectstorage.api;

import com.microservice.framework.common.error.FrameworkErrorCode;
import com.microservice.framework.common.error.FrameworkException;

/**
 * Object Storage 操作异常
 * <p>
 * 所有对象存储操作错误统一使用此异常，包括但不限于：
 * <ul>
 *   <li>输入参数为 null 或不合法</li>
 *   <li>存储桶不存在或不可访问</li>
 *   <li>对象不存在（下载/删除/获取元数据时）</li>
 *   <li>上传文件超出大小限制或类型不允许</li>
 *   <li>预签名 URL 生成失败</li>
 *   <li>文件治理规则触发（如隔离）</li>
 *   <li>底层 SDK 抛出的未知异常</li>
 * </ul>
 *
 * @author Andy Yang
 */
public class ObjectStorageException extends FrameworkException {

    /** 输入参数为 null 或不合法 */
    public static final FrameworkErrorCode OS_PARAM_NULL =
            FrameworkErrorCode.of("OS", "PARAM", 1);

    /** 存储桶不存在或不可访问 */
    public static final FrameworkErrorCode OS_BUCKET_NOT_FOUND =
            FrameworkErrorCode.of("OS", "BUCKET", 1);

    /** 对象不存在 */
    public static final FrameworkErrorCode OS_OBJECT_NOT_FOUND =
            FrameworkErrorCode.of("OS", "OBJECT", 1);

    /** 上传文件大小超过限制 */
    public static final FrameworkErrorCode OS_UPLOAD_SIZE_EXCEEDED =
            FrameworkErrorCode.of("OS", "UPLOAD", 1);

    /** 上传文件类型不允许 */
    public static final FrameworkErrorCode OS_UPLOAD_TYPE_NOT_ALLOWED =
            FrameworkErrorCode.of("OS", "UPLOAD", 2);

    /** 预签名 URL 生成失败 */
    public static final FrameworkErrorCode OS_PRESIGN_FAILED =
            FrameworkErrorCode.of("OS", "PRESIGN", 1);

    /** 文件被治理规则隔离 */
    public static final FrameworkErrorCode OS_GOVERNANCE_QUARANTINED =
            FrameworkErrorCode.of("OS", "GOVERNANCE", 1);

    /** 通用存储操作内部错误 */
    public static final FrameworkErrorCode OS_INTERNAL_ERROR =
            FrameworkErrorCode.of("OS", "INTERNAL", 1);

    /**
     * 创建仅包含错误码的异常
     *
     * @param errorCode 错误码
     */
    public ObjectStorageException(FrameworkErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 创建包含错误码和详细信息的异常
     *
     * @param errorCode 错误码
     * @param message   详细描述信息
     */
    public ObjectStorageException(FrameworkErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 创建包含错误码、详细信息和原始异常的异常
     *
     * @param errorCode 错误码
     * @param message   详细描述信息
     * @param cause     原始异常
     */
    public ObjectStorageException(FrameworkErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 创建包含错误码和原始异常的异常
     *
     * @param errorCode 错误码
     * @param cause     原始异常
     */
    public ObjectStorageException(FrameworkErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
