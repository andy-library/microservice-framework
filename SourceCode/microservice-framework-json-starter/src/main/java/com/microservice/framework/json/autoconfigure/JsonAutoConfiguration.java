package com.microservice.framework.json.autoconfigure;

import com.microservice.framework.json.JsonProperties;
import com.microservice.framework.json.api.JsonCodec;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * JSON 自动配置入口
 * <p>
 * 注册 {@link JsonProperties} 并启用 JSON Starter 基础配置。
 * 具体的 {@link JsonCodec} 实现由 {@link JacksonJsonAutoConfiguration} 或
 * {@link Fastjson2JsonAutoConfiguration} 提供，两者互斥。
 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties(JsonProperties.class)
public class JsonAutoConfiguration {
}
