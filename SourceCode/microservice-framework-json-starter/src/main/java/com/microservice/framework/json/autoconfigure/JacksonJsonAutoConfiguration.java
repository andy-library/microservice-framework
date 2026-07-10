package com.microservice.framework.json.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.framework.json.JsonProperties;
import com.microservice.framework.json.api.JsonCodec;
import com.microservice.framework.json.api.JsonCodecCustomizer;
import com.microservice.framework.json.jackson.JacksonJsonCodec;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Jackson JSON 编解码器自动配置
 * <p>
 * 当 {@code framework.json.provider} 为 {@code jackson}（默认值）时激活，
 * 创建 {@link JacksonJsonCodec} 作为 {@link JsonCodec} 的默认实现。
 * <p>
 * 安全保障：
 * <ul>
 *   <li>默认阻断多态反序列化</li>
 *   <li>限制嵌套深度和最大数据长度</li>
 *   <li>用户可通过 {@link JsonCodecCustomizer} 微调 ObjectMapper</li>
 * </ul>
 *
 * @author Andy Yang
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "framework.json", name = "provider",
        havingValue = "jackson", matchIfMissing = true)
public class JacksonJsonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(JsonCodec.class)
    public JsonCodec jacksonJsonCodec(ObjectMapper objectMapper,
                                      JsonProperties jsonProperties,
                                      List<JsonCodecCustomizer> customizers) {
        return new JacksonJsonCodec(objectMapper, jsonProperties, customizers);
    }
}
