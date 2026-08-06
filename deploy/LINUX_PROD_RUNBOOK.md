# ⚠️ 已废弃：Linux 生产部署手册

> **本文内容已不再适用**：项目已取消 dev/prod 双环境，统一为本机单环境（local）。
> 所有开发/联调请见 `docs/dev-prod-guide.md` 与 `deploy/README.md`。
> （本文件保留仅作历史参考，勿再按它部署。）

---

# EduAgent Linux 生产部署手册（纯 CLI，无宝塔）[历史存档]

> 适用：另一台 Linux 机器（双系统 / 独立机），Ubuntu 22.04 LTS 推荐。
> 全程命令行，不安装任何面板（无 宝塔 / 无 cPanel）。
> 对外只走 **Cloudflare 隧道**（出站连接，无需开放任何入站端口）。

---

## 0. 架构速览（部署视角）

```
                    Cloudflare 隧道（出站，不开放入站）
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                                            ▼
  网关 gateway:8080  ──────────────        web:80（前端，可选 profile）
        │  /api/** 路由 + JWT 校验 + 注入 X-User-Id/X-User-Roles
        ├── auth(8081)   learning(8082)   resource(8083)
        ├── teacher(8084) code(8085)      ai(8001, FastAPI)
        │
  基础设施（仅内网，绝不对外）：
   mysql(3306)  nacos(8848)  redis(6379)  rabbitmq(5672/15672)  chroma(8000)
```

- **DB-per-service**：1 个 MySQL 容器 + 5 个逻辑库（auth_db / learning_db / resource_db / teacher_db / code_db），各服务独立 datasource，禁止跨库外键。
- **Nacos 双重角色**：服务发现 + 配置中心。各 Java 服务启动后先到 Nacos 拉取自己的 data-id。
- **鉴权链路**：auth 签发 JWT → gateway 校验并注入 Header → 下游 `AuthContextFilter` 读入 → Feign `AuthFeignInterceptor` 转发。
- **ai 例外**：FastAPI 挂在根路径（`/chat`、`/resource/generate`…），网关对其 `StripPrefix=2`。

---

## 1. 硬件 / 系统要求

| 项目 | 最低 | 推荐 |
|---|---|---|
| 内存（RAM） | 16 GB | 32 GB |
| 磁盘 | 30 GB 空闲 SSD | 50 GB+ 空闲 SSD |
| CPU | 4 核 | 8 核 |
| 系统 | Ubuntu 20.04+ | **Ubuntu 22.04 LTS** |
| 网络 | 能联网拉镜像 | 能联网拉镜像 + ai 出网（用真模型时） |

> 内存是主要瓶颈，不是硬盘。空闲约 6–10 GB，构建/压测峰值 12–16 GB+。16 GB 能跑；32 GB 才舒服。

---

## 2. 系统初始化（一次性）

```bash
# 用 root 或 sudo 用户登录后
sudo apt update && sudo apt -y full-upgrade
sudo apt -y install docker.io docker-compose-plugin git python3 python3-pip curl openssl ufw

# 开机自启 Docker
sudo systemctl enable --now docker

# 把当前用户加入 docker 组，免 sudo（改完需重登录）
sudo usermod -aG docker $USER

# 验证
docker --version
docker compose version
```

> 重登录使 docker 组生效：`exit` 再登回，或 `newgrp docker`。

### 2.1 防火墙（关键：只开 SSH，其余全关）

隧道是**出站**的，Linux 机器不需要对任何外部 IP 开放端口。把入站全部关掉，只留 SSH：

```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp          # 仅 SSH
sudo ufw enable
sudo ufw status
```

> 不要在防火墙上开放 3306 / 8848 / 5672 / 6379 / 8000 / 8080 / 8081-8085。它们只在 Docker 内网里互通，外部一律不可达。

---

## 3. 获取代码

```bash
git clone https://github.com/boy6666/school_ai_system.git
cd school_ai_system
git checkout feature/microservice
```

> 后续升级：在该目录 `git pull` → 重新 build → 重新 up（见 §9）。

---

## 4. 配置强密钥（必须改）

```bash
cd deploy
cp .env.example .env
```

生成强随机密钥，分别用于 JWT 和 admin 初始口令：

```bash
openssl rand -base64 48      # 复制第一行 → 作为 JWT_SECRET
openssl rand -base64 24      # 复制 → 作为 AUTH_BOOTSTRAP_ADMIN_PASSWORD
```

编辑 `.env`：

```bash
nano .env
```

至少改这两项（其余保持默认即可，详见 §4.1）：

```dotenv
JWT_SECRET=把上面第一个输出贴这里
AUTH_BOOTSTRAP_ADMIN_PASSWORD=把上面第二个输出贴这里
```

### 4.1 `.env` 各变量说明

| 变量 | 默认 | 改不改 | 说明 |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `formal` | 不改 | Linux 生产固定 formal |
| `NACOS_NAMESPACE` | `edu-agent-formal` | 不改 | 生产命名空间 |
| `MYSQL_ROOT_PASSWORD` | `edu_agent_root` | 建议改 | MySQL root 口令 |
| `DB_USER` / `DB_PASSWORD` | `edu_agent` | 建议改 | 业务库账号 |
| `MQ_USER` / `MQ_PASSWORD` | `edu_agent` | 建议改 | RabbitMQ 账号 |
| `JWT_SECRET` | `please-change-...` | **必须改** | 强随机；泄漏=可伪造任意身份 |
| `AUTH_BOOTSTRAP_ADMIN_PASSWORD` | `please-change-admin` | **必须改** | 首次启动给 admin 账号设的口令 |
| `NACOS_USERNAME` / `NACOS_PASSWORD` | `nacos` | 先不改 | 当前 Nacos 鉴权关闭，脚本直推 |

> `.env` 含密钥，**切勿提交进 git**（`.env` 已被忽略）。只在本机持有。

---

## 5. 构建镜像

```bash
cd deploy
docker compose build
```

- 首次较慢：每个 Java 服务都在 maven 容器里从根 pom 做一次 `mvn -pl <module> -am`（重新拉依赖、编译）。
- 只改某服务可单独构建，例如：`docker compose build auth`。
- 宿主机**不需要**装 JDK / Maven / Node，全部在构建容器里完成（根 `Dockerfile` 用 `ARG MODULE`，`edu-agent-web` 用自带 `Dockerfile`，`edu-agent-ai` 用 Python 镜像）。

---

## 6. 起基础设施

```bash
docker compose up -d mysql nacos redis rabbitmq chroma
docker ps
```

- `mysql` 首次启动会自动执行 `database/init-microservice.sql` → 建 5 个库 + `edu_agent@'%'` 账号。
- 等 **nacos** 状态变 `healthy`（约 20–40 秒）。可轮询：

```bash
watch -n 3 'docker inspect -f "{{.State.Health.Status}}" edu-agent-nacos'
```

---

## 7. 推送 Nacos 配置（关键一步，漏了服务起不来）

```bash
NACOS_ADDR=127.0.0.1:8848 python3 push-nacos-config.py
```

- 脚本会创建 `edu-agent-dev` / `edu-agent-formal` 两个 namespace，并把 `nacos-config/*.yaml` 推到各 data-id。
- Java 服务依赖这些配置（datasource、JWT 公钥等），**没推就起会一直重启失败**。`restart: unless-stopped` 会在配置就绪后自动恢复。

---

## 8. 起全部服务

```bash
docker compose up -d
docker ps
```

### 8.1 前端（等 web 模块交付后）

`web` 在 `--profile web` 下，默认不启：

```bash
docker compose --profile web up -d web
```

> 若 web 未就绪，先不启；网关 `:8080` 单独可用，可直接拿 Postman / 前端调试。

### 8.2 可选 APM（SkyWalking）

```bash
docker compose --profile skywalking up -d     # 控制台 http://<host>:8088
```

---

## 9. 验证

```bash
# 容器全绿
docker ps

# 网关探活
curl -s http://localhost:8080/actuator/health

# 看网关日志（看路由注册、JWT 链路）
docker compose logs -f gateway

# 看某服务日志（如 auth 起不来）
docker compose logs -f auth
```

冒烟路径：用 admin 账号（bootstrap 口令已在 §4 设）通过 gateway 登录 → 拿到 JWT → 调 `/api/auth/**` 等接口。

### 9.1 升级流程（后续每次改代码）

```bash
cd ~/school_ai_system
git pull
cd deploy
docker compose build
docker compose up -d
# 若 nacos-config 有改：重新跑 push-nacos-config.py，再 restart 对应服务
```

---

## 10. Cloudflare 隧道（对外发布）

隧道是**出站**连接：你的机器主动连 Cloudflare，外部用户经 Cloudflare 反向打进来。**不需要在防火墙上开放任何端口**，也不需要公网 IP。

### 10.1 安装 cloudflared

```bash
# Ubuntu
curl -L --output cloudflared.deb https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb
sudo dpkg -i cloudflared.deb
cloudflared --version
```

### 10.2 方式 A：临时隧道（最快，URL 每次变）

```bash
cloudflared tunnel --url http://localhost:8080
```

终端会打印一个 `https://xxxx.trycloudflare.com`，直接能用。进程在就在线；关掉终端/进程就下线。

> 想让它在后台一直跑（不因 SSH 断开而退出）：
> ```bash
> nohup cloudflared tunnel --url http://localhost:8080 > /var/log/cf-tunnel.log 2>&1 &
> ```
> 但这仍是临时 URL（重启变）。长期用方式 B。

### 10.3 方式 B：命名隧道（稳定 URL，推荐生产）

```bash
# 1) 登录 Cloudflare（浏览器换取 token）
cloudflared tunnel login

# 2) 建隧道（取个名，如 edu-agent）
cloudflared tunnel create edu-agent

# 3) 写 ingress 配置（/root/.cloudflared/config.yml）
cat > /root/.cloudflared/config.yml <<'EOF'
tunnel: edu-agent
credentials-file: /root/.cloudflared/<你的隧道id>.json

ingress:
  - hostname: api.你的域名.com
    service: http://localhost:8080        # 网关：所有 /api/**
  - hostname: 你的域名.com
    service: http://localhost:80          # 前端（若启了 web profile）
  - service: http://localhost:8080        # 兜底：没配 host 也走网关
EOF

# 4) 让 Cloudflare 接管你的域名 DNS（一条 CNAME 自动加）
cloudflared tunnel route dns edu-agent api.你的域名.com
cloudflared tunnel route dns edu-agent 你的域名.com

# 5) 跑（后台常驻）
cloudflared tunnel run edu-agent
```

- 访问：浏览器开 `https://api.你的域名.com` → 网关；`https://你的域名.com` → 前端。
- CORS（网关配置里）应改成你的前端真实域名，别留 `*`。

### 10.4 把隧道做成系统服务（开机自启、掉线重连）

```bash
sudo cloudflared service install
sudo systemctl enable --now cloudflared
sudo systemctl status cloudflared
```

> 用 `config.yml`（方式 B）时，`service install` 会自动读它。**临时隧道（方式 A）不适合常驻**，仅用于临时联调。

---

## 11. 安全收口清单

| 项 | 状态 / 动作 |
|---|---|
| 防火墙 | ufw 仅开 22，其余 deny（§2.1） |
| 端口暴露 | 3306/8848/5672/6379/8000/8081-8085 **不对外**；只 tunnel → 8080 / 80 |
| `JWT_SECRET` | 强随机，已改（§4） |
| `AUTH_BOOTSTRAP_ADMIN_PASSWORD` | 强口令，已改（§4）；首次登录后请改 admin 口令 |
| MySQL / RabbitMQ 口令 | 建议改默认（§4.1） |
| Nacos 鉴权 | 当前 `NACOS_AUTH_ENABLE=false`（便于脚本直推）；生产建议开启，并给 `push-nacos-config.py` 补 token 逻辑 |
| CORS | 网关改为前端真实域名，不要 `*` |
| `.env` | 含密钥，已在 gitignore，**不提交** |
| 系统更新 | 定期 `sudo apt update && sudo apt -y upgrade` |

---

## 12. 真·讯飞星火接入（可选，默认 mock）

生产 compose 里 `ai` 服务默认 `USE_MOCK_LLM=1`（无需 key 即可跑通）。要接真实模型：

1. 在 `edu-agent-ai/.env`（或 compose 的 `ai` 服务 environment）设置：
   ```dotenv
   MOCK_LLM=0
   OPENAI_BASE_URL=https://spark-api-open.xf-yun.com/v1
   LLM_MODEL=lite
   OPENAI_API_KEY=你的讯飞星火APIKey
   ```
2. 重启 ai 服务：
   ```bash
   docker compose up -d ai
   ```

> AI 服务需要**出网**到 `spark-api-open.xf-yun.com`；隧道只管入站，出网不受影响（默认 allow outgoing）。

---

## 13. 日常运维

```bash
cd deploy

docker compose ps                       # 状态
docker compose logs -f <svc>           # 看日志（gateway/auth/learning/resource/teacher/code/ai/web）
docker compose restart <svc>            # 单服务重启
docker compose up -d                   # 配置/镜像变了重新拉起
docker compose down                    # 全停（数据在 volume，不丢）
docker compose down -v                 # 全停并删 volume（⚠️ 会清库，慎用）
```

资源占用查看：

```bash
docker stats
df -h
free -h
```

---

## 14. 排错

| 现象 | 原因 / 解决 |
|---|---|
| Java 服务一直 `restarting` | 多半**没推 Nacos 配置**（§7）。推完会自动恢复；或 `docker compose logs <svc>` 看缺哪个配置 |
| 服务报 `Unknown database` / 连不上 mysql | mysql 还没 healthy 就起了服务；等 healthy 后 `docker compose restart <svc>` |
| `docker: command not found` / 权限拒绝 | `$USER` 没进 docker 组，重登录或 `sudo usermod -aG docker $USER && newgrp docker` |
| 构建极慢 / 卡在 maven 拉包 | 首次正常；确认能联网。可 `docker compose build <svc>` 只构建改动服务 |
| ai 返回 mock 内容 | 默认 `USE_MOCK_LLM=1`；要真模型见 §12 |
| 前端打不开 / 404 | `web` 在 `--profile web`，默认没启；确认 `docker compose --profile web up -d web` |
| 隧道连上但 502 | gateway 没起好或 Nacos 没推配置；查 `docker compose logs gateway` |
| 端口冲突 | 确认宿主机没别的程序占用 3306/8080 等；用 `sudo ss -ltnp` 排查 |
| chroma 镜像拉不到 | `ghcr.io/chroma-core/chroma:0.5.5` 可能下线；按官方最新 tag 改 compose |
| 改了 nacos-config 没生效 | 重跑 `push-nacos-config.py`，再 `docker compose restart <对应服务>` |

---

## 15. 一键速查（标准起服顺序）

```bash
# 首次部署
sudo apt -y install docker.io docker-compose-plugin git python3 curl openssl ufw
sudo systemctl enable --now docker
sudo usermod -aG docker $USER        # 重登录

sudo ufw default deny incoming && sudo ufw default allow outgoing
sudo ufw allow 22/tcp && sudo ufw enable

git clone https://github.com/boy6666/school_ai_system.git
cd school_ai_system && git checkout feature/microservice
cd deploy && cp .env.example .env     # 改 JWT_SECRET / AUTH_BOOTSTRAP_ADMIN_PASSWORD

docker compose build
docker compose up -d mysql nacos redis rabbitmq chroma   # 等 nacos healthy
NACOS_ADDR=127.0.0.1:8848 python3 push-nacos-config.py
docker compose up -d
docker compose --profile web up -d web                    # web 就绪后

# 最后起 Cloudflare 隧道（§10 方式 B 推荐生产）
```

---

文档配套文件：`deploy/docker-compose.yml`、`deploy/.env.example`、`deploy/push-nacos-config.py`、`deploy/nacos-config/*.yaml`、根 `Dockerfile`、`database/init-microservice.sql`。完整开发与联调说明另见 `docs/dev-prod-guide.md`。
