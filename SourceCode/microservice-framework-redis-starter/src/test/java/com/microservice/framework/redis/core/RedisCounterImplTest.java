package com.microservice.framework.redis.core;

import com.microservice.framework.redis.RedisProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class RedisCounterImplTest {

    private RedisCounterImpl counter;

    @BeforeEach
    void setUp() {
        counter = new RedisCounterImpl(mock(StringRedisTemplate.class),
                new RedisProperties.CounterProperties());
    }

    @Test
    @DisplayName("increment(key, delta) 应拒绝非正数步长")
    void incrementShouldRejectNonPositiveDelta() {
        assertThatThrownBy(() -> counter.increment("order", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delta");
    }

    @Test
    @DisplayName("decrement(key, delta) 应拒绝非正数步长")
    void decrementShouldRejectNonPositiveDelta() {
        assertThatThrownBy(() -> counter.decrement("order", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delta");
    }
}
