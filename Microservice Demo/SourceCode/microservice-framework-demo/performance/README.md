# Performance Testing with k6

Author: Andy Yang

This directory contains k6 performance test scripts for the microservice framework demo.
Each script targets a specific endpoint to measure throughput and latency at different
framework layers, from bare Spring Boot serialization to the full framework chain.

## Prerequisites

Install k6 following the official guide: https://k6.io/docs/get-started/installation/

Quick install on macOS:

```bash
brew install k6
```

Verify installation:

```bash
k6 version
```

## Starting the Demo Application

Before running any performance test, start the demo application.

### Embedded baseline (no middleware dependencies)

```bash
cd microservice-framework-demo
mvn spring-boot:run -Dspring-boot.run.profiles=full-embedded
```

All middleware uses in-memory providers — no Redis/Kafka/ES server needed.

### Full-middleware baseline (requires real middleware)

```bash
cd microservice-framework-demo
mvn spring-boot:run -Pfull-middleware -Dspring-boot.run.profiles=full-middleware
```

Requires real Redis (localhost:6379), Kafka (localhost:9092), ES (localhost:9200).

### Pre-test verification

Before running k6, verify all endpoints return 2xx:

```bash
curl -i http://localhost:8080/perf/plain
curl -i http://localhost:8080/perf/framework-web
curl -i http://localhost:8080/perf/cache-hit
curl -i -X POST http://localhost:8080/perf/full-chain-async
```

All four must return HTTP 200. If any returns 500, do not proceed with k6 testing.

## Environment Variables

All scripts support the following environment variables:

| Variable   | Default              | Description                         |
|------------|----------------------|-------------------------------------|
| `BASE_URL` | `http://localhost:8080` | Target application base URL      |
| `DURATION` | `30s`                | Test duration                        |
| `VUS`      | `100`                | Number of pre-allocated virtual users |
| `RPS`      | `5000`               | Target requests per second           |

## Scripts and Thresholds

Each script has specific P99 and error rate thresholds based on the endpoint complexity:

| Script | Endpoint | P99 threshold | Error rate threshold |
|--------|----------|---------------|---------------------|
| `plain.js` | `/perf/plain` | P99 < 30ms | rate < 0.01% (0.0001) |
| `framework-web.js` | `/perf/framework-web` | P99 < 50ms | rate < 0.01% (0.0001) |
| `cache-hit.js` | `/perf/cache-hit` | P99 < 60ms | rate < 0.01% (0.0001) |
| `full-chain-async.js` | `/perf/full-chain-async` | P99 < 120ms | rate < 0.05% (0.0005) |

### plain.js — Bare Spring Boot baseline

Measures raw Spring Boot throughput with no framework overhead. The `/perf/plain`
endpoint returns a plain Map (no ApiResponse wrapping, no requestId, no interceptors).

```bash
k6 run performance/k6/plain.js

# With custom parameters:
k6 run -e BASE_URL=http://localhost:8080 -e DURATION=60s -e VUS=200 -e RPS=10000 performance/k6/plain.js
```

### framework-web.js — Full framework web chain

Measures throughput through the complete framework web stack: requestId injection,
observability interceptors, logging, and ApiResponse JSON serialization.

```bash
k6 run performance/k6/framework-web.js

# With custom parameters:
k6 run -e BASE_URL=http://localhost:8080 -e DURATION=60s -e VUS=200 -e RPS=10000 performance/k6/framework-web.js
```

### cache-hit.js — Cache layer performance

Measures cache read performance. Uses embedded Redis (ConcurrentHashMap) in full-embedded
profile, or real Redis in full-middleware profile.

```bash
k6 run performance/k6/cache-hit.js

# With custom parameters:
k6 run -e BASE_URL=http://localhost:8080 -e DURATION=60s -e VUS=200 -e RPS=10000 performance/k6/cache-hit.js
```

### full-chain-async.js — Complete request pipeline

Measures the full request lifecycle: auth context, logging/trace metadata, cache lookup,
DB read, and async task submission. This is the heaviest endpoint and represents
the maximum framework overhead scenario.

```bash
k6 run performance/k6/full-chain-async.js

# With custom parameters:
k6 run -e BASE_URL=http://localhost:8080 -e DURATION=60s -e VUS=200 -e RPS=3000 performance/k6/full-chain-async.js
```

## Embedded vs Full-Middleware Baselines

### Embedded baseline (full-embedded profile)

- All middleware uses in-memory providers (ConcurrentHashMap, ConcurrentLinkedQueue)
- Sub-millisecond latency expected — measures pure framework overhead
- No network I/O for Redis/Kafka/ES operations
- Suitable for: framework overhead analysis, capacity model scaling calculations

### Full-middleware baseline (full-middleware profile)

- Real Redis/Kafka/ES with network I/O
- Expected latency: P99 5–50ms depending on middleware and network
- Suitable for: production capacity planning, real-world performance validation
- Requires: Redis at localhost:6379, Kafka at localhost:9092, ES at localhost:9200

**IMPORTANT:** Embedded baselines cannot prove production performance. Always run
full-middleware baselines before making production capacity decisions.

## Interpreting Results

Each script includes specific P99 and error rate thresholds (see table above).
A threshold crossing indicates the endpoint does not meet its performance target.

To compare framework overhead, run `plain.js` first as the baseline, then run
`framework-web.js` and observe the latency difference. The delta between the two
represents the cost of requestId injection, interceptors, and ApiResponse serialization.

Percentile metrics must satisfy: P999 >= P99 >= P95 >= P50. If your results violate
this ordering, verify that the k6 output was not truncated or incorrectly parsed.

For deeper analysis, output results to JSON:

```bash
k6 run --out json=plain-results.json performance/k6/plain.js
k6 run --out json=framework-web-results.json performance/k6/framework-web.js
k6 run --out json=cache-hit-results.json performance/k6/cache-hit.js
k6 run --out json=full-chain-async-results.json performance/k6/full-chain-async.js
```

Result files must be kept alongside scripts for traceability. Report metrics must be
extracted from these result files — never hand-entered without a traceable source.
