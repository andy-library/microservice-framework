package com.microservice.framework.common.id;

import com.microservice.framework.common.error.FrameworkErrorCode;
import com.microservice.framework.common.error.FrameworkException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 雪花算法 ID 生成器
 * <p>
 * 64-bit ID 结构：1 bit 符号位 | 41 bits 时间戳 | 10 bits 工作节点 | 12 bits 序列号
 * <p>
 * 时间戳自自定义纪元（2024-01-01T00:00:00Z）起算，单位为毫秒，
 * 可用约 69 年。工作节点 ID 范围为 0-1023，序列号范围为 0-4095，
 * 单节点单毫秒内最多生成 4096 个 ID。
 * <p>
 * 线程安全：{@link #generate()} 方法使用 {@code synchronized} 保证串行化。
 *
 * @author Andy Yang
 */
public class SnowflakeIdGenerator implements IdGenerator {

    /**
     * 自定义纪元：2024-01-01T00:00:00Z 对应的毫秒数
     */
    static final long EPOCH_MILLIS = 1704067200000L;

    /**
     * 工作节点 ID 占用位数
     */
    private static final int WORKER_ID_BITS = 10;

    /**
     * 序列号占用位数
     */
    private static final int SEQUENCE_BITS = 12;

    /**
     * 工作节点 ID 最大值：1023
     */
    private static final long MAX_WORKER_ID = (1L << WORKER_ID_BITS) - 1;

    /**
     * 序列号最大值：4095
     */
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;

    /**
     * 工作节点 ID 左移位数（序列号位数）
     */
    private static final int WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 时间戳左移位数（序列号位数 + 工作节点位数）
     */
    private static final int TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /**
     * 工作节点 ID
     */
    private final long workerId;

    /**
     * 时钟回拨容忍阈值（毫秒）
     */
    private final long clockBackwardToleranceMs;

    /**
     * 当前序列号
     */
    private long sequence = 0L;

    /**
     * 上次生成 ID 的时间戳（自定义纪元毫秒数）
     */
    private long lastTimestamp = -1L;

    /**
     * 构造雪花算法 ID 生成器
     *
     * @param workerId               工作节点 ID，范围 0-1023
     * @param clockBackwardToleranceMs 时钟回拨容忍阈值（毫秒），必须 >= 0
     * @throws FrameworkException workerId 超出范围时抛出
     */
    SnowflakeIdGenerator(long workerId, long clockBackwardToleranceMs) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new FrameworkException(
                    FrameworkErrorCode.COMMON_ID_WORKER_INVALID,
                    "Worker ID must be between 0 and " + MAX_WORKER_ID + ", but was " + workerId);
        }
        if (clockBackwardToleranceMs < 0) {
            throw new FrameworkException(
                    FrameworkErrorCode.COMMON_ID_CONFIG_INVALID,
                    "Clock backward tolerance must be >= 0, but was " + clockBackwardToleranceMs);
        }
        this.workerId = workerId;
        this.clockBackwardToleranceMs = clockBackwardToleranceMs;
    }

    /**
     * 静态工厂方法
     *
     * @param workerId               工作节点 ID，范围 0-1023
     * @param clockBackwardToleranceMs 时钟回拨容忍阈值（毫秒）
     * @return 雪花算法 ID 生成器实例
     * @throws FrameworkException workerId 超出范围时抛出
     */
    public static SnowflakeIdGenerator of(long workerId, long clockBackwardToleranceMs) {
        return new SnowflakeIdGenerator(workerId, clockBackwardToleranceMs);
    }

    /**
     * 生成单个分布式 ID
     * <p>
     * 线程安全，使用 {@code synchronized} 保证串行化。
     *
     * @return 全局唯一的长整型 ID
     * @throws FrameworkException 时钟回拨超过容忍阈值时抛出
     */
    @Override
    public synchronized long generate() {
        long currentTimestamp = currentEpochMillis();

        // 时钟回拨检测
        if (currentTimestamp < lastTimestamp) {
            long backwardMs = lastTimestamp - currentTimestamp;
            if (backwardMs <= clockBackwardToleranceMs) {
                // 回拨在容忍范围内，等待时间追平
                currentTimestamp = waitUntilNextMillis(lastTimestamp);
            } else {
                throw new FrameworkException(
                        FrameworkErrorCode.COMMON_ID_CLOCK_BACKWARD,
                        "Clock backward " + backwardMs + "ms exceeded tolerance " + clockBackwardToleranceMs + "ms");
            }
        }

        // 同一毫秒内，序列号递增
        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // 序列号溢出，自旋等待下一毫秒
                currentTimestamp = waitUntilNextMillis(lastTimestamp);
            }
        } else {
            // 新毫秒，序列号重置为 0
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        return ((currentTimestamp) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 批量生成分布式 ID
     *
     * @param count 需要生成的 ID 数量，必须大于 0
     * @return 包含 count 个全局唯一 ID 的列表
     */
    @Override
    public List<Long> batchGenerate(int count) {
        if (count <= 0) {
            throw new FrameworkException(
                    FrameworkErrorCode.COMMON_ID_CONFIG_INVALID,
                    "Batch generate count must be > 0, but was " + count);
        }
        List<Long> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(generate());
        }
        return ids;
    }

    /**
     * 获取当前时间戳（自定义纪元毫秒数）
     *
     * @return 当前时间相对于自定义纪元的毫秒数
     */
    private long currentEpochMillis() {
        return Instant.now().toEpochMilli() - EPOCH_MILLIS;
    }

    /**
     * 自旋等待直到时间戳超过给定值
     *
     * @param lastTimestamp 上次时间戳
     * @return 新的时间戳（严格大于 lastTimestamp）
     */
    private long waitUntilNextMillis(long lastTimestamp) {
        long timestamp = currentEpochMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentEpochMillis();
        }
        return timestamp;
    }
}
