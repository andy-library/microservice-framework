# JMeter Starter API Smoke Test

Author: Andy Yang

This folder contains the JMeter smoke plan for the demo application's stable
starter capability APIs under `/test/{starter}`.

## Start Demo

Embedded mode:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=full-embedded -Dspring-boot.run.arguments=--server.port=18080
```

Real middleware mode:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=full-middleware -Dspring-boot.run.arguments=--server.port=18080
```

## Run Smoke Test

```bash
mkdir -p target/jmeter
jmeter -n \
  -t performance/jmeter/starter-api-smoke.jmx \
  -l target/jmeter/starter-api-smoke.jtl \
  -j target/jmeter/starter-api-smoke.log \
  -Jprotocol=http \
  -Jhost=127.0.0.1 \
  -Jport=18080
```

Pass criteria:

- JMeter exits with code `0`.
- `target/jmeter/starter-api-smoke.jtl` contains no `false` success rows.
- The sampler response summary reports all starter API requests completed.

This plan is intentionally a low-concurrency functional smoke test. Use it to
verify all starter APIs are healthy before creating high-concurrency JMeter load
plans.

## Run Initial 100K Load Test

The first load plan runs 10 lightweight, low-side-effect starter APIs per loop.
With the default `100` threads and `100` loops, the main thread group sends
`100 * 100 * 10 = 100,000` requests.

```bash
mkdir -p target/jmeter
jmeter -n \
  -t performance/jmeter/starter-api-100k-load.jmx \
  -l target/jmeter/starter-api-100k-load.jtl \
  -j target/jmeter/starter-api-100k-load.log \
  -Jprotocol=http \
  -Jhost=127.0.0.1 \
  -Jport=18080 \
  -Jthreads=100 \
  -Jloops=100 \
  -Jramp=10 \
  -f
```

This is an early local baseline only. It deliberately avoids high-write,
message-producing, and destructive endpoints so the result reflects framework
request overhead more than downstream data growth or broker backlog.
