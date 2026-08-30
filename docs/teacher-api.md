---
# EduAgent Teacher 服务接口文档（教师六大模块）

> 服务名：`edu-agent-teacher` ｜ 服务端口：`8084` ｜ 数据源：`teacher_db`
> 来源：基于 `feat/teacher` 分支 `edu-agent-teacher` 实际代码生成（controller / dto / vo / service / entity / mq / feign）。
> 维护：吴友诚（teacher）

---

## 1. 通用约定

### 1.1 地址前缀

- 服务监听端口 `8084`，接口统一挂在网关 `8080`，完整路径为 `http://<网关>/api/edu-agent-teacher/...`。
- 六大模块：班级 / 作业 / 题库 / 提交与批改 / 学情看板 / AI 助教。

### 1.2 鉴权与身份透传

| Header | 说明 | 示例 |
|---|---|---|
| `X-User-Id` | 网关注入的用户 ID | `10` |
| `X-User-Roles` | 网关注入的角色 | `ROLE_TEACHER` |

- 服务内 `AuthContext` 读取上述头；班级/题库/看板为教师专属（属主校验在 service）；
  提交口学生可用，成绩查看学生仅限本人（教师/管理员可看全部）。

### 1.3 统一响应体 `Result<T>`

```json
{ "code": 0, "message": "success", "data": { } }
```

- 成功：`code=0`；业务失败：`code` 非 0（400 参数 / 401 未认证 / 403 无权限 / 404 不存在 / 409 冲突 / 500 系统异常）。

### 1.4 数据模型要点

| 表 | 说明 |
|---|---|
| `classes` | 班级；`status`：1=启用 0=归档 |
| `assignments` | 作业；`type`: homework / code；`status`：0=草稿 1=已发布 |
| `assignment_items` | 作业题目项（questionId + 分值） |
| `questions` | 题库；`type`: choice / blank / code |
| `grades` | 成绩（核心表）；`status`：0=待批 1=已批；`submission_id`=code 服务受理号（**教师重判分入口**） |
| `class_students` | 班级-学生关联 |

---

## 2. 班级管理（全部 ROLE_TEACHER，属主校验）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/classes` | 创建班级 |
| GET | `/classes` | 我教的班级列表 |
| GET | `/classes/{id}` | 班级详情 |
| PUT | `/classes/{id}` | 更新班级 |
| DELETE | `/classes/{id}` | 删除班级 |
| POST | `/classes/{id}/students` | 添加学生（体：`{studentId}`） |
| DELETE | `/classes/{id}/students/{studentId}` | 移除学生 |
| GET | `/classes/{id}/students` | 花名册（studentId/studentName/joinedAt） |

**创建/更新请求体**

```json
{ "name": "Java 提高班", "course": "Java 程序设计", "semester": "2026 秋" }
```

**班级响应 `ClassVO`**

```json
{ "id": 1, "name": "Java 提高班", "teacherId": 10, "course": "Java 程序设计",
  "semester": "2026 秋", "status": 1, "createTime": "2026-08-30T10:00:00", "studentCount": 32 }
```

> 非本人班级的操作返回 `403`。

---

## 3. 作业管理

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/assignments` | 创建作业（草稿，status=0） |
| GET | `/assignments?classId=` | 作业列表（可按班级过滤） |
| GET | `/assignments/{id}` | 作业详情（含题目项 + 题目 + 提交/批改计数） |
| PUT | `/assignments/{id}` | 更新（title/deadline/status） |
| DELETE | `/assignments/{id}` | 删除 |
| POST | `/assignments/{id}/items` | 追加题目项 |
| POST | `/assignments/{id}/publish` | **发布**（status→1，发 `assignment.published` 事件） |

**创建请求体**

```json
{
  "classId": 1,
  "title": "第三章 作业",
  "type": "code",
  "deadline": "2026-09-07T23:59:59",
  "description": "完成两数之和",
  "items": [
    { "questionId": 200, "score": 100 }
  ]
}
```

- `classId`/`title`/`type`/`items` 必填；`items[].questionId` 必填，`items[].score` 默认 10。
- 发布后学生端才可见（学习服务消费事件推班级）；草稿可反复改。

---

## 4. 题库管理（ROLE_TEACHER）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/questions` | 创建题目 |
| GET | `/questions?chapter=&topic=&type=&difficulty=` | 多条件筛选 |
| GET | `/questions/{id}` | 题目详情 |
| PUT | `/questions/{id}` | 更新 |
| DELETE | `/questions/{id}` | 删除 |
| POST | `/questions/generate` | **AI 出题草稿**（调 ai 服务，不落库，返回候选列表） |

**创建请求体**

```json
{
  "type": "choice",
  "chapter": "第3章 流程控制",
  "topic": "循环",
  "content": "while 循环最少执行几次？",
  "options": ["0 次", "1 次", "2 次"],
  "answer": "0 次",
  "explanation": "while 先判断后执行",
  "difficulty": "medium"
}
```

- `type`/`content` 必填；`chapter`/`topic` ≤64 字符；`difficulty` 默认 `medium`。
- **AI 出题**请求体：`{chapter, topic, type, difficulty=medium, count=5}`；返回 `QuestionVO[]` 草稿（`answer`/`explanation` 为 AI 生成，教师确认后走 POST `/questions` 落库）。

---

## 5. 提交与批改（核心，跨服务）

### 5.1 `POST /api/edu-agent-teacher/assignments/{id}/submit` — 学生提交作业

学生逐题提交；`choice`/`blank` 本地即时判分（status=1），`code` 题异步两段式（status=0 待批）。

```json
{
  "items": [
    { "itemId": 10, "submission": "B" },
    { "itemId": 20, "submission": "public class Main {...}", "language": "java" }
  ]
}
```

- `items` 必填非空；`items[].itemId`/`submission` 必填，`language` ≤16（code 题建议带，默认 java）。
- code 题：teacher 经 Feign 向 code 服务 `/submit` 受理，**回执中的 `submissionId` 随成绩落库**（受理失败不阻断提交，成绩仍落库待批）。
- 返回 `GradeVO[]`（每题一条）。

### 5.2 `GET /api/edu-agent-teacher/assignments/{id}/grades?studentId=` — 成绩列表（教师复核）

`GradeVO[]`：

```json
{
  "id": 5, "assignmentId": 1, "studentId": 1001, "itemId": 20,
  "type": "code", "language": "java", "submissionId": 1024, "submission": "public class Main {...}",
  "score": 40, "status": 1, "gradedAt": "2026-08-30T21:00:00", "hasAiReport": true
}
```

| 字段 | 说明 |
|---|---|
| `status` | 0=待批（code 判分未回） 1=已批 |
| `submissionId` | code 服务受理号；**有值即可调重判分** `POST /api/edu-agent-code/submissions/{submissionId}/regrade` |
| `hasAiReport` | 是否已有 AI 建议（避免列表传输大 JSON） |

### 5.3 `GET /api/edu-agent-teacher/grades/{gradeId}` — 成绩详情

`GradeDetailVO`：在列表字段基础上补全 `runResult` / `staticReport` / `aiReport` / `comment`（JSON 字符串，前端直接解析渲染）。

```json
{
  "id": 5, "assignmentId": 1, "studentId": 1001, "itemId": 20,
  "type": "code", "language": "java", "submissionId": 1024,
  "submission": "public class Main {...}",
  "score": 40, "status": 1, "gradedAt": "2026-08-30T21:00:00",
  "runResult": "{\"stdout\":\"...\",\"runTimeMs\":42,\"status\":\"done\",\"runPassed\":true}",
  "staticReport": "{\"compileOk\":1,\"checkstyle\":\"...\",\"pmd\":\"...\"}",
  "aiReport": "{\"aiSuggestion\":\"...\"}",
  "comment": ""
}
```

> 权限：学生仅限查本人；教师/管理员可查任意。

### 5.4 `PUT /api/edu-agent-teacher/grades/{gradeId}` — 教师复核成绩

```json
{ "score": 9, "comment": "整体良好，注意命名规范", "aiReportOverride": "..." }
```

- 三字段均可选，仅传需要覆盖的项；覆盖后 `status=1`、`gradedAt` 刷新。
- `comment` ≤2000 字符。非本人班级 `403`。

### 5.5 `GET /api/edu-agent-teacher/students/{studentId}/assignments` — 学生作业汇总

学生查自己（或教师查他人）：按班级过滤出的作业列表 + 我的得分/总分/最新提交时间（`StudentAssignmentVO[]`）。

### 5.6 判分回填（内部，非接口）

code 服务判分完成后发 `assignment.graded` 事件（RabbitMQ），teacher 消费后按
`uk_stu_item(assignment_id, student_id, item_id)` 定位同一行 grade 回填
`run_result / static_report / ai_report / score / submission_id`，`status→1`。天然幂等；**重判分事件复用同一行**。

---

## 6. 学情看板（ROLE_TEACHER，属主校验）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/classes/{id}/analytics` | 班级学情聚合（ECharts 直接消费） |
| GET | `/classes/{id}/overview` | 班级概览卡片 |

**analytics 响应 `ClassAnalyticsVO`（消费 study.progress 近实时）**

```json
{
  "classId": 1, "className": "Java 提高班", "studentCount": 32,
  "avgMastery": 0.72, "avgPathProgress": 0.65, "avgStudySec": 1840,
  "masteryDist": [{ "level": "good", "count": 12 }],
  "dimensionAvg": { "语法": 0.8, "循环": 0.6 },
  "taskCompletion": [{ "studentId": 1001, "name": "张三", "progress": 80, "lastScore": 92 }],
  "weakTopics": [{ "topic": "递归", "count": 7 }],
  "trend": [{ "day": "2026-08-29", "activeStudents": 20 }]
}
```

`overview` 返回 `{classId, className, studentCount, avgMastery, completionRate, activeStudents}`。

---

## 7. AI 助教（经 ai 服务，透传教师身份）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/ai/ask` | 教师向 AI 助教提问 |
| POST | `/ai/explain-grade` | AI 解读某学生某作业的成绩报告 |

**ask 请求体**：`{message(必填), classId, context}`。
**explain-grade 请求体**：`{studentId, assignmentId}`（均必填）——teacher 组装该生的成绩报告调 ai，返回 `{answer, ...}`。

---

## 8. 跨服务协作一览

| 方向 | 通道 | 说明 |
|---|---|---|
| teacher → code | Feign `POST /api/edu-agent-code/submit` | code 题提交受理（202），回执 submissionId 落 grades |
| code → teacher | MQ `assignment.graded` | 判分完成回填成绩（含 submission_id，幂等） |
| teacher → MQ | `assignment.published` | 作业发布事件（学习服务消费推学生） |
| teacher → ai | HTTP | AI 出题草稿 / 助教问答 / 成绩解读 |
| teacher → study | 数据聚合 | 学情看板消费 study.progress（近实时） |
