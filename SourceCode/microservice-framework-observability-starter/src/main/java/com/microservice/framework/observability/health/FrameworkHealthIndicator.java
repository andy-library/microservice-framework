package com.microservice.framework.observability.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * 框架健康检查指示器抽象基类
 * 业务项目可以继承此类实现自定义健康检查
 * 
 * @author Andy Yang
 */
public abstract class FrameworkHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            return doHealthCheck();
        } catch (Exception e) {
            return Health.down()
                    .withException(e)
                    .build();
        }
    }

    /**
     * 执行健康检查
     * 子类实现具体的健康检查逻辑
     * 
     * @return Health 对象
     */
    protected abstract Health doHealthCheck();
}
