# Microservice Framework Observability Starter

Author: Andy Yang

为微服务应用提供**开箱即用的全栈可观测性**解决方案，包括分布式追踪、指标监控、日志关联和网关集成。

## 组件定位

`microservice-framework-observability-starter` 是 Microservice Framework 的**可观测性核心组件**，基于 Micrometer Tracing + OpenTelemetry 实现：
- **Traces（追踪）**：自动创建和传播分布式调用链
- **Metrics（指标）**：暴露 Prometheus 格式的运行时指标
- **Logs（日志关联）**：自动将 TraceId/SpanId 注入 MDC

## 核心能力

### 1. 分布式追踪 (Distributed Tracing)
- 自动为 HTTP 请求创建 Trace 上下文
- 支持 `@Traceable` 注解标记业务方法
- 支持 `@Scheduled` 定时任务自动追踪
- 兼容 W3C Trace Context 标准

### 2. Request ID 三级降级
```
优先级1: TraceId (Micrometer 自动创建)
    ↓
优先级2: 网关 Header (如 X-Request-ID)
    ↓
优先级3: UUID 兜底生成
```

### 3. Baggage 传播
支持在请求中携带自定义键值对，自动传播到下游服务：
- `user-id`
- `tenant-id`
- 自定义 correlation fields

### 4. MDC 自动注入
每条日志自动包含：
| 字段 | 说明 |
|------|------|
| `traceId` | 完整调用链 ID |
| `spanId` | 当前操作 ID |
| `requestId` | 请求 ID（等于 TraceId） |

## 使用场景

### 场景 1：HTTP API 服务
```java
@RestController
public class UserController {
    
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        // 日志自动包含 traceId, spanId
        log.info("查询用户: {}", id);
        return userService.findById(id);
    }
}
```

### 场景 2：定时任务追踪
```java
@Component
public class ReportJob {
    
    @Scheduled(cron = "0 0 2 * * ?")
    @Traceable("job:daily-report")  // 自动创建 Trace
    public void generateDailyReport() {
        log.info("开始生成日报");
        // 业务逻辑
    }
}
```

### 场景 3：网关透传 Request ID
```bash
# Kong/MSE 网关透传
curl -H "X-Request-ID: gateway-123" http://service/api
```

## 快速开始

### 1. 添加依赖
```xml
<dependency>
    <groupId>com.microservice.framework</groupId>
    <artifactId>microservice-framework-observability-starter</artifactId>
</dependency>
```

### 2. 配置 application.yml

#### 完整配置示例（含所有参数说明）
```yaml
# ============================================================================
# Microservice Framework Observability 配置
# ============================================================================
framework:
  observability:
    # ------------------------------------------------------------------------
    # 追踪配置 (Tracing)
    # 控制分布式追踪的行为
    # ------------------------------------------------------------------------
    tracing:
      # 是否启用追踪功能
      # 类型: boolean | 默认值: true
      # 设为 false 将完全禁用追踪功能
      enabled: true
      
      # 需要自动传播的 Baggage 键列表
      # 类型: List<String> | 默认值: []
      # 这些键值对会在服务间自动传播，可用于传递用户ID、租户ID等上下文信息
      baggage-keys:
        - user-id       # 用户标识
        - tenant-id     # 租户标识
        - request-source # 请求来源
    
    # ------------------------------------------------------------------------
    # 指标配置 (Metrics)
    # 控制 Prometheus 指标的采集
    # ------------------------------------------------------------------------
    metrics:
      # 是否启用指标采集
      # 类型: boolean | 默认值: true
      # 启用后可通过 /actuator/prometheus 获取指标
      enabled: true
    
    # ------------------------------------------------------------------------
    # 网关集成配置 (Gateway Integration)
    # 处理来自 API 网关的请求 ID 透传
    # ------------------------------------------------------------------------
    gateway-integration:
      # 是否启用网关集成
      # 类型: boolean | 默认值: false
      # 启用后会激活 GatewayIntegrationFilter，处理 Request ID 的三级降级逻辑
      enabled: true
      
      # 请求 ID 的 HTTP 头名称
      # 类型: String | 默认值: "X-Request-ID"
      # 常见配置：
      #   - Kong 网关: X-Kong-Request-Id
      #   - 阿里云 MSE: X-Request-Id
      #   - AWS API Gateway: X-Amzn-Trace-Id
      #   - 通用: X-Request-ID
      request-id-header: X-Request-ID

# ============================================================================
# Spring Boot Management (Actuator) 配置
# ============================================================================
management:
  endpoints:
    web:
      exposure:
        # 暴露的端点列表
        # 可选值: health, info, prometheus, metrics, loggers, env, beans 等
        include: health,info,prometheus,metrics,loggers
      # Actuator 端点的基础路径
      base-path: /actuator
  
  endpoint:
    health:
      # 是否显示健康检查详情
      # 可选值: never, when_authorized, always
      show-details: always
  
  metrics:
    tags:
      # 全局指标标签，会添加到所有指标中
      application: ${spring.application.name}
    export:
      prometheus:
        # 是否启用 Prometheus 指标导出
        enabled: true

  # OTLP (OpenTelemetry Protocol) 导出配置
  # 用于将 Trace 数据发送到 Jaeger/Zipkin/OTEL Collector
  otlp:
    tracing:
      # OTLP 接收端点
      # 常见配置：
      #   - Jaeger OTLP: http://jaeger:4318/v1/traces
      #   - OTEL Collector: http://otel-collector:4318/v1/traces
      endpoint: http://localhost:4318/v1/traces
    metrics:
      # OTLP 指标端点
      endpoint: http://localhost:4318/v1/metrics
```

### 3. 使用注解
```java
// 标记需要追踪的方法
@Traceable("business:order-create")
public Order createOrder(OrderRequest request) {
    // ...
}

// 在 Span 中添加自定义 Tag
public User getUser(@SpanTag("user.id") Long id) {
    // ...
}
```

## 环境配置最佳实践

### 开发环境
```yaml
framework:
  observability:
    tracing:
      enabled: true
      # 开发环境 100% 采样，便于调试
    gateway-integration:
      enabled: true
      request-id-header: X-Request-ID

# 开发环境不需要导出到外部系统
# management.otlp.tracing.endpoint: (不配置)
```

### 测试环境
```yaml
framework:
  observability:
    tracing:
      enabled: true
      baggage-keys:
        - user-id
        - tenant-id
    gateway-integration:
      enabled: true
      request-id-header: X-Request-ID

management:
  otlp:
    tracing:
      # 测试环境可连接共享的 Jaeger 实例
      endpoint: http://jaeger-test.internal:4318/v1/traces
```

### 生产环境
```yaml
framework:
  observability:
    tracing:
      enabled: true
      baggage-keys:
        - user-id
        - tenant-id
        - request-source
    gateway-integration:
      enabled: true
      # 根据实际使用的网关配置
      request-id-header: X-Kong-Request-Id

management:
  otlp:
    tracing:
      # 生产环境连接 OTEL Collector 集群
      endpoint: http://otel-collector.monitoring:4318/v1/traces
    metrics:
      endpoint: http://otel-collector.monitoring:4318/v1/metrics
```

---

## Prometheus + Grafana 集成指南

本组件原生支持 **OpenTelemetry + Prometheus + Grafana** 私有化部署方案。

### 架构总览

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Your Microservices                            │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐       │
│  │ Service A   │ │ Service B   │ │ Service C   │ │ Service D   │       │
│  │ :8080       │ │ :8081       │ │ :8082       │ │ :8083       │       │
│  └──────┬──────┘ └──────┬──────┘ └──────┬──────┘ └──────┬──────┘       │
│         │               │               │               │               │
│         │ /actuator/prometheus (Pull)   │               │               │
│         └───────────────┼───────────────┼───────────────┘               │
│                         │               │                               │
│                         │  OTLP (Push)  │                               │
│                         │               │                               │
└─────────────────────────┼───────────────┼───────────────────────────────┘
                          │               │
                          ▼               ▼
              ┌───────────────────┐ ┌───────────────────┐
              │    Prometheus     │ │  OTEL Collector   │
              │    (Metrics)      │ │  (Traces)         │
              │    :9090          │ │  :4318            │
              └─────────┬─────────┘ └─────────┬─────────┘
                        │                     │
                        │                     ▼
                        │           ┌───────────────────┐
                        │           │ Jaeger / Tempo    │
                        │           │ (Trace Storage)   │
                        │           │ :16686            │
                        │           └─────────┬─────────┘
                        │                     │
                        ▼                     ▼
              ┌─────────────────────────────────────────┐
              │              Grafana                    │
              │  - Prometheus 数据源 (Metrics)          │
              │  - Jaeger/Tempo 数据源 (Traces)         │
              │              :3000                      │
              └─────────────────────────────────────────┘
```

### 1. Prometheus 配置

#### 应用端配置 (application.yml)
```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,info,metrics
  metrics:
    tags:
      # 推荐的全局标签，便于 Grafana 筛选
      application: ${spring.application.name}
      env: ${spring.profiles.active:default}
      instance: ${HOSTNAME:localhost}
    export:
      prometheus:
        enabled: true
        # 直方图桶配置（用于 P99 计算）
        histogram-flavor: prometheus
        step: 1m
```

#### Prometheus Server 配置 (prometheus.yml)
```yaml
# prometheus.yml - Prometheus 服务端配置
global:
  scrape_interval: 15s          # 抓取间隔
  evaluation_interval: 15s      # 规则评估间隔

scrape_configs:
  # 微服务 - 静态配置方式
  - job_name: 'microservice-services'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
          - 'service-a:8080'
          - 'service-b:8081'
          - 'service-c:8082'
        labels:
          group: 'microservice-framework'
    
  # Kubernetes 服务发现方式（推荐生产使用）
  - job_name: 'kubernetes-pods'
    kubernetes_sd_configs:
      - role: pod
    relabel_configs:
      # 只抓取有 prometheus.io/scrape: "true" 注解的 Pod
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      # 使用注解中的 path
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
      # 使用注解中的 port
      - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
        action: replace
        regex: ([^:]+)(?::\d+)?;(\d+)
        replacement: $1:$2
        target_label: __address__
```

#### Kubernetes Pod 注解
```yaml
# 在 K8s Deployment 中添加以下注解
metadata:
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/path: "/actuator/prometheus"
    prometheus.io/port: "8080"
```

### 2. Grafana 配置

#### 添加数据源

**Prometheus 数据源：**
```
名称: Prometheus
类型: Prometheus
URL: http://prometheus:9090
访问: Server (default)
```

**Jaeger/Tempo 数据源（可选，用于 Trace 查看）：**
```
名称: Jaeger
类型: Jaeger
URL: http://jaeger:16686
```

#### 推荐监控指标

本组件自动暴露以下关键指标：

| 指标名称 | 类型 | 说明 | PromQL 示例 |
|---------|------|------|-------------|
| `http_server_requests_seconds` | Histogram | HTTP 请求延迟 | `histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))` |
| `http_server_requests_seconds_count` | Counter | HTTP 请求总数 | `rate(http_server_requests_seconds_count[5m])` |
| `jvm_memory_used_bytes` | Gauge | JVM 内存使用 | `jvm_memory_used_bytes{area="heap"}` |
| `jvm_gc_pause_seconds` | Summary | GC 暂停时间 | `rate(jvm_gc_pause_seconds_sum[5m])` |
| `system_cpu_usage` | Gauge | 系统 CPU 使用率 | `system_cpu_usage` |
| `process_cpu_usage` | Gauge | 进程 CPU 使用率 | `process_cpu_usage` |
| `hikaricp_connections_active` | Gauge | 活跃数据库连接 | `hikaricp_connections_active` |
| `logback_events_total` | Counter | 日志事件数量 | `rate(logback_events_total{level="error"}[5m])` |

#### 常用 Grafana Panel 配置

**1. 服务 QPS (每秒请求数)**
```promql
sum(rate(http_server_requests_seconds_count{application="$application"}[1m])) by (uri)
```

**2. 服务 P99 延迟**
```promql
histogram_quantile(0.99, 
  sum(rate(http_server_requests_seconds_bucket{application="$application"}[5m])) by (le, uri)
)
```

**3. 错误率**
```promql
sum(rate(http_server_requests_seconds_count{application="$application", status=~"5.."}[5m])) 
/ 
sum(rate(http_server_requests_seconds_count{application="$application"}[5m])) * 100
```

**4. JVM 堆内存使用率**
```promql
jvm_memory_used_bytes{application="$application", area="heap"} 
/ 
jvm_memory_max_bytes{application="$application", area="heap"} * 100
```

### 3. OTEL Collector 配置（可选，用于 Traces）

如果需要查看分布式调用链，需要部署 OTEL Collector：

```yaml
# otel-collector-config.yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

processors:
  batch:
    timeout: 10s

exporters:
  jaeger:
    endpoint: jaeger:14250
    tls:
      insecure: true
  
  prometheus:
    endpoint: "0.0.0.0:8889"

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [jaeger]
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [prometheus]
```

### 4. Docker Compose 快速启动（开发/测试）

```yaml
# docker-compose-observability.yml
version: '3.8'
services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--web.enable-lifecycle'

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-storage:/var/lib/grafana
    depends_on:
      - prometheus

  jaeger:
    image: jaegertracing/all-in-one:latest
    ports:
      - "16686:16686"  # UI
      - "4317:4317"    # OTLP gRPC
      - "4318:4318"    # OTLP HTTP

volumes:
  grafana-storage:
```

启动命令：
```bash
docker-compose -f docker-compose-observability.yml up -d
```

### 5. 验证集成

```bash
# 1. 启动应用后，验证 Prometheus 端点
curl http://localhost:8080/actuator/prometheus | head -20

# 2. 查看 Prometheus 是否成功抓取
# 访问 http://prometheus:9090/targets

# 3. 在 Grafana 中创建 Dashboard
# 访问 http://grafana:3000 (admin/admin)
```

### 工具类
```java
// 获取当前 TraceId
String traceId = ObservabilityUtils.getTraceId();

// 获取当前 SpanId
String spanId = ObservabilityUtils.getSpanId();
```

### 注解
| 注解 | 作用 |
|------|------|
| `@Traceable("name")` | 自动创建 Span |
| `@SpanTag("key")` | 添加 Span 属性 |

## Actuator 端点

| 端点 | 说明 |
|------|------|
| `/actuator/health` | 健康检查 |
| `/actuator/prometheus` | Prometheus 指标 |
| `/actuator/metrics` | 指标列表 |

## 常见问题

### Q: 日志中 TraceId 为空？
确保请求经过 Spring MVC 处理，Trace 会自动创建。

### Q: 定时任务没有 TraceId？
使用 `@Traceable` 注解标记定时任务方法。

### Q: 如何与 Jaeger/Zipkin 集成？
配置 OTLP Exporter 指向 Jaeger/Zipkin 的收集端点。

## 相关文档

- [Logging Starter](../microservice-framework-logging-starter/README.md)
- [Framework Demo](../../Microservice%20Demo/SourceCode/microservice-framework-demo/README.md)
