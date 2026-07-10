# Microservice Demo 性能压测报告

作者：Andy Yang  
日期：2026-07-02  
工具：Apache JMeter 5.6.3  
范围：Microservice Demo 本地 API 性能基线测试。

## 1. 测试环境

| 项目 | 值 |
| --- | --- |
| 操作系统 | 本地 macOS 开发环境 |
| Java | OpenJDK 21.0.11 |
| Maven | Apache Maven 3.9.15 |
| Spring Boot | 3.3.13 |
| JMeter | 5.6.3 |
| Demo Profile | `full-middleware` |
| Demo 端口 | `18080` |

## 2. 中间件状态

压测前后 Demo 应用健康检查结果：

- MySQL：`UP`
- Redis：`UP`，版本 `7.4.9`
- Elasticsearch：`UP`，本地单节点集群状态为 `yellow`
- Application：`UP`

Kafka 发送链路已通过框架 `KafkaPublisher` 抽象验证：

```json
{
  "code": 0,
  "data": {
    "topic": "microservice-framework-demo-topic",
    "published": true,
    "publisherUsed": true,
    "status": "sent"
  }
}
```

## 3. JMeter 脚本

脚本目录：

`Microservice Demo/Reports/jmeter`

包含脚本：

- `starter-api-smoke.jmx`
- `starter-api-100k-load.jmx`

## 4. 50 万请求压测

执行参数：

```bash
jmeter -n \
  -t performance/jmeter/starter-api-100k-load.jmx \
  -l target/jmeter/starter-api-500k-load.jtl \
  -j target/jmeter/starter-api-500k-load.log \
  -Jprotocol=http \
  -Jhost=127.0.0.1 \
  -Jport=18080 \
  -Jthreads=100 \
  -Jloops=500 \
  -Jramp=30 \
  -f
```

## 5. 压测结果

| 指标 | 值 |
| --- | ---: |
| 总采样数 | 500001 |
| 成功采样数 | 500001 |
| 失败采样数 | 0 |
| 错误率 | 0.0000% |
| 持续时间 | 32.195 s |
| 吞吐量 | 15530.39 req/s |
| 平均延迟 | 0.583 ms |
| 最小延迟 | 0 ms |
| p50 延迟 | 1 ms |
| p90 延迟 | 1 ms |
| p95 延迟 | 1 ms |
| p99 延迟 | 2 ms |
| 最大延迟 | 31 ms |

## 6. 各接口采样结果

| Label | 采样数 | 错误数 |
| --- | ---: | ---: |
| `common-id` | 50000 | 0 |
| `common-time` | 50000 | 0 |
| `common-page` | 50000 | 0 |
| `json-provider` | 50000 | 0 |
| `json-roundtrip` | 50000 | 0 |
| `web-success` | 50000 | 0 |
| `observability-tracing-current` | 50000 | 0 |
| `observability-metrics-counter` | 50000 | 0 |
| `redis-cache-get` | 50000 | 0 |
| `security-public` | 50000 | 0 |
| `setup redis cache key` | 1 | 0 |

## 7. 结论

状态：PASS

本次本地性能基线验证显示：Microservice Demo 在当前机器和本地中间件环境下完成 50 万次混合轻量 API 请求，错误率为 `0.0000%`，吞吐量约 `15530 req/s`，p99 延迟为 `2 ms`。

## 8. 使用说明

该结果是本地基线，不等同于分布式生产容量承诺。生产容量评估应在独立环境中重新执行，并结合真实业务请求体、真实读写比例、JVM/GC、CPU、内存、连接池和中间件监控大盘进行综合分析。
