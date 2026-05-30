# 管理后台后端 API 文档

> 基础路径：`http://localhost:8080/api`

---

## 一、认证模块 `/auth`

### 1.1 登录

```
POST /auth/login
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### 1.2 注册

```
POST /auth/register
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |
| nickname | String | 否 | 昵称 |
| email | String | 否 | 邮箱 |
| role | String | 是 | 角色：student/teacher/admin |

### 1.3 退出登录

```
POST /auth/logout
```

需要 Token。

### 1.4 刷新 Token

```
POST /auth/refresh
```

需要 Token。

---

## 二、用户管理 `/admin/users`

所有接口需要 Admin 角色 Token。

### 2.1 用户列表

```
GET /admin/users?keyword=&role=&status=&page=1&pageSize=10
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 搜索关键词（用户名/昵称） |
| role | String | 否 | 角色筛选 |
| status | String | 否 | 状态筛选 |
| page | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页条数，默认 10 |

### 2.2 创建用户

```
POST /admin/users
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |
| nickname | String | 否 | 昵称 |
| email | String | 否 | 邮箱 |
| role | String | 是 | student/teacher/admin |
| status | String | 否 | active/inactive/locked |

### 2.3 更新用户

```
PUT /admin/users/{id}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| nickname | String | 否 | 昵称 |
| email | String | 否 | 邮箱 |
| phone | String | 否 | 手机号 |
| role | String | 否 | 角色 |

### 2.4 删除用户

```
DELETE /admin/users/{id}
```

### 2.5 切换用户状态

```
POST /admin/users/{id}/toggle
```

切换用户的启用/禁用状态。

---

## 三、角色管理 `/admin/roles`

### 3.1 角色列表

```
GET /admin/roles
```

### 3.2 创建角色

```
POST /admin/roles
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| roleName | String | 是 | 角色名称 |
| roleDesc | String | 否 | 角色描述 |
| permissions | List | 是 | 权限列表 |

### 3.3 更新角色

```
PUT /admin/roles/{id}
```

### 3.4 删除角色

```
DELETE /admin/roles/{id}
```

---

## 四、资源管理 `/admin/resources`

### 4.1 资源列表

```
GET /admin/resources?keyword=&type=&status=&page=1&pageSize=10
```

### 4.2 更新资源状态

```
POST /admin/resources/{id}/status
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | String | 是 | draft/published/archived |

---

## 五、课程管理 `/admin/courses`

### 5.1 课程列表

```
GET /admin/courses?keyword=&status=&page=1&pageSize=10
```

### 5.2 更新课程状态

```
POST /admin/courses/{id}/status
```

---

## 六、数据统计 `/admin/statistics`

### 6.1 统计概览

```
GET /admin/statistics
```

### 6.2 用户增长

```
GET /admin/statistics/user-growth?period=month
```

### 6.3 学习数据

```
GET /admin/statistics/learning?period=week
```

### 6.4 导出报表

```
POST /admin/statistics/export
```

---

## 七、系统设置 `/admin/settings`

### 7.1 获取设置

```
GET /admin/settings
```

### 7.2 更新设置

```
PUT /admin/settings
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| settings | Map | 是 | 键值对配置 |

---

## 八、操作日志 `/admin/logs`

### 8.1 日志列表

```
GET /admin/logs?page=1&pageSize=10
```

---

## 九、备份管理 `/admin`

### 9.1 创建备份

```
POST /admin/backup
```

### 9.2 备份列表

```
GET /admin/backups
```

### 9.3 恢复备份

```
POST /admin/backup/{id}/restore
```

### 9.4 下载备份

```
GET /admin/backup/{id}/download
```

### 9.5 删除备份

```
DELETE /admin/backup/{id}
```

---

## 十、Agent 管理 `/admin/agents`

### 10.1 Agent 列表

```
GET /admin/agents?keyword=&status=&type=
```

返回 3 个 Agent：ProfileAgent（学生画像）、TutorAgent（智能辅导）、QuizAgent（智能出题）。

### 10.2 保存 Agent 配置

```
PUT /admin/agents/{id}
```

### 10.3 切换 Agent 状态

```
POST /admin/agents/{id}/status
```

---

## 十一、内容审核 `/admin/reviews`

### 11.1 审核列表

```
GET /admin/reviews?keyword=&status=&type=&riskLevel=
```

### 11.2 审核通过

```
POST /admin/reviews/{id}/approve
```

### 11.3 审核拒绝

```
POST /admin/reviews/{id}/reject
```

---

## 十二、技术架构

| 层次 | 技术 |
|------|------|
| 框架 | Spring Boot 3.4.1 |
| Java | JDK 17+（兼容至 25） |
| 数据库 | MySQL 8.0 + JPA/Hibernate |
| 鉴权 | JWT（jjwt 0.12.6） |
| 密码加密 | BCrypt |
| 连接池 | HikariCP |
| API 前缀 | `/api`（context-path） |

### 项目结构

```
edu-agent-server/src/main/java/com/eduagent/
├── common/           # 通用类：Result、PageResult
├── config/           # 配置：SecurityConfig、WebConfig
├── controller/       # 接口层（11 个 Controller）
├── dto/              # 数据传输对象
├── entity/           # 数据库实体
├── repository/       # JPA Repository
├── security/         # JWT 过滤器、鉴权切面
└── service/          # 业务逻辑层
```

