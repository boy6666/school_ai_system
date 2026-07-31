# P0 子 Spec：基础设施 + 公共模块 + auth + Gateway + JWT 透传

> 阶段：P0 ｜ 负责人：**吴友诚（架构/地基）** ｜ 状态：开发中（主蓝图 §12.1 已分配）
> 本文是把主蓝图 §1/§2/§3 中 P0 部分展开为**可直接开发的文档**：需求 → 接口契约 → 数据模型 → 关键实现 → 测试 → 验收。
> 这是所有人的基线：陈嘉成/陈海洋/曾姿妍 在 P0 完成后才能开始各自模块。

---

## 1. 目标与范围

### 1.1 交付物（P0 必须产出）
1. 一个 `docker-compose.yml` 一键拉起全部基础设施容器（MySQL / Redis / RabbitMQ / Chroma / Nacos / SkyWalking）。
2. `common` 公共 Maven 模块（被后续所有 Java 服务依赖）。
3. `auth-service`：注册/登录/JWT/RBAC，库 `auth_db`。
4. `gateway-service`：路由 + JWT 全局过滤 + Sentinel 限流，对前端暴露 8080。
5. **JWT 跨服务透传端到端跑通**：用户登录 → 网关校验 → 下游服务能拿到身份。
6. SkyWalking agent 挂到 gateway/auth，OAP+UI 能看到一条跨服务链路。

### 1.2 不在本 spec 范围
- 各业务服务（learning/resource/code/teacher）逻辑 → 见各自子 spec。
- RAG 索引、代码沙箱、教师/管理前端 → 后续阶段。
- K8s、生产级多节点 Nacos 集群 → 后续演进。

### 1.3 前置条件
- 仅需要：**Docker + Docker Compose**（本机或单台云 VM，≥16GB 内存）。
- Java 17、Maven 3.9+、Node（前端暂不涉及）。

---

## 2. 基础设施容器编排（docker-compose）

### 2.1 服务清单与版本（建议，可按实际微调）

| 容器 | 镜像 | 端口(宿主) | 关键配置 |
|------|------|------|------|
| mysql | `mysql:8.0` | 3306 | `MYSQL_ROOT_PASSWORD`、utf8mb4、auth plugin=mysql_native_password；healthcheck `mysqladmin ping` |
| redis | `redis:7` | 6379 | 默认；healthcheck `redis-cli ping` |
| rabbitmq | `rabbitmq:3-management` | 5672 / 15672 | 管理插件；healthcheck `rabbitmq-diagnostics` |
| chroma | `chromadb/chroma:0.5.5` | 8000 | 向量库；无需账号（后续 ai-service 连） |
| nacos | `nacos/nacos-server:v2.3.2` | 8848 / 9848 | **standalone 模式** `MODE=standalone`；建议开 `NACOS_AUTH_ENABLE=true` |
| skywalking-oap | `apache/skywalking-oap-server:9.7.0` | 11800 / 12800 | 存储用内置 H2（演示够用） |
| skywalking-ui | `apache/skywalking-ui:9.7.0` | 8081→8080 | `SW_OAP_ADDRESS` 指向 oap |

> 说明：Chroma/Nacos/SkyWalking 镜像版本请以各服务接入时的最新稳定版为准；端口冲突时改宿主端口。

### 2.2 compose 要点
- 统一 `networks: edu-net`（服务名即主机名，供 Nacos/Feign 互访）。
- 统一 `volumes` 持久化 MySQL/Redis/RabbitMQ/Nacos 数据，避免重启丢数据。
- 各容器加 `healthcheck` + `depends_on: condition: service_healthy`，保证启动顺序。
- Nacos 必须在 gateway/auth **之前** healthy（gateway 启动要能连上注册中心）。
- 本地演示用 `docker-compose up -d`；启动脚本 `start-docker.bat` 同步更新覆盖新服务。

---

## 3. 公共模块 `common`

> 所有 Java 服务 `pom.xml` 依赖 `com.eduagent:common`，避免重复代码。

### 3.1 内容清单
| 类/包 | 职责 |
|------|------|
| `com.eduagent.common.Result<T>` | 统一响应 `{code,message,data}` |
| `com.eduagent.common.PageResult<T>` | 分页响应 |
| `com.eduagent.common.BusinessException` | 业务异常（带 code） |
| `com.eduagent.common.GlobalExceptionHandler` | `@RestControllerAdvice` 统一异常→Result |
| `com.eduagent.common.JwtUtil` | 签发/解析/校验 JWT（**算法与 Gateway 过滤器必须一致**） |
| `com.eduagent.common.AuthContext` | 当前请求 userId/roles 解析与读取（ThreadLocal / 请求属性） |
| `com.eduagent.common.config.*` | Feign/Redis/RabbitMQ/Sentinel/MyBatis-Plus 公共自动配置 |
| `com.eduagent.common.constant.RoleConstants` | `ROLE_STUDENT / ROLE_TEACHER / ROLE_ADMIN` |

### 3.2 关键约定
- JWT 密钥：放 Nacos 配置或环境变量 `JWT_SECRET`，**auth 与 gateway 必须同一值同一算法（HS256）**。
- `AuthContext` 由「AuthContextFilter（每个服务 once-per-request）」从请求头 `X-User-Id` / `X-User-Roles` 填充；服务内任意处 `AuthContext.getUserId()` 读取。
- Feign 客户端配置 `AuthFeignInterceptor`：发出服务间调用时，从 `AuthContext` 读取并注入 `X-User-Id` / `X-User-Roles` 头，保证透传（见 §6）。

---

## 4. auth-service

### 4.1 职责
注册、登录、JWT 签发、RBAC 角色维护。库 `auth_db`。

### 4.2 库表（DDL，MySQL 8 / utf8mb4）
```sql
CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4;
USE auth_db;

CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) UNIQUE NOT NULL,
  password VARCHAR(100) NOT NULL,      -- BCrypt 哈希
  real_name VARCHAR(64),
  status TINYINT DEFAULT 1,            -- 1启用 0禁用
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE roles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(32) UNIQUE NOT NULL,   -- ROLE_STUDENT / ROLE_TEACHER / ROLE_ADMIN
  name VARCHAR(32) NOT NULL
);
CREATE TABLE role_user (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (role_id) REFERENCES roles(id)
);
-- 初始化数据：插入 3 个角色；插入 admin 与测试账号，密码 BCrypt 预生成
```

### 4.3 API 契约（OpenAPI，供全员对齐）
| 方法 | 路径 | 说明 | 请求 | 响应 |
|------|------|------|------|------|
| POST | `/api/auth/register` | 注册（指定角色） | `{username,password,realName,role}` | `Result<userinfo>` |
| POST | `/api/auth/login` | 登录 | `{username,password}` | `Result<{token, userId, roles, realName}>` |
| POST | `/api/auth/refresh` | 刷新 token | `{token}` | `Result<{token}>` |
| GET | `/api/auth/me` | 当前用户（需登录） | Header `Authorization: Bearer <token>` | `Result<userinfo>` |

> 路径前缀 `/api/auth/**` 由网关路由到 auth-service（见 §5）。

### 4.4 关键实现
- 登录：`UserDetails` 校验密码（BCrypt）→ `JwtUtil.generate(userId, roles)` → 返回 token。
- 注册：密码 BCrypt 加密；默认角色可指定（学生自助注册=`ROLE_STUDENT`，教师/管理员由初始化或管理员创建）。
- **移除硬编码**：删除现有 `EduAgentApplication` 里「启动强制重置 admin 密码」的逻辑；改为 `auth_db` 初始化脚本/种子设定默认账号（安全 + 可维护）。
- 角色以 `ROLE_xxx` 字符串存入 JWT 的 `roles` claim（数组）。

---

## 5. gateway-service

### 5.1 职责
- 统一入口（对前端 8080），按路径路由到各服务（经 Nacos 服务发现）。
- **JWT 全局过滤器**：校验 `Authorization` 头，解析出 userId/roles，注入 `X-User-Id` / `X-User-Roles` 请求头后转发下游。
- Sentinel 限流（网关层）。

### 5.2 路由表（初版，随服务增加扩展）
| 断言/路径 | 目标服务(Nacos 名) |
|------|------|
| `/api/auth/**` | `auth-service` |
| `/api/learning/**` | `learning-service`（P1 接入） |
| `/api/resource/**` | `resource-service`（P1 接入） |
| `/api/code/**` | `code-service`（P2 接入） |
| `/api/teacher/**` | `teacher-service`（P3 接入） |
| `/api/ai/**` | `ai-service`（P1 接入，Python） |

> 路由用 **Nacos 服务发现**（lb://service-name），不用硬编码 IP。

### 5.3 JWT 全局过滤器（核心，伪代码）
```java
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {
  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    // 登录/注册等白名单路径直接放行
    if (isPublicPath(exchange.getRequest().getURI().getPath())) return chain.filter(exchange);
    if (auth == null || !auth.startsWith("Bearer ")) return unauthorized(exchange);
    try {
      Claims c = JwtUtil.parse(auth.substring(7));          // 与 auth-service 同密钥同算法
      String uid = c.getSubject();
      List<String> roles = c.get("roles", List.class);
      ServerHttpRequest mutated = exchange.getRequest().mutate()
          .header("X-User-Id", uid)
          .header("X-User-Roles", String.join(",", roles))
          .build();
      return chain.filter(exchange.mutate().request(mutated).build());
    } catch (Exception e) {
      return unauthorized(exchange);
    }
  }
}
```
> 安全要点：下游服务**只信任网关注入的 `X-User-Id/X-User-Roles` 头**，且应在网络层限制下游端口仅网关可访问（或下游也校验 Bearer，双保险）。**禁止**前端直接带 `X-User-*` 头绕过——网关对入站请求应剥离/忽略外部传入的这两个头，只相信自己解析 JWT 后注入的。

### 5.4 Sentinel 限流（网关层）
- 引入 `spring-cloud-starter-alibaba-sentinel` + `spring-cloud-alibaba-sentinel-gateway`。
- 对高代价路由（如 `/api/code/**`、AI 生成）配置网关流控规则（QPS 阈值），超限返回 429。
- 规则可放 Nacos 动态配置，后续 P4 固化。

---

## 6. JWT 跨服务透传（P0 最关键交付，必须端到端验证）

### 6.1 两跳透传模型
```
浏览器 ──Bearer token──▶ Gateway
  Gateway: 校验 JWT → 注入 X-User-Id / X-User-Roles ──▶ 下游服务A (从请求头读取→AuthContext)
  服务A ──Feign调用──▶ 服务B: AuthFeignInterceptor 从 AuthContext 取出 → 注入 X-User-* ──▶ 服务B
```
- **第 1 跳（外部→网关→服务A）**：由 Gateway 的过滤器和反向代理完成（§5.3）。
- **第 2 跳（服务A→服务B，Feign）**：由 `common` 的 `AuthFeignInterceptor` 完成，把当前 `AuthContext` 里的身份写入 outgoing 请求头，服务B 收到后同样由 `AuthContextFilter` 读入。

### 6.2 各服务统一动作
- 每个服务都加 `AuthContextFilter`（once-per-request）：读 `X-User-Id`/`X-User-Roles` → 填 `AuthContext`。
- 每个服务都加 `AuthFeignInterceptor`：发 Feign 时把 `AuthContext` 写入头。
- 控制器通过 `AuthContext.getUserId()` / `getRoles()` 取身份，不再自己解析 JWT。

### 6.3 安全与坑
- Gateway、auth、各服务的 **JWT 密钥与算法必须完全一致**（建议统一从 Nacos 取）。
- 时钟：JWT 有效期用相对时间（exp），服务间无需严格时钟同步，但建议容器内 NTP。
- 防伪造：下游端口仅允许网关网段访问；Gateway 必须**丢弃/覆盖**入站请求自带的 `X-User-*` 头。
- 协议：内部服务间建议也走 HTTPS/或至少网络隔离（演示期 HTTP 可接受，需注明）。

---

## 7. Nacos 配置

- 每个服务独立 **namespace**（如 `edu-dev`）与 **group**（`DEFAULT_GROUP` 或按服务分组），配置互不干扰。
- 典型配置项（示例）：
  - `auth-service`：`jwt.secret`、`jwt.expire-seconds`、`spring.datasource.*`
  - `gateway-service`：`jwt.secret`、路由/限流规则
  - 公共：`redis.host/port`、`rabbitmq.addresses`、`skywalking.backend`
- 服务注册：每个服务 `spring.cloud.nacos.discovery.server-addr= nacos:8848`，`spring.application.name` = 服务名（与网关路由对应）。

---

## 8. SkyWalking 接入（P0 起步）

- 每个 Java 服务构建/运行时挂 agent：
  - 方式一：Dockerfile 内 `COPY agent /skywalking/agent`，`JAVA_OPTS=-javaagent:/skywalking/agent/skywalking-agent.jar -Dskywalking.agent.service_name=xxx -Dskywalking.collector.backend_service=skywalking-oap:11800`
  - 方式二：compose 用 `volume` 挂载 agent 目录 + 环境变量注入（便于不重建镜像调参）。
- P0 只需 gateway + auth 挂上，验证 OAP/UI 能看到「登录请求跨 gateway→auth」的链路与耗时。
- 采样率等后续 P4 再细化。

---

## 9. 端到端验证步骤（P0 验收脚本）
1. `docker-compose up -d` → 所有容器 healthy（尤其 nacos、mysql）。
2. 调 `POST /api/auth/login`（经网关 8080）→ 拿到 token。
3. 调 `GET /api/auth/me` 带 Bearer → 返回用户信息（证明网关校验 + AuthContext 生效）。
4. 在 auth-service 里临时一个调试端点打印 `AuthContext.getUserId()`，确认值 = 登录用户（证明透传）。
5. SkyWalking UI 看到一条 `gateway → auth-service` 的 trace。
6. 故意用错误/过期 token 调受保护接口 → 401（证明鉴权拦截）。

---

## 10. 测试

### 10.1 单测
- `JwtUtil` 签发/解析/过期/篡改 单测（auth 与 gateway 共用同一套基准）。
- `JwtAuthGlobalFilter` 白名单放行 / 无 token 拒 / 有效 token 注入头 / 篡改 token 拒。

### 10.2 集成
- 用 **Testcontainers** 起 MySQL + Nacos（或嵌 Nacos standalone），启动 gateway+auth，用 `WebTestClient` 跑 §9 的端到端断言。
- Feign 透传单测：mock 一个服务A→服务B 调用，断言 `X-User-*` 头被正确转发。

### 10.3 契约
- auth-service 的 OpenAPI（`/v3/api-docs`）作为契约基线，供陈嘉成/陈海洋/曾姿妍 前端与后续服务对齐（配合 Pact/Spring Cloud Contract 在 P3/P4 接入）。

---

## 11. 验收标准（Definition of Done）
- [ ] `docker-compose up -d` 一键起全部 6 类基础设施，全部 healthy。
- [ ] `common` 模块编译通过，被 gateway/auth 依赖。
- [ ] auth-service 注册/登录/签发 JWT 正常；默认账号由初始化脚本设定（无硬编码重置）。
- [ ] gateway 路由到 auth；受保护接口无 token → 401，有 token → 放行并注入身份头。
- [ ] **下游服务能经 `AuthContext` 拿到正确 userId/roles**（两跳透传验证通过）。
- [ ] SkyWalking 能看到一条跨服务链路。
- [ ] 上述端到端流程写进 `README` / 演示脚本，陈嘉成/陈海洋/曾姿妍 可据此开始各自模块。

## 12. 交接基线（给陈嘉成/陈海洋/曾姿妍）
- 服务发现地址、Nacos namespace/group 约定、JWT 密钥来源（Nacos）。
- auth-service OpenAPI 契约（登录/注册/me）。
- `common` 的 `AuthContext` 用法与 `AuthFeignInterceptor` 接入方式（每个新服务照抄这两段）。
- 网关路由命名规范（`/api/<服务>/**` → `<服务>-service`）。

---

*P0 子 spec 结束。下一项按主蓝图 §12.3 推进（建议 P1：learning/resource + 学生前端 + RAG 接入）。*
