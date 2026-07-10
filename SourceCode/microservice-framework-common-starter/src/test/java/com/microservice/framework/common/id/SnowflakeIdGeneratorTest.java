package com.microservice.framework.common.id;

import com.microservice.framework.common.error.FrameworkException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SnowflakeIdGenerator 单元测试
 * <p>
 * 验证雪花算法 ID 生成器的单次生成、批量生成、唯一性、单调递增、
 * 参数校验、时钟回拨容忍以及序列号溢出等场景。
 *
 * @author Andy Yang
 */
class SnowflakeIdGeneratorTest {

    @Nested
    @DisplayName("基本 ID 生成")
    class BasicGeneration {

        @Test
        @DisplayName("单个 ID 生成应为正数")
        void singleIdShouldBePositive() {
            SnowflakeIdGenerator generator = SnowflakeIdGenerator.of(1, 5000L);
            long id = generator.generate();
            assertThat(id).isPositive();
        }

        @Test
        @DisplayName("批量生成应产生正确数量")
        void batchGenerationShouldProduceCorrectCount() {
            SnowflakeIdGenerator generator = SnowflakeIdGenerator.of(1, 5000L);
            List<Long> ids = generator.batchGenerate(100);
            assertThat(ids).hasSize(100);
        }

        @Test
        @DisplayName("批量生成中所有 ID 应唯一")
        void allIdsInBatchShouldBeUnique() {
            SnowflakeIdGenerator generator = SnowflakeIdGenerator.of(1, 5000L);
            List<Long> ids = generator.batchGenerate(1000);
            Set<Long> uniqueIds = new HashSet<>(ids);
            assertThat(uniqueIds).hasSize(ids.size());
        }

        @Test
        @DisplayName("同一 worker ID 生成的序列应单调递增")
        void idsShouldBeMonotonicallyIncreasing() {
            SnowflakeIdGenerator generator = SnowflakeIdGenerator.of(1, 5000L);
            long previous = generator.generate();
            for (int i = 0; i < 100; i++) {
                long current = generator.generate();
                assertThat(current).isGreaterThan(previous);
                previous = current;
            }
        }
    }

    @Nested
    @DisplayName("参数校验")
    class ParameterValidation {

        @Test
        @DisplayName("负数 worker-id 应抛出 FrameworkException")
        void negativeWorkerIdShouldThrowFrameworkException() {
            assertThatThrownBy(() -> SnowflakeIdGenerator.of(-1, 5000L))
                    .isInstanceOf(FrameworkException.class)
                    .hasMessageContaining("Worker ID must be between 0 and 1023");
        }

        @Test
        @DisplayName("超出范围的 worker-id (>1023) 应抛出 FrameworkException")
        void workerIdAboveMaxShouldThrowFrameworkException() {
            assertThatThrownBy(() -> SnowflakeIdGenerator.of(1024, 5000L))
                    .isInstanceOf(FrameworkException.class)
                    .hasMessageContaining("Worker ID must be between 0 and 1023");
        }

        @Test
        @DisplayName("合法边界 worker-id=0 应正常工作")
        void workerIdAtLowerBoundShouldWork() {
            SnowflakeIdGenerator generator = SnowflakeIdGenerator.of(0, 5000L);
            assertThat(generator.generate()).isPositive();
        }

        @Test
        @DisplayName("合法边界 worker-id=1023 应正常工作")
        void workerIdAtUpperBoundShouldWork() {
            SnowflakeIdGenerator generator = SnowflakeIdGenerator.of(1023, 5000L);
            assertThat(generator.generate()).isPositive();
        }

        @Test
        @DisplayName("负数回拨容忍阈值应抛出 FrameworkException")
        void negativeToleranceShouldThrowFrameworkException() {
            assertThatThrownBy(() -> SnowflakeIdGenerator.of(1, -1L))
                    .isInstanceOf(FrameworkException.class)
                    .hasMessageContaining("Clock backward tolerance must be >= 0");
        }

        @Test
        @DisplayName("批量生成数量 <=0 应抛出 FrameworkException")
        void batchGenerateWithZeroOrNegativeShouldThrow() {
            SnowflakeIdGenerator generator = SnowflakeIdGenerator.of(1, 5000L);
            assertThatThrownBy(() -> generator.batchGenerate(0))
                    .isInstanceOf(FrameworkException.class)
                    .hasMessageContaining("Batch generate count must be > 0");
            assertThatThrownBy(() -> generator.batchGenerate(-1))
                    .isInstanceOf(FrameworkException.class)
                    .hasMessageContaining("Batch generate count must be > 0");
        }
    }

    @Nested
    @DisplayName("序列号溢出")
    class SequenceOverflow {

        @Test
        @DisplayName("同一毫秒内超过 4096 个 ID 应自旋等待下一毫秒")
        void sequenceOverflowShouldSpinToNextMillis() {
            SnowflakeIdGenerator generator = SnowflakeIdGenerator.of(1, 5000L);
            // 生成 4097 个 ID，超出单毫秒容量，最后一个 ID 应仍在不同毫秒中生成
            List<Long> ids = generator.batchGenerate(4097);
            Set<Long> uniqueIds = new HashSet<>(ids);
            assertThat(uniqueIds).hasSize(ids.size());
        }
    }
}
