package com.microservice.demo.embedded;

import com.microservice.framework.xxljob.api.IdempotentJobHandler;
import com.microservice.framework.xxljob.api.JobExecutionContext;
import com.microservice.framework.xxljob.api.JobHandler;
import com.microservice.framework.xxljob.api.JobResult;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Embedded Job Configuration
 *
 * Provides a simple {@link IdempotentJobHandler} bean using ConcurrentHashMap-based
 * idempotency tracking. This enables the xxl-job demo even without
 * {@code com.xxl.job.core.handler.IJobHandler} on the classpath.
 *
 * <p>Since the xxl-job starter auto-configuration is gated by
 * {@code @ConditionalOnClass(name = "com.xxl.job.core.handler.IJobHandler")},
 * and xxl-job-core is declared as an optional dependency in the starter, the
 * auto-config will not activate in the demo project. This configuration class
 * provides the bean directly, bypassing the auto-config entirely.</p>
 *
 * <p>Activates only when {@code framework.xxl-job.provider=embedded} is set.</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "framework.xxl-job", name = "provider", havingValue = "embedded")
public class EmbeddedJobConfiguration {

    @Bean
    public IdempotentJobHandler inMemoryIdempotentJobHandler() {
        return new InMemoryIdempotentJobHandler();
    }

    /**
     * ConcurrentHashMap-backed IdempotentJobHandler that provides full idempotency
     * protection without requiring xxl-job-core on the classpath.
     *
     * <p>Implements all four interface methods:</p>
     * <ul>
     *   <li>{@code isDuplicate()} — checks the processed-set for the context key</li>
     *   <li>{@code markProcessed()} — adds the context key to the processed-set</li>
     *   <li>{@code execute()} — runs the delegate JobHandler (or returns success if no delegate)</li>
     *   <li>{@code getDelegate()} — returns the simple demo delegate handler</li>
     * </ul>
     */
    static class InMemoryIdempotentJobHandler implements IdempotentJobHandler {

        private final Set<String> processedKeys = ConcurrentHashMap.newKeySet();
        private final DemoJobHandler delegate = new DemoJobHandler();

        /**
         * Build a unique idempotency key from the execution context.
         * Format: jobId-executorId-param (matches the framework's MemoryIdempotencyStore pattern).
         */
        private String buildKey(JobExecutionContext context) {
            return context.getJobId() + "-" + context.getExecutorId() + "-" + context.getParam();
        }

        @Override
        public boolean isDuplicate(JobExecutionContext context) {
            if (context == null) {
                return false;
            }
            return processedKeys.contains(buildKey(context));
        }

        @Override
        public void markProcessed(JobExecutionContext context) {
            if (context == null) {
                return;
            }
            processedKeys.add(buildKey(context));
        }

        @Override
        public JobResult execute(JobExecutionContext context) {
            // Check idempotency before executing
            if (isDuplicate(context)) {
                return JobResult.success("Job already processed — skipped (idempotent)");
            }

            // Execute via the delegate handler
            JobResult result = delegate.execute(context);

            // Mark as processed after successful execution
            markProcessed(context);

            return result;
        }

        @Override
        public JobHandler getDelegate() {
            return delegate;
        }

        /**
         * Returns the count of processed job keys (for monitoring/debugging).
         */
        public int getProcessedCount() {
            return processedKeys.size();
        }

        /**
         * Clears all processed keys (for test reset).
         */
        public void clearProcessed() {
            processedKeys.clear();
        }
    }

    /**
     * Simple demo JobHandler that returns a success result with a descriptive message.
     * This serves as the delegate within InMemoryIdempotentJobHandler, demonstrating
     * the JobHandler delegation pattern without requiring actual business logic.
     */
    static class DemoJobHandler implements JobHandler {

        @Override
        public JobResult execute(JobExecutionContext context) {
            if (context == null) {
                return JobResult.fail("Job context is null");
            }

            String message = String.format(
                    "Demo job executed successfully — jobId=%d, executorId=%s, param=%s",
                    context.getJobId(),
                    context.getExecutorId(),
                    context.getParam()
            );
            return JobResult.success(message);
        }
    }
}
