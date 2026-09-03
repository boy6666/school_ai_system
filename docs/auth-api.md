---
# EduAgent Auth 服务接口文档（鉴权）

> 服务名：`edu-agent-auth` ｜ 服务端口：`8081` ｜ 数据源：`auth_db`
> 来源：基于 `feat/auth` 分支 `edu-agent-auth` 实际代码生成（controller / dto / vo / entity / mapper / service）。
> 维护：吴友诚（auth）

---

## 1. 通用约定

### 1.1 地址前缀

- 服务监听端口 `8081`，接口统一挂在网关 `8080`，完整路径为 `http://<网关>/api/edu-agent-auth/...`。
- 5 个端点：登录 / 注册 / 令牌轮换 / 登出 / 引导完成标记。

### 1.2 鉴权模型（重要）

| 端点 | 网关白名单 | 说明 |
|---|---|---|
| `/login` `/register` | ✅ 匿名 | 无需 token，签发 JWT |
| `/refresh` `/logout` `/onboard-done` | ❌ 须登录 | 需携带有效 JWT 通过网关验签 |

- 网关 `AuthGlobalFilter` 是**唯一验签点**，auth 与网关共用 common `JwtUtil` 与同一 `JwtUtil.secret`（Nacos `edu-agent.jwt.secret`），不一致会导致网关 401「未认证或令牌无效」。
- 经网关验签后，`X-User-Id` / `X-User-Roles` 被注入下游头，服务内 `AuthContext` 读取；`logout` / `onboard-done` 从 `AuthContext` 取当前用户。

### 1.3 统一响应体 `Result<T>`

```json
{ "code": 0, "message": "success", "data": { } }
```

- 成功：`code=0`；业务失败：`code` 非 0（400 参数 / 401 未认证 / 403 无权限 / 404 不存在 / 409 冲突 / 500 系统异常）。
- 登录失败、令牌失效均为 `401`；用户名已存在为 `409`；标记引导时用户不存在为 `404`。

### 1.4 鉴权成功后的调用方式

登录/注册返回的 `token` 是后续所有接口的凭证，放入请求头：

```
Authorization: Bearer <token>
```

---

## 2. 认证

### 2.1 `POST /api/edu-agent-auth/login` — 登录（匿名）

```json
{ "username": "student01", "password": "student123" }
```

- `username` / `password` 必填非空。
- 流程：按 `username` 且 `status='active'` 查用户 → BCrypt 比对密码 → 更新 `last_login_time` → 签发 JWT。
- 用户名不存在或密码错误均返回 `401`「用户名或密码错误」（不区分，防枚举）。
- 返回 `LoginVO`：`{ token, userInfo }`。

**返回示例**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userInfo": {
      "id": 1, "username": "student01", "nickname": "小明",
      "email": "xm@example.com", "phone": null, "avatar": null,
      "role": "student", "onboarded": 0, "status": "active",
      "createTime": "2026-09-03T21:00:00", "lastLoginTime": "2026-09-03T21:00:00"
    }
  }
}
```

### 2.2 `POST /api/edu-agent-auth/register` — 注册（匿名）

```json
{
  "username": "student01",
  "password": "student123",
  "nickname": "小明",
  "email": "xm@example.com",
  "role": "student"
}
```

- `username`（3-20 字符）、`password`（6-30 字符）必填；`email` 需邮箱格式；`nickname` / `email` / `role` 可选，`role` 缺省 `student`。
- 用户名已存在返回 `409`「用户名已存在」；`password` 以 BCrypt 哈希落库（明文不存）。
- 落库默认 `status='active'`、`onboarded=0`；注册成功即自动签发 token（免二次登录）。

### 2.3 `POST /api/edu-agent-auth/refresh` — 令牌轮换

```json
{ "token": "eyJhbGciOiJIUzI1NiJ9..." }
```

- `token` 必填，可带 `"Bearer "` 前缀（服务端剥掉）。
- 校验该 token 有效 → 重新签发。token 无效或过期返回 `401`「登录已失效或已过期」。
- ⚠ 该端点**不在网关口白名单**，调用前须带任一有效 token 通过网关验签（因此它更接近"已登录用户的令牌更新"，而非"过期后的免密续期"）。

---

## 3. 账户

### 3.1 `POST /api/edu-agent-auth/logout` — 登出

- 无需请求体。JWT 无状态，服务端仅记录日志，**登出由前端删除本地 token 完成**。
- 当前用户从 `AuthContext`（网关注入 `X-User-Id`）取，未登录（未带 token）返回 `401`。

### 3.2 `POST /api/edu-agent-auth/onboard-done` — 标记引导完成

- 无需请求体。将当前用户 `onboarded` 置为 `1`。
- **前端 OnboardOverlay 依赖此标记**：完整走完引导后调用；用户不存在返回 `404`。

---

## 4. JWT 细节

| 项 | 值 |
|---|---|
| 算法 | HS256（jjwt 0.12.x） |
| secret | `edu-agent.jwt.secret`（Nacos 配置，≥32 字节；网关/所有服务必须一致） |
| 有效期 | `edu-agent.jwt.expiration`（毫秒，默认 86400000 = 1 天） |
| subject | 用户 ID（Long） |
| claim `roles` | 逗号隔开的 `ROLE_*`（student→`ROLE_STUDENT`、teacher→`ROLE_TEACHER`、admin→`ROLE_ADMIN`） |

- `logout` / `onboard-done` 等需登录端点，其鉴权与身份注入由网关完成，auth 内部 `SecurityConfig` 对所有请求 `permitAll`（不再二次验签）。

---

## 5. OpenAPI

- 本接口对应的 OpenAPI 3.0 见 `docs/auth-api.yaml`。
- 其余模块契约：teacher → `docs/teacher-api.yaml`；code → `docs/code-api.yaml`。
