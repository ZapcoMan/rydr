# Architecture Guide

[Back to README](../README.md) | [Modules](modules.md) | [Enterprise Gap Analysis](enterprise-gap-analysis.md) | [Operations](operations.md)

> 本文档描述 **当前代码库真实的技术栈与架构**。已从历史文档中校正：Zuul → Spring Cloud Gateway、Hystrix → OpenFeign circuitbreaker/fallback、Ribbon → Spring Cloud LoadBalancer、Druid → HikariCP、WebSocket → SSE、Sleuth/Zipkin 已移除。

## System Architecture

Rydr follows a **three-layer microservices architecture** powered by Spring Cloud 2025.0.3 (Boot 3.5.16, JDK 17):

```
                        ┌─────────────┐
                        │   Clients   │
                        │ (Mobile/Web)│
                        └──────┬──────┘
                               │
                    ┌──────────▼──────────┐
                    │ Spring Cloud Gateway│
                    │      (Port 9100)    │   ← rydr-zuul (migrated from Zuul)
                    │  - JWT Auth filter  │
                    │  - Rate limit (off) │
                    │  - Request routing  │
                    └──────────┬──────────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                     │
   ┌──────▼──────┐     ┌──────▼──────┐     ┌───────▼───────┐
   │api-passenger │     │  api-driver │     │api-listen-order│
   │   (9011)    │     │ (9002-9003) │     │    (8084)      │
   │  - OpenFeign│     │ - LoadBal.  │     │  - SSE stream  │
   │  - cb/fallback     │ - RestTmpl  │     │  (order/location)
   └──────┬──────┘     └──────┬──────┘     └───────┬───────┘
          │                    │                     │
          └────────────────────┼─────────────────────┘
                               │ (Service Discovery via Eureka)
          ┌──────────┬─────────┼──────────┬──────────┬──────────┐
          │          │         │          │          │          │
   ┌──────▼───┐ ┌───▼────┐ ┌─▼──────┐ ┌─▼──────┐ ┌─▼─────┐ ┌─▼──────┐
   │  Order   │ │  SMS   │ │Valuatn │ │Passenger│ │Verify │ │ Wallet │
   │8004-8005 │ │8002-03 │ │8060-61 │ │  8012   │ │ 8011  │ │  8007  │
   └──────┬───┘ └───┬────┘ └─┬──────┘ └┬────────┘ └┬──────┘ └───┬────┘
          │          │        │         │           │           │
          │     ┌────┴────────┴────┐    │           │           │
          │     │ service-order-  │    │           │           │
          │     │ dispatch (8006) │    │           │           │
          │     └─────────────────┘    │           │           │
     ┌────┴──────────┴────────┴─────────┴───────────┴───────────┘
     │
┌────▼─────────────────────────────────────────────────────┐
│              Data & Infrastructure                        │
│  ┌───────┐  ┌───────┐  ┌─────────┐  ┌────────┐  ┌──────┐ │
│  │ MySQL  │  │ Redis  │  │RabbitMQ │  │ActiveMQ│  │Eureka│ │
│  │(rydr/  │  │(single │  │(config  │  │(JMS    │  │ 7900 │ │
│  │ rydr-  │  │ /sentin)│  │ bus)    │  │ demo)  │  │      │ │
│  │ three) │  │        │  │         │  │        │  │      │ │
│  └───────┘  └───────┘  └─────────┘  └────────┘  └──────┘ │
└──────────────────────────────────────────────────────────┘
```

## Service Communication

### Synchronous Communication

**OpenFeign (Declarative HTTP Client)**
- Used by `api-passenger` / `api-driver` to call downstream services.
- Circuit breaker & fallback enabled via `spring.cloud.openfeign.circuitbreaker.enabled=true` (replaces Hystrix).
- `api-passenger` uses `FallbackFactory` (e.g. `SmsClientFallbackFactory`) for graceful degradation.
- Example: `api-passenger` → `service-valuation` for price forecast (`/forecast/single`).

**RestTemplate + Spring Cloud LoadBalancer**
- Used by `api-driver` with `@LoadBalanced` (replaces Ribbon).
- Eager loading for critical dependencies (`spring.cloud.loadbalancer.eager-load`).

### Asynchronous Communication

**ActiveMQ (JMS, jakarta.jms)**
- Queue/topic messaging for background tasks (replaces removed `@EnableBinding` Stream sample; functional Spring Cloud Stream still used in `config-client-diy`).
- Example modules: `service-jms-produce`, `service-jms-consumer` (queue name unified to `ActiveMQQueue`).

**RabbitMQ (Spring Cloud Bus)**
- Used for distributed configuration refresh in `config-client` / `rydr-config-server`.

### Real-time Communication

**Server-Sent Events (SSE)**
- `api-listen-order` pushes order dispatch and driver live location over `text/event-stream` (`ListenController`, `DriverLocationController`). The demo `rydr-demo-app` still ships a WebSocket server, but the production push path is SSE.

## Resilience Patterns

### OpenFeign Circuit Breaker (replaces Hystrix)
- `spring.cloud.openfeign.circuitbreaker.enabled=true` on API modules.
- Fallback via `FallbackFactory` returning a normal `ResponseResult` failure instead of throwing.

### Service Discovery (Eureka)
- All services register with Eureka (`:7900`) on startup.
- Heartbeat interval: 1s; expiration: 1s.
- `hystrix-dashboard` module retained in source but excluded from the build.

### Load Balancing (Spring Cloud LoadBalancer, replaces Ribbon)
- Client-side load balancing, eager-loaded for critical callers.

## Configuration Management

### Spring Cloud Config
- Centralized configuration (`rydr-config-server` at `:6001`), **native (local filesystem) backend** loading `{app}-dev.yml` from `classpath:/config-repo/`.
- **All 18 business/infrastructure modules are connected** via Eureka service discovery (`spring.cloud.config.discovery.service-id=config-server`), with a local `application.yml` fallback (`fail-fast=false`).
- The optional git backend (`CONFIG_GIT_URI`) is commented out and can be enabled for a reachable private repository.

### Dynamic Refresh
- RabbitMQ bus for config change propagation (`config-client`).

## Security Architecture

### API Gateway Security (Spring Cloud Gateway)
- `AuthFilter`: global JWT validation (whitelist: `/auth`, `/verification-code`, `/forecast`, `/api-driver/auth`, `/api-listen-order`, actuator). Invalid token → 401.
- `RequestCheckFilter`: optional signature verification (`ZUUL_SECRET`).
- `RateFilter`: rate limiting, off by default (`rydr.gateway.rate-limit.enabled`).
- `PreFilter`: request logging.

### Authentication
- JWT (JJWT 0.13.0), secret via `JWT_SECRET` env var.
- Issued at `api-passenger` / `api-driver` auth after verification-code + login.

### Credential Management
- Most credentials externalized via `${ENV:default}`; **default values are placeholders (`changeme` / `admin`)** and must be overridden in production (see gap analysis P0).

## Database Architecture

### Dual Database Setup
- `rydr`: order and valuation data.
- `rydr-three`: passenger and SMS data (108 tables total across both).

### Key Design Decisions
- MyBatis (`mybatis-spring-boot` 3.0.5) for flexible SQL mapping.
- **HikariCP** connection pool (Boot 3 default; Druid removed).
- Redis (single / sentinel) for caching, distributed locking (Redisson 3.46.0), online-driver set & location.
- Idempotency backed by UNIQUE constraints: `tbl_wallet_transaction.biz_no`, `tbl_wallet_recharge.out_trade_no`, `passenger_user_info.passenger_phone`.

## Monitoring & Observability

| Tool | Port | Purpose | Status |
|------|------|---------|--------|
| Spring Boot Admin | 6010 | Service health, logs | ✅ |
| Actuator | per-service | `health,info,prometheus` (exposure converged) | ✅ |
| Micrometer + Prometheus | 9090 | Metrics (`/actuator/prometheus`) | ✅ |
| Micrometer Tracing + Zipkin | 9411 | Distributed tracing (Brave bridge, traceId/spanId) | ✅ |
| Grafana | 3000 | Metrics dashboards (`rydr-jvm-overview`) | ✅ |
| Structured logging | — | logstash-logback-encoder + traceId/spanId MDC (JSON) | ✅ |
| Hystrix Dashboard | — | (removed in Boot 3 upgrade) | ❌ removed |
