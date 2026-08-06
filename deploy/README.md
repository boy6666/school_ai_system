# EduAgent 本机开发与联调（单环境 local）

> 已收敛为**单环境**：不再区分 dev/formal。所有服务统一在
> **Nacos 命名空间 `edu-agent-local`、Spring profile `local`** 下运行。
> 工作流（方案 B）：**中间件跑 Docker，Java 服务本机 IDE 直跑**，注册进同一
> namespace 即视为同一内网。

```
deploy/
├── docker-compose.yml        # 仅基础设施：nacos + redis + rabbitmq + chroma + ai（MySQL 用本机已有实例）
├── push-nacos-config.py      # 把各服务配置推到 Nacos（创建 edu-agent-local 命名空间）
└── nacos-config/             # 各服务的 Nacos data-id（yaml 源文件）
    ├── edu-agent-gateway.yaml
    ├── edu-agent-auth.yaml
    ├── edu-agent-learning.yaml
    ├── edu-agent-resource.yaml
    ├── edu-agent-teacher.yaml
    └── edu-agent-code.yaml

根目录：
├── Dockerfile                # 通用多阶段构建（按 MODULE 参数构建任一服务，备用）
└── database/init-microservice.sql   # 建 5 个逻辑库 + 账号（不建表）
```

---

## 1. 架构与约定

- **DB-per-service**：**本机 MySQL**（localhost:3306）+ 5 个逻辑库（`auth_db` / `learning_db` / `resource_db` / `teacher_db` / `code_db`），各服务独立 datasource，**禁止跨库外键**。库由 `init-microservice.sql` 创建；**建表 DDL 由各开发者在自己模块的 Flyway 迁移脚本里执行**。
- **Nacos 双重角色**：既做**服务发现**（`lb://edu-agent-*` 经 gateway 路由、Feign 调用），也做**配置中心**（各服务启动时拉取自己的 data-id）。standalone 单机、单命名空间 `edu-agent-local`。
- **单环境**：所有服务 `NACOS_NAMESPACE=edu-agent-local`、`SPRING_PROFILES_ACTIVE=local`（`application.yml` 已设为默认值，IDE 直跑无需额外设）。
- **鉴权链路**：auth 签发 JWT → gateway `AuthGlobalFilter` 校验并注入 `X-User-Id` / `X-User-Roles` → 下游 `AuthContextFilter` 读入 `AuthContext` → Feign `AuthFeignInterceptor` 转发。
- **统一端点**：所有服务以 `/api/<service>/...` 暴露，gateway **不 StripPrefix**（仅 ai 例外，见下）。
- **ai 例外**：ai 是外部 FastAPI，路由挂在根路径（`/chat`、`/resource/generate`…），故 gateway 对其 `StripPrefix=2`。

---

## 2. 本机准备 + 一键起基础设施

**① 本机 MySQL 建 5 个库（一次性，用本机 MySQL 的 root）：**

```bash
mysql -uroot -p < database/init-microservice.sql
```

**② 起其余基础设施（Docker）：**

```bash
cd deploy
docker compose up -d                # 起 nacos + redis + rabbitmq + chroma + ai
docker compose ps                   # 看健康状态（重点等 nacos healthy）

# 等 Nacos healthy 后，把各服务配置推送到配置中心（创建 edu-agent-local 命名空间）：
python deploy/push-nacos-config.py
```

供本机 Java 服务连接的端口：

| 服务 | 端口 |
|---|---|
| MySQL（本机已有实例） | 3306 |
| Nacos | 8848（控制台 `/nacos/`，账号 nacos/nacos） |
| Redis | 6379 |
| RabbitMQ | 5672（控制台 15672，账号 guest/guest） |
| Chroma | 8000 |
| AI（FastAPI，默认 mock LLM） | 8001 |

> 顺序很关键：5 库必须先建好，配置必须先推到 Nacos，Java 服务启动时才拿得到 datasource / JWT 等配置。

---

## 3. 本机跑 Java 服务（IDE / mvn）

6 个 Java 服务（gateway + auth/learning/resource/teacher/code）不在 compose 里，用 IDE 直跑对应 `*Application`：

- 前置：JDK 17+（本机 21，编译 `release 17`）、Maven、Docker Desktop。
- `application.yml` 已默认 `namespace=edu-agent-local`、`profile=local`，IDE Run 即可；如需覆盖：
  ```bash
  set NACOS_NAMESPACE=edu-agent-local
  set SPRING_PROFILES_ACTIVE=local
  set NACOS_ADDR=127.0.0.1:8848
  set DB_HOST=127.0.0.1        # 中间件若不在本机再覆盖
  set MQ_HOST=127.0.0.1
  ```
- 先建 5 个库（见 §2 ①：`mysql -uroot -p < database/init-microservice.sql`，一次性）。
- 启动后各服务注册到 `edu-agent-local`，互为本机内网；联调入口统一走网关 `http://localhost:8080/api/<服务>/...`。

---

## 4. 启用 APM / 前端（可选）

- **SkyWalking**：需要时用 `docker compose --profile skywalking up -d`（旧版 compose 含此 profile，已从本地栈移除，按需自行追加）。
- **前端**：曾姿妍交付的 `edu-agent-web` 另行构建，经 nginx 代理网关 `:8080`。

---

## 5. 常见问题

- **服务起不来 / 缺配置**：多半是配置还没推到 Nacos → 先 `python deploy/push-nacos-config.py`。
- **8080 被占用**：关掉占用程序或改 `server.port`。
- **JWT 密钥不一致**：所有服务（含网关）必须共享同一 `JWT_SECRET`（默认 `edu-agent-local-secret-please-change`，本地够用；联调各机器保持一致）。
- **本地改了公共服务（common/gateway）**：非架构批准不得改（见 `docs/GIT_CONVENTIONS.md`）。
