# EduAgent 团队联调上服务器手册（feat/teacher）

> 场景：把整栈（6 个 Java 微服务 + 全部中间件）部署到**一台常开的共享服务器**，
> 前端与各后端成员统一连它联调，不再各跑各的本机。
> 编排文件：`deploy/docker-compose.deploy.yml`（全容器栈）。
>
> ⚠️ **不要用根目录那份 `docker-compose.yml` / `README-交付说明.md`** —— 那是单体时代残留，与微服务骨架对不上。
> ⚠️ 本场景生产核对项见 `deploy/README.deploy.md §3`；以下是"联调就绪"的最小可执行流程。

---

## 0. 开机前最后检查（漏了会白忙，务必先在本地确认）

在叫任何人碰服务器之前，先确保以下三点，否则服务器上根本没有你的改动：

- [ ] **`feat/teacher` 已 PR 合入 `feature/microservice`**
      —— 服务器部署的是 `feature/microservice`（或个人分支已 rebase 到并包含 teacher 改动）。
      按 `docs/GIT_CONVENTIONS.md`，个人分支不得直推集成线，须走 PR。
- [ ] **本地构建能绿**：
      ```bash
      mvn -B -ntp -Dmaven.repo.local=D:/software/apache-maven-3.9.4/mvn_repo clean verify
      ```
- [ ] **选定部署路线**：明确告诉服务器运维用 `deploy/docker-compose.deploy.yml`，
      提醒他**忽略**根目录旧 `docker-compose.yml`。

---

## 1. 服务器端一次性初始化

```bash
sudo apt update
sudo apt -y install docker.io docker-compose-plugin git python3 curl
sudo systemctl enable --now docker
sudo usermod -aG docker $USER     # 之后 exit 重登录生效
docker --version && docker compose version
```

## 2. 拉代码 + 切到目标线

```bash
git clone <仓库地址> edu-agent && cd edu-agent
git checkout feature/microservice     # ← 必须包含教师端改动
```

## 3. 配密钥（必须改，防伪造身份）

```bash
cd deploy && cp .env.example .env
openssl rand -base64 48        # 第 1 行 → JWT_SECRET
nano .env                      # 至少改：JWT_SECRET、MYSQL_ROOT_PASSWORD
```

> `.env` 含密钥，已 gitignore，勿提交。所有 Java 服务（含网关）共享同一 `JWT_SECRET`，
> 不一致会导致网关 401「未认证或令牌无效」。

## 4. 起基建 + 建库（顺序关键，等 healthy）

```bash
cd deploy
docker compose -f docker-compose.deploy.yml up -d mysql nacos redis rabbitmq chroma ai
docker compose -f docker-compose.deploy.yml ps    # 等 mysql、nacos 都 healthy
```

## 5. 推 Nacos 配置（漏了 Java 服务起不来）

```bash
cd E:\college_information\edu-agent
python deploy/push-nacos-config.py
```

## 6. 起全部 Java 服务（首次 build 慢，几分钟）

```bash
cd deploy
docker compose -f docker-compose.deploy.yml up -d
```

## 7. 验证

```bash
docker compose -f docker-compose.deploy.yml ps                                  # 全 green
curl -s http://localhost:8080/actuator/health                                     # 网关探活
docker compose -f docker-compose.deploy.yml logs -f teacher                       # 看 teacher 注册
docker compose -f docker-compose.deploy.yml logs -f <服务名>                      # 任一服务日志
```

---

## 8. 给联调同事的地址清单（直接转发）

| 用途 | 地址 |
|---|---|
| **团队联调主入口（网关）** | `http://<服务器IP>:8080/api/<服务>/...` |
| 前端页面（若起了 web） | `http://<服务器IP>` |
| Nacos 控制台 | `http://<服务器IP>:8848/nacos`（账号 `nacos/nacos`） |
| RabbitMQ 控制台 | `http://<服务器IP>:15672`（guest/guest） |
| 直连 MySQL 调试（需开放端口） | `http://<服务器IP>:3306` |
| 判分/聊天 AI（**必须配真实 LLM key，无 mock 分支**） | 网关 `/api/edu-agent-ai/**` |

> AI 服务的 key 只有一个来源：Nacos 配置 `edu-agent-ai.yaml` 的 `env.OPENAI_API_KEY`。
> **Nacos 配置会覆盖容器环境变量**（`nacos_registry.apply_config_to_env()` 直接写 `os.environ`），
> 所以 compose 里传 `OPENAI_API_KEY` 是无效的；留空则所有 AI 接口返回
> `HTTP 200 + code 500 + "LLM 调用失败…"`（`llm_client` 失败不抛异常，靠前缀识别）。

教师端接口契约：`docs/teacher-api.yaml`（六模块 30 操作）。

AI 服务接口契约：`docs/ai-api.yaml`（4 端点；双入参形态、`Result` 信封、HTTP 200 + 非 0 code 的错误语义、
网关逐条枚举与已知缺口都在里面）。字段级对接细节另见 `edu-agent-ai/docs/java-python-contract.md`。

**给前端（曾姿妍）**：nginx 代理指向服务器网关 `:8080`；确认网关 CORS；
前端地址是 web profile，需 `docker compose -f docker-compose.deploy.yml --profile web up -d web`。

**给后端成员（陈海洋/陈嘉成）**：统一连这台服务器，**不要本机混跑** —— 跨机器混跑会因
服务发现重复注册而随机路由到错误实例。

---

## 9. 日常升级 / 重启

```bash
cd deploy
docker compose -f docker-compose.deploy.yml build teacher        # 只重打 teacher 镜像
docker compose -f docker-compose.deploy.yml up -d                # 重新拉起
docker compose -f docker-compose.deploy.yml logs -f <svc>        # 看日志
# 若 nacos-config 有改动：重跑 push-nacos-config.py → restart 对应服务
```

教师端改完后典型发布循环：
```bash
git push origin feat/teacher        # 日常推到个人分支
# PR 合入 feature/microservice 后：
# 服务器 cd edu-agent && git pull
cd deploy && docker compose -f docker-compose.deploy.yml build teacher
docker compose -f docker-compose.deploy.yml up -d
```

---

## 10. 排错速查

| 现象 | 原因 / 解决 |
|---|---|
| Java 服务一直 `restarting` | 没推 Nacos 配置（§5）→ 重推；或 `logs -f <svc>` 看缺哪个配置 |
| 网关 401「未认证或令牌无效」 | JWT_SECRET 不一致 → 所有服务同一份 |
| DB 连不上 | 容器内用 `DB_HOST=mysql`（compose 已设），别用 `127.0.0.1` |
| 服务报 `Unknown database` | mysql 没 healthy 就起了服务 → 等 healthy 后 `restart <svc>` |
| 前端 404 / 打不开 | web 在 `--profile web`，默认没启 → 显式 `up -d web` |
| 构建极慢 / 卡 maven | 首次正常；可 `build <svc>` 只构建改动的服务 |
| 改了 nacos-config 没生效 | 重跑 `push-nacos-config.py` → `restart` 对应服务 |
| 端口冲突 | `sudo ss -ltnp` 排查宿主机占用 3306/8080 等 |

---

## 11. 安全收口（正式对外前才需要，联调内网可暂缓）

- ufw 只开 22，其余 deny（隧道出站 / 或内网仅开放 8080）。
- 3306/8848/5672/6379/8000 不对外；只暴露网关 8080（+ 前端 80）。
- JWT_SECRET / MYSQL_ROOT_PASSWORD 用强随机。
- CORS 网关收紧为前端真实域名，不要 `*`。
