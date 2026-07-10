package com.microservice.framework.json.api;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON 编解码定制器
 * <p>
 * 用户可通过注册此接口的 Bean 来微调由 Spring Boot 管理的 {@link ObjectMapper} 配置，
 * 如添加自定义模块、修改序列化特征、注册自定义序列化器/反序列化器等。
 * <p>
 * 仅在 {@code framework.json.provider=jackson}（默认）时生效；
 * 当切换到 Fastjson2 实现时，此定制器不会被调用。
 *
 * @author Andy Yang
 */
public interface JsonCodecCustomizer {

    /**
     * 定制 {@link ObjectMapper} 配置
     * <p>
     * 此方法在框架完成基础安全配置（阻断多态反序列化、限制嵌套深度等）之后调用，
     * 用户定制不应覆盖安全基线配置。
     *
     * @param objectMapper 由 Spring Boot 管理的 ObjectMapper 实例
     */
    void customize(ObjectMapper objectMapper);
}
