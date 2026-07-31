# EduAgent 前端开发文档（子 spec · 成员D 曾姿妍 · 纯前端）

> 阶段：贯穿 P1（学生端对接微服务）→ P3（教师端 + 管理端治理/监控/审计）
> 负责人：**曾姿妍（成员D，纯前端）** ｜ 状态：开发中 ｜ 关联主蓝图 §5、§9、§12.1
> 本文把主蓝图 §5（前端架构）、§9（可观测）、§5.5（零 mock）展开为「页面/路由需求 → API 契约对齐 → 状态/类型模型 → 实现要点 → 联调/测试 → 验收」的可落地文档。
>
> ⚠️ 依赖前置：本文 §3–§5 的字段级契约以 **P0 网关路由（已锁定：`/api/<服务>/**`）** 为基线；**teacher-service / code-service / ai-service 的子 spec 尚未生成**（截至撰写时 `docs/superpowers/specs/` 仅有 design + p0 两份）。因此 §4 教师端、§5 治理页中**按主蓝图 DDL（classes/questions/assignments/grades、code_submissions/code_check_reports）与服务描述给出的端点为"建议契约"，待陈海洋（teacher）、吴友诚（code/ai）子 spec 定稿后，仅改 `api/*.ts` 的 path/类型，页面零改动**。前端 TS 类型最终由 OpenAPI（`/api/<服务>/v3/api-docs`）生成（见 §7.4）。

---

## 1. 前端总览

### 1.1 工程结构确认（以哪份为准）

仓库里存在两份疑似前端工程，经核对 `vite.config.ts` / `package.json` / `package-lock.json` 引用关系与文件修改时间，**唯一正确工程为 `edu-agent-web/`**：

```
edu-agent-web/                      # ✅ 正确工程根（vite.config.ts / package.json / lock 在此）
├── vite.config.ts                  # @ 别名 → src；dev proxy /api → 8080(网关)
├── package.json                    # 依赖：vue3/ts/vite/element-plus/pinia/vue-router/echarts/axios/marked/markmap 等
└── src/                            # ✅ 唯一 source root（@ 别名指向此处）
    ├── main.ts  App.vue  style.css
    ├── router/index.ts             # 三角色路由 + 守卫
    ├── utils/request.ts            # axios 封装（baseURL=/api，token 注入）
    ├── utils/markdown.ts
    ├── stores/user.ts  stores/index.ts(Pinia)
    ├── layouts/StudentLayout.vue  AdminLayout.vue   （TeacherLayout 见 §4）
    ├── components/                 # HelloWorld / MermaidRenderer（新增 CodeEditor / BaseChart 见 §3.4/§5.3）
    ├── api/                        # auth/learning/resource/code/teacher/ai（按服务分模块，见 §3）
    ├── views/student/  views/admin/   （teacher 见 §4，admin/govern 见 §5）
    └── views/Login.vue  Register.vue  AdminLogin.vue
```

> ❗ **清理动作（不阻塞开发，建议提交时一并删）**：`edu-agent-web/src/edu-agent-web/` 为**陈旧重复副本**（其 `package.json` 依赖更少、无 `__tests__`、文件停留在 6 月），与正确工程无关，应删除，避免 CI 误打包或队友混淆。**本文所有路径均指 `edu-agent-web/src/**`。**

### 1.2 技术栈

| 类 | 选型 | 说明 |
|------|------|------|
| 框架 | Vue 3 + TypeScript + Vite | SFC `<script setup lang="ts">`；`@` = `src` |
| UI | Element Plus + `@element-plus/icons-vue` | 中文 locale（main.ts 已挂 `zhCn`） |
| 状态 | Pinia | `stores/user.ts`（已存在），新增 `stores/teacher.ts` / `stores/admin.ts` / `stores/monitor.ts`（§6） |
| 路由 | vue-router 4 | `createWebHistory()`；三角色守卫（§1.4） |
| 图表 | **ECharts 6**（已装） | 学情图/监控图统一封装 `BaseChart`（§5.3） |
| 代码编辑 | **Monaco Editor（⚠️ 待安装）** | `monaco-editor` 尚未进入 `package.json`，§3.4 给出安装与封装 |
| HTTP | axios | `utils/request.ts` 统一封装（§1.3） |
| 测试 | Vitest + @vue/test-utils | `src/views/__tests__` 已存在；零 mock（§2.4） |

### 1.3 `utils/request.ts` 改造（baseURL=/api、token 注入、401 跳登录、Result 解包）

现有实现已做 `Result{code,message,data}` 解包与 `Authorization: Bearer` 注入，但**缺 401 统一处理**且依赖散落。改造目标：网关统一返回 `common.Result<T>`（`{code,message,data}`）与 `PageResult<T>`（`{records,total,page,pageSize}`），拦截器只解包 `data`/`PageResult` 整对象，并在 `code===401` 或 HTTP 401 时清 token 跳登录。

```ts
// utils/request.ts
import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

/** 后端 common.Result<T> */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}
/** 后端 common.PageResult<T>（分页接口 data 即此对象） */
export interface PageResult<T = unknown> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',  // 永远只打网关 8080
  timeout: 120000,
})

// —— 请求拦截：自动带 token ——
request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.set('Authorization', `Bearer ${token}`)
  return config
})

// —— 响应拦截：Result 解包 + 401 跳登录 ——
request.interceptors.response.use(
  (resp) => {
    const body = resp.data
    // 网关/服务统一包了 Result
    if (body && typeof body === 'object' && 'code' in body && 'data' in body) {
      const r = body as ApiResult
      if (r.code === 401) { clearAndGoLogin(); return Promise.reject(new Error(r.message || '未登录')) }
      if (r.code !== 200) { ElMessage.error(r.message || '请求失败'); return Promise.reject(new Error(r.message || '请求失败')) }
      return r.data as any          // 页面拿到的是 data（含 PageResult 整对象）
    }
    return body                     // 文件下载等裸流
  },
  (error) => {
    if (error?.response?.status === 401) clearAndGoLogin()
    else ElMessage.error(error?.response?.data?.message || error.message || '网络异常')
    return Promise.reject(error)
  },
)

function clearAndGoLogin() {
  localStorage.removeItem('token'); localStorage.removeItem('roles'); localStorage.removeItem('role')
  // 用 location 跳转避免与 router 循环依赖；守卫会拦截到 /login 或 /admin/login
  if (!location.pathname.startsWith('/admin/login') && location.pathname !== '/login') {
    location.href = location.pathname.startsWith('/admin') ? '/admin/login' : '/login'
  }
}

export default request
```

> 循环依赖说明：`request.ts` 直接 `import router` 易与 `router/index.ts` 形成环。推荐 **`clearAndGoLogin` 用 `location.href`**（上例）或发一个事件由 `main.ts` 监听，二者都避免环。

### 1.4 三角色路由隔离方案（守卫，扩展教师端）

沿用现有「`localStorage.role` + 守卫」思路，但**升级为 `roles` 数组**（P0 返回 `roles: string[]`，如 `['ROLE_STUDENT']`；`RoleConstants`：`ROLE_STUDENT/ROLE_TEACHER/ROLE_ADMIN`，见 P0 §3.1）。守卫规则：
- `/login` `/register` `/admin/login`：白名单放行。
- `/student/**`：需登录；`ROLE_ADMIN` 访问 → 跳 `/admin/dashboard`；`ROLE_TEACHER` 访问 → 跳 `/teacher/dashboard`。
- `/teacher/**`：需登录且含 `ROLE_TEACHER`；否则按最高角色分流。
- `/admin/**`（除 `/admin/login`）：需登录且含 `ROLE_ADMIN`；否则跳对应角色首页。
- 未知已认证路径：按 `roles` 首位角色跳对应首页。

```ts
// router/index.ts（守卫核心补充，配合 §6 的 useAuth 解析）
function roleOf(): string[] {
  try { return JSON.parse(localStorage.getItem('roles') || '[]') } catch { return [] }
}
const is = (r: string) => roleOf().includes(r)

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (['/login', '/register', '/admin/login'].includes(to.path)) return true
  if (!token) return to.path.startsWith('/admin') ? '/admin/login' : '/login'

  if (to.path.startsWith('/admin') && !is('ROLE_ADMIN')) return is('ROLE_TEACHER') ? '/teacher/dashboard' : '/student/dashboard'
  if (to.path.startsWith('/teacher') && !is('ROLE_TEACHER')) return is('ROLE_ADMIN') ? '/admin/dashboard' : '/student/dashboard'
  if (to.path.startsWith('/student') && is('ROLE_ADMIN')) return '/admin/dashboard'
  return true
})
```

> 部署约束（nginx）：`createWebHistory()` 需 nginx `try_files $uri /index.html;`，**三个角色路由共用一份 SPA**，由前端守卫隔离，禁止服务端按角色拆站。

### 1.5 与网关的对接方式（唯一入口 8080）

- 开发：`vite.config.ts` 已配 `proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } }`。**删除现有的 `/ai` 直连代理**（它 `target:8000` 直打 ai-service，绕过网关，违反"只走网关"）。AI 一律走 `/api/ai/**`，由网关路由到 ai-service。
- 联调后端未就绪：Prism/Mockoon 桩**监听 8080 模拟网关**（契约见 §2），`VITE_API_BASE_URL` 保持 `/api`，**前端零改动**。
- 生产：nginx 反代 `/api` → 网关 8080；前端静态资源走 nginx。

---

## 2. 零 mock 策略落地（主蓝图 §5.5，前端执行细则）

根因：mock 写在组件 → 遗忘删除 → 魔法数字。硬规则：**假数据永不进组件**。三类兜底，优先级从高到低。

### 2.1 主：真实后端 + DB 种子数据（首选）
- P0 骨架一好即调真接口；"像样数据"由 **Flyway/SQL 种子**灌入真实 DB（在 SQL 里，不在前端）。
- 种子约定（与后端对齐）：`auth_db` 给 1 管理员 + N 学生 + M 教师（BCrypt 预生成密码）；`learning_db`/`resource_db`/`teacher_db`/`code_db` 给≥3 班级、≥20 学生、≥50 题目、≥10 资源、≥5 条 `code_submissions`。种子名形如 `V1__seed_*.sql`，可审可清。
- 前端只调接口拿数据，**不写 `const mockData=[...]`、不写魔法数字阈值**（图表阈值/颜色从后端返回或集中常量文件 `utils/constants.ts` 管理，且该文件只放"结构"，不放"业务样例数据"）。

### 2.2 兜底：网络层 stub（Prism / Mockoon），契约一致
- 后端某服务未好时，用 **Prism（`npx @stoplight/prism-cli mock <openapi.yaml>`）或 Mockoon** 按 OpenAPI 起独立 HTTP 服务在 **8080**（即模拟网关），前端只切 `baseURL`（其实默认就是 `/api`），**组件零改动、零 mock 代码**。
- 桩必须与真实契约一致：字段名、类型、`Result` 包装、`PageResult` 结构。P3/P4 移除桩 = 关进程，前端代码无残留。
- CI/prod **强制关闭 stub**：`VITE_API_BASE_URL` 指向真实网关；构建期可加断言 `import.meta.env.DEV` 才允许桩。

### 2.3 防漂移：契约测试
- 后端 OpenAPI（`/api/<服务>/v3/api-docs`）作为契约基线（P0 §10.3）。
- 前端用 **openapi-typescript 生成 `types/api/*.ts`**（§7.4），类型漂移在 `tsc`/构建期即报错。P3/P4 接 Pact / Spring Cloud Contract。

### 2.4 Code Review 检查点（禁 mock）
PR 合并前逐项核对：
1. 组件/store 内禁止出现字面量 `mock`/`fake`/`dummy` 数组或对象作数据源；禁止 `setTimeout` 伪造接口返回。
2. 所有请求经 `@/utils/request`，path 以 `/api/<服务>/` 开头；**不得出现 `baseURL:'/ai'`、直连 `:8001/:8082` 等绕过网关的 axios 实例**（现有 `profile.ts` 的 `aiClient` 必须改，见 §3.3）。
3. 图表阈值/枚举集中到 `types/` 或后端返回；不得组件内写死"分数线=60"等业务魔法数。
4. `localStorage` 仅存 `token`/`roles`/`userInfo` 快照；不得存业务列表数据当缓存绕过接口。
5. 测试用 `@vue/test-utils` + `vi.mock('@/utils/request')` 注入**契约 stub**，不写组件内 mock。

---

## 3. 学生端调整（P1）

### 3.1 登录/注册改造（对接 P0 契约）
P0 契约（§4.3）：`POST /api/auth/login {username,password}` → `Result<{token, userId, roles:string[], realName}>`；`POST /api/auth/register {username,password,realName,role}`；`GET /api/auth/me`（带 Bearer）→ `Result<userinfo>`。

**改动点：**
- `api/auth.ts`：`/auth/login` → `/api/auth/login`；新增 `getMe()` → `/api/auth/me`；`logout` → `/api/auth/logout`；`refresh` → `/api/auth/refresh`。
- `stores/user.ts`：`setUserInfo` 改存 `roles: string[]`（来自 `loginResponse.roles`），移除单 `role` 字符串（兼容：保留 `role` = 首位角色便于旧守卫过渡，但守卫最终读 `roles`）。
- `views/Login.vue`：`handleLogin` 中 `res` 已是 `data`（`request` 已解包），取 `res.token / res.roles / res.realName`；按 `roles.includes('ROLE_ADMIN')` 分流首页；`onboarded` 取自 `me` 或 login 的 `userInfo.onboarded`。
- `StudentLayout.vue` 引导判断：`userInfo.onboarded === 0` 显示 `OnboardOverlay`（保持现有逻辑，字段对齐 `me` 返回）。

```ts
// api/auth.ts（改造后）
import request from '@/utils/request'
export interface LoginParams { username: string; password: string }
export interface LoginResult { token: string; userId: number; roles: string[]; realName: string; onboarded: number }
export const login = (p: LoginParams) => request.post<LoginResult>('/api/auth/login', p)
export const register = (p: { username: string; password: string; realName: string; role: string }) =>
  request.post('/api/auth/register', p)
export const getMe = () => request.get<UserInfo>('/api/auth/me')
export const logout = () => request.post('/api/auth/logout')
```

### 3.2 现有页面 → 微服务接口映射表（learning/resource/ai）

下表左侧为现有 `api/*.ts` 中的旧 path（相对 baseURL，实际为 `/旧path`），右侧为对齐网关后的新 path。`request.ts` 已带 `/api` 前缀，故新 path 写全称 `/api/<服务>/...`。

| 现有模块 | 旧 path | 新 path（网关） | 归属服务 | 备注 |
|------|------|------|------|------|
| auth | `/auth/login` `/auth/register` `/auth/refresh` | `/api/auth/login` `/api/auth/register` `/api/auth/refresh` | auth | 加 `/api` |
| learning | `/learning/log` `/learning/summary` `/learning/path` `/learning/regenerate` `/learning/task/:id` `/learning/goal` `/learning/history` `/learning/daily-trend` `/learning/evaluation` `/learning/init` | 全部 `/api/learning/...` 同名 | learning | 加 `/api/learning` |
| learning | `/student/learning-path/current` `/student/learning-path/generate` `/student/learning-path/task` | `/api/learning/learning-path/current` `/generate` `/task` | learning | 规范化 |
| resource | `/resources/chapter/:id` `/resources/chapter/:id/:type` `/resources/:id` `/resources/generate` `/resources/:id/regenerate` `/resources/:id/feedback` | **`/api/resource/...`**（注意**单数 resource**！网关路由为 `/api/resource/**`） | resource | 关键：复数→单数 |
| tutor | `/tutor/chat` | **`/api/ai/chat`** | ai | 对话走 ai-service |
| tutor | `/tutor/history` `/tutor/sessions` | `/api/learning/conversations` `/api/learning/conversations/sessions` | learning | 会话存 learning_db |
| tutor | `/tutor/explain` | `/api/ai/explain`（或 `/api/learning/explain`，待吴友诚契约） | ai/learning | 讲解由 AI 出 |
| tutor | `/quiz/answered` `/quiz/wrong-questions` `/quiz/wrong-questions/:id` | `/api/learning/quiz/answered` `/api/learning/quiz/wrong-questions` `/:id` | learning | quiz_records 在 learning_db |
| course | `/courses` `/courses/:id` `/courses/:id/chapters/:cid/progress` `/courses/:id/chapters/:cid/note` | `/api/learning/courses` `/:id` `/:id/chapters/:cid/progress` `/:id/chapters/:cid/note` | learning（待陈海洋契约；若 P3 课程归 teacher-service 则仅改 path） | 见 §4 |
| task | `/student/tasks` `/student/tasks/:id/status` | `/api/learning/tasks` `/:id/status` | learning | |
| practice | `/practice/list` `/practice/:id` `/practice/submit` `/practice/progress` | `/api/learning/practice/list` `/:id` `/submit` `/progress` | learning | |
| report | `/report/learning` `/report/statistics` `/report/comparison` `/report/export` | `/api/learning/report/learning` `/statistics` `/comparison` `/export` | learning | export 用 `responseType:'blob'` |
| notes | `/notes/categories` `/notes` `/notes/:id` | `/api/learning/notes/categories` `/notes` `/:id` | learning | |
| project | `/project/list` `/project/:id` `/project/:id/join` `/project/submit` `/project/:id/progress` | `/api/learning/project/list` `/:id` `/:id/join` `/submit` `/:id/progress` | learning（如后期归 resource 仅改 path） | |
| message | `/message/list` `/message/:id` `/message/:id/read` `/message/read-all` `/message/:id`(del) `/message/send` `/message/conversations` `/message/conversation/:id` | `/api/learning/message/...` 同名 | learning（通知如独立服务仅改 path） | |
| user | `/user/info` `/user/info`(put) `/user/avatar` `/user/change-password` | `/api/auth/user/info` `/api/auth/user/info` `/api/auth/user/avatar` `/api/auth/user/change-password` | auth | 用户信息归 auth |
| **profile（⚠️ 整改）** | `aiClient.post('/profile/build')` `aiClient.get('/profile/:id')` | **`/api/ai/profile/build`** `/api/ai/profile/:id` | ai | **删除 rogue `aiClient`（baseURL:/ai）** |
| profile | `/profile/:id` `/profile`(post) | `/api/learning/profile/:id` `/api/learning/profile` | learning | 画像读取/存储走 learning |
| dashboard（散落在 `Dashboard.vue` 的硬编码） | `/dashboard/summary` `/dashboard/tasks` `/dashboard/path` `/dashboard/evaluation` `/dashboard/ai-summary` `/dashboard/learning-review` | `/api/learning/dashboard/summary` `/tasks` `/path` `/evaluation`；AI 类 → `/api/learning/dashboard/ai-summary` `/learning-review`（归属 learning-service，见陈海洋 A.2.5；ai-service 无 `/api/ai/dashboard/*` 端点） | learning | **移入 `api/learning.ts`，删除组件内硬编码** |

> 执行顺序：先批量改 `api/*.ts` 的 path（组件 import 不变），再处理 `Dashboard.vue` 内 6 处 `request.post('/dashboard/...')` 硬编码（抽到 `api/learning.ts` 的 `getDashboardSummary()` 等）。改完跑 `tsc` + 起网关联调。

### 3.3 ECharts 学情图 & Monaco 接入点

**ECharts（已集成，复用）：** `Report.vue` / `Statistics.vue` / `ProfileOverview.vue` 已用 ECharts。统一抽 `components/BaseChart.vue` 封装（见 §5.3），学生端 `Report.vue` 的"学习时长趋势/各模块时长"改用它，数据来自 `/api/learning/report/*`。

**Monaco（代码作业提交，对接 code-service）：** 现状 `package.json` **未装 `monaco-editor`**，现有代码编辑用 `el-input textarea`。P1/P2 需安装并封装：

```bash
# edu-agent-web/ 下
npm i monaco-editor
```

```vue
<!-- components/CodeEditor.vue（封装，供学生提交 & 教师布置代码题共用） -->
<script setup lang="ts">
import * as monaco from 'monaco-editor'
import { onMounted, onBeforeUnmount, ref, shallowRef } from 'vue'
const props = defineProps<{ modelValue: string; language?: string; readOnly?: boolean }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: string): void }>()
const el = ref<HTMLElement>()
const editor = shallowRef<monaco.editor.IStandaloneCodeEditor>()
onMounted(() => {
  editor.value = monaco.editor.create(el.value!, {
    value: props.modelValue, language: props.language ?? 'java', readOnly: props.readOnly,
    theme: 'vs-dark', automaticLayout: true, minimap: { enabled: false },
  })
  editor.value.onDidChangeModelContent(() => emit('update:modelValue', editor.value!.getValue()))
})
onBeforeUnmount(() => editor.value?.dispose())
</script>
<template><div ref="el" class="code-editor"></div></template>
<style scoped>.code-editor{height:360px;border:1px solid var(--hairline);border-radius:8px}</style>
```

**学生代码作业提交接入点**（对接 code-service，P2 服务就绪；P1 用 §2.2 stub 联调）：
```ts
// api/code.ts
import request from '@/utils/request'
export interface CodeSubmitReq { studentId: number; language: 'java'; sourceCode: string; assignmentItemId?: number }
export interface CodeCheckReport {
  submissionId: number; status: 0|1|2|3;            // 0待运行1成功2失败3超时
  stdout: string; stderr: string; runTimeMs: number
  compileOk: boolean; compileMsg: string
  checkstyle: unknown; pmd: unknown; aiSuggestion: string; overallScore: number
}
export const submitCode = (p: CodeSubmitReq) => request.post<CodeCheckReport>('/api/code/submit', p)
export const getCodeSubmission = (id: number) => request.get<CodeCheckReport>(`/api/code/submissions/${id}`)
```
学生端页面（新增 `views/student/CodePractice.vue` 或在 `Practice.vue` 内嵌 `CodeEditor`）：提交 → `submitCode` → 展示 `CodeCheckReport`（编译/静态检查/AI 建议/总分）。**AI 分析 `/api/ai/code/analyze` 为 code-service 内部调用，学生端不直接调。**

---

## 4. 教师端前端（P3）

> ⚠️ **契约状态：teacher-service / code-service 子 spec 尚未生成**。以下端点依据主蓝图 §3.3（teacher_db DDL：classes/class_students/questions/assignments/assignment_items/grades）、§7（code 判分回写 grades）、§12（陈海洋负责）整理为**建议契约**。定稿后仅改 `api/teacher.ts`/`api/code.ts` 的 path 与类型，页面零改。

### 4.1 新增 TeacherLayout + /teacher/* 路由与守卫
- 新增 `layouts/TeacherLayout.vue`（菜单：教学管理/班级、题库、作业、批改、学情看板、AI 助教、资源发布），风格沿用 `StudentLayout`。
- `router/index.ts` 增加 `teacher` 段（见 §1.4 守卫已含 `/teacher/**` 拦截）。
- 学生端"代码作业"结果经 `assignment.graded` MQ → teacher-service，教师端可复核/微调（见下 `grades`）。

### 4.2 教师端页面清单与端点对齐

| 页面（views/teacher/*） | 调用端点（建议契约，前缀 `/api`） | 归属 | 说明 |
|------|------|------|------|
| ClassManage（班级/学生） | `GET /teacher/classes?page&pageSize&keyword`；`POST /teacher/classes`；`GET /teacher/classes/:id`；`PUT /teacher/classes/:id`；`DELETE /teacher/classes/:id`；`GET /teacher/classes/:id/students`；`POST /teacher/classes/:id/students`；`DELETE /teacher/classes/:id/students/:sid` | teacher | classes/class_students |
| QuestionBank（题库） | `GET /teacher/questions?type&chapter&topic&page`；`POST /teacher/questions`；`PUT /teacher/questions/:id`；`DELETE /teacher/questions/:id` | teacher | questions 表 |
| Assignment（作业布置） | `GET /teacher/assignments?classId`；`POST /teacher/assignments`（含 items→questions）；`GET /teacher/assignments/:id`；`POST /teacher/assignments/:id/publish` | teacher | assignments/assignment_items |
| Grade（批改/复核） | `GET /teacher/assignments/:id/grades?studentId`；`GET /teacher/grades/:id`；`PUT /teacher/grades/:id`（教师微调 score/评语）；`POST /teacher/assignments/:id/grade`（触发/重跑 code 判分） | teacher+code | 代码作业判分回写 grades |
| Analytics（班级学情看板，ECharts） | `GET /teacher/classes/:id/analytics`（维度：完成率/平均分/薄弱点分布）；`GET /teacher/dashboard`（教师首页统计） | teacher（聚合 learning+code） | 数据来自 MQ 同步的 study.progress/assignment.graded |
| AiTutor（AI 助教对话） | `POST /api/ai/chat`（复用学生端 tutor，context 带 teacher 身份）；`POST /api/ai/resource/generate`（生成班级讲义） | ai | 复用 ai-service |
| ResourcePublish（资源发布） | `POST /api/resource/generate`（生成）→ `POST /api/teacher/resources/publish`（发布到班级） | resource+teacher | 资源生成走 ai，发布走 teacher |

**关键页面调用示例（Analytics 学情看板）：**
```vue
<!-- views/teacher/Analytics.vue（片段） -->
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { get } from '@/utils/request'      // 或 api/teacher.ts 的 getAnalytics
import BaseChart from '@/components/BaseChart.vue'
const route = useRoute()
const analytics = ref<{
  completionRate: number
  avgScore: number
  weaknessDist: { name: string; value: number }[]
}>({ completionRate: 0, avgScore: 0, weaknessDist: [] })

onMounted(async () => {
  analytics.value = await get('/api/teacher/classes/' + route.params.id + '/analytics')
})
const weaknessOption = computed(() => ({
  tooltip: { trigger: 'item' }, legend: { bottom: 0 },
  series: [{ type: 'pie', radius: ['40%','70%'], data: analytics.value.weaknessDist }],
}))
</script>
<template>
  <el-card><template #header>班级薄弱环节分布</template>
    <BaseChart :option="weaknessOption" height="320px" />
  </el-card>
</template>
```

---

## 5. 管理端治理/监控/审计（P3）

在现有 `views/admin/*`（Dashboard/UserManage/ResourceManage/ContentReview/Statistics/Settings/AdminLogin）基础上**扩展治理页**，并补齐网关路由（当前 `/admin/*` 无对应网关路由，§5.5 处理）。

### 5.1 AI/Agent 治理页（对接 auth/ai 配置与开关）
- 复用现有 `UserManage` 的 Agent 段（或新增 `views/admin/govern/AiAgent.vue`）。
- 端点：`GET /api/ai/admin/agents`、`POST /api/ai/admin/agents`、`PUT /api/ai/admin/agents/:id/status`、`GET /api/ai/admin/settings`（prompt/模型热更新）、`PUT /api/ai/admin/settings`、`POST /api/ai/kb/rebuild`（触发向量索引重建，主蓝图 §6.2）。
- 开关/阈值存 `ai-service` 配置，管理端只读写，不写死前端。

### 5.2 系统监控页（拉真实指标，禁止 mock）
- 指标来源（主蓝图 §9）：**SkyWalking**（OAP `12800` 查询接口 / GraphQL）、**Prometheus**（`:9090/api/v1/query`）、**Actuator**（`/actuator/prometheus`、`/actuator/health`）、**Redis/RabbitMQ** 暴露指标。
- 网关需新增 `/api/monitor/**` 路由（P3 加），前端 `GET /api/monitor/skywalking/services`、`/api/monitor/prometheus/query?expr=...`、`/api/monitor/mq/backlog`、`/api/monitor/redis/info`、`/api/monitor/services/health`。
- **硬规则**：监控页所有数值来自上述端点；无数据时显示"加载中/无数据"，**绝不渲染占位数字**。`BaseChart` 渲染（见 §5.3）。

### 5.3 `components/BaseChart.vue`（ECharts 统一封装，复用）
```vue
<script setup lang="ts">
import * as echarts from 'echarts'
import { onMounted, onBeforeUnmount, ref, shallowRef, watch } from 'vue'
const props = defineProps<{ option: echarts.EChartsOption; height?: string }>()
const el = ref<HTMLElement>()
const chart = shallowRef<echarts.ECharts>()
function render() { chart.value?.setOption(props.option, true) }
onMounted(() => { chart.value = echarts.init(el.value!); render() })
watch(() => props.option, render, { deep: true })
onBeforeUnmount(() => chart.value?.dispose())
</script>
<template><div ref="el" :style="{ height: props.height || '300px' }"></div></template>
```
> 管理端 `Statistics.vue`、学生端 `Report.vue`、教师端 `Analytics.vue` 全部改用 `BaseChart`，统一主题与 resize。

### 5.4 课程班级治理 & 审计与运营
- **课程班级治理**：`GET /api/teacher/classes`（全量班级）、`GET /api/teacher/courses`（课程，若归 teacher）、`PUT /api/teacher/classes/:id/status`（启停）。
- **审计**：新增 `views/admin/govern/Audit.vue`，端点 `GET /api/audit/logs?page&pageSize&type`（type: login/resource_review/kb_rebuild/class_op），来源 `audit_log` 表（主蓝图 §9.3）。**需 P3 在网关加 `/api/audit/**` 路由**（归属待定，建议 audit 模块挂 auth 或独立轻服务）。
- **运营统计**：`GET /api/resource/admin/stats`（用户/资源/对话量，来自 `admin_stats_cache`）、`GET /api/auth/admin/stats/user-growth`（用户增长曲线，ECharts）。

### 5.5 网关路由缺口对齐（P3 必须补）
现有 admin 端点（`/admin/stats`、`/admin/users`、`/admin/agents`、`/admin/conversations`、`/admin/resources`、`/admin/settings`）**当前无网关路由**。P3 在网关增加（映射到底层服务，**前端不新增 admin-service**）：

| 前端新 path | 网关路由 | 后端归属 |
|------|------|------|
| `/api/auth/admin/users` 等 | `/api/auth/**` | auth-service（用户/角色/用户增长） |
| `/api/ai/admin/agents` `/api/ai/admin/settings` `/api/ai/kb/rebuild` | `/api/ai/**` | ai-service |
| `/api/resource/admin/resources` `/api/resource/admin/stats` | `/api/resource/**` | resource-service |
| `/api/learning/admin/conversations`（内容审核）`/api/learning/dashboard/*` | `/api/learning/**` | learning-service |
| `/api/audit/logs` | `/api/audit/**`（P3 新增路由） | audit（建议 auth 或轻服务） |
| `/api/monitor/**` | `/api/monitor/**`（P3 新增路由） | 监控聚合（网关转发 SkyWalking/Prometheus/Actuator） |

> 前端 `api/admin.ts` 全面改 path 对齐上表；`PageResult<T>` 结构沿用（§1.3）。

---

## 6. 类型 / 状态模型（TS 约定 + Pinia 划分）

### 6.1 与后端对齐的类型约定
- `utils/request.ts` 导出 `ApiResult<T>` / `PageResult<T>`（§1.3），与 `common.Result` / `common.PageResult` 完全一致（`records/total/page/pageSize`）。
- 每个 `api/*.ts` 内聚本服务的请求/响应 interface；**禁止 `any` 作为对外类型**（现有 `userInfo: any`、`LoginResponse.userInfo: any` 必须收紧为 `UserInfo`）。
- 枚举/角色常量集中：`utils/constants.ts` 导出 `ROLE = { STUDENT:'ROLE_STUDENT', TEACHER:'ROLE_TEACHER', ADMIN:'ROLE_ADMIN' }`、资源类型 `ResourceType = 'mindmap'|'quiz'|'reading'|'code'` 等（仅结构，无业务数据）。

### 6.2 Pinia stores 划分
```
stores/
├── index.ts            # createPinia()
├── user.ts             # （现有）token / roles:string[] / userInfo / setToken / setUserInfo / logout
├── teacher.ts          # 【新增】当前教师班级列表、选中班级、作业草稿
├── admin.ts            # 【新增】治理页缓存（agents 列表、监控快照、审计查询条件）
└── monitor.ts          # 【新增】SkyWalking/Prometheus 指标拉取与图表数据归一化
```
> `user.ts` 改造：`userInfo` 类型收紧为 `UserInfo`（含 `roles:string[]`、`realName`、`onboarded`），`setUserInfo` 同时写 `localStorage.roles`（JSON）。多角色切换靠路由+守卫，不刷新页面（主蓝图 §5.4）。

---

## 7. 联调与测试

### 7.1 真实契约生成前端类型
```bash
npm i -D openapi-typescript
# package.json scripts 增加：
# "types:api": "openapi-typescript http://localhost:8080/api/auth/v3/api-docs -o src/types/api/auth.ts && ...（各服务）"
```
生成 `src/types/api/*.ts`，业务 `api/*.ts` 复用其类型，契约漂移在 `vue-tsc` 阶段即报错。

### 7.2 端到端联调清单（P1→P3）
- [ ] P0 后：`POST /api/auth/login` 拿 token → `getMe` 返回 `roles` → 守卫按 `roles` 分流。
- [ ] 学生：Dashboard（learning/dashboard/*）、CourseCenter（learning/courses）、TutorChat（`/api/ai/chat`）、ResourceGenerate（`/api/resource/generate`）、Report（learning/report/* + ECharts）、CodePractice（`/api/code/submit`，P2）。
- [ ] 教师（P3）：班级/题库/作业/批改/学情看板/AI 助教全部走 `/api/teacher/**` + `/api/ai/**`。
- [ ] 管理（P3）：用户(auth)、Agent/配置(ai)、资源/统计(resource)、内容审核(learning)、监控(monitor)、审计(audit) 全部走 §5.5 路由。
- [ ] 越权：admin 直接访问 `/student/dashboard` → 跳 `/admin/dashboard`；teacher 访问 `/admin/*` → 跳 `/teacher/dashboard`（§1.4）。

### 7.3 测试（Vitest + 零 mock）
- store 测试：`user.ts` 的 `setUserInfo` 写入 `roles`；`logout` 清 `roles`。
- 组件测试：`vi.mock('@/utils/request')` 注入契约 stub 验证渲染；**不写组件内 mock**（§2.4）。
- 路由测试：守卫对 `roles` 数组的越权拦截断言。

### 7.4 验收标准（DoD）
- [ ] 前端**只调网关 `/api/<服务>/**`**；无直连端口、无 `/ai` 直连代理、无组件内 mock/魔法数（§2.4 检查点全过）。
- [ ] `request.ts` 统一解包 `Result`、401 跳登录；`roles:string[]` 分流三角色，守卫隔离生效。
- [ ] 学生端全部页面 path 对齐 §3.2 映射表；`Dashboard.vue` 硬编码路径已抽离；`profile.ts` rogue `aiClient` 已删除。
- [ ] ECharts 经 `BaseChart` 复用；Monaco 封装 `CodeEditor` 接入代码作业提交（`/api/code/submit`）。
- [ ] 教师端 `TeacherLayout` + `/teacher/*` 全页面跑通，端点对齐 §4.2（待陈海洋契约定稿后 path 一致）。
- [ ] 管理端治理/监控/审计页拉**真实指标**，监控页无占位数字；网关 `/api/monitor/**` `/api/audit/**` 路由已加。
- [ ] `npm run build`（含 `vue-tsc`）通过；`npm test`（Vitest）通过；陈旧重复目录 `edu-agent-web/src/edu-agent-web/` 已删。

---

*前端子 spec 结束（成员D 曾姿妍）。教师端/治理页字段级契约待陈海洋（teacher）、吴友诚（code/ai）子 spec 定稿后回填 `api/*.ts`，页面与组件无需改动。*
