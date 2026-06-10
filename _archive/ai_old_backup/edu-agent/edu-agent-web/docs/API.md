# EduAgent Web 接口文档

本文档根据前端 `src/api` 目录整理，用于后端对接与联调。

## 通用约定

- Base URL: `VITE_API_BASE_URL`，未配置时默认 `/api`
- 超时时间: `30000ms`
- 认证方式: 登录后前端会从 `localStorage.token` 读取 token，并在请求头中添加：

```http
Authorization: Bearer <token>
```

- 推荐统一响应格式：

```ts
interface ApiResult<T> {
  code: number
  message: string
  data: T
}
```

前端拦截器会在 `code === 200` 时返回 `data`，否则抛出错误。如果后端直接返回非上述结构，前端会原样返回响应体。

## 认证 Auth

| 功能 | 方法 | 路径 | 请求参数 | 返回 |
| --- | --- | --- | --- | --- |
| 登录 | `POST` | `/auth/login` | `{ username: string; password: string }` | `{ token: string; userInfo: any }` |
| 退出登录 | `POST` | `/auth/logout` | 无 | - |
| 注册 | `POST` | `/auth/register` | `any` | - |
| 刷新登录态 | `POST` | `/auth/refresh` | 无 | - |

## 用户 User

| 功能 | 方法 | 路径 | 请求参数 | 返回 |
| --- | --- | --- | --- | --- |
| 获取用户信息 | `GET` | `/user/info` | 无 | 用户信息 |
| 更新用户信息 | `PUT` | `/user/info` | `any` | - |
| 上传头像 | `POST` | `/user/avatar` | `multipart/form-data: file` | - |
| 修改密码 | `POST` | `/user/change-password` | `any` | - |

## 学生端课程 Course

### 类型

```ts
type CourseStatus = 'not-started' | 'learning' | 'done'
type ChapterStatus = 'not-started' | 'learning' | 'done'

interface CourseListQuery {
  keyword?: string
  status?: string
}
```

| 功能 | 方法 | 路径 | 请求参数 | 返回 |
| --- | --- | --- | --- | --- |
| 课程列表 | `GET` | `/courses` | query: `CourseListQuery` | `{ list: CourseListItem[]; total: number }` |
| 课程详情 | `GET` | `/courses/{id}` | path: `id: string` | `CourseDetail` |
| 更新章节进度 | `POST` | `/courses/{courseId}/chapters/{chapterId}/progress` | `{ progress: number }` | `{ success: boolean }` |
| 保存课程笔记 | `POST` | `/courses/{courseId}/chapters/{chapterId}/note` | `{ content: string }` | `{ success: boolean }` |

## 学生端资源 Resource

### 类型

```ts
type ResourceSortType = 'hot' | 'new' | 'score'

interface ResourceListQuery {
  keyword?: string
  type?: string
  difficulty?: string
  courseId?: string
  sort?: ResourceSortType
  page?: number
  pageSize?: number
}
```

| 功能 | 方法 | 路径 | 请求参数 | 返回 |
| --- | --- | --- | --- | --- |
| 资源列表 | `GET` | `/resources` | query: `ResourceListQuery` | `{ list; total; recommended; hotTags }` |
| 资源详情 | `GET` | `/resources/{id}` | path: `id: number` | `ResourceDetailItem` |
| 相关资源 | `GET` | `/resources/{id}/related` | path: `id: number` | `ResourceListItem[]` |
| 收藏/取消收藏 | `POST` | `/resources/{id}/favorite` | `{ favorite: boolean }` | `{ favorite: boolean }` |
| 加入学习计划 | `POST` | `/resources/{id}/plan` | path: `id: number` | `{ success: boolean }` |

## 学生端学习任务 Task

```ts
type TaskStatus = 'todo' | 'doing' | 'done'
type TaskPriority = 'high' | 'middle' | 'low'

interface LearningTaskQuery {
  keyword?: string
  status?: string
  priority?: string
}
```

| 功能 | 方法 | 路径 | 请求参数 | 返回 |
| --- | --- | --- | --- | --- |
| 学习任务列表 | `GET` | `/student/tasks` | query: `LearningTaskQuery` | `{ summary; list }` |
| 更新任务状态 | `POST` | `/student/tasks/{id}/status` | `{ status: TaskStatus }` | `{ success: boolean }` |

## 学生端智能辅导 Tutor

| 功能 | 方法 | 路径 | 请求参数 | 返回 |
| --- | --- | --- | --- | --- |
| 获取会话 | `GET` | `/student/tutor/session` | 无 | `{ messages; suggestions }` |
| 发送消息 | `POST` | `/student/tutor/chat` | `{ content: string }` | `TutorMessage` |

## 学生端练习 Practice

| 功能 | 方法 | 路径 | 请求参数 | 返回 |
| --- | --- | --- | --- | --- |
| 练习列表 | `GET` | `/practice/list` | query: `any` | - |
| 练习详情 | `GET` | `/practice/{id}` | path: `id: number` | - |
| 提交答案 | `POST` | `/practice/submit` | `any` | - |
| 练习进度 | `GET` | `/practice/progress` | 无 | - |

## 学生端项目 Project

| 功能 | 方法 | 路径 | 请求参数 | 返回 |
| --- | --- | --- | --- | --- |
| 项目列表 | `GET` | `/project/list` | query: `any` | - |
| 项目详情 | `GET` | `/project/{id}` | path: `id: number` | - |
| 加入项目 | `POST` | `/project/{id}/join` | path: `id: number` | - |
| 提交项目 | `POST` | `/project/submit` | `any` | - |
| 项目进度 | `GET` | `/project/{id}/progress` | path: `id: number` | - |

## 学生端报告 Report

| 功能 | 方法 | 路径 | 请求参数 | 返回 |
| --- | --- | --- | --- | --- |
| 学习报告 | `GET` | `/report/learning` | 无 | - |
| 报告统计 | `GET` | `/report/statistics` | 无 | - |
| 对比数据 | `GET` | `/report/comparison` | 无 | - |
| 导出报告 | `GET` | `/report/export` | 无 | `blob` |

## 消息 Message

| 功能 | 方法 | 路径 | 请求参数 | 返回 |
| --- | --- | --- | --- | --- |
| 消息列表 | `GET` | `/message/list` | query: `any` | - |
| 消息详情 | `GET` | `/message/{id}` | path: `id: number` | - |
| 标记已读 | `POST` | `/message/{id}/read` | path: `id: number` | - |
| 全部已读 | `POST` | `/message/read-all` | 无 | - |
| 删除消息 | `DELETE` | `/message/{id}` | path: `id: number` | - |
| 发送消息 | `POST` | `/message/send` | `any` | - |
| 会话列表 | `GET` | `/message/conversations` | 无 | - |
| 会话消息 | `GET` | `/message/conversation/{id}` | path: `id: number` | - |

## 管理端系统设置

| 功能 | 方法 | 路径 | 请求参数 | 返回 |
| --- | --- | --- | --- | --- |
| 获取系统设置 | `GET` | `/admin/settings` | 无 | - |
| 更新系统设置 | `PUT` | `/admin/settings` | `any` | - |

## 管理端用户管理

| 功能 | 方法 | 路径 | 请求参数 | 返回 |
| --- | --- | --- | --- | --- |
| 用户列表 | `GET` | `/admin/users` | query: `any` | - |
| 创建用户 | `POST` | `/admin/users` | `any` | - |
| 更新用户 | `PUT` | `/admin/users/{id}` | `any` | - |
| 删除用户 | `DELETE` | `/admin/users/{id}` | path: `id: number` | - |
| 切换用户状态 | `POST` | `/admin/users/{id}/toggle` | path: `id: number` | - |

## 管理端角色权限

| 功能 | 方法 | 路径 | 请求参数 | 返回 |
| --- | --- | --- | --- | --- |
| 角色列表 | `GET` | `/admin/roles` | 无 | - |
| 创建角色 | `POST` | `/admin/roles` | `any` | - |
| 更新角色 | `PUT` | `/admin/roles/{id}` | `any` | - |
| 删除角色 | `DELETE` | `/admin/roles/{id}` | path: `id: number` | - |

## 管理端智能体

```ts
type AgentStatus = 'running' | 'stopped'

interface AdminAgentListQuery {
  keyword?: string
  status?: string
  type?: string
}
```

| 功能 | 方法 | 路径 | 请求参数 | 返回 |
| --- | --- | --- | --- | --- |
| 智能体列表 | `GET` | `/admin/agents` | query: `AdminAgentListQuery` | `{ list: AgentItem[]; total?: number }` |
| 保存智能体配置 | `PUT` | `/admin/agents/{id}` | `AgentItem` | `{ success: boolean }` |
| 更新智能体状态 | `POST` | `/admin/agents/{id}/status` | `{ status: AgentStatus }` | `{ success: boolean }` |

## 管理端课程与资源

```ts
type ManageStatus = 'published' | 'draft' | 'reviewing' | 'offline'

interface AdminManageListQuery {
  keyword?: string
  status?: string
  type?: string
  page?: number
  pageSize?: number
}
```

| 功能 | 方法 | 路径 | 请求参数 | 返回 |
| --- | --- | --- | --- | --- |
| 管理端课程列表 | `GET` | `/admin/courses` | query: `AdminManageListQuery` | `{ list: AdminCourseItem[]; total?: number }` |
| 管理端资源列表 | `GET` | `/admin/resources` | query: `AdminManageListQuery` | `{ list: AdminResourceItem[]; total?: number }` |
| 更新课程状态 | `POST` | `/admin/courses/{id}/status` | `{ status: ManageStatus }` | `{ success: boolean }` |
| 更新资源状态 | `POST` | `/admin/resources/{id}/status` | `{ status: ManageStatus }` | `{ success: boolean }` |

## 管理端内容审核

```ts
type ReviewStatus = 'pending' | 'approved' | 'rejected'
type RiskLevel = 'low' | 'middle' | 'high'

interface AdminReviewListQuery {
  keyword?: string
  status?: string
  type?: string
  riskLevel?: string
}
```

| 功能 | 方法 | 路径 | 请求参数 | 返回 |
| --- | --- | --- | --- | --- |
| 审核列表 | `GET` | `/admin/reviews` | query: `AdminReviewListQuery` | `{ list: ReviewItem[]; total?: number }` |
| 审核通过 | `POST` | `/admin/reviews/{id}/approve` | path: `id: number` | `{ success: boolean }` |
| 审核拒绝 | `POST` | `/admin/reviews/{id}/reject` | `{ reason: string }` | `{ success: boolean }` |

## 管理端数据统计

| 功能 | 方法 | 路径 | 请求参数 | 返回 |
| --- | --- | --- | --- | --- |
| 总览统计 | `GET` | `/admin/statistics` | 无 | - |
| 用户增长 | `GET` | `/admin/statistics/user-growth` | query: `any` | - |
| 学习数据 | `GET` | `/admin/statistics/learning` | query: `any` | - |
| 导出数据 | `POST` | `/admin/statistics/export` | `any` | `blob` |

## 管理端日志与备份

| 功能 | 方法 | 路径 | 请求参数 | 返回 |
| --- | --- | --- | --- | --- |
| 操作日志 | `GET` | `/admin/logs` | query: `any` | - |
| 创建备份 | `POST` | `/admin/backup` | 无 | - |
| 备份列表 | `GET` | `/admin/backups` | 无 | - |
| 恢复备份 | `POST` | `/admin/backup/{id}/restore` | path: `id: number` | - |
| 下载备份 | `GET` | `/admin/backup/{id}/download` | path: `id: number` | `blob` |
| 删除备份 | `DELETE` | `/admin/backup/{id}` | path: `id: number` | - |

