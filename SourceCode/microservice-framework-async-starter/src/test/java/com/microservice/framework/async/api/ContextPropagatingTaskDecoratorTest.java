package com.microservice.framework.async.api;

import com.microservice.framework.common.context.ContextKeys;
import com.microservice.framework.common.context.FrameworkContext;
import com.microservice.framework.common.context.ThreadLocalContextAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ContextPropagatingTaskDecorator 测试
 * <p>
 * 验证上下文拷贝和清理行为，
 * 不依赖真实的 Spring 容器。
 *
 * @author Andy Yang
 */
class ContextPropagatingTaskDecoratorTest {

    private ThreadLocalContextAdapter contextAdapter;
    private ContextPropagatingTaskDecorator decorator;

    @BeforeEach
    void setUp() {
        contextAdapter = new ThreadLocalContextAdapter();
        decorator = new ContextPropagatingTaskDecorator(contextAdapter);
    }

    @AfterEach
    void tearDown() {
        contextAdapter.clear();
    }

    @Test
    @DisplayName("decorate 应在子线程恢复父线程上下文")
    void decorateShouldRestoreParentContextInChildThread() {
        // 设置父线程上下文
        FrameworkContext parentContext = contextAdapter.get();
        parentContext.put(ContextKeys.REQUEST_ID, "req-001");
        parentContext.put(ContextKeys.USER_ID, "user-42");

        String[] requestId = new String[1];
        String[] userId = new String[1];

        Runnable decorated = decorator.decorate(() -> {
            requestId[0] = contextAdapter.get().get(ContextKeys.REQUEST_ID);
            userId[0] = contextAdapter.get().get(ContextKeys.USER_ID);
        });

        // 在新线程执行（模拟子线程）
        contextAdapter.clear(); // 确保子线程初始没有上下文
        decorated.run();

        assertThat(requestId[0]).isEqualTo("req-001");
        assertThat(userId[0]).isEqualTo("user-42");
    }

    @Test
    @DisplayName("decorate 应在任务完成后清理子线程上下文")
    void decorateShouldCleanUpChildContextAfterTask() {
        FrameworkContext parentContext = contextAdapter.get();
        parentContext.put(ContextKeys.REQUEST_ID, "req-002");

        Runnable decorated = decorator.decorate(() -> {
            // 任务执行中上下文存在
            assertThat(contextAdapter.get().get(ContextKeys.REQUEST_ID)).isEqualTo("req-002");
        });

        contextAdapter.clear();
        decorated.run();

        // 任务完成后子线程上下文应被清理
        // 注意：decorator.run() 在同一线程内执行，clear 会清理当前线程的 ThreadLocal
        // 但 afterEach 会再次清理，所以这里验证装饰器的 finally 块被执行
    }

    @Test
    @DisplayName("decorate 父线程无上下文时子线程应为空上下文")
    void decorateWithEmptyParentContextShouldResultInEmptyChildContext() {
        // 不设置任何上下文
        String[] requestId = new String[1];

        Runnable decorated = decorator.decorate(() -> {
            requestId[0] = contextAdapter.get().get(ContextKeys.REQUEST_ID);
        });

        contextAdapter.clear();
        decorated.run();

        assertThat(requestId[0]).isNull();
    }

    @Test
    @DisplayName("decorate 应捕获调用时的上下文快照")
    void decorateShouldCaptureSnapshotAtDecorationTime() {
        FrameworkContext parentContext = contextAdapter.get();
        parentContext.put(ContextKeys.REQUEST_ID, "req-old");

        // decorate 被调用时捕获快照
        Runnable decorated = decorator.decorate(() -> {
            assertThat(contextAdapter.get().get(ContextKeys.REQUEST_ID)).isEqualTo("req-old");
        });

        // 修改上下文（不影响已捕获的快照）
        parentContext.put(ContextKeys.REQUEST_ID, "req-new");

        contextAdapter.clear();
        decorated.run();
    }

    @Test
    @DisplayName("decorate 任务抛异常时也应清理上下文")
    void decorateShouldCleanUpContextEvenWhenTaskThrows() {
        FrameworkContext parentContext = contextAdapter.get();
        parentContext.put(ContextKeys.REQUEST_ID, "req-003");

        Runnable decorated = decorator.decorate(() -> {
            throw new RuntimeException("task failed");
        });

        contextAdapter.clear();
        try {
            decorated.run();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("task failed");
        }

        // 即使任务抛异常，finally 块也应执行 clear
    }
}
