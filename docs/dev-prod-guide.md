# EduAgent 本机开发与联调指南（单环境 local）

> 面向：在 EduAgent 微服务骨架上做开发的同事（学习/教师 — 陈海洋；资源 — 陈嘉成）。
> 本文讲**本机怎么把整条链路跑起来实现联调**，以及**日常 Git 怎么操作**。
> 业务代码怎么写见 `docs/FRAMEWORK_DEV_GUIDE.md`；Git 完整红线见 `docs/GIT_CONVENTIONS.md`。

---

## 0. 先建立认知：单环境，所有服务跑在同一台本机

已**取消 dev/formal 双环境**。全项目只保留一个环境：

| 维度 | 本机（单环境 local） |
|---|---|
| Nacos namespace | `edu-agent-local`（唯一） |
| Spring profile | `local` |
| 基础设施 | **Docker**：MySQL / Nacos / Redis / RabbitMQ / Chroma / AI |
| 6 个 Java 服务 | **本机 IDE 直跑**（gateway + auth/learning/resource/teacher/code） |
| 内网 | 所有服务注册进同一 `edu-agent-local` 命名空间即视为同一内网 |
| 密钥 | 本地默认值即可（同一份 `JWT_SECRET` 全网共享） |
| 对外入口 | 网关 `http://localhost:8080` |
| AI 服务 | Docker 内 FastAPI `:8001`（默认 mock LLM，无需 API key） |

**核心原则**：代码一份，环境唯一。`application.yml` 已默认 `namespace=edu-agent-local` / `profile=local`，IDE 直跑即可，一般不设环境变量。

---

## 1. 本机起环境（一次性）

### 1.1 前置

- **JDK 17+**（本机 21，编译 `release 17`，运行 17+ 均可）。
- **Maven**，指向本地仓库 `D:\software\apache-maven-3.9.4\mvn_repo`（配 `settings.xml` 的 `<localRepository>`，或每次 `-Dmaven.repo.local=...`）。
- **Docker Desktop**（跑基础设施）。

### 1.2 起基础设施 + 推配置

```bash
cd deploy
docker compose up -d                 # mysql/nacos/redis/rabbitmq/chroma/ai，等 mysql、nacos healthy
python deploy/push-nacos-config.py    # 创建 edu-agent-local 并推送各服务配置
```

`docker-compose.yml` 已把 `database/init-microservice.sql` 挂到 MySQL 初始化：**5 个库（auth_db/learning_db/resource_db/teacher_db/code_db）自动建好**，无需手跑；表由各服务 Flyway 迁移脚本建。

### 1.3 起 6 个 Java 服务

在 IDE 里分别 Run `EduAgentGateway`、`EduAgentAuth`、`EduAgentLearning`、`EduAgentResource`、`EduAgentTeacher`、`EduAgentCode`（按需，联调哪些起哪些）。

或用 mvn 起单个（示例，带本地仓库 + 当前分支模块）：

```bash
mvn -Dmaven.repo.local=D:/software/apache-maven-3.9.4/mvn_repo ^
    -pl edu-agent-code -am spring-boot:run
```

**关键环境变量**：`application.yml` 已默认 `NACOS_NAMESPACE=edu-agent-local`、`SPRING_PROFILES_ACTIVE=local`。只有中间件不在本机时才需覆盖：

```bash
set NACOS_ADDR=127.0.0.1:8848
set DB_HOST=127.0.0.1
set MQ_HOST=127.0.0.1
set AI_HOST=127.0.0.1
```

联调入口统一走网关：`http://localhost:8080/api/<服务>/...`。

---

## 2. 本机联调注意点（坑位清单）

| 坑 | 现象 / 解决 |
|---|---|
| 服务起不来 / 缺配置 | 配置没推到 Nacos → 先 `python deploy/push-nacos-config.py` |
| 忘了起基础设施 | 连不上 3306/8848 → `docker compose up -d` 并等 healthy |
| MySQL 缺库 | 报 `Unknown database 'xxx_db'` → 确认 compose 挂了 `init-microservice.sql`（外部库需手动执行一次） |
| namespace 错了 | 连到 `public` / 其它 → 显式 `NACOS_NAMESPACE=edu-agent-local` |
| 8080 被占用 | 网关起不来 → 关占用程序或改 `server.port` |
| JWT 报 401 / 验签失败 | 各服务 JWT_SECRET 不一致 → 全网同一份 |
| AI 接口 502 | `/api/edu-agent-ai/**` 须 AI 容器在（`:8001`）；没起不报错于 Java 服务 |
| 测试账号丢失 | `teststudent` 必须保留，DataInitializer 会重建 |

---

## 3. 常见疑问

**Q：本地能不能只跑我改的那个服务，其他用共享环境？**
A：不建议。跨机器/跨环境混跑会因服务发现重复注册而随机路由到错误实例。本机全起想省内存就**只起要联调的某几个**，但都落在 `edu-agent-local`。

**Q：Flyway 脚本每次都跑吗？**
A：不，只跑一次（记录于 `flyway_schema_history`）。**已执行的脚本禁止修改**，要改表就新增 `V{n+1}__*.sql`。

**Q：要改 common / gateway 怎么办？**
A：非架构批准不得改（见 GIT_CONVENTIONS §3 红线 2）。有公共能力需求先提给架构（WYC）评估，统一在集成线落地。

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
- 模块负责人：common/gateway/auth/code/teacher → WYC；resource → 陈嘉成；learning/ai → 陈海洋；web → 曾姿妍。
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
