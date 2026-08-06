# EduAgent 全容器「部署 / 团队联调」指南

> 适用：一台**常开的共享机器**（团队联调机），整栈全部容器化，Windows / Linux 均可。
> 成员各自的电脑**不需要常开**，统一访问这台机器的网关 `:8080`。
> 这份编排与开发用 `deploy/docker-compose.yml`（基建容器 + MySQL/Java 裸跑）互不影响。

```
deploy/docker-compose.deploy.yml   # 全容器编排：mysql + nacos + redis + rabbitmq + chroma + ai + 6 个 Java 服务
deploy/README.deploy.md            # 本文档
deploy/../database/init-microservice.sql   # 建 5 库 + edu_agent 账号（mount 进 mysql 首次初始化）
deploy/../Dockerfile               # 通用多阶段构建：按 MODULE 打任一 Java 服务镜像
```

## 1. 端口一览（团队访问入口）

| 端口 | 用途 |
|---|---|
| **8080** | **网关（团队联调主入口）** `http://<机器IP>:8080/api/<服务>/...` |
| 3306 | MySQL（需要直连调试时） |
| 8848 | Nacos 控制台（`/nacos/`，账号 nacos/nacos） |
| 8001 | AI（FastAPI） |
| 8000 | Chroma |
| 15672 | RabbitMQ 控制台（guest/guest） |

其余 Java 服务（auth 8081 / learning 8082 / resource 8083 / teacher 8084 / code 8085）默认**只在容器内网**访问，团队统一走网关 8080；确需直连时取消 compose 里对应 `ports:` 注释。

## 2. 首次启动（顺序很关键）

**① 起基建 + 库**（mysql / nacos / redis / rabbitmq / chroma / ai）
```bash
cd deploy
docker compose -f docker-compose.deploy.yml up -d mysql nacos redis rabbitmq chroma ai
docker compose -f docker-compose.deploy.yml ps   # 等 nacos、mysql 都 healthy
```

**② 推配置到 Nacos**（必须先于 Java 服务；需能访问本机 `127.0.0.1:8848`）
```bash
cd E:\college_information\edu-agent
python deploy/push-nacos-config.py
```

**③ 起全部 Java 服务**
```bash
cd deploy
docker compose -f docker-compose.deploy.yml up -d
```
首次会 build 6 个 Java 镜像（较慢，几分钟），完成后各服务注册进 Nacos，团队即可经 `:8080` 联调。

> 之后日常 `docker compose -f docker-compose.deploy.yml up -d` 一条即可（Nacos 配置已持久化在卷里，无需重推）。

## 3. 密钥与凭据（务必生产环境覆盖）

在 `deploy/.env` 里设置（`docker compose` 会自动读取同目录 `.env`）：
```
JWT_SECRET=换成足够长的随机串
MYSQL_ROOT_PASSWORD=换成强口令
```
所有 Java 服务（含网关）共享同一个 `JWT_SECRET` —— 保证 auth 签发与网关验签一致。
如不设，走 compose 里的默认值（仅限内网联调，**禁止生产使用**）。

业务账号固定 `edu_agent / edu_agent`（由 init 脚本建，授权 5 库）。各服务建表 DDL 由 Flyway 在首次启动时自动执行，无需手动建表。

## 4. Windows ↔ Linux 迁移要点

- **整栈在容器里，迁移 = 目标机器装好 Docker + 拉这份代码 + 同一条 `up -d`**。
- 数据都在 **命名卷**（非 Windows 绝对路径），跨平台无路径问题；卷数据随容器保留，重起不丢。
- 仓库内的 `Dockerfile` / `.sql` / `.py` 保持 **LF 换行**（在 Windows 上 git 检出后若被转成 CRLF，脚本可能报错；可配 `git config core.autocrlf input` 或 `.gitattributes`）。
- Java 服务镜像在 Windows 上构建的是 Linux 容器（Docker Desktop 走 WSL2），与 Linux 同构，可互换。

## 5. 排查

- **服务起不来 / 缺配置**：多半是第②步配置没推或 Nacos 分组不对（resource 用 `resource-group`）→ 重推 `push-nacos-config.py`。
- **网关 401「未认证或令牌无效」**：JWT_SECRET 不一致 → 检查所有 Java 服务的 `JWT_SECRET` 环境变量是否同一份。
- **DB 连不上**：容器内要用 `DB_HOST=mysql`（compose 已设），不要用 `127.0.0.1`。
- **看日志**：`docker compose -f docker-compose.deploy.yml logs -f <服务名>`。
