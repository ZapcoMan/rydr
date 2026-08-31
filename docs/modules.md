# Module Guide

[Back to README](../README.md) | [Architecture](architecture.md) | [Enterprise Gap Analysis](enterprise-gap-analysis.md)

This document provides detailed descriptions of each module in the Rydr platform.
Technical stack below reflects the **current** code (Spring Cloud 2025.0.3 / Boot 3.5.16 / JDK 17):
Hystrix → OpenFeign circuitbreaker/fallback, Ribbon → Spring Cloud LoadBalancer, Druid → HikariCP,
Netflix Zuul → Spring Cloud Gateway, WebSocket → SSE.

---

## API Layer

### api-passenger
**Port:** 9011 | **Path:** `rydr/api-passenger`

Passenger-facing REST API providing endpoints for:
- SMS verification + login (`/sms/verify-code/send`, `/auth/login`)
- Ride price forecasting (`/order/forecast` → `service-valuation`)
- Order create / pay / cancel (`/order/*`)
- JWT issuance after verification-code + login

**Key Technologies:** OpenFeign (service calls, `FallbackFactory` for `service-sms`), LoadBalancer (eager-load SERVICE-SMS), Gateway JWT upstream.

### api-driver
**Port:** 9002, 9003 | **Path:** `rydr/api-driver`

Driver-facing REST API providing endpoints for:
- SMS verification and login
- Order grabbing (`/grab/do`) via RestTemplate → `service-order`
- Driver online / offline / location (`/driver/online`, `/driver/location`) backing the real dispatch strategy
- Trip start / end (`/trip/*`)

**Key Technologies:** Spring Cloud LoadBalancer (replaces Ribbon), RestTemplate, OpenFeign (`service-valuation` / `service-order`).

### api-listen-order
**Port:** 8084 | **Path:** `rydr/api-listen-order`

Real-time order event service for drivers:
- **SSE**-based order dispatch push (`/listen/driver/{id}`, `text/event-stream`)
- SSE driver live-location stream (`/location/driver/{id}`)
- Demo dispatch endpoint (`/order/send`)

---

## Service Layer

### service-order
**Port:** 8004, 8005 | **Path:** `rydr/service-order`

Core order management service featuring:
- Order lifecycle state machine (`OrderServiceImpl`: create → grab(0→2) → startTrip(2→5) → endTrip(5→6) → pay(6→7→8) → cancel) with strict `OrderStatusEnum` validation.
- **Distributed locking** implementations (teaching comparison, most `@Deprecated`):
  - JVM-level locks
  - MySQL-based locks
  - Redis-based locks
  - Redisson single-instance lock (**production**)
  - RedLock (multi-Redis) implementation
- Feign client to `service-wallet` for payment settlement.

**Key Technologies:** Redis, Redisson 3.46.0, MyBatis, HikariCP, MySQL (`rydr`).

### service-order-dispatch
**Port:** 8006 | **Path:** `rydr/service-order-dispatch`

Driver-order matching and dispatch:
- **Real driver-selection strategy**: online set + Haversine distance + rating ranking (`DispatchServiceImpl.selectDrivers`).
- Auto dispatch (`/dispatch/auto`) and manual dispatch (`/dispatch/call`).
- Pending offers use `setIfAbsent` + TTL (`dispatch.order-ttl-minutes`, default 30) to avoid permanent Redis keys.

### service-passenger-user
**Port:** 8012 | **Path:** `rydr/service-passenger-user`

Passenger / driver account management:
- Login / register with concurrent-safe insert + `DuplicateKeyException` read-back (`passenger_user_info.passenger_phone` UNIQUE).
- Address CRUD, phone-based lookup.
- Token issuance delegate (JWT created via `TokenServiceImpl` → `JwtUtil`).

**Key Technologies:** MyBatis, HikariCP, MySQL (`rydr-three`).

### service-sms
**Port:** 8002, 8003 | **Path:** `rydr/service-sms`

SMS notification service:
- Template-based SMS delivery with `${placeholder}` rendering.
- Pluggable transport: `ConsoleSmsSender` (default, logs to console) / `AliyunSmsSender` (optional, credentials externalized).
- In-memory template cache with TTL (`ConcurrentHashMap.compute` to prevent duplicate DB queries).

**Key Technologies:** MyBatis, HikariCP, MySQL (`rydr-three`).

### service-valuation
**Port:** 8060, 8061 | **Path:** `rydr/service-valuation`

Ride pricing engine:
- Full rule engine (`ValuationServiceImpl`): segment / night / beyond-distance / tag / dynamic discount pricing.
- `ForecastController` exposes `/forecast/single` (simplified Haversine estimate, base + per-km) used by the API layer; internal calls are whitelisted at the gateway.
- Redis caches for price & rule.

**Key Technologies:** Spring Security (Basic auth for admin), Redis caching, complex rule engine, HikariCP, MySQL (`rydr`).

### service-verification-code
**Port:** 8011 | **Path:** `rydr/service-verification-code`

Login verification code service:
- 6-digit code generation, Redis storage (120s TTL).
- Three-tier rate limiting via Redis counters (1min / 10min / 24h windows).

### service-wallet
**Port:** 8007 | **Path:** `rydr/service-wallet`

User wallet management (real implementation):
- Balance query, recharge order creation, recharge callback (idempotent via `out_trade_no`), order payment (concurrency-safe conditional UPDATE, idempotent via `biz_no`), transaction ledger (`tbl_wallet_transaction`).
- All balance changes `@Transactional` + idempotent + over-deduction-proof.

**Key Technologies:** MyBatis, HikariCP, MySQL (`rydr`).

---

## Infrastructure

### eureka
**Port:** 7900 | **Path:** `rydr/eureka`

Netflix Eureka service registry:
- HTTP Basic auth protection (`admin/changeme` by default — override in production).
- Self-preservation disabled for dev; 5s eviction interval.
- CSRF protection scoped to `/eureka/**`.

### rydr-config-server
**Port:** 6001 | **Path:** `rydr/rydr-config-server`

Spring Cloud Config Server:
- Git-based configuration repository.
- RabbitMQ bus for dynamic refresh.
- Only demo clients (`config-client`) consume it today (gap: business modules not yet connected).

### rydr-zuul
**Port:** 9100 | **Path:** `rydr/rydr-zuul`

**Spring Cloud Gateway** (migrated from Netflix Zuul) with custom `GlobalFilter`s:
- `AuthFilter`: JWT token validation (401 on failure, whitelist support).
- `PreFilter`: request logging.
- `RateFilter`: rate limiting (off by default).
- `RequestCheckFilter`: optional signature validation.

Routes use `lb://` (Spring Cloud LoadBalancer) to downstream services.

### cloud-admin
**Port:** 6010 | **Path:** `rydr/cloud-admin`

Spring Boot Admin monitoring dashboard:
- Service health monitoring.
- Email notification alerts (SMTP configurable).

> Note: `hystrix-dashboard` source is retained but **excluded from the build** (Hystrix removed in the Boot 3 upgrade).

---

## Shared Library

### rydr-common
**Path:** `rydr/rydr-common`

Shared artifacts used across all services:
- **DTOs**: `ResponseResult`, `ShortMsgRequest`, `PriceResult`, `ForecastDetail`, `Order`, entity POJOs (54+).
- **Constants**: `OrderStatusEnum`, `CommonStatusEnum`, `BusinessInterfaceStatus`, `RedisKeyConstant`.
- **Utilities**: `JwtUtil` (JJWT), `RSAEncrypt` (externalized key), `Md5Util`, `Sha1Util`, `PhoneUtil`, `BigDecimalUtil`, `JsonUtil`.
- **Validation**: `PhoneNumberValidator` + `@PhoneNumberValidation`.
- **Aspects**: `WebLogAspect` (`web-log.enabled`, matchIfMissing), `SupervisionAspect` (`government-upload.enabled` — disabled by default).
- **Exception**: `GlobalExceptionHandler` (single `@RestControllerAdvice`, code `BusinessInterfaceStatus`).

> Design note: `rydr-common` mixes entities, DTOs, utils, AOP and JMS config. In a stricter enterprise layout, AOP / middleware would be split into dedicated starters (see gap analysis).

---

## Example / Demo Modules

### config-client / config-client-diy
Config Server client implementations demonstrating:
- Spring Cloud Config consumption (discovery-based).
- Dynamic property refresh via RabbitMQ bus (`config-client`).
- Functional Spring Cloud Stream bindings (`config-client-diy`, `StreamBridge`).

### service-jms-produce / service-jms-consumer
ActiveMQ (jakarta.jms) integration examples:
- Queue and topic message patterns.
- Producer-consumer architecture; queue name unified to `ActiveMQQueue` on both sides.
- Consumer uses client ACK mode with `session.recover()` on failure.

### rydr-demo-app
**Port:** 8083

Full demo application:
- WebSocket server demo (note: production real-time push uses SSE in `api-listen-order`, not WebSocket).
- Taxi login / order controllers (demo only).
