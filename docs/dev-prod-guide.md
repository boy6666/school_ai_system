# 本地 Win 开发 / Linux 生产部署 / Git 协作 指南

> 面向：在 EduAgent 微服务骨架上做开发的同事（学习/教师 — 陈海洋；资源 — 陈嘉成）。
> 本文讲**两套环境怎么跑、注意什么、有什么区别**，以及**日常 Git 怎么操作**。
> 业务代码怎么写见 `docs/FRAMEWORK_DEV_GUIDE.md`；Git 完整红线见 `docs/GIT_CONVENTIONS.md`。

---

## 0. 先建立认知：两套环境是"同一套代码 + 不同环境变量"

代码只有一份，靠 Nacos 的**两个 namespace** 区分环境，差异**全部用环境变量注入**，不写死在任何 yaml 里：

| 维度 | 本地 Win 开发 | Linux 生产 |
|---|---|---|
| Nacos namespace | `edu-agent-dev` | `edu-agent-formal` |
| 服务怎么跑 | 本机 JVM（IDE / `mvn`） | Docker 容器（`docker-compose`） |
| 中间件 | 本机装 MySQL/Nacos/RabbitMQ（或 MySQL 连共享） | 容器编排一起起 |
| 数据库 | 本机 MySQL 的 5 个库 | 生产 MySQL（独立/云） |
| 密钥 | 可用本地默认值 | **必须**环境变量注入强密钥 |
| CORS 源 | `*`（`allowCredentials=false`，本地前端随便联调） | 前端真实域名 |
| 对外入口 | 网关 `:8080` 直连 | 网关 `:8080` + `nginx` 反代 |
| AI 服务 | 本机另起 FastAPI（`:8001`） | 容器同编排 |

**核心原则**：同一份 Nacos yaml 推到两个 namespace，靠 `DB_HOST` / `JWT_SECRET` / `AUTH_BOOTSTRAP_ADMIN_PASSWORD` / `CORS_ALLOWED_ORIGINS` / `AI_HOST` 等环境变量区分。换环境 = 换这串变量，**不动代码、不动 yaml 结构**。

---

## 1. 本地 Win 开发（本机原生 + 本地中间件）

推荐的全本地方案：**本机装好 MySQL / Nacos(standalone) / RabbitMQ，把网关 + 全部 6 个服务都在本机跑起来，统一在 `edu-agent-dev` 命名空间**。这样你独占所有实例，对接任何接口（前端→网关→任意服务）都不依赖别人，最省心。

### 1.1 一次性前置

- **JDK 17+**（本机是 21，编译按 `release 17`，运行 17+ 均可）。
- **Maven**，并指向**本地仓库** `D:\software\apache-maven-3.9.4\mvn_repo`（不是默认 `~/.m2`）：
  - 配 `conf/settings.xml` 的 `<localRepository>`，或每次加 `-Dmaven.repo.local=D:/software/apache-maven-3.9.4/mvn_repo`。
- 本机安装 **MySQL 8**、**Nacos（standalone 模式）**、**RabbitMQ**。

### 1.2 初始化数据库（最容易漏的一步）

Flyway **只建表、不建库**。每个服务启动时会连自己的库（auth_db / learning_db / resource_db / teacher_db / code_db），库不存在就启动失败。

```bash
# 用 root 连本机 MySQL，先建 5 个空库 + 授权用户（一次性）
mysql -uroot -p < database/init-microservice.sql
```

之后各服务启动，Flyway 按 `src/main/resources/db/migration/V1__*.sql` 自动建表。

### 1.3 启动顺序

```
① 起 Nacos(standalone)  ② 起 MySQL、RabbitMQ  ③ 起各微服务（顺序无所谓，等注册到 Nacos 即可）
```

本机起单个服务（示例）：

```bash
mvn -Dmaven.repo.local=D:/software/apache-maven-3.9.4/mvn_repo ^
    -pl edu-agent-code -am spring-boot:run
```

或直接在 IDE 里 Run 各 `*Application`。**关键环境变量**（本地默认值已够用，但 namespace 要对）：

```bash
set NACOS_NAMESPACE=edu-agent-dev
set SPRING_PROFILES_ACTIVE=dev
:: 若中间件不在本机/端口不同，再覆盖：
set DB_HOST=127.0.0.1
set MQ_HOST=127.0.0.1
set NACOS_ADDR=127.0.0.1:8848
set AI_HOST=127.0.0.1
```

前端联调地址：`http://localhost:8080/api/<服务>/...`（网关不开 CORS 限制，localhost 前端随便调）。

### 1.4 轻量变体：Nacos/RabbitMQ 本机 + MySQL 连共享测试库

不想本机装 MySQL 时——**Nacos 和 RabbitMQ 仍本机跑（独占 dev namespace，避免发现冲突），只把 `DB_HOST` 指到共享测试库**：

```bash
set NACOS_ADDR=127.0.0.1:8848      :: 本机 Nacos
set DB_HOST=<共享测试库地址>         :: 共享 MySQL
```

这样少装一个重组件，且因为 Nacos 是本机独占的，不会和别人冲突。

### 1.5 ⚠️ 千万别这么干：只起自己一个服务 + 连别人的共享 Nacos

```
坏处：你的本地实例和共享环境里的同名服务，会在同一个 namespace 注册出【两个实例】。
      网关 lb 是负载均衡的，请求可能随机打到你的本地实例，也可能打到共享实例；
      更糟的是，别人的请求也会命中你的本地实例 → 环境互相污染、接口对不上、还难排查。
```

**结论**：要么全本地（1.3），要么 Nacos 也本机独占（1.4）。不要"半本地半共享"。

### 1.6 本地注意点（坑位清单）

| 坑 | 现象 / 解决 |
|---|---|
| Maven 仓库路径不对 | 依赖下不到 / 编译慢 → 确认指向 `D:\software\apache-maven-3.9.4\mvn_repo` |
| JDK 版本 | 编译 `release 17`；本机 21 可跑。别用 8/11 |
| 忘了先建 5 个库 | 服务启动报 `Unknown database 'xxx_db'` → 先跑 `init-microservice.sql` |
| namespace 错了 | 连到 `edu-agent-formal` 或默认 `public` → 显式 `NACOS_NAMESPACE=edu-agent-dev` |
| 8080 被占用 | 网关起不来 → 关掉占用端口的程序，或改 `server.port` |
| AI 接口 502 | `/api/edu-agent-ai/**` 需要本机另起 FastAPI（`:8001`）→ 没起就报 502，**不影响 Java 服务** |
| 测试账号丢失 | `teststudent` 测试账号必须保留，初始化脚本会重建 |

---

## 2. Linux 生产（Docker Compose）

生产用 Docker Compose 编排。仓库 `deploy/` 下已提供 `docker-compose.yml`、`Dockerfile.*`（ai/server/web）、`nginx/nginx.conf`——以下步骤围绕它们，不重复造。

### 2.1 构建镜像

每个服务先出 jar，再用对应 Dockerfile 打镜像：

```bash
mvn -B -ntp clean package -DskipTests        # 出各服务 jar
docker compose -f deploy/docker-compose.yml build   # 按 Dockerfile.* 构建镜像
```

### 2.2 用环境变量注入强密钥（绝不写进镜像/yaml）

`.env` 或编排平台 secret 里放（**不要**提交真实值）：

```bash
NACOS_NAMESPACE=edu-agent-formal
JWT_SECRET=<强随机，≥32 字节>
AUTH_BOOTSTRAP_ADMIN_PASSWORD=<强口令>
DB_HOST=<生产 MySQL 地址>
DB_USER=<生产用户>
DB_PASSWORD=<生产口令>
MQ_HOST=<生产 RabbitMQ>
CORS_ALLOWED_ORIGINS=https://your-frontend.domain   # 不再是 *
AI_HOST=<AI 服务地址>
```

### 2.3 起全套

```bash
docker compose -f deploy/docker-compose.yml up -d
```

编排内包含：mysql / nacos / rabbitmq / gateway / 各微服务 / nginx。网关 `:8080` 由 `nginx` 反代到对外端口（见 `deploy/nginx/nginx.conf`）。

### 2.4 探针已就绪

各服务已暴露 `/actuator/health/liveness` 与 `/actuator/health/readiness`，Compose/K8s 直接挂 `healthcheck` 或探针即可（配置见各服务 Nacos yaml 的 `management` 块）。

### 2.5 上线步骤清单

1. `mvn clean package -DskipTests` 出 jar
2. `docker compose build` 构建镜像
3. 在生产机注入 `.env`（强密钥 + `edu-agent-formal` + 真实域名/库地址）
4. `docker compose up -d`
5. 查日志确认各服务注册到 `edu-agent-formal`、5 个库 Flyway 迁移成功
6. `curl http://<网关>/actuator/health` 探活
7. 用 `teststudent` / admin 走一遍登录→调接口冒烟

---

## 3. 两个环境的核心区别速查

| 要改的东西 | 本地 Win | Linux 生产 |
|---|---|---|
| Nacos namespace | `edu-agent-dev` | `edu-agent-formal` |
| 服务运行 | 本机 JVM（IDE/mvn） | 容器（docker-compose） |
| 中间件 | 本机装（或 MySQL 连共享） | 容器编排一起起 |
| DB 地址 | `127.0.0.1` | 生产库地址 |
| JWT 密钥 | 本地默认即可 | 必须强随机环境变量 |
| admin 口令 | 本地默认 `admin123` | 必须 `AUTH_BOOTSTRAP_ADMIN_PASSWORD` 覆盖 |
| CORS | `*` | 前端真实域名 |
| AI 服务 | 本机 FastAPI `:8001` | 容器同编排 |
| 对外入口 | 网关 `:8080` 直连 | nginx 反代网关 |
| 调试方式 | 直接打本地端口 / 网关 | 只走网关，不外透服务端口 |

**一句话**：本地图省事（默认值 + 全本地 + namespace=dev），生产图安全（强密钥 + namespace=formal + 域名 CORS + 容器隔离），二者**代码和 yaml 结构完全一致，只换环境变量**。

---

## 4. Git 操作规范

> 完整红线见 `docs/GIT_CONVENTIONS.md`，本节给开发同学最常用的实操。

### 4.1 分支模型

| 分支 | 角色 | 能否直推 |
|------|------|----------|
| `main` | 稳定基线 | ❌ 仅 PR + 审批 + CI 绿 |
| `develop` | 单体基线（保留不动） | ❌ |
| `feature/microservice` | 微服务后端**集成线** | ❌ 仅 PR 合入（架构维护） |
| `feat/<module>` | **个人模块分支**（common/gateway/auth/code/ai/resource/learning/teacher/web） | ✅ 个人自由推，但**合入需 PR** |

- 个人分支从 `feature/microservice` 起，`feat/<module>` **PR 回 `feature/microservice`**。
- 模块负责人：common/gateway/auth/code/ai → WYC；resource → 陈嘉成；learning/teacher → 陈海洋；web → 曾姿妍。
- **禁止**在 `main` / `develop` / `feature/microservice` 上直接提交。

### 4.2 每日标准流程

```bash
git fetch origin
git switch feat/resource                       # 切到自己的模块分支
# 只改自己模块目录（edu-agent-<module>/ 或 edu-agent-web/），别碰 common/gateway/他人模块
git add edu-agent-resource/...
git commit -m "feat(resource): KB 采集清洗流水线"
git push -u origin feat/resource
# GitHub 开 PR：feat/resource → feature/microservice，等 reviewer 审批 + CI 绿后合入

# 开 PR 前先 rebase 到最新集成线，避免冲突堆积：
git fetch origin
git rebase origin/feature/microservice
git push --force-with-lease                   # 仅个人分支允许强推
```

### 4.3 提交信息规范

格式：`type(scope): 简述`（英文 scope 用模块名）

| type | 含义 | type | 含义 |
|------|------|------|------|
| `feat` | 新功能 | `refactor` | 重构 |
| `fix` | 修 bug | `docs` | 文档 |
| `style` | 仅样式/格式 | `chore` | 构建/配置 |

示例：`feat(auth): 登录注册 + JWT 签发`、`fix(learning): 修复路径聚合统计偏差`、`chore: 补充 Nacos 配置`。

### 4.4 CI 门禁

合入前 **`mvn clean verify` 必须绿**（`.github/workflows/ci.yml` 在 push/PR 到 `feature/microservice` 时自动跑）。本地编译不过不要推。

```bash
mvn -B -ntp -Dmaven.repo.local=D:/software/apache-maven-3.9.4/mvn_repo clean verify
```

### 4.5 常用命令

```bash
git status                                       # 先看状态，再决定
git stash                                        # 临时保存未提交改动
git switch -c feat/xxx origin/feat/xxx           # 基于远端建个人分支
git rebase origin/feature/microservice           # 同步集成线
git push --force-with-lease                      # 仅个人分支 rebase 后强推
git log --oneline -10                            # 看提交历史
git diff origin/feature/microservice             # 看与集成线差异
```

---

## 5. 常见疑问

**Q：本地能不能只跑我改的那个服务，其他用共享环境？**
A：不推荐（见 1.5，服务发现冲突）。最省事的折中是 1.4：Nacos/RabbitMQ 本机 + MySQL 连共享，且你本机 Nacos 独占 dev namespace。

**Q：为什么本地 CORS 是 `*`？**
A：本地前端用 localhost 联调、鉴权走 Bearer JWT（非 Cookie），`allowCredentials=false` 时 `*` 没问题。生产必须收紧为前端域名。

**Q：Flyway 脚本在本地和生产都会跑吗？**
A：会，且只跑一次（记录于 `flyway_schema_history`）。**已执行的脚本禁止修改**，要改表就新增 `V{n+1}__*.sql`。本地和生产走同一套迁移。

**Q：要改 common / gateway 怎么办？**
A：非架构批准不得改（见 GIT_CONVENTIONS §3 红线 2）。有公共能力需求先提给架构（WYC）评估，统一在集成线落地，避免各分支各改一份。
