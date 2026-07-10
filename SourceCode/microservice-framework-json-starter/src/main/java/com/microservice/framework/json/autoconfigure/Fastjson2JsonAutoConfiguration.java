package com.microservice.framework.json.autoconfigure;

import com.microservice.framework.json.JsonProperties;
import com.microservice.framework.json.api.JsonCodec;
import com.microservice.framework.json.fastjson2.Fastjson2JsonCodec;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Fastjson2 JSON 编解码器自动配置
 * <p>
 * 仅当以下两个条件同时满足时激活：
 * <ul>
 *   <li>{@code framework.json.provider=fastjson2}（用户显式配置）</li>
 *   <li>{@code com.alibaba.fastjson2.JSON} 类存在于类路径（fastjson2 依赖已引入）</li>
 * </ul>
 * <p>
 * 此配置与 {@link JacksonJsonAutoConfiguration} 互斥：
 * 当 provider=jackson（默认）时只有 Jackson 配置生效；
 * 当 provider=fastjson2 且类路径有 Fastjson2 时只有此配置生效。
 *
 * @author Andy Yang
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "framework.json", name = "provider", havingValue = "fastjson2")
@ConditionalOnClass(name = "com.alibaba.fastjson2.JSON")
public class Fastjson2JsonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(JsonCodec.class)
    public JsonCodec fastjson2JsonCodec(JsonProperties jsonProperties) {
        return new Fastjson2JsonCodec(jsonProperties);
    }
}
