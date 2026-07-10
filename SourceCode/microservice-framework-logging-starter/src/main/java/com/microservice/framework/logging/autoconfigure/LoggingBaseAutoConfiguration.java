package com.microservice.framework.logging.autoconfigure;

import com.microservice.framework.logging.properties.LoggingProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 日志基础自动配置
 * 
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties(LoggingProperties.class)
public class LoggingBaseAutoConfiguration {

}
