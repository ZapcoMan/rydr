# 运维手册与故障预案（Operations & Runbook）

[返回 README](../README.md) | [架构](architecture.md) | [模块](modules.md) | [企业级差距分析](enterprise-gap-analysis.md)

> 本文档面向**部署与运维**，涵盖启动顺序、环境变量、监控告警、常见故障排查与恢复预案。技术栈以当前代码为准（Spring Cloud 2025.0.3 / Boot 3.5.16 / JDK 17）。

---

## 一、部署拓扑与端口

| 组件 | 端口 | 说明 |
|------|------|------|
| MySQL | 3306 | 双库：`rydr`（订单/计价）、`rydr-three`（乘客/短信），共 108 张表 |
| Redis | 6379 | 缓存、验证码、在线司机集合、分布式锁 |
| ActiveMQ | 61616 / 8161 | JMS 事务消息（支付可靠消息 outbox）+ Web 控制台 |
| RabbitMQ | 5672 / 15672 | Spring Cloud Bus（配置刷新） |
| Eureka | 7900 | 服务注册中心 |
| Config Server | 6001 | 配置中心（native 后端） |
| Gateway | 9100 | Spring Cloud Gateway（统一鉴权/路由） |
| Cloud Admin | 6010 | Spring Boot Admin 监控 |
| Zipkin | 9411 | 分布式链路追踪 |
| Prometheus | 9090 | 指标采集 |
| Grafana | 3000 | 指标可视化（默认 admin/admin） |

业务服务端口见 [README「服务端口」](../README.md#服务端口)。

---

## 二、启动顺序（严格依赖）

> 关键：`eureka` 与 `config-server` 必须先于业务模块启动；其余业务模块通过 Eureka 服务发现定位 config-server，本地 `application.yml` 作为兜底（`spring.cloud.config.fail-fast=false`），配置中心不可用时仍可启动。

```
1. 基础设施（docker compose up -d）
   → MySQL / Redis / ActiveMQ / RabbitMQ / Zipkin / Prometheus / Grafana

2. Eureka        (7900)
3. Config Server (6001)   ← native 后端，从 config-repo/ 提供各模块 {app}-dev.yml
4. 业务/基础设施模块（任意顺序）
5. Gateway       (9100)
```

启动命令：

```bash
# 1. 基础设施
docker compose up -d

# 2. 注册中心
cd rydr/eureka && mvn spring-boot:run -Dspring.profiles.active=7900

# 3. 配置中心
cd rydr/rydr-config-server && mvn spring-boot:run

# 4. 业务服务（示例）
cd rydr/service-order && mvn spring-boot:run -Dspring.profiles.active=8004

# 5. 网关
cd rydr/rydr-zuul && mvn spring-boot:run
```

---

## 三、环境变量（密钥管理）

所有敏感配置统一通过环境变量注入，**默认值为 `REQUIRED_CHANGEME` 强占位符**，生产环境必须覆盖。完整清单见 [`.env.example`](../.env.example)。

| 变量 | 用途 | 是否必填 |
|------|------|----------|
| `DB_PASSWORD` | MySQL 密码 | ✅ 必填 |
| `EUREKA_PASSWORD` | Eureka 认证密码 | ✅ 必填 |
| `JWT_SECRET` | JWT 签名密钥（**无默认值，缺失即启动失败**） | ✅ 必填 |
| `ACTIVEMQ_PASSWORD` | ActiveMQ 密码 | ✅ 必填 |
| `ADMIN_PASSWORD` | Cloud Admin / valuation 管理密码 | ✅ 必填 |
| `MAIL_PASSWORD` | Admin 邮件通知 SMTP 密码 | 可选（启用邮件告警时） |
| `REDIS_PASSWORD` | Redis 密码 | 可选 |
| `GRAFANA_ADMIN_PASSWORD` | Grafana 管理密码 | 建议修改 |

生产部署示例：

```bash
export DB_PASSWORD='<强密码>' \
       EUREKA_PASSWORD='<强密码>' \
       JWT_SECRET='<至少 256bit 随机密钥>' \
       ACTIVEMQ_PASSWORD='<强密码>' \
       ADMIN_PASSWORD='<强密码>'
```

> **安全红线**：`JwtUtil` 在类加载时即解析 `JWT_SECRET`，未设置将直接 fail-fast 启动失败（这是预期行为）。切勿将 `.env` 提交到版本库。

---

## 四、配置中心（Spring Cloud Config）

- **后端**：native（本地文件系统），配置文件位于 `rydr-config-server/src/main/resources/config-repo/{app}-dev.yml`。
- **消费方式**：各模块 `bootstrap.yml` 通过 Eureka 服务发现（`service-id=config-server`）+ `profile=dev` 拉取；`eureka` 自身用 URI 直连。
- **兜底**：本地 `application.yml` 保留 + `fail-fast=false`，配置中心不可用时业务模块仍能启动（使用本地配置）。
- **切换 git 后端**：将 `spring.profiles.active` 改为 `git` 并设置 `CONFIG_GIT_URI` 指向可达私有仓库（当前已注释保留）。
- **动态刷新**：config-client 通过 RabbitMQ Bus 演示 `/actuator/refresh` + bus 广播。

---

## 五、监控与告警

| 能力 | 入口 | 说明 |
|------|------|------|
| 健康检查 | `http://<svc>/actuator/health` | 各服务暴露 `health,info,prometheus` |
| 指标 | `http://<svc>/actuator/prometheus` | Prometheus 抓取 |
| 链路追踪 | Zipkin `http://localhost:9411` | traceId/spanId 透传 |
| 指标看板 | Grafana `http://localhost:3000` | 预置 `rydr-jvm-overview` 看板 |
| 服务监控 | Cloud Admin `http://localhost:6010` | 服务健康 + 邮件告警 |

**告警建议**：Prometheus 可配置 Alertmanager 规则（CPU/内存/JVM GC/接口延迟/错误率）；Cloud Admin 已内置邮件通知（配置 `MAIL_*` 环境变量启用）。

---

## 六、故障排查预案（Runbook）

### 6.1 服务启动失败：`JWT_SECRET` 未设置
- **现象**：`api-*` / `service-*` 启动即抛 `IllegalStateException`，提示 secret 缺失。
- **处置**：`export JWT_SECRET='<随机强密钥>'` 后重启。

### 6.2 数据库连接失败：`Access denied` / `REQUIRED_CHANGEME`
- **现象**：启动日志 `Access denied for user ... (using password: YES)`，或连接串出现 `REQUIRED_CHANGEME`。
- **原因**：`DB_PASSWORD` 未覆盖，仍为强占位符。
- **处置**：设置真实 `DB_PASSWORD` 环境变量；确认 `docker compose up -d` 已拉起 MySQL 并导入 `rydr-sql.sql`。

### 6.3 服务注册不上 Eureka
- **现象**：Eureka 控制台（7900）看不到服务实例；网关路由 `lb://` 404。
- **排查**：确认 `EUREKA_PASSWORD` 两侧一致（服务端 `spring.security.user.password` 与客户端 `defaultZone` 内嵌密码）；确认 eureka 先于业务模块启动。

### 6.4 配置中心拉取失败（不影响启动）
- **现象**：日志出现 `Could not locate PropertySource` / `No instances found of configserver`。
- **影响**：因 `fail-fast=false` + 本地兜底，模块仍用本地 `application.yml` 启动，**不阻塞**。
- **处置**：确认 config-server(6001) 已启动并注册到 Eureka；或临时接受本地兜底配置。

### 6.5 支付链路不一致（订单已付但钱包未扣 / 反之）
- **机制**：`service-order` 支付走**本地消息表 `tx_message` + ActiveMQ 事务消息 + 补偿任务**（`TxMessageCompensationJob` 每 30s 重投 `status in (0,1,3)`）。
- **排查**：查 `tx_message` 表状态（0 待发/1 已发/2 完成/3 失败/4 终态放弃）；查 ActiveMQ 队列 `queue.order.pay.deduct` / `queue.wallet.result` 积压。
- **处置**：确认 ActiveMQ 可连、`service-wallet` 已启动；补偿任务会自动兜底重投，必要时手动触发补偿或重发。

### 6.6 网关 401 / 429
- **401**：JWT 缺失或过期。白名单路径为 `/auth`、`/verification-code`、`/forecast`、`/api-listen-order`、actuator；其余 `/api-*` 需携带有效 `Authorization: Bearer <token>` 或 `token` 头。
- **429**：触发 `RateFilter`（`rydr.gateway.rate-limit.enabled=true`，默认 5 QPS）。生产按压测调整令牌桶，或按需关闭。

### 6.7 可观测性看板无数据
- **现象**：Grafana / Zipkin / Prometheus 无数据。
- **排查**：确认各服务 `management.tracing.sampling.probability=1.0` 与 `management.zipkin.tracing.endpoint`；Prometheus `docker/prometheus/prometheus.yml` 中 target 使用 `host.docker.internal`（因服务跑在宿主机）。

---

## 七、日常运维清单

- [ ] 生产前覆盖全部 `REQUIRED_CHANGEME` 与 `JWT_SECRET`
- [ ] 按二、启动顺序拉起服务，核对 Eureka 实例注册数
- [ ] 巡检 `tx_message` 失败/积压记录（支付一致性兜底）
- [ ] 观察 Prometheus/Grafana 关键指标（JVM、接口延迟、错误率）
- [ ] 定期轮换数据库/JWT/网关密钥
- [ ] 数据库备份与恢复演练（108 张表，双库）
