package com.microservice.framework.apollo.autoconfigure;

/**
 * Nacos Starter 与 Apollo Starter 冲突异常
 * <p>
 * 当 Nacos 和 Apollo Starter 同时存在于类路径时抛出此异常，
 * 防止两个配置中心同时生效导致配置冲突。
 * <p>
 * 此异常会被 {@link NacosExclusionFailureAnalyzer} 捕获并转换为
 * 用户友好的错误报告。
 *
 * @author Andy Yang
 */
public class NacosStarterConflictException extends RuntimeException {

    /**
     * 创建 Nacos Starter 冲突异常
     */
    public NacosStarterConflictException() {
        super("Apollo Starter and Nacos Starter cannot coexist on the classpath. " +
                "Please remove one of them to avoid configuration conflicts.");
    }

    /**
     * 创建 Nacos Starter 冲突异常，带自定义消息
     *
     * @param message 异常消息
     */
    public NacosStarterConflictException(String message) {
        super(message);
    }
}
