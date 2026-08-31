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
| 可观测性三件套 | 父 POM `micrometer-*`/`zipkin-reporter-brave`/`logstash-logback-encoder`；`rydr-common/logback/base.xml`；21 个 yml `management.tracing` + `prometheus`；`docker-compose` 含 zipkin/prometheus/grafana | Metrics + Tracing + 结构化日志 |
| 支付最终一致 | `rydr-sql.sql` `tx_message`；`service-order/ActiveMQTxConfig` + `TxMessageCompensationJob`；`service-wallet/PayDeductConsumer`；队列 `queue.order.pay.deduct` / `queue.wallet.result` | 本地消息表 + ActiveMQ 事务消息 + 补偿 |

### 2.2 扫描发现的残留 / 缺陷（尚未处理）

| 编号 | 问题 | 证据 | 风险 |
|------|------|------|------|
| D-1 | 响应码体系不统一 | `ResponseResult.success()` 用 `BusinessInterfaceStatus`(0/1)；原 `CommonStatusEnum.SUCCESS=1`、`FAIL=0`、`INTERNAL_SERVER_EXCEPTION=0`（两套码值并存） | ✅ 已修复：`CommonStatusEnum` 反向码值已删除，收敛为 `BusinessInterfaceStatus` 唯一来源（3.2） |
| D-2 | 计价双实现并存 | `service-valuation/ForecastController.calculatePrice`（简化 Haversine 10+2/km）与 `ValuationServiceImpl`（完整分段/夜间/远途/标签/动态折扣引擎）并存；API 调 `/forecast/single`（简化版） | 预估与实际计费口径不一致 |
| D-3 | 订单金额取 memo 字符串 | `OrderServiceImpl.orderAmount()` 解析 `order.memo` 的 `"fare=xxx"` | ✅ 已修复：`tbl_order` 增加 `fare_amount` 字段，`orderAmount()` 优先取 `fareAmount`（3.6） |
| D-4 | 硬编码弱密钥 | 原 `EncriptUtil.KEY="pio-tech"`（DES）；`FeignAuthConfiguration` `BasicAuthRequestInterceptor("root","root")` | ✅ 已修复：两文件已删除，无外部调用残留（3.3） |
| D-5 | 默认弱口令 | 原 18 个 yml 的 `EUREKA_PASSWORD:changeme`、`DB_PASSWORD:changeme`、`JWT_SECRET` 默认 `changeme-override-in-production` | ✅ 已修复：改为 `REQUIRED_CHANGEME` 强占位符 + `JWT_SECRET` 强制注入无默认（3.3/3.4） |
| D-6 | actuator 全暴露历史 | 多数 yml 已收敛为 `health,info`（已修复），但 `eureka` 仍 `include` 默认 | 个别模块仍有暴露面 |
| D-7 | 配置中心未全量接入 | `rydr-config-server` 存在，但 `api-*`、业务服务多未接入，仍用本地 yml | 配置分散 |
| D-8 | 无分布式事务 | 订单/计价/钱包分库，仅靠本地 `@Transactional` 与幂等，无跨服务补偿 | ✅ 已修复：引入本地消息表 `tx_message` + ActiveMQ 事务消息 + 补偿任务，订单→钱包扣款最终一致（3.6） |
| D-9 | 无统一可观测性 | 仅 actuator + Spring Boot Admin，无 Micrometer/Prometheus/TraceID | ✅ 已修复：Micrometer + Zipkin + Prometheus + Grafana + 结构化日志三件套全链路接入（3.5） |
| D-10 | 测试薄弱 | 多为空 `contextLoads` 或桩，无单元/集成测试覆盖业务 | 回归无保障 |
| D-11 | `ServiceForecast` Feign 鉴权被注释 | `api-passenger/feign/ServiceForecast.java` 第 19 行 `@FeignClient(name="service-valuation")` 未挂 `FeignAuthConfiguration` | 计价内部调用绕过鉴权（当前为有意放行） |
| D-12 | 死代码 / 空文件 / 历史残留 | 13 个 0 字节空 Java 文件；硬编码凭据/历史残留：`api-passenger`(MyBasicAuthRequestInterceptor/FeignDisableHystrixConfiguration/SmsClientFallback)、`api-driver`(HystrixIgnoreException/BusinessException/HelloRequest)、`RestTemplateRequestServiceImpl` Hystrix 注释、`ServiceForecast` 无用 import | ✅ 已清理：删除空文件与残留类、清理注释/import（`mvn clean package` 21 模块全 SUCCESS 验证） |
| D-13 | yml 弃用键 / 不合理配置 | `spring.redis`(Boot3 弃用→`spring.data.redis`)、`spring.application.admin.enabled`、`env: NaN`、若干 YAML 格式错误 | ✅ 已修复：4 模块改 `spring.data.redis`；移除 `spring.application.admin.enabled`(2 处)；删除 5× `env: NaN`；修正 service-order-dispatch/api-listen-order/config-client YAML 格式（Python pyyaml 唯一键校验 21 文件全绿） |

---

## 三、企业级差距分析（8 维度）

### 3.1 业务闭环完整性 ✅（已达基线）
登录、计价、下单、派单、行程、支付、钱包、短信、网关鉴权均已跑通，具备完整网约车主链路。差距在于**深度**而非**有无**。

### 3.2 统一响应 / 异常规范 ✅（已落地）
- **现状（已整改）**：`ResponseResult<T>` + `GlobalExceptionHandler` 统一返回结构；`CommonStatusEnum` 的反向码值（`SUCCESS(1)`/`FAIL(0)`/`INTERNAL_SERVER_EXCEPTION(0)`）已删除，**收敛为 `BusinessInterfaceStatus`（SUCCESS=0/FAIL=1）唯一码值来源**；两个空壳 `BaseResponse` 类已删除并清理无用 import。
- **保留说明**：`CommonStatusEnum` 的业务码（10001-10004 验证码类）与 `SmsStatusEnum`（短信域专用 0/-1/1）仍按域保留——前者是真实业务码、后者是短信子域专用返回码，二者不与主码值体系冲突，文档注明即可，不强并入主枚举以免跨域耦合。
- **企业级要求**：单一状态枚举 + 全局异常码表 + OpenAPI 文档。
- **已完成**：码值归一 + 死代码清理（`mvn clean package` 21 模块全 SUCCESS 验证）。OpenAPI 文档化为后续 P2 项。

### 3.3 安全（鉴权 / 密钥 / 限流）✅（已落地）
- **现状（已整改）**：网关 JWT 校验真实生效；**限流 `RateFilter` 默认开启**（`rydr.gateway.rate-limit.enabled` 由 `false` 改 `true`，保留开关）；RSA 私钥与 JWT 密钥已外部化；**DES 弱加密 `EncriptUtil` 已删除**（无任何外部调用）；**`FeignAuthConfiguration` 的 `BasicAuth("root","root")` 硬编码已删除**（`ServiceForecast` 未实际挂载，已清理 import）；**JWT_SECRET 改为强制注入无默认**，缺失即启动失败（fail-fast）。
- **企业级要求**：Vault / KMS 密钥管理、默认强密钥、限流默认开启、网关签名鉴权落地。
- **已完成**：弱加密与硬编码鉴权清除 + 限流默认开 + JWT 密钥强制注入。Vault/KMS 与网关签名鉴权为后续 P1/P0 演进项（见第四节）。

### 3.4 配置与密钥管理 ✅（全量接入已落地，KMS 为后续）
- **现状（已整改）**：`rydr-config-server` 已改为**本地 native 后端**，18 个业务/基础设施模块**全部接入配置中心**，敏感配置外置到配置中心并保留环境变量覆盖。
- **已实现证据**：
  - **config-server 后端改 native**：`rydr-config-server/application.yml` 设 `spring.profiles.active=native` + `spring.cloud.config.server.native.search-locations=classpath:/config-repo/`（原占位 git 仓库 `oi/rydr-config-profile` 已注释为可选，本地无需任何外部 git 即可启动）。
  - **配置仓库**：`rydr-config-server/src/main/resources/config-repo/` 下为 18 个模块各建 `{application}-dev.yml`，外置 DB / EUREKA / ACTIVEMQ / JWT / REDIS / MAIL / ADMIN 等敏感/共享配置，保留 `${ENV:REQUIRED_CHANGEME}` 占位。
  - **父 POM 依赖管理**：`dependencyManagement` 统一托管 `spring-cloud-starter-config` 与 `spring-cloud-starter-bootstrap`（版本由 Spring Cloud BOM 托管，未手 pin）。
  - **18 个模块接入**：各模块 pom 引入上述两依赖；新建 `bootstrap.yml` 通过 **Eureka 服务发现**连接 config-server（`spring.cloud.config.discovery.enabled=true` + `service-id=config-server` + `profile=dev` + `label=master`）；`eureka` 自身用 **URI 直连**（`http://localhost:6001`，规避鸡生蛋）。
  - **本地兜底**：各模块 `application.yml` 保留原配置 + `spring.cloud.config.fail-fast=false` + `allow-override=true` + `override-none=true`，保证**不启动 config-server 也能本地运行**（教学/演示友好）。
  - **启动顺序**：eureka(7900) → config-server(6001，注册到 eureka) → 业务模块（discovery 发现 config-server）。
- **企业级要求**：全量接入配置中心 + profile 隔离 + 密钥外置（Vault/KMS）。
- **已完成**：18 模块全量接入 config-server（native 后端）+ 敏感项外置 + 环境变量覆盖 + 本地兜底。
- **后续**：KMS/Vault 真实密钥托管为 P1 演进项（当前密钥经环境变量注入，未接 KMS）。生产部署通过环境变量提供真实值：`export DB_PASSWORD=xxx EUREKA_PASSWORD=xxx JWT_SECRET=xxx`。

### 3.5 可观测性（Metrics / Tracing / Logging）✅（已落地）
- **现状（已整改）**：已构建 Micrometer + Zipkin + Prometheus + Grafana + 结构化日志三件套。
- **已实现证据**：
  - **依赖（父 POM，由 Boot 3.5 BOM 托管，仅手 pin `logstash-logback-encoder=8.1`）**：`micrometer-registry-prometheus`、`micrometer-tracing-bridge-brave`、`zipkin-reporter-brave`、`logstash-logback-encoder`，经 `mvn -pl rydr-common -am dependency:resolve` 验证 BUILD SUCCESS。
  - **结构化日志**：`rydr-common` 提供 `logback/base.xml`（LogstashEncoder + `traceId`/`spanId` MDC + `JSON_LOG` 开关）；13 个依赖 `rydr-common` 的模块 `logback-spring.xml` 复用之；6 个不依赖 `rydr-common` 的模块（eureka / rydr-config-server / cloud-admin / rydr-demo-app / service-jms-*）内联 JSON logback。
  - **端点与采样**：全部 21 个 yml 增加 `management.endpoints.web.exposure.include: health,info,prometheus`（网关加 `gateway`）+ `management.tracing.sampling.probability: 1.0` + `management.zipkin.tracing.endpoint: ${ZIPKIN_ENDPOINT:http://localhost:9411}/api/v2/spans` + `discovery-client-enabled: false`。
  - **后端栈**：`docker/` 下新增 `prometheus/prometheus.yml`、`grafana/provisioning/{datasources,prometheus/zipkin}`、`grafana/dashboards/rydr-jvm-overview.json`；`docker-compose.yml` 追加 `zipkin`(9411) / `prometheus`(9090) / `grafana`(3000) 三个服务及持久化卷。
- **企业级要求**：Metrics + Tracing + Logging 三件套。
- **已完成**：三件套全链路接入 + 后端可视化栈（docker-compose 一键拉起）。TraceID 已随日志与 Zipkin 透传；Prometheus 抓取各服务 `/actuator/prometheus`。

### 3.6 数据一致性（分布式事务 / 本地消息表补偿）✅（已落地，基于现有 ActiveMQ 事务消息）
- **现状（已整改）**：订单 / 计价 / 钱包三域分库，已在本地事务 + 幂等之外，引入**可靠消息（本地消息表 + ActiveMQ）最终一致**方案，覆盖支付全链路。
- **已实现证据**：
  - **表结构**：`rydr-sql.sql` 新增 `tx_message`（`biz_key` UNIQUE、`topic`、`payload`、`status` 0=INIT/1=SENT/2=DONE/3=FAIL/4=ABANDONED、`retry`、`next_retry_at`、`idx_status_next_retry`）；`tbl_order` 增加 `fare_amount decimal(10,2)`。
  - **service-order（消息生产方）**：启动类 `@EnableJms` + `@EnableScheduling`；`ActiveMQTxConfig` 声明队列 `queue.order.pay.deduct` / `queue.wallet.result` + `JmsTemplate` + `JmsTransactionManager`；`OrderServiceImpl.pay()` 重写为 **本地事务内**写 `tx_message(INIT)` → 发 JMS 扣款命令；`onPaySuccess`（@Transactional，标记 PAID + DONE）、`onPayFailure`（@Transactional，回滚行程 + 标记 ABANDONED 终态）；`WalletResultConsumer` 监听结果队列；`TxMessageCompensationJob` 每 30s 补偿（重试 `status in (0,1,3)`，避免死信陷阱）。
  - **service-wallet（消息消费方）**：启动类 `@EnableJms`；`ActiveMQConfig` 队列 + 监听器工厂；`PayDeductConsumer` 监听扣款队列 → 幂等扣款 → 回复结果队列。
  - **队列名一致**：生产方与消费方队列名统一（见 2.2 D-8）。
- **企业级要求**：Saga / 消息事务 / TCC 保障最终一致。
- **已完成**：基于 ActiveMQ 的事务消息 + 本地消息表 + 补偿任务，实现订单→钱包扣款的最终一致与失败回滚；幂等允许 DONE/ABANDONED 后重新发起支付。Saga 编排与 TCC 为后续深化项（见第四节 P3）。

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

| 优先级 | 主题 | 关键动作 | 状态 |
|--------|------|----------|------|
| P0 | 安全收敛 | 移除 DES/Feign 硬编码、默认强密钥、`JWT_SECRET` 强制；限流生产默认开 | ✅ 已落地（3.3） |
| P0 | 响应码归一 | 统一 `BusinessInterfaceStatus` 为唯一码值来源 | ✅ 已落地（3.2） |
| P1 | 配置中心全量接入 | 18 模块接入 `rydr-config-server`（native 后端）+ 敏感项外置（KMS 仍为后续） | ✅ 已落地（3.4） |
| P1 | 计价口径统一 | `/forecast/single` 复用 `ValuationServiceImpl` 引擎，删除简化版 | ⬜ 待办 |
| P1 | 可观测性 | Micrometer + Prometheus + Tracing 接入 | ✅ 已落地（3.5） |
| P2 | 数据一致性 | 钱包失败回滚订单、事务消息补偿 | ✅ 已落地（3.6） |
| P2 | 测试与 CI | 集成测试 + 流水线 + Docker 化 | ⬜ 待办 |
| P3 | 业务深化 | 司机注册/实名独立服务、JMS 订单事件驱动、动态定价接入真实地图 | ⬜ 待办 |

---

## 五、扫描范围说明

- **模块（21）**：`eureka`、`rydr-config-server`、`rydr-zuul`、`cloud-admin`、`rydr-common`、`service-order`、`service-order-dispatch`、`service-passenger-user`、`service-sms`、`service-valuation`、`service-verification-code`、`service-wallet`、`api-passenger`、`api-driver`、`api-listen-order`、`service-jms-produce`、`service-jms-consumer`、`rydr-demo-app`、`config-client`、`config-client-diy`、`hystrix-dashboard`（源码保留，未参与构建）。
- **文件类型**：全部 `.java`（含 `src/test`）、全部 `application.yml` / `bootstrap.yml`、全部 `mybatis/mapper/*.xml`、`rydr-sql.sql`（108 张表，含 `tbl_order` / `tbl_passenger_wallet` / `tbl_wallet_transaction` / `passenger_user_info` 等关键表及其 UNIQUE 约束）。
- **方法约束**：单线程串行、主代理逐文件读取，未使用任何子代理 / 并行搜索，符合用户要求。
