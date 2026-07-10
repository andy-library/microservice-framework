package com.microservice.framework.apollo.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 配置刷新策略
 * <p>
 * 控制配置键在配置变更时是否触发动态刷新。通过定义不可刷新的键前缀集合，
 * 防止某些关键配置（如数据库连接池大小、线程池参数）被意外刷新。
 * <p>
 * 默认策略为 {@link Strategy#ON_CHANGE}，即配置变更时自动刷新，
 * 但受不可刷新前缀集合约束。
 * <p>
 * 不可变对象，通过构造方法或 Builder 创建。
 *
 * @author Andy Yang
 */
public final class RefreshPolicy {

    /**
     * 刷新策略类型
     */
    public enum Strategy {
        /** 配置变更时自动刷新（受不可刷新前缀集合约束） */
        ON_CHANGE,
        /** 需手动触发刷新 */
        MANUAL,
        /** 禁止任何刷新 */
        NONE
    }

    private final Strategy strategy;
    private final Set<String> nonRefreshablePrefixes;

    /**
     * 创建刷新策略
     *
     * @param strategy           刷新策略类型
     * @param nonRefreshablePrefixes 不可刷新的配置键前缀集合
     */
    public RefreshPolicy(Strategy strategy, Set<String> nonRefreshablePrefixes) {
        this.strategy = Objects.requireNonNull(strategy, "strategy must not be null");
        this.nonRefreshablePrefixes = nonRefreshablePrefixes == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(nonRefreshablePrefixes));
    }

    /**
     * 创建默认刷新策略（ON_CHANGE，无不可刷新前缀）
     *
     * @return 默认刷新策略
     */
    public static RefreshPolicy defaultPolicy() {
        return new RefreshPolicy(Strategy.ON_CHANGE, Collections.emptySet());
    }

    /**
     * 创建禁止刷新的策略
     *
     * @return 禁止刷新的策略
     */
    public static RefreshPolicy none() {
        return new RefreshPolicy(Strategy.NONE, Collections.emptySet());
    }

    /**
     * 判断指定配置键是否允许刷新
     * <p>
     * 判断逻辑：
     * <ul>
     *   <li>NONE 策略：任何键都不允许刷新</li>
     *   <li>ON_CHANGE 策略：如果键匹配任何不可刷新前缀，则不允许刷新</li>
     *   <li>MANUAL 策略：配置变更时不自动刷新，需手动触发</li>
     * </ul>
     *
     * @param key 配置键
     * @return 是否允许自动刷新
     */
    public boolean isRefreshable(String key) {
        if (strategy == Strategy.NONE) {
            return false;
        }
        if (strategy == Strategy.MANUAL) {
            return false;
        }
        // ON_CHANGE: check non-refreshable prefixes
        for (String prefix : nonRefreshablePrefixes) {
            if (key.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    // Getters

    public Strategy getStrategy() {
        return strategy;
    }

    public Set<String> getNonRefreshablePrefixes() {
        return nonRefreshablePrefixes;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RefreshPolicy)) {
            return false;
        }
        RefreshPolicy other = (RefreshPolicy) obj;
        return strategy == other.strategy
                && nonRefreshablePrefixes.equals(other.nonRefreshablePrefixes);
    }

    @Override
    public int hashCode() {
        int result = strategy.hashCode();
        result = 31 * result + nonRefreshablePrefixes.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "RefreshPolicy{" +
                "strategy=" + strategy +
                ", nonRefreshablePrefixes=" + nonRefreshablePrefixes +
                '}';
    }
}
