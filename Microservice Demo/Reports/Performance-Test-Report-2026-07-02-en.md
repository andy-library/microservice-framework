# Microservice Demo Performance Test Report

Author: Andy Yang  
Date: 2026-07-02  
Tool: Apache JMeter 5.6.3  
Scope: Local API performance baseline for Microservice Demo.

## 1. Environment

| Item | Value |
| --- | --- |
| OS | Local macOS development environment |
| Java | OpenJDK 21.0.11 |
| Maven | Apache Maven 3.9.15 |
| Spring Boot | 3.3.13 |
| JMeter | 5.6.3 |
| Demo Profile | `full-middleware` |
| Demo Port | `18080` |

## 2. Middleware Status

Application health checks before and after the load test:

- MySQL: `UP`
- Redis: `UP`, version `7.4.9`
- Elasticsearch: `UP`, local single-node cluster status `yellow`
- Application: `UP`

Kafka send was verified through the framework `KafkaPublisher` abstraction:

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

## 3. JMeter Scripts

Script directory:

`Microservice Demo/Reports/jmeter`

Included scripts:

- `starter-api-smoke.jmx`
- `starter-api-100k-load.jmx`

## 4. 500k Load Test

Execution parameters:

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

## 5. Result

| Metric | Value |
| --- | ---: |
| Total samples | 500001 |
| Successful samples | 500001 |
| Failed samples | 0 |
| Error rate | 0.0000% |
| Duration | 32.195 s |
| Throughput | 15530.39 req/s |
| Average latency | 0.583 ms |
| Min latency | 0 ms |
| p50 latency | 1 ms |
| p90 latency | 1 ms |
| p95 latency | 1 ms |
| p99 latency | 2 ms |
| Max latency | 31 ms |

## 6. Per-Endpoint Samples

| Label | Samples | Errors |
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

## 7. Conclusion

Status: PASS

This local baseline shows that Microservice Demo completed 500k mixed lightweight API requests with `0.0000%` error rate, around `15530 req/s` throughput, and `2 ms` p99 latency on the tested local environment.

## 8. Notes

This result is a local baseline, not a distributed production capacity guarantee. Production sizing should be repeated in an isolated environment with realistic business payloads, real read/write ratios, and full JVM, GC, CPU, memory, connection pool, and middleware observability dashboards.
