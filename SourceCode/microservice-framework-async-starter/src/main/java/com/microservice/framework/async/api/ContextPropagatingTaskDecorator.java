package com.microservice.framework.async.api;

import com.microservice.framework.common.context.ContextSnapshot;
import com.microservice.framework.common.context.ThreadLocalContextAdapter;
import org.springframework.core.task.TaskDecorator;

/**
 * 上下文传播任务装饰器
 * <p>
 * 实现 Spring {@link TaskDecorator}，在异步任务执行前将父线程的
 * {@link ContextSnapshot} 恢复到子线程的 {@link ThreadLocalContextAdapter}，
 * 执行完成后自动清理子线程的上下文绑定，防止 ThreadLocal 泄漏。
 * <p>
 * 使用流程：
 * <pre>
 * // 父线程
 * ThreadLocalContextAdapter adapter = ...;
 * adapter.get().put(ContextKeys.REQUEST_ID, "req-123");
 * ContextSnapshot snapshot = adapter.snapshot();
 *
 * // 提交异步任务时，装饰器自动传播上下文
 * ContextPropagatingTaskDecorator decorator = new ContextPropagatingTaskDecorator(adapter);
 * taskExecutor.execute(decorator.decorate(() -&gt; {
 *     // 子线程可以读取父线程的上下文
 *     String requestId = adapter.get().get(ContextKeys.REQUEST_ID); // "req-123"
 * }));
 * </pre>
 *
 * @author Andy Yang
 */
public class ContextPropagatingTaskDecorator implements TaskDecorator {

    private final ThreadLocalContextAdapter contextAdapter;

    /**
     * 创建上下文传播装饰器
     *
     * @param contextAdapter ThreadLocal 上下文适配器
     */
    public ContextPropagatingTaskDecorator(ThreadLocalContextAdapter contextAdapter) {
        this.contextAdapter = contextAdapter;
    }

    /**
     * 装饰异步任务，在执行前恢复父线程上下文到子线程。
     * <p>
     * 执行流程：
     * 1. 在父线程捕获当前上下文快照
     * 2. 在子线程恢复快照到 ThreadLocalContextAdapter
     * 3. 执行异步任务
     * 4. 清理子线程的上下文绑定（防止线程池复用导致的数据泄漏）
     *
     * @param runnable 原始异步任务
     * @return 被装饰的任务，包含上下文传播逻辑
     */
    @Override
    public Runnable decorate(Runnable runnable) {
        // 在父线程捕获上下文快照
        ContextSnapshot snapshot = contextAdapter.snapshot();
        return () -> {
            try {
                // 在子线程恢复上下文
                contextAdapter.restore(snapshot);
                runnable.run();
            } finally {
                // 清理子线程上下文，防止 ThreadLocal 泄漏
                contextAdapter.clear();
            }
        };
    }
}
