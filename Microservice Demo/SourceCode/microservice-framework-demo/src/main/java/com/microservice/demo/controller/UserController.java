package com.microservice.demo.controller;

import com.microservice.demo.model.User;
import com.microservice.demo.service.UserService;
import com.microservice.framework.logging.util.Log;
import com.microservice.framework.observability.annotation.SpanTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 用户 Controller
 * 演示基础 CRUD 操作 + 追踪 + 日志
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 创建用户
     * 演示：数据脱敏（phone, email, idCard, bankCard）
     */
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        Log.info("接收创建用户请求")
                .with("username", user.getUsername())
                .log();

        User created = userService.createUser(user);

        return ResponseEntity.ok(created);
    }

    /**
     * 查询用户
     * 演示：@SpanTag 注解
     */
    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@SpanTag("userId") @PathVariable String userId) {
        User user = userService.findById(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{userId}")
    public ResponseEntity<User> updateUser(@PathVariable String userId,
            @RequestBody User user) {
        User updated = userService.updateUser(userId, user);
        return ResponseEntity.ok(updated);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 模拟异常
     * 演示：异常日志自动提取
     */
    @GetMapping("/{userId}/error")
    public ResponseEntity<Void> simulateError(@PathVariable String userId) {
        userService.simulateError(userId);
        return ResponseEntity.ok().build();
    }
}
