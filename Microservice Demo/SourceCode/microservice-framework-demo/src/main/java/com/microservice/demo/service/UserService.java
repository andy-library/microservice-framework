package com.microservice.demo.service;

import com.microservice.demo.model.User;
import com.microservice.framework.logging.util.Log;
import com.microservice.framework.observability.annotation.SpanTag;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户服务
 * 演示：
 * 1. 结构化日志
 * 2. 异常日志
 * 3. @SpanTag 注解
 */
@Service
public class UserService {

    private final Map<String, User> userStore = new ConcurrentHashMap<>();

    /**
     * 根据 ID 查询用户
     */
    public User findById(@SpanTag("userId") String userId) {
        Log.info("查询用户信息")
                .with("userId", userId)
                .with("operation", "findById")
                .log();

        User user = userStore.get(userId);

        if (user == null) {
            Log.warn("用户不存在")
                    .with("userId", userId)
                    .log();
            throw new RuntimeException("User not found: " + userId);
        }

        return user;
    }

    /**
     * 创建用户
     */
    public User createUser(@SpanTag("username") User user) {
        String userId = UUID.randomUUID().toString();
        user.setId(userId);
        user.setCreatedAt(System.currentTimeMillis());

        // 记录包含敏感信息的日志（用于测试脱敏）
        Log.info("创建用户")
                .with("userId", userId)
                .with("username", user.getUsername())
                .with("phone", user.getPhone())
                .with("email", user.getEmail())
                .with("idCard", user.getIdCard())
                .with("bankCard", user.getBankCard())
                .log();

        userStore.put(userId, user);

        return user;
    }

    /**
     * 更新用户
     */
    public User updateUser(@SpanTag("userId") String userId, User user) {
        User existingUser = findById(userId);

        Log.info("更新用户信息")
                .with("userId", userId)
                .with("changes", "username,email,phone")
                .log();

        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());

        return existingUser;
    }

    /**
     * 删除用户
     */
    public void deleteUser(@SpanTag("userId") String userId) {
        Log.info("删除用户")
                .with("userId", userId)
                .log();

        userStore.remove(userId);
    }

    /**
     * 模拟异常场景
     */
    public void simulateError(@SpanTag("userId") String userId) {
        Log.info("模拟异常场景")
                .with("userId", userId)
                .log();

        try {
            // 模拟业务异常
            throw new IllegalStateException("Simulated error for user: " + userId);
        } catch (Exception e) {
            Log.error("用户操作失败")
                    .withException(e)
                    .with("userId", userId)
                    .with("operation", "simulateError")
                    .log();
            throw e;
        }
    }
}
