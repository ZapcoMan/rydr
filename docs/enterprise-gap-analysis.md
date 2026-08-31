# 企业级差距分析（Enterprise Gap Analysis）

> 本文档基于对 `rydr` 全部 21 个模块、每一个 Java 文件（含 `test`、`rydr-demo-app`、`service-jms-*` 示例）、全部 `application.yml` / `bootstrap.yml`、全部 MyBatis Mapper XML 与 `rydr-sql.sql` 的**单线程串行全量扫描**得出，并与真实企业级网约车系统的标准逐项比对。
>
> 扫描方式：主代理逐一 `read_file`，不使用任何子代理 / 多线程并行（依据用户要求与历史经验，并行扫描会导致工具卡死）。
>
> 关联文档：[架构指南](architecture.md) · [模块指南](modules.md) · [说明文件](../README.md)

---

## 一、已实现并跑通的业务闭环

> 以下内容原样保留自说明文件，作为本项目的业务基线记录。它描述的是当前代码库**已经真实跑通**的链路，而非规划目标。

- **登录/验证码链路**：`api-passenger`/`api-driver` → `service-verification-code`（验证码生成/校验 + 三档 Redis 限流）→ `service-sms`（短信下发，可插拔 `console`/`aliyun`）→ `service-passenger-user`（乘客/司机登录建号，并发安全，唯一索引去重）→ JWT 签发。
- **计价链路**：`api-passenger`/`api-driver` → `service-valuation`（`/forecast/single` 基于 Haversine 的真实预估计价；内部服务间调用已放行鉴权，不再 401）。
- **下单 → 派单 → 接单 → 行程 → 支付 全状态机**：`api-passenger` 创建订单（status 0）→ `service-order-dispatch` 按**真实司机选择策略**（在线 + 距离 + 评分）自动派单 → `api-listen-order` 通过 **SSE** 推送派单与司机实时位置 → 司机端接单（0→2）、行程起止（2→5→6）→ `service-wallet` 钱包扣款支付（6→7→8，并发安全防超扣、幂等防重复入账）→ 完成。
- **钱包/支付域**：`service-wallet` 提供余额查询、充值下单、充值回调入账、订单支付扣款、流水记录（`tbl_wallet_transaction`），全部 `@Transactional` + 幂等（`biz_no` / `out_trade_no` 唯一约束）+ 并发安全（条件 UPDATE 防超扣）。
- **统一安全与可运维**：`rydr-zuul` 网关全局 JWT 校验（白名单除外）+ 限流预留；actuator 收敛为 `health,info`；统一 `GlobalExceptionHandler`（参数校验 400 / 全局 500，码值统一 `BusinessInterfaceStatus`）；密钥全部 `${ENV:default}` 外部化，JWT 密钥支持 `JWT_SECRET` 注入。
- **异步消息**：`service-jms-produce` → `service-jms-consumer`（ActiveMQ 队列/主题，已统一队列名）。

---

## 二、全量扫描事实清单（作为差距分析的证据）

### 2.1 已实现且经扫描确认的真实能力

| 能力 | 证据（文件 / 行） | 说明 |
|------|------------------|------|
| 网关 JWT 校验 | `rydr-zuul/.../filter/AuthFilter.java`（`GlobalFilter`，401 拒绝 + 白名单） | 已从 Zuul 迁移到 Spring Cloud Gateway，真实生效 |
| 限流预留 | `RateFilter.java`（`rydr.gateway.rate-limit.enabled` 默认 false） | 未默认开启 |
| 订单状态机 | `service-order/.../impl/OrderServiceImpl.java`（`grab`/`startTrip`/`endTrip`/`pay`/`cancel`，严格 `OrderStatusEnum` 校验） | 0→2→5→6→7→8 完整 |
| 真实司机选择 | `service-order-dispatch/.../impl/DispatchServiceImpl.java`（`haversine` + 评分排序 + `selectDrivers`） | 在线 + 距离 + 评分 |
| SSE 实时推送 | `api-listen-order/.../ListenController.java`、`DriverLocationController.java` | 真 SSE，非 WebSocket |
| 钱包并发安全 | `service-wallet/.../WalletAccountMapper.deductCapital`（条件 `UPDATE ... WHERE capital >= ?`） | 防超扣 |
| 钱包幂等 | `tbl_wallet_transaction.idx_biz_no` UNIQUE、`tbl_wallet_recharge.idx_out_trade_no` UNIQUE | 防重复入账 |
| 登录并发安全 | `service-passenger-user/.../ServiceImpl.login`（insert + `DuplicateKeyException` 回读，`passenger_user_info.UK_PHONE` UNIQUE） | 去重建号 |
| 验证码限流 | `service-verification-code/.../VerifyCodeServiceImpl.checkSendCodeTimeLimit`（1min/10min/24h 三档 Redis 计数） | 三级防护 |
| 短信可插拔 | `service-sms/.../SmsSender` + `ConsoleSmsSender`（默认）/ `AliyunSmsSender` | 外部化选择 |
| 统一异常 | `rydr-common/.../GlobalExceptionHandler.java`（`@RestControllerAdvice`，码值 `BusinessInterfaceStatus`） | 单 `@RestControllerAdvice` |
| Feign 优雅降级 | `api-passenger/.../SmsClientFallbackFactory.java`（`FallbackFactory`） | 返回失败响应而非抛异常 |
| 配置外部化 | 全部 yml 使用 `${ENV:default}` | 默认值多为 `changeme`/`admin` |
| 双库分片 | `service-passenger-user`、`service-sms` → `rydr-three`；其余 → `rydr` | 108 张表 |

### 2.2 扫描发现的残留 / 缺陷（尚未处理）

| 编号 | 问题 | 证据 | 风险 |
|------|------|------|------|
| D-1 | 响应码体系不统一 | `ResponseResult.success()` 用 `BusinessInterfaceStatus`(0/1)；`CommonStatusEnum.SUCCESS=1`、`FAIL=0`、`INTERNAL_SERVER_EXCEPTION=0`（两套码值并存） | 跨模块判码易错（登录失效根因已修复，但体系未归一） |
| D-2 | 计价双实现并存 | `service-valuation/ForecastController.calculatePrice`（简化 Haversine 10+2/km）与 `ValuationServiceImpl`（完整分段/夜间/远途/标签/动态折扣引擎）并存；API 调 `/forecast/single`（简化版） | 预估与实际计费口径不一致 |
| D-3 | 订单金额取 memo 字符串 | `OrderServiceImpl.orderAmount()` 解析 `order.memo` 的 `"fare=xxx"` | 脆弱、非正规字段 |
| D-4 | 硬编码弱密钥 | `EncriptUtil.KEY="pio-tech"`（DES）；`FeignAuthConfiguration` `BasicAuthRequestInterceptor("root","root")` | 生产不可用 |
| D-5 | 默认弱口令 | 18 个 yml 的 `EUREKA_PASSWORD:changeme`、`DB_PASSWORD:changeme`、`JWT_SECRET` 默认 `changeme-override-in-production` | 部署即暴露 |
| D-6 | actuator 全暴露历史 | 多数 yml 已收敛为 `health,info`（已修复），但 `eureka` 仍 `include` 默认 | 个别模块仍有暴露面 |
| D-7 | 配置中心未全量接入 | `rydr-config-server` 存在，但 `api-*`、业务服务多未接入，仍用本地 yml | 配置分散 |
| D-8 | 无分布式事务 | 订单/计价/钱包分库，仅靠本地 `@Transactional` 与幂等，无 Saga / 消息补偿 | 跨服务不一致 |
| D-9 | 无统一可观测性 | 仅 actuator + Spring Boot Admin，无 Micrometer/Prometheus/TraceID | 排障困难 |
| D-10 | 测试薄弱 | 多为空 `contextLoads` 或桩，无单元/集成测试覆盖业务 | 回归无保障 |
| D-11 | `ServiceForecast` Feign 鉴权被注释 | `api-passenger/feign/ServiceForecast.java` 第 19 行 `@FeignClient(name="service-valuation")` 未挂 `FeignAuthConfiguration` | 计价内部调用绕过鉴权（当前为有意放行） |

---

## 三、企业级差距分析（8 维度）

### 3.1 业务闭环完整性 ✅（已达基线）
登录、计价、下单、派单、行程、支付、钱包、短信、网关鉴权均已跑通，具备完整网约车主链路。差距在于**深度**而非**有无**。

### 3.2 统一响应 / 异常规范 ⚠️
- **现状**：`ResponseResult<T>` + `GlobalExceptionHandler` 已统一返回结构；但**码值体系分裂**（`BusinessInterfaceStatus` 0/1 与 `CommonStatusEnum` 1/0、`SmsStatusEnum` 0/-1/1 三套并存），`BaseResponse` 空壳未使用。
- **企业级要求**：单一状态枚举 + 全局异常码表 + OpenAPI 文档。
- **建议**：收敛为 `BusinessInterfaceStatus` 唯一来源；删除 `CommonStatusEnum` 的反向码值；`BaseResponse` 删除或统一。

### 3.3 安全（鉴权 / 密钥 / 限流）⚠️
- **现状**：网关 JWT 校验真实生效；限流 `RateFilter` 默认关闭；RSA 私钥与 JWT 密钥已外部化；但 **DES 弱加密、Feign `root/root`、默认 `changeme`** 仍硬编码。
- **企业级要求**：Vault / KMS 密钥管理、默认强密钥、限流默认开启、网关签名鉴权落地。
- **建议**：引入配置中心 + 密钥管理；移除 `EncriptUtil` DES 与 `FeignAuthConfiguration` 硬编码；限流生产默认开启。

### 3.4 配置与密钥管理 ⚠️
- **现状**：`rydr-config-server` 存在且 `config-client` 已演示接入（RabbitMQ Bus），但**核心业务/API 模块未接入**，配置仍散落各 yml。
- **企业级要求**：全量接入配置中心 + profile 隔离 + 密钥外置。
- **建议**：将 `api-*`、各 `service-*` 接入 `rydr-config-server`；移除本地敏感默认值。

### 3.5 可观测性（Metrics / Tracing / Logging）❌
- **现状**：仅有 `actuator(health,info)` + Spring Boot Admin；`Sleuth/Zipkin` 已在升级中移除且未补 `Micrometer Tracing`。
- **企业级要求**：Metrics（Prometheus）+ Tracing（TraceID 透传）+ 结构化日志三件套。
- **建议**：接入 `micrometer-registry-prometheus` + `micrometer-tracing-bridge-brave` + Grafana；网关 / 各服务打 TraceID。

### 3.6 数据一致性（分布式事务 / Saga）❌
- **现状**：订单 / 计价 / 钱包三域分库，仅本地事务 + 幂等（biz_no / out_trade_no）。无跨服务补偿。
- **企业级要求**：Saga / 消息事务 / TCC 保障最终一致。
- **建议**：钱包扣款失败需触发订单状态回滚；引入可靠消息（事务消息 / 本地消息表）补偿。

### 3.7 CI/CD 与测试 ❌
- **现状**：仅 `docker-compose` 雏形；测试多为空 `contextLoads`；无单元 / 集成测试。
- **企业级要求**：流水线 + 单测 / 集成测试 + 覆盖率门禁 + 镜像化。
- **建议**：GitHub Actions / Jenkins 流水线；`testcontainers` 做集成测试；Dockerfile 多阶段构建。

### 3.8 文档与运维 ⚠️
- **现状**：README 偏重业务叙事；`docs/` 技术栈描述过时（Zuul/Hystrix/Ribbon/Druid）。
- **企业级要求**：架构 / 模块 / 运维手册 / 故障预案分文档沉淀。
- **建议**：本文档与 `architecture.md` / `modules.md` 已校正为真实栈（见同目录）。

---

## 四、后续演进路线（按优先级）

| 优先级 | 主题 | 关键动作 |
|--------|------|----------|
| P0 | 安全收敛 | 移除 DES/Feign 硬编码、默认强密钥、`JWT_SECRET` 强制；限流生产默认开 |
| P0 | 响应码归一 | 统一 `BusinessInterfaceStatus` 为唯一码值来源 |
| P1 | 配置中心全量接入 | 核心模块接入 `rydr-config-server` + KMS |
| P1 | 计价口径统一 | `/forecast/single` 复用 `ValuationServiceImpl` 引擎，删除简化版 |
| P1 | 可观测性 | Micrometer + Prometheus + Tracing 接入 |
| P2 | 数据一致性 | 钱包失败回滚订单、事务消息补偿 |
| P2 | 测试与 CI | 集成测试 + 流水线 + Docker 化 |
| P3 | 业务深化 | 司机注册/实名独立服务、JMS 订单事件驱动、动态定价接入真实地图 |

---

## 五、扫描范围说明

- **模块（21）**：`eureka`、`rydr-config-server`、`rydr-zuul`、`cloud-admin`、`rydr-common`、`service-order`、`service-order-dispatch`、`service-passenger-user`、`service-sms`、`service-valuation`、`service-verification-code`、`service-wallet`、`api-passenger`、`api-driver`、`api-listen-order`、`service-jms-produce`、`service-jms-consumer`、`rydr-demo-app`、`config-client`、`config-client-diy`、`hystrix-dashboard`（源码保留，未参与构建）。
- **文件类型**：全部 `.java`（含 `src/test`）、全部 `application.yml` / `bootstrap.yml`、全部 `mybatis/mapper/*.xml`、`rydr-sql.sql`（108 张表，含 `tbl_order` / `tbl_passenger_wallet` / `tbl_wallet_transaction` / `passenger_user_info` 等关键表及其 UNIQUE 约束）。
- **方法约束**：单线程串行、主代理逐文件读取，未使用任何子代理 / 并行搜索，符合用户要求。
