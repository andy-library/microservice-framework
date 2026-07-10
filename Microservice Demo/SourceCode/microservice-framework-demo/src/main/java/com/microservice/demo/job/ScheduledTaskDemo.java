package com.microservice.demo.job;

import com.microservice.framework.logging.util.Log;
import com.microservice.framework.observability.annotation.Traceable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 定时任务演示
 * 演示非 HTTP 入口的自动追踪能力
 */
@Component
public class ScheduledTaskDemo {

    /**
     * 每 60 秒执行一次模拟报表任务
     */
    @Scheduled(fixedRateString = "${app.demo.job.rate:60000}")
    @Traceable(name = "job:daily-report")
    public void executeDailyReport() {
        String taskId = UUID.randomUUID().toString().substring(0, 8);

        Log.info("开始执行日报生成任务")
                .with("taskId", taskId)
                .log();

        try {
            // 模拟业务处理
            Thread.sleep(1000);

            Log.info("日报数据处理中...")
                    .with("taskId", taskId)
                    .with("progress", "50%")
                    .log();

            Thread.sleep(1000);

            Log.info("日报任务执行完成")
                    .with("taskId", taskId)
                    .log();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.error("任务由于中断而失败")
                    .withException(e)
                    .log();
        }
    }
}
