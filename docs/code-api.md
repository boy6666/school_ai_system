---
# EduAgent Code 服务接口文档（判分受理口）

> 服务名：`edu-agent-code` ｜ 服务端口：`8085` ｜ 数据源：`code_db`
> 来源：基于 `feature/microservice` 分支 `edu-agent-code` 实际代码生成（controller / dto / vo / service / entity），与《契约对齐决议》C1（异步两段式判分）对齐。
> 维护：吴友诚（code）

---

## 1. 通用约定

### 1.1 地址前缀

- 服务监听端口 `8085`，接口统一挂在网关 `8080`，经网关访问完整路径为 `http://<网关>/api/edu-agent-code/...`。
- 判分全链路：`编译 → 静态检查 → Docker 沙箱运行 → AI 纠错 → 判分`（独立微服务，蓝色 🔴 高危安全点）。

### 1.2 鉴权与身份透传

| Header | 说明 | 示例 |
|---|---|---|
| `X-User-Id` | 网关注入的用户 ID | `12` |
| `X-User-Roles` | 网关注入的角色 | `ROLE_STUDENT` |

- 服务内 `AuthContext` 读取上述头；`studentId` 缺省时回退 `AuthContext`（学生直连推送 / 网关代提交均覆盖）。

### 1.3 统一响应体 `Result<T>`

```json
{ "code": 0, "message": "success", "data": { } }
```

- 成功：`code=0`；业务失败：`code` 非 0；校验失败 `code=400`；未捕获 `code=500`。
- `POST /submit`、`POST /submissions/{id}/regrade` 使用 HTTP 状态码 **202 Accepted** 表达"已受理异步判分"。

### 1.4 数据模型要点

| 表 | 说明 |
|---|---|
| `code_submissions` | 提交记录；`status`：0=待处理 / 1=运行中 / 2=已完成 / 3=超时 / 4=编译失败 / 5=判分失败 |
| `code_check_reports` | 判分报告；`checkstyle` / `pmd` / `ai_suggestion` 为 JSON **字符串**，前端需再解析 |

---

## 2. 判分受理口（核心，见 C1）

### 2.1 `POST /api/edu-agent-code/submit` — 提交判分（202 异步）

接收一次代码提交，**持久化后立即返回受理回执**，真正的判分由后台 Worker 异步执行；结果经 `GET /result/{id}` 查询或 `assignment.graded` 事件推送。

**请求体（主形态：多文件 files[] + 入口 className）**

```json
{
  "studentId": 1001,
  "assignmentId": 5,
  "assignmentItemId": 12,
  "language": "java",
  "files": [
    { "name": "Dog.java",  "sourceCode": "public class Dog {}" },
    { "name": "Main.java", "sourceCode": "public class Main { public static void main(String[] a){ System.out.println(\"hi\"); } }" }
  ],
  "className": "Main",
  "expectedOutput": "hi\n",
  "mode": "IO"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `studentId` | number | 否 | 缺省时回退 `AuthContext` |
| `assignmentId` | number | 否 | 作业 id，用于事件回填关联 |
| `assignmentItemId` | number | 否 | 作业项 id |
| `language` | string | 是 | 语言标识，默认 `java` |
| `files[]` | array | 条件 | 多文件源码（每项 `{name, sourceCode}`），可多文件 / 多包 / 不打包 |
| `className` | string | 是 | 入口类名，可简单类名（如 `Test1`，不强制 `Main`） |
| `sourceCode` | string | 条件 | 兼容 C1 单文件形态；仅当 `files` 为空时使用 |
| `expectedOutput` | string | 否 | 判题类型 (a) 标准 I/O 比对的期望输出 |
| `mode` | string | 否 | 判题类型：`IO`（默认）/ `HARNESS`（隐藏测试，预留） |

**成功响应（HTTP 202）**

```json
{ "code": 0, "message": "success", "data": { "submissionId": 1024, "status": 0 } }
```

| 字段 | 说明 |
|---|---|
| `submissionId` | 受理号，后续用其查结果 |
| `status` | 0=待处理（判分未开始） |

> 说明：编译失败等问题不在此同步返回；统一走 `GET /result/{id}` 查全量报告。

### 2.2 `GET /api/edu-agent-code/result/{id}` — 查询判分结果

**路径参数**：`id`=submissionId。

**成功响应（字段与 teacher `grades` 列一一对应）**

```json
{
  "code": 0,
  "data": {
    "submissionId": 1024,
    "status": 2,
    "stdout": "hi\n",
    "runTimeMs": 42,
    "compileOk": 1,
    "checkstyle": "{\"errorCount\":0,\"warningCount\":1}",
    "pmd": "{\"violationCount\":0}",
    "aiSuggestion": "代码整体简洁，建议补充判空。",
    "overallScore": 96
  }
}
```

| 字段 | 归属 grades 列 | 说明 |
|---|---|---|
| `stdout` / `runTimeMs` / `compileOk` | `run_result` | 运行结果 |
| `checkstyle` / `pmd` | `static_report` | 静态检查，JSON 字符串 |
| `aiSuggestion` | `ai_report` | AI 评审 |
| `overallScore` | `score` | 综合分 |
| `status` | — | 判分状态（见 1.4） |

`404`：对应 `submissionId` 不存在。

> 内部 `scoreDetail` 权重明细不对前端暴露。

### 2.3 `POST /api/edu-agent-code/submissions/{id}/regrade` — 教师重新判分（202 异步）

学生首次自动判分失败（TIMEOUT / COMPILE_ERROR / FAILED）或结果有争议时，教师可对**已出终态**的提交触发再判分：系统重置状态与旧运行痕迹、删除旧报告行，随后 Worker 全量重跑流水线（编译 → 检查 → 沙箱 → AI → 判分），并发新 `assignment.graded` 事件回填 teacher 成绩（同作业同行更新，不产生新 Grade）。

**路径参数**：`id`=submissionId。

**成功响应（HTTP 202）**

```json
{ "code": 0, "message": "success", "data": { "submissionId": 1024, "status": 0 } }
```

**错误**

| HTTP | code | 场景 |
|---|---|---|
| 403 | 403 | 非 `ROLE_TEACHER`（code 服务无班级归属数据，仅做角色门禁；作业归属校验由 teacher 侧消费事件时兜底） |
| 404 | 404 | `submissionId` 不存在 |
| 409 | 409 | 提交处于 `PENDING(0)` / `RUNNING(1)`，首判未完成，重复触发会并发双判 |

> 再判分同样异步：回执仅表示"已受理重判"，结果仍走 `GET /result/{id}` 或事件推送。

---

## 3. 基础练习骨架

### 3.1 `POST /api/edu-agent-code/exercises` — 创建练习

```json
{ "title": "两数之和", "description": "...", "difficulty": "EASY", "language": "java" }
```
（`difficulty` 默认 `EASY`，`language` 默认 `java`）

### 3.2 `GET /api/edu-agent-code/exercises?page=1&size=10` — 分页列表

返回 `Result<PageResult<CodeExerciseVO>>`；`records` 元素：`{id, title, difficulty, language, status, createTime}`。

---

## 4. 后续规划

- `POST /run`：免作业快速运行（不调 AI、不写 report）。
- `GET /submissions`：分页查提交。
- ~~判分 Worker~~：**已实现**（feat/code）：编译 → Checkstyle/PMD → 本地/Docker 沙箱 → AI 纠错（fail-open）→ 综合判分 → 发 `assignment.graded` 事件；教师重判入口见 2.3。
