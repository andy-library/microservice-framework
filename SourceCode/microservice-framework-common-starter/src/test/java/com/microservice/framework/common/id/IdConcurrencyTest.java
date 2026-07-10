package com.microservice.framework.common.id;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ID 并发唯一性测试
 * <p>
 * 使用多线程并发生成大量 ID，验证在并发场景下所有 ID 唯一且无异常。
 *
 * @author Andy Yang
 */
class IdConcurrencyTest {

    @Test
    @DisplayName("10 个线程并发生成 100 万个 ID 应全部唯一且无异常")
    void concurrentGenerationShouldProduceUniqueIdsWithoutExceptions() throws InterruptedException {
        int threadCount = 10;
        int idsPerThread = 100_000;
        int totalIds = threadCount * idsPerThread;

        SnowflakeIdGenerator generator = SnowflakeIdGenerator.of(1, 5000L);
        Set<Long> allIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        AtomicInteger exceptionCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < idsPerThread; i++) {
                        long id = generator.generate();
                        allIds.add(id);
                    }
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(30, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        assertThat(exceptionCount.get()).isZero();
        assertThat(allIds).hasSize(totalIds);
    }

    @Test
    @DisplayName("多线程并发批量生成应无异常")
    void concurrentBatchGenerationShouldNotThrow() throws InterruptedException {
        int threadCount = 5;
        int batchCount = 200;

        SnowflakeIdGenerator generator = SnowflakeIdGenerator.of(1, 5000L);
        Set<Long> allIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        AtomicInteger exceptionCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < batchCount; i++) {
                        List<Long> ids = generator.batchGenerate(100);
                        allIds.addAll(ids);
                    }
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(30, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        assertThat(exceptionCount.get()).isZero();
        assertThat(allIds).hasSize(threadCount * batchCount * 100);
    }
}
