package com.microservice.framework.logging.core.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 日志限流 TurboFilter
 * 基于令牌桶算法实现日志风暴防护
 * 
 * @author Andy Yang
 */
public class RateLimitingTurboFilter extends TurboFilter {

    /**
     * 每秒允许的日志数量（令牌生成速率）
     */
    private volatile int rate = 10;

    /**
     * 突发容量（桶的最大容量）
     */
    private volatile int burstCapacity = 100;

    /**
     * 当前令牌数量
     */
    private final AtomicLong tokens = new AtomicLong(0);

    /**
     * 上次补充令牌的时间戳（纳秒）
     */
    private volatile long lastRefillTimestamp = System.nanoTime();

    /**
     * 是否启用限流
     */
    private volatile boolean enabled = true;

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        if (!enabled) {
            return FilterReply.NEUTRAL;
        }

        // 补充令牌
        refillTokens();

        // 尝试获取令牌
        if (tryAcquire()) {
            return FilterReply.NEUTRAL; // 允许通过
        } else {
            return FilterReply.DENY; // 拒绝（丢弃日志）
        }
    }

    /**
     * 补充令牌（令牌桶算法）
     */
    private void refillTokens() {
        long now = System.nanoTime();
        long lastRefill = lastRefillTimestamp;

        // 计算时间差（秒）
        double elapsedSeconds = (now - lastRefill) / 1_000_000_000.0;

        if (elapsedSeconds > 0) {
            // 计算应该补充的令牌数量
            long tokensToAdd = (long) (elapsedSeconds * rate);

            if (tokensToAdd > 0) {
                // 更新时间戳
                lastRefillTimestamp = now;

                // 补充令牌，但不超过桶的容量
                long currentTokens = tokens.get();
                long newTokens = Math.min(currentTokens + tokensToAdd, burstCapacity);
                tokens.set(newTokens);
            }
        }
    }

    /**
     * 尝试获取一个令牌
     * 
     * @return true 如果成功获取令牌，false 如果没有可用令牌
     */
    private boolean tryAcquire() {
        while (true) {
            long currentTokens = tokens.get();
            if (currentTokens > 0) {
                // 尝试消费一个令牌
                if (tokens.compareAndSet(currentTokens, currentTokens - 1)) {
                    return true;
                }
                // CAS 失败，重试
            } else {
                // 没有可用令牌
                return false;
            }
        }
    }

    // Getters and Setters

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public int getBurstCapacity() {
        return burstCapacity;
    }

    public void setBurstCapacity(int burstCapacity) {
        this.burstCapacity = burstCapacity;
        // 重置令牌数量，避免超过新的容量
        long currentTokens = tokens.get();
        if (currentTokens > burstCapacity) {
            tokens.set(burstCapacity);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
