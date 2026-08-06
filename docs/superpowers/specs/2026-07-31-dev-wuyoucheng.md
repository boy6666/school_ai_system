# 吴友诚 开发文档（子 spec）：teacher-service · code-service · P4 加固 · 拆分指导

> 阶段：P0（已交付，见 `2026-07-31-p0-infra-gateway.md`）→ P2（ai/code）→ P4（加固）
> 负责人：**吴友诚（架构/地基）** ｜ 日期：2026-07-31 ｜ 状态：开发文档（可直接落地）
> 本文把主蓝图 §6 / §7 / §9 / §10 / §11 / §12 中**吴友诚名下**的部分展开为
> 「需求 → 接口契约 → 数据模型 → 关键实现 → 测试 → 验收」粒度的开发文档。
> **本文不重复 P0**：common / auth / Gateway / JWT 透传一律引用 P0，仅对齐接口与契约。

---

## 0. 吴友诚职责总览

主蓝图 §12.1 已确认：吴友诚拥有**地基（P0，已建）+ code-service（最难）+ teacher-service** 两块皇冠模块，并负责 P4 全栈加固与「单体→微服务」拆分指导。

| 板块 | 本文章节 | 端口/库 | 对接人 |
|------|---------|--------|--------|
| 微服务地基、common、auth、Gateway、JWT 透传 | ——（引用 P0） | 见 P0 | 全员基线 |
| **teacher-service**（Java：班级/题库/作业布置/批改/学情看板） | §B | `8084`，`teacher_db` | 吴友诚（原陈海洋，已对调） |
| **code-service**（Java：编译+静态检查+Docker 沙箱+AI 判分） | §2 | `8085`，`code_db` | 吴友诚(teacher 作业批改) |
| **P4 加固**（Sentinel / SkyWalking / 压测 / CI 质量门 / 文档演示） | §3 | 全服务 | 全员 |
| **单体→微服务拆分指导** | §4 | `edu-agent-server` 现有类→各服务 | 陈嘉成 / 陈海洋 |

> 命名/对齐硬约束（来自主蓝图 + P0）：
> - 服务名：`auth-service` `gateway-service` `learning-service` `resource-service` `code-service` `teacher-service` `ai-service`。
> - 网关路由前缀：`/api/<服务>/**`。前端与 Feign 都只认网关前缀。
> - ai-service 经 Nacos 注册；Java 经 **OpenFeign + LoadBalancer** 调 `lb://ai-service/...`。
> - 全部 DB-per-service，跨服务**一律走 Feign/MQ**，禁止直连对方库（§4.3 详述）。

---

# §B. teacher-service（P3，教师端后端）

## B.1 需求与职责边界

### B.1.1 四大功能映射
1. **教学管理**：班级（classes）、班级学生（class_students）的 CRUD 与成员管理。
2. **学情看板**：聚合 learning-service 的逐生学情 → 班级维度可视化数据接口（ECharts 直接消费）。
3. **批改与发布**：题库（questions）、作业（assignments + assignment_items）、提交批改（grades）；代码题提交**对接 code-service 判分**。
4. **AI 助教**：Feign 调 ai-service 做答疑 / 题目生成 / 学情解读。

### B.1.2 边界
- 不持有学情画像/路径/日志原始数据（在 learning-service）；teacher-service **逐生 Feign 拉取**聚合。
- 不持有代码判分细节（编译/沙箱/静态检查在 code-service）；只消费 `code-service` 的判分结果写入 `grades`。
- 不持有资源素材（resource-service）。

### B.1.3 跨服务依赖
| 调谁 | 端点（内部路径） | 用途 |
|------|------|------|
| learning-service | `GET /analytics/student/{id}`、`GET /analytics/student/{id}/progress`、`GET /profile/{studentId}` | 看板聚合、学生画像读取 |
| code-service | `POST /submissions`（见 B.4.2） | 代码作业判分 |
| ai-service | `POST /chat`、`POST /resource/generate` | AI 助教答疑、AI 出题 |

---

## B.2 接口契约（`/api/teacher/**`，端口 8084）

> 角色：`T`=ROLE_TEACHER，`S`=ROLE_STUDENT（学生提交作业），`A`=ROLE_ADMIN。

### B.2.1 班级管理 Class
| 方法 | 路径 | 角色 | 入参 | 出参 |
|------|------|------|------|------|
| POST | `/api/teacher/classes` | T | body：`{name, course, semester}` | `Result<ClassVO>`（teacher_id 取 AuthContext） |
| GET | `/api/teacher/classes` | T | — | `Result<List<ClassVO>>`（仅本人班级） |
| GET | `/api/teacher/classes/{id}` | T | path `id` | `Result<ClassVO>` |
| PUT | `/api/teacher/classes/{id}` | T | body：`{name?, course?, semester?}` | `Result<ClassVO>` |
| DELETE | `/api/teacher/classes/{id}` | T | path `id` | `Result<Void>` |
| POST | `/api/teacher/classes/{id}/students` | T | body：`{studentId}` | `Result<Void>`（写 class_students + 回写 `learning_db.student_profiles.class_id`） |
| DELETE | `/api/teacher/classes/{id}/students/{studentId}` | T | path | `Result<Void>` |
| GET | `/api/teacher/classes/{id}/students` | T | path `id` | `Result<List<ClassStudentVO>>`（`{studentId, studentName?, joinedAt}`） |

> `studentId` 合法性由 auth-service 保证（teacher-service 不校验密码，仅逻辑引用）。回写 `student_profiles.class_id` 通过 Feign→learning-service `POST /profile/{studentId}/class` 完成（该端点 T 角色可写，见 A.2.1 扩展）。

### B.2.2 题库 Question
| 方法 | 路径 | 角色 | 入参 | 出参 |
|------|------|------|------|------|
| POST | `/api/teacher/questions` | T | body：`{type, chapter, topic, content, options?, answer, explanation?, difficulty}` | `Result<QuestionVO>` |
| GET | `/api/teacher/questions` | T | query `chapter?,topic?,type?,difficulty?` | `Result<List<QuestionVO>>` |
| GET | `/api/teacher/questions/{id}` | T | path `id` | `Result<QuestionVO>` |
| PUT | `/api/teacher/questions/{id}` | T | body：同创建 | `Result<QuestionVO>` |
| DELETE | `/api/teacher/questions/{id}` | T | path `id` | `Result<Void>` |
| POST | `/api/teacher/questions/generate` | T | body：`{chapter, topic, type, difficulty, count?}` | `Result<List<QuestionDraftVO>>`（Feign→ai `/resource/generate` mode=quiz → 返回草稿，教师确认后落库） |

`QuestionVO`：`{id, type, chapter, topic, content, options:JSON, answer, explanation, difficulty, creatorId, createdAt}`。`type`∈`choice/code/blank`。

### B.2.3 作业 Assignment
| 方法 | 路径 | 角色 | 入参 | 出参 |
|------|------|------|------|------|
| POST | `/api/teacher/assignments` | T | body：`{classId, title, type, deadline, description?, items:[{questionId, score}]}` | `Result<AssignmentVO>`（写 assignments + assignment_items；建完发 `assignment.published` 事件） |
| GET | `/api/teacher/assignments` | T | query `classId?` | `Result<List<AssignmentVO>>` |
| GET | `/api/teacher/assignments/{id}` | T | path `id` | `Result<AssignmentDetailVO>`（含 items+每题 QuestionVO+提交情况） |
| PUT | `/api/teacher/assignments/{id}` | T | body：`{title?, deadline?, status?}` | `Result<AssignmentVO>` |
| DELETE | `/api/teacher/assignments/{id}` | T | path `id` | `Result<Void>` |
| POST | `/api/teacher/assignments/{id}/items` | T | body：`{questionId, score}` | `Result<AssignmentVO>` |
| POST | `/api/teacher/assignments/{id}/publish` | T | — | `Result<Void>`（重发 `assignment.published` 事件，状态置已发布） |

`type`∈`homework/code`。`AssignmentDetailVO`：`{id, classId, title, type, deadline, status, items:[{itemId, questionId, score, question:QuestionVO, submittedCount, gradedCount}]}`。

### B.2.4 提交与批改 Grade（核心跨服务）
| 方法 | 路径 | 角色 | 入参 | 出参 |
|------|------|------|------|------|
| POST | `/api/teacher/assignments/{id}/submit` | S | body：`{items:[{itemId, submission, language?}]}`（studentId 取 AuthContext） | `Result<List<GradeVO>>`（逐 item：choice/blank 本地判分；code 调 code-service） |
| GET | `/api/teacher/assignments/{id}/grades` | T | query `studentId?` | `Result<List<GradeVO>>` |
| GET | `/api/teacher/grades/{gradeId}` | T,S | path `gradeId` | `Result<GradeDetailVO>` |
| PUT | `/api/teacher/grades/{gradeId}` | T | body：`{score?, comment?, aiReportOverride?}` | `Result<GradeVO>`（教师复核微调） |
| GET | `/api/teacher/students/{studentId}/assignments` | S,T | path `studentId` | `Result<List<AssignmentVO（含我的成绩）>>` |

`GradeVO`：`{id, assignmentId, studentId, itemId, type, submission, score, status, gradedAt, hasAiReport}`。
`GradeDetailVO`：`{...GradeVO, runResult:JSON, staticReport:JSON, aiReport:JSON, comment}`。

### B.2.5 学情看板 Analytics
| 方法 | 路径 | 角色 | 入参 | 出参 |
|------|------|------|------|------|
| GET | `/api/teacher/classes/{id}/analytics` | T | path `id` | `Result<ClassAnalyticsVO>`（逐生 Feign→learning，聚合成班级） |
| GET | `/api/teacher/classes/{id}/overview` | T | path `id` | `Result<ClassOverviewVO>`（轻量：均分、完成率、活跃度，供首页卡片） |

`ClassAnalyticsVO`（ECharts 直接消费）：
```json
{ "classId":3, "className":"JavaSE-1班", "studentCount":30,
  "avgMastery":66.5, "avgPathProgress":42, "avgStudySec":5400,
  "masteryDist":[{"level":"level_1","count":5},{"level":"level_2","count":20},{"level":"level_3","count":5}],
  "dimensionAvg":{"knowledge_mastery":68,"learning_goal_clarity":60,...},
  "taskCompletion":[{"studentId":12,"name?":"","progress":40,"lastScore":72}],
  "weakTopics":[{"topic":"多线程","count":12}],
  "trend":[{"day":"2026-07-28","activeStudents":18},...] }
```
> 实现：读班级学生列表 → 并发 Feign→learning `/analytics/student/{id}/progress` → 聚合。注意分页/限流，班级人数多时用 `List<CompletableFuture>` + `Semimiter` 保护（见 B.4.3）。

### B.2.6 AI 助教 AiTutor
| 方法 | 路径 | 角色 | 入参 | 出参 |
|------|------|------|------|------|
| POST | `/api/teacher/ai/ask` | T | body：`{message, classId?, context?}` | `Result<{answer, intent, references?}>`（Feign→ai `/chat`，以教师身份 + 班级学情上下文） |
| POST | `/api/teacher/ai/explain-grade` | T | body：`{studentId, assignmentId}` | `Result<{analysis}>`（拉学情+成绩→ai 解读，Feign→ai `/resource/generate` mode=evaluation） |

---

## B.3 数据模型（`teacher_db`，DDL）

```sql
CREATE DATABASE IF NOT EXISTS teacher_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE teacher_db;

-- 1. 班级（teacher_id 逻辑引用 auth_db.users，无 FK）
CREATE TABLE classes (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(64) NOT NULL,
  teacher_id BIGINT NOT NULL,
  course VARCHAR(64) DEFAULT NULL,
  semester VARCHAR(32) DEFAULT NULL,
  status TINYINT DEFAULT 1 COMMENT '1启用 0归档',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- 2. 班级学生（无 FK，纯逻辑引用）
CREATE TABLE class_students (
  class_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (class_id, student_id),
  INDEX idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级学生关系表';

-- 3. 题库
CREATE TABLE questions (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  type VARCHAR(16) NOT NULL COMMENT 'choice/code/blank',
  chapter VARCHAR(64) DEFAULT NULL,
  topic VARCHAR(64) DEFAULT NULL,
  content TEXT,
  options JSON COMMENT '选择题选项',
  answer TEXT,
  explanation TEXT,
  difficulty VARCHAR(8) DEFAULT 'medium' COMMENT 'easy/medium/hard',
  creator_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_chapter (chapter),
  INDEX idx_topic (topic),
  INDEX idx_type (type),
  INDEX idx_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库表';

-- 4. 作业
CREATE TABLE assignments (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  class_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  type VARCHAR(16) NOT NULL COMMENT 'homework/code',
  description TEXT,
  deadline DATETIME DEFAULT NULL,
  status TINYINT DEFAULT 0 COMMENT '0草稿 1已发布',
  creator_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_class (class_id),
  INDEX idx_creator (creator_id),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业表';

-- 5. 作业题目项
CREATE TABLE assignment_items (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  assignment_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  item_type VARCHAR(16) DEFAULT 'choice' COMMENT 'choice/code/blank',
  score INT DEFAULT 10,
  order_num INT DEFAULT 0,
  INDEX idx_assignment (assignment_id),
  INDEX idx_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业题目项表';

-- 6. 成绩/批改
CREATE TABLE grades (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  assignment_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  item_id BIGINT NOT NULL,
  item_type VARCHAR(16) DEFAULT 'choice',
  language VARCHAR(16) DEFAULT NULL COMMENT 'code 题语言，如 java',
  submission TEXT COMMENT '学生提交内容/代码',
  run_result JSON COMMENT '运行结果（来自 code-service）',
  static_report JSON COMMENT '静态检查报告（来自 code-service）',
  ai_report JSON COMMENT 'AI 建议（来自 code-service / ai）',
  score INT DEFAULT 0,
  status TINYINT DEFAULT 0 COMMENT '0待批 1已批',
  comment TEXT COMMENT '教师评语/复核',
  graded_at DATETIME DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_stu_item (assignment_id, student_id, item_id),
  INDEX idx_assignment (assignment_id),
  INDEX idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩表';
```
> 关系：classes 1—N class_students N—1 users(逻辑)；classes 1—N assignments；assignments 1—N assignment_items N—1 questions；assignments 1—N grades；grades N—1 users(逻辑,student)。**全部无跨库 FK**。

---

## B.4 关键实现

### B.4.1 分层结构
```
teacher-service/
├── TeacherServiceApplication.java
├── controller/  ClassController, QuestionController, AssignmentController,
│                GradeController, AnalyticsController, AiTutorController
├── service/     ClassService, QuestionService, AssignmentService,
│                GradeService, AnalyticsService, AiTutorService
├── service/impl/ *ServiceImpl
├── mapper/       ClassesMapper, ClassStudentMapper, QuestionMapper,
│                AssignmentMapper, AssignmentItemMapper, GradeMapper
├── entity/       Classes, ClassStudent, Question, Assignment, AssignmentItem, Grade
├── vo/           ClassVO, QuestionVO, AssignmentVO, AssignmentDetailVO,
│                GradeVO, GradeDetailVO, ClassAnalyticsVO, ClassOverviewVO
├── dto/          CreateClassRequest, CreateQuestionRequest, CreateAssignmentRequest,
│                SubmitAssignmentRequest, AiAskRequest
├── feign/        LearningServiceClient, CodeServiceClient, AiServiceClient
├── mq/           AssignmentPublishedEvent, AssignmentPublishedPublisher,
│                StudyProgressConsumer, AssignmentGradedConsumer
├── config/       FeignConfig, RabbitConfig, MybatisPlusConfig, AsyncConfig
└── resources/    application.yml, mapper/*.xml
```

### B.4.2 与 code-service 的作业-判分对接契约（异步两段式，见《契约对齐决议》C1）
teacher-service 提交代码题时调 `code-service`，采用**异步两段式**（提交仅拿受理号，判分结果经事件/结果端点回填）。**约定契约如下（吴友诚在 code-service 实现，路径前缀 `/api/code/**`，网关已路由）**：

**请求** `POST /api/code/submit`（HTTP 202 Accepted）
```json
{ "studentId": 12,
  "assignmentId": 7,
  "assignmentItemId": 45,
  "language": "java",
  "sourceCode": "public class Main{...}",
  "expectedOutput": "...",
  "className": "Main" }
```
**响应**（仅受理回执，**非**全量 VO，HTTP 202）
```json
{ "submissionId": 901, "status": "pending" }   // status: pending/running/done/failed
```
> submit 仅返回 `{submissionId, status}` 受理回执，**不**在此同步返回 stdout/compileOk/checkstyle/pmd/aiSuggestion/overallScore 等全量报告（原"submit 同步返回 `CodeSubmissionVO` 直接落 `grades`"的错误假设已作废）。teacher-service 提交后**不读取 submit 响应落库**。
> **采用方案 A（事件驱动，见《契约对齐决议》C1）**：判分结果由 `AssignmentGradedConsumer` 消费 `assignment.graded` 完整事件体回填 `grades`（见 B.4.5），teacher-service **不轮询**。退路 B：在 `CodeServiceClient` 新增 `GET /api/code/result/{id}`，submit 后轮询至 `status=done` 再落库（code-service 该端点返回 `{submissionId, status, stdout, runTimeMs, compileOk, checkstyle, pmd, aiSuggestion, overallScore}`）。
> 选择题/填空题（choice/blank）：不调 code-service，本地比对 `questions.answer` 判分（或小题调 ai `/resource/generate` mode=judge）。仅 `code` 类型走 code-service。
> 该契约是**双向约定**：吴友诚的 code-service 必须提供 `POST /api/code/submit`（202 + 上述受理回执），并按 `expectedOutput/className/assignmentId` 参与判分权重与事件回填关联；陈海洋侧 `CodeServiceClient` 按此签名实现。AI 分析底层复用 ai-service `/code/analyze`（由 code-service 内部调用，teacher-service 不直接调）。

### B.4.3 跨服务 Feign 客户端
```java
// feign/LearningServiceClient.java  → 学习学情（本服务拥有的另一个服务；路径带 /api/learning 前缀，与网关一致）
@FeignClient(name = "learning-service", url = "${learning.base-url:}", path = "/api/learning")
public interface LearningServiceClient {
    @GetMapping("/analytics/student/{studentId}")
    Result<StudentAnalyticsVO> getAnalytics(@PathVariable("studentId") Long studentId);
    @GetMapping("/analytics/student/{studentId}/progress")
    Result<StudentProgressVO> getProgress(@PathVariable("studentId") Long studentId);
    @GetMapping("/profile/{studentId}")
    Result<ProfileVO> getProfile(@PathVariable("studentId") Long studentId);
    @PostMapping("/profile/{studentId}/class")   // T 角色可写，回写 class_id
    Result<Void> bindClass(@PathVariable("studentId") Long studentId, @RequestBody Map<String,Object> body);
}

// feign/CodeServiceClient.java  → 代码判分（路径带 /api/code 前缀，与网关一致）
// 异步两段式（见《契约对齐决议》C1）：submit 仅返回受理回执 {submissionId, status}（HTTP 202），
// 判分结果经 AssignmentGradedConsumer 消费 assignment.graded 事件体落库（方案 A，不轮询）。
// 退路 B 可增 GET /api/code/result/{id} 轮询至 status=done（见 B.4.2）。
@FeignClient(name = "code-service", url = "${code.base-url:}", path = "/api/code")
public interface CodeServiceClient {
    @PostMapping("/submit")
    Result<CodeSubmitReceiptVO> submit(@RequestBody CodeSubmissionRequest req);
}

// feign/AiServiceClient.java  → AI 助教（路径带 /api/ai 前缀，与网关一致）
@FeignClient(name = "ai-service", url = "${ai.base-url:}", path = "/api/ai")
public interface AiServiceClient {
    @PostMapping("/chat") Result<AiChatResult> chat(@RequestBody AiChatRequest req);
    @PostMapping("/resource/generate") Result<Map<String,Object>> generate(@RequestBody AiResourceRequest req);
}
// generate 的 AiResourceRequest 必须带 mode 字段（见《契约对齐决议》C3），按 mode 解析：
//   mode=quiz        → 解析 {items:[...]}        （questions/generate B.2.2，出题草稿）
//   mode=evaluation  → 解析 {analysis}          （ai/explain-grade B.2.6，成绩解读）
//   mode=resource   → 解析 {content, resourceType, chapter}（默认）
```
> 三个 Feign 客户端均自动套用 `common` 的 `AuthFeignInterceptor`，把当前 `AuthContext`（教师/学生身份）透传下游；下游（learning/code/ai）均从 `X-User-*` 头读取。

### B.4.4 学情看板聚合（并发 + 限流）
```java
// AnalyticsServiceImpl.getClassAnalytics(Long classId)
List<Long> studentIds = classStudentMapper.selectStudentIds(classId);
Semaphore sem = new Semaphore(8); // 保护 learning-service
List<CompletableFuture<StudentProgressVO>> futures = studentIds.stream()
    .map(sid -> CompletableFuture.supplyAsync(() -> {
        sem.acquire(); try { return learningClient.getProgress(sid).getData(); }
        finally { sem.release(); }
    }, executor))
    .toList();
// 等待全部 → 聚合 avgMastery / masteryDist / dimensionAvg / taskCompletion / weakTopics
```
> 班级人数大时避免瞬时打爆 learning-service；配合 Sentinel（P4）对 `/api/teacher/classes/{id}/analytics` 限流。

### B.4.5 RabbitMQ 异步
**发布**（teacher-service → 学生通知/资源）：
- `assignment.published` / routing `assignment.published`：作业发布后发事件 `{assignmentId, classId, title, type, deadline}`，供前端轮询/通知（当前无独立通知服务，先落事件，前端可轮询 assignments 列表）。exchange 名 = 事件名（与 study.progress / resource.generate 风格统一，见《契约对齐决议》C12）。
**消费**（teacher-service 作为订阅方，对齐主蓝图 §8）：
- `study.progress`（来自 learning-service）：`StudyProgressConsumer` 接收后**更新内存/Redis 班级看板缓存**（`teacher:class:{classId}:dashboard`），使教师看板近实时，避免每次重算聚合。
- `assignment.graded`（来自 code-service）：`AssignmentGradedConsumer` **事件驱动**消费（方案 A，见《契约对齐决议》C1）。code-service 判分完成后发该事件，**payload 必须携带完整报告体**（不只 overallScore/aiSuggestion）：
  `{ assignmentId, assignmentItemId, studentId, submissionId, status, runPassed, compileOk, stdout, runTimeMs, checkstyle, pmd, aiSuggestion, overallScore }`。
  据此回写 `grades` 的 `run_result`/`static_report`/`ai_report`/`score` 列（status→1 已批），并触发教师端"待复核"提醒计数：
  ```java
  @RabbitListener(queues = "teacher.assignment.graded.queue")
  public void onAssignmentGraded(AssignmentGradedEvent e) {
      Grade grade = gradeMapper.selectByStuItem(e.getAssignmentId(), e.getStudentId(), e.getAssignmentItemId());
      if (grade == null) return;
      grade.setRunResult(Map.of("stdout", e.getStdout(), "runTimeMs", e.getRunTimeMs(),
                                "status", e.getStatus(), "runPassed", e.getRunPassed()));
      grade.setStaticReport(Map.of("compileOk", e.getCompileOk(), "checkstyle", e.getCheckstyle(), "pmd", e.getPmd()));
      grade.setAiReport(Map.of("aiSuggestion", e.getAiSuggestion()));
      grade.setScore(e.getOverallScore());
      grade.setStatus(1);
      grade.setGradedAt(LocalDateTime.now());
      gradeMapper.updateById(grade);
      // 触发教师端"待复核"提醒计数
  }
  ```
```java
@RabbitListener(queues = "teacher.study.progress.queue")
public void onStudyProgress(StudyProgressEvent e) {
    redisTemplate.opsForHash().put("teacher:class:"+e.getClassId()+":dashboard",
        "student:"+e.getStudentId(), e);  // 轻量缓存，看板读取时合并
}
```

### B.4.6 配置骨架
```yaml
server:
  port: 8084
spring:
  application:
    name: teacher-service
  cloud:
    nacos:
      discovery: { server-addr: nacos:8848 }
      config: { server-addr: nacos:8848, namespace: edu-dev, group: teacher-group }
  datasource:
    url: jdbc:mysql://mysql:3306/teacher_db?useSSL=false&serverTimezone=Asia/Shanghai
    username: ${DB_USER}; password: ${DB_PWD}
    driver-class-name: com.mysql.cj.jdbc.Driver
mybatis-plus:
  mapper-locations: classpath*:mapper/*.xml
rabbitmq: { host: rabbitmq, port: 5672 }
learning: { base-url: http://learning-service:8082 }
code:     { base-url: http://code-service:8085 }
ai:       { base-url: http://ai-service:8001 }
```
> 入口：`@SpringBootApplication` + `@EnableDiscoveryClient` + `@EnableFeignClients` + `@MapperScan` + `@EnableAsync`（聚合并发线程池）。

---

## B.5 测试

### B.5.1 单测（JUnit5 + Mockito）
- `GradeService.submit`：mock `CodeServiceClient`，断言 code 题提交后 `grades` 的 `run_result/static_report/ai_report/score` 映射正确；choice 题本地比对正确。
- `ClassService`：增删学生后 `class_students` 与 `LearningServiceClient.bindClass` 被调用（mock 验证）。
- `AnalyticsService`：mock `LearningServiceClient.getProgress` 返回多组 → 断言 `avgMastery`/`masteryDist` 聚合正确。
- `AssignmentService`：发布后 `AssignmentPublishedPublisher` 发出 `assignment.published` 事件（mock `RabbitTemplate` 验证）。

### B.5.2 集成（Testcontainers）
- MySQL(teacher_db) + RabbitMQ 容器 → 启 teacher-service → WebTestClient：
  - 建班级→加学生→建作业(含 code 题)→学生提交（mock code-service 返回 overallScore）→断言 `grades` 落库、事件 `assignment.graded` 进入测试队列。
  - 看板：`GET /api/teacher/classes/{id}/analytics` 在 mock learning-service（返回固定 progress）下返回正确聚合。
- Feign 透传：用 MockMvc 注入 `X-User-Roles: ROLE_TEACHER` 验证 `AuthFeignInterceptor` 把头带去下游（断言下游收到）。

### B.5.3 契约
- 导出 teacher-service OpenAPI 契约；重点冻结 `CodeServiceClient` 的 `/api/code/submit` 请求/响应字段（与吴友诚 code-service 子 spec 互为契约）。

---

## B.6 验收（DoD）
- [ ] `teacher_db` 建库建表完成（6 张表，无跨库 FK）；种子数据（1 教师、1 班级、若干学生、题库、1 作业）可灌入。
- [ ] 班级管理 / 题库 / 作业 CRUD 全部按契约跑通，错误角色→403。
- [ ] 学生提交作业：choice/blank 本地判分；code 题经 `CodeServiceClient`→code-service 判分并正确落 `grades`（run_result/static_report/ai_report/score）。
- [ ] 学情看板 `GET /api/teacher/classes/{id}/analytics` 并发聚合 learning-service 数据正确，且消费 `study.progress` 近实时更新缓存。
- [ ] AI 助教 `/api/teacher/ai/ask`、`/questions/generate` 经 `AiServiceClient` 调通，身份透传。
- [ ] RabbitMQ：`assignment.published` 发布、`study.progress`/`assignment.graded` 消费均验证。
- [ ] 单测 + Testcontainers 集成绿；与 code-service 的判分契约冻结并 mutual-review 通过（吴友诚确认）。

---

# §2. code-service 开发文档（Java · 端口 8085 · code_db）★最难模块

## 2.1 需求

独立微服务，提供 Java 代码的**编译 → 静态检查 → Docker 沙箱运行 → AI 纠错 → 判分**全链路。是全局唯一 🔴 高危安全点（主蓝图 §7）。

能力：
1. 接收学生提交（源码 + 语言 + 可选作业项），落 `code_submissions`。
2. 内存/落盘编译 `javax.tools.JavaCompiler`。
3. 静态检查 **Checkstyle + PMD**，产出结构化 JSON 报告。
4. **Docker 沙箱**一次性容器运行（`--memory=256m --cpus=1 --network=none --read-only`，5s 超时强杀）。
5. Feign 调 `ai-service /api/ai/code/analyze` 取得 AI 纠错讲解。
6. 综合判分（编译/检查/运行结果 → `overall_score`），写 `code_check_reports`。
7. 结果经 MQ 事件 `assignment.graded` 通知 teacher-service（吴友诚作业批改）。

**非目标**：不自己训练模型；不支持 Java 以外语言的沙箱运行（本期仅 JavaSE，`language=java`）。

## 2.2 数据模型（DDL — `code_db`）

```sql
CREATE DATABASE IF NOT EXISTS code_db CHARACTER SET utf8mb4;
USE code_db;

CREATE TABLE code_submissions (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id        BIGINT        NOT NULL,
  assignment_item_id BIGINT       NULL,
  language          VARCHAR(16)   NOT NULL DEFAULT 'java',
  class_name        VARCHAR(128)  NULL,
  source_code       LONGTEXT      NOT NULL,
  status            TINYINT       NOT NULL DEFAULT 0,
  stdout            LONGTEXT      NULL,
  stderr            LONGTEXT      NULL,
  run_time_ms       INT           NULL,
  created_at        DATETIME      DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_student (student_id),
  INDEX idx_assignment (assignment_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码提交表';

CREATE TABLE code_check_reports (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  submission_id     BIGINT        NOT NULL,
  compile_ok        TINYINT       NOT NULL DEFAULT 0,
  compile_msg       LONGTEXT      NULL,
  checkstyle        JSON          NULL,
  pmd               JSON          NULL,
  ai_suggestion     LONGTEXT      NULL,
  overall_score     INT           NOT NULL DEFAULT 0,
  score_detail      JSON          NULL,
  created_at         DATETIME      DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_submission (submission_id),
  CONSTRAINT fk_report_submission FOREIGN KEY (submission_id) REFERENCES code_submissions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码检查/判分报告';
```

**MyBatis-Plus 实体**（包 `com.eduagent.code.entity`）：`CodeSubmission`、`CodeCheckReport`（字段与上表一一对应，用 `@TableName`）。

## 2.3 接口契约（供前端/teacher 对齐）

### 2.3.1 `POST /api/code/submit`
**请求**
```json
{
  "studentId": 1001,
  "assignmentId": 5,
  "assignmentItemId": 12,
  "language": "java",
  "sourceCode": "public class Main{ public static void main(String[] a){System.out.println(\"hi\");} }",
  "expectedOutput": "hi\n",
  "className": "Main"
}
```
**响应（202，异步判分）**
```json
{ "code": 0, "message": "ok", "data": { "submissionId": 1024, "status": 0 } }
```
> 判分在后台线程/Worker 执行。前端轮询 `GET /api/code/result/{id}`。
> **请求体最终形态（见 C1）**：`{studentId, assignmentId, assignmentItemId, language, sourceCode, expectedOutput, className}` —— code 接收 `assignmentId` 用于事件回填关联，以 `expectedOutput` 参与判分权重；`className` 用于编译/运行定位入口类。

### 2.3.2 `GET /api/code/result/{id}`
```json
{
  "code": 0, "message": "ok",
  "data": {
    "submissionId": 1024,
    "status": 1,
    "stdout": "hi\n",
    "runTimeMs": 42,
    "compileOk": 1,
    "checkstyle": { "errorCount": 0, "warningCount": 1,
      "violations": [ { "file":"Main.java","line":1,"severity":"warning","message":"缺少 Javadoc","source":"JavadocMethod","rule":"JavadocMethod" } ] },
    "pmd": { "violationCount": 0, "violations": [] },
    "aiSuggestion": "整体良好，建议补充方法注释。",
    "overallScore": 96
  }
}
```
> **响应字段与 `grades` 列一一对应（见 C1）**：`stdout`/`runTimeMs`/`compileOk` → `run_result`；`checkstyle`/`pmd` → `static_report`；`aiSuggestion` → `ai_report`；`overallScore` → `score`；`status` 为判分状态。内部 `scoreDetail` 权重明细不再外暴露（判分逻辑见 §2.4.6）。

### 2.3.3 `GET /api/code/submissions`
Query：`?studentId=1001&assignmentItemId=12&page=1&pageSize=20`
响应：`Result<PageResult<CodeSubmission>>`（records 不含 report）。

### 2.3.4 `POST /api/code/run`（免作业快速运行）
请求同 submit 但不写 `assignment_item_id`，仅编译+沙箱+返回 stdout/stderr，**不调 AI、不写 report**（用于编辑器"运行"按钮，低延迟）。

### 2.3.5 `GET /api/code/health`
```json
{ "status":"ok", "docker":"available", "ai":"reachable" }
```

## 2.4 关键实现

### 2.4.1 包结构与启动
```
com.eduagent.code
 ├─ CodeServiceApplication
 ├─ controller/CodeController
 ├─ service/
 │   ├─ SubmissionService
 │   ├─ compiler/JavaCompileService
 │   ├─ checker/StaticCheckService
 │   ├─ sandbox/DockerSandboxService
 │   └─ score/ScoreService
 ├─ client/AiServiceClient
 ├─ mq/CodeGradedProducer
 ├─ entity/  mapper/  config/  dto/
 └─ (复用 common 的 Result/AuthContext 等)
```
> 复用 P0 `common`：`Result`、`PageResult`、`AuthContext`、`AuthContextFilter`、`AuthFeignInterceptor`、`BusinessException`。

### 2.4.2 编译（`javax.tools.JavaCompiler`）
内存编译（不落盘，安全且快）：
```java
public CompileResult compile(String className, String source) {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();
    try (StandardJavaFileManager fm = compiler.getStandardFileManager(diags, null, UTF_8)) {
        JavaFileObject srcObj = new MemorySourceJavaFileObject(className, source);
        StringWriter err = new StringWriter();
        boolean ok = compiler.getTask(err, fm, diags,
                List.of("-Xlint:all"), null, List.of(srcObj)).call();
        if (!ok) return CompileResult.fail(err.toString());
        Map<String, byte[]> classes = collectClasses(fm);
        return CompileResult.success(classes);
    }
}
```
> `MemorySourceJavaFileObject` / `MemoryClassFileManager` 为标准实现。若内存编译踩坑，退化为"写临时目录 + `javac`"。**编译在宿主 JVM 内完成，不在沙箱**——编译只是字节码生成，不执行用户代码，安全。

### 2.4.3 静态检查（Checkstyle + PMD）
- 配置文件放 `src/main/resources/checkstyle/checkstyle.xml` 与 `pmd/ruleset.xml`（见 §2.4.3.1）。
- API：`com.puppycrawl.tools.checkstyle.Main` 程序化调用 / `checkstyle` API；PMD 用 `net.sourceforge.pmd.PMD` / `PmdAnalysis`。
- 产出统一为 `List<Violation>` → 转 JSON 存 `code_check_reports.checkstyle/pmd`。

**错误模型**
```java
public record CheckstyleViolation(String file, int line, int column,
        String severity, String message, String source, String rule) {}
public record PmdViolation(String file, int line, String ruleSet,
        String rule, int priority, String message, String description) {}
```

**§2.4.3.1 配置片段**
`checkstyle.xml`（教学级，不过度严格）：
```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
  "https://checkstyle.org/dtds/configuration_1_3.dtd">
<module name="Checker">
  <property name="severity" value="warning"/>
  <module name="TreeWalker">
    <module name="JavadocMethod"/>
    <module name="ConstantName"/>
    <module name="LocalVariableName"/>
    <module name="MethodName"/>
    <module name="AvoidStarImport"/>
    <module name="EmptyStatement"/>
    <module name="MissingOverride"/>
    <module name="MagicNumber"/>
  </module>
</module>
```
`pmd/ruleset.xml`：包含 `Best Practices`(空指针/资源未关)、`Code Style`、`Design`(上帝类) 子集，`priority` ≤ 3 记为违规。

### 2.4.4 Docker 沙箱（★安全红线）

**两种挂载方式对比（主蓝图 §7.3）**

| 方案 | 做法 | 优点 | 缺点 | 结论 |
|------|------|------|------|------|
| (a) 挂载宿主 docker.sock | code-service 容器内 `-v /var/run/docker.sock:/var/run/docker.sock`，用 docker-java 连 `unix:///var/run/docker.sock` 起子容器 | 简单、启动快、资源占用低 | 容器获宿主 Docker 控制权，需评估 | **推荐先验证 (a)** |
| (b) Docker-in-Docker | 独立 `docker:dind` 容器，code-service 连 dind 的 2376 TLS 守护 | 隔离更好，子容器崩溃不影响宿主 | 运维重、镜像大、构建慢 | 生产再评估 |

**本 spec 选 (a)，用 `com.github.docker-java:docker-java` 库（不依赖宿主 docker CLI）**：
```java
@Bean
public DockerClient dockerClient() {
    return DockerClientBuilder.getInstance("unix:///var/run/docker.sock").build();
}
```
```java
public RunResult run(String className, Map<String,byte[]> classes) {
    String sid = UUID.randomUUID().toString();
    String hostDir = "/tmp/sandbox/" + sid;
    writeClasses(hostDir, classes);
    try {
        CreateContainerResponse c = dockerClient.createContainerCmd("openjdk:17-slim")
            .withCmd("sh","-c","cd /code && java " + className)
            .withHostConfig(HostConfig.newHostConfig()
                .withMemory(256L * 1024 * 1024)
                .withCpuCount(1L)
                .withNetworkMode("none")
                .withReadonlyRootfs(true)
                .withBinds(new Bind(hostDir, new Volume("/code"), AccessMode.ro)))
                .withAutoRemove(true)
            .exec();
        String cid = c.getId();
        dockerClient.startContainerCmd(cid).exec();
        boolean finished = dockerClient.waitContainerCmd(cid)
            .start().awaitStatusCode(5, TimeUnit.SECONDS) != null;
        if (!finished) { dockerClient.killContainerCmd(cid).exec();
                         return RunResult.timeout(); }
        String log = dockerClient.logContainerCmd(cid)
            .withStdOut(true).withStdErr(true).exec(new FrameReader()).read();
        return RunResult.ok(log, extractTime(cid));
    } finally { cleanup(hostDir); }
}
```
> **安全清单（硬约束，缺一不可）**：内存限制 / CPU 限制 / `--network=none` / `--read-only` / 只读挂载代码目录 / 5s `waitFor` 超时 + `killContainer` 强杀 / `withAutoRemove` 即用即焚 / 宿主临时目录用后即清。任何一项缺失都禁止上线。

### 2.4.5 Feign 调 AI 纠错
```java
@FeignClient(name = "ai-service", url = "${ai.service-url:http://ai-service:8001}",
             configuration = AiFeignConfig.class)
public interface AiServiceClient {
    @PostMapping("/api/ai/code/analyze")
    Result<CodeAnalyzeData> analyze(@RequestBody CodeAnalyzeRequest req);
}
```
`CodeAnalyzeRequest` 字段见 ai-service §1.3.4。**容错**：AI 调用必须设短超时 + fallback（返回空建议），绝不能因 AI 抖动导致判分失败（Sentinel/超时在 P4 §3.1 固化）。

### 2.4.6 判分逻辑（§2.6）
综合三项，封顶 100、底 0：
```
compile(40)  : 编译通过 +40，否则 0（且 status=4 直接结束，不进沙箱）
checkstyle   : 每 error -3，每 warning -1（下限 -20）
pmd          : 每 violation -3（下限 -20）
run(60)      : 编译+检查通过才运行；运行完成 +40，stdout==expectedOutput 再 +20
AI(参考)     : 不加减分，仅写入 ai_suggestion 供教师/学生查看
```
> 例：编译过(40) + 检查 1warning(-1) + 运行通过且输出正确(60) = **99**；有 1 error(-3) → 96。权重在 `ScoreService` 常量集中，便于调参。

### 2.4.7 与 teacher-service 对接（吴友诚 assignment 调 code）
```
teacher-service 发布代码作业(assignment_items.type=code)
   → 学生前端调用 code-service /api/code/submit(assignmentItemId=?)
   → code-service 判分完 发 MQ 事件 assignment.graded
   → teacher-service 消费：按 (assignment_id, student_id, item_id) 幂等写 grades
       grades.submission=report, grades.run_result=..., grades.static_report=...,
       grades.ai_report=ai_suggestion, grades.score=overall_score
   → 教师在 teacher 端"批改/复核"页可微调 score（人工覆盖）
```
> code-service **只写 code_db**；成绩归属 teacher_db.grades，由 teacher 消费事件写入（跨服务最终一致，避免分布式事务，见 §4.3）。`assignment.graded` 事件体（**必须携带完整报告体，不只 overallScore/aiSuggestion，见 C1**）：
```json
{ "assignmentId":5, "assignmentItemId":12, "studentId":1001,
  "submissionId":1024, "status":"graded",
  "runPassed":true, "compileOk":1,
  "stdout":"hi\n", "runTimeMs":42,
  "checkstyle":{ "errorCount":0, "warningCount":1, "violations":[ { "file":"Main.java","line":1,"severity":"warning","message":"缺少 Javadoc","source":"JavadocMethod","rule":"JavadocMethod" } ] },
  "pmd":{ "violationCount":0, "violations":[] },
  "aiSuggestion":"整体良好", "overallScore":96 }
```

## 2.5 测试

- 单测：`ScoreService` 多组样例（编译失败/检查违规/输出不符）断言分数；`JavaCompileService` 正确/错误源码断言 `compileOk`。
- 静态检查：构造含魔法数字/缺 `@Override` 的源码，断言 Checkstyle/PMD violation 命中。
- 沙箱集成（**仅 CI 宿主有 Docker 时跑**，否则 `@Disabled`）：提交 `System.out.println("hi")` → 断言 stdout=`hi\n`、status=1；提交 `while(true){}` → 断言 5s 超时 status=3。
- 容错：mock `AiServiceClient` 抛异常 → 断言判分仍完成、`aiSuggestion=null`。

## 2.6 验收

- [ ] 提交合法 Java → 编译/检查/沙箱全过 → `overall_score` 合理（如 90+）。
- [ ] 提交含死循环代码 → 5s 超时强杀，`status=3`，**宿主不被拖垮**。
- [ ] 提交危险代码（`Runtime.getRuntime().exec(...)`）→ 因 `--network=none --read-only` 无法造成实质破坏（安全验证项）。
- [ ] `docker ps` 运行后无残留容器（`--rm`/`withAutoRemove` 生效）。
- [ ] AI 不可用时不阻塞判分，仅 `aiSuggestion` 为空。
- [ ] 判分完 `assignment.graded` 事件被 teacher-service 消费并落 `grades`。

---

# §3. P4 全栈加固文档

> 前置：全部服务已存在（P0–P3）。本阶段把"临时配置"固化为"可演示、可监控、质量门全绿"的工程状态。

## 3.1 Sentinel 规则固化

**网关层**（P0 已引入依赖）：对高代价路由限流，规则放 Nacos `dataId=sentinel-gateway-rules`：
```json
[
  { "resource": "ai_chat", "limitApp": "default", "grade": 1, "count": 20, "strategy": 0 },
  { "resource": "code_submit", "limitApp": "default", "grade": 1, "count": 5, "strategy": 0 }
]
```
> `code_submit` 限流最严（沙箱宝贵）；`ai_chat` 次之（LLM 稀缺）。网关流控用 `SentinelGatewayBlockException` → 返回 429。

**服务层**：`@SentinelResource` + `blockHandler`/`fallback`：
```java
@SentinelResource(value = "codeSubmit", blockHandler = "onBlock", fallback = "onFallback")
public Result<?> submit(CodeSubmitRequest req) { ... }
public Result<?> onBlock(CodeSubmitRequest req, BlockException e) {
    return Result.error(429, "代码判分繁忙，请稍后再试");
}
```
规则同样经 Nacos `SentinelDataSource` 动态推送（不写死在代码）。

## 3.2 SkyWalking 自定义看板/告警

- 每个 Java 服务挂 agent（P0 §8 方式二 volume 挂载）：
  ```yaml
  environment:
    - JAVA_TOOL_OPTIONS=-javaagent:/skywalking/agent/skywalking-agent.jar
    - SW_AGENT_NAME=code-service
    - SW_AGENT_COLLECTOR_BACKEND_SERVICES=skywalking-oap:11800
  volumes: [ "./skywalking-agent:/skywalking/agent:ro" ]
  ```
- 自定义 Dashboard（OAP `ui-templates`）：新增「代码判分耗时 P99」「沙箱超时率」「AI 调用失败率」「网关 QPS」面板。
- 告警 `config/alarm-settings.yml`：
  ```yaml
  rules:
    - name: code-service-high-latency
      metrics-name: service_resp_time
      op: ">"
      threshold: 3000
      period: 10
      count: 3
      silence-period: 30
      message: "code-service 平均响应 >3s"
  ```
- 采样率：`agent.sample_rate=100`（演示期全采样；生产调低）。

## 3.3 压测方案

- 工具：JMeter（GUI 建计划，CLI `jmeter -n -t` 跑）。基准环境：本地 Docker 全栈，≥16GB。
- 场景：
  1. **对话基线**：`POST /api/ai/chat` 50 并发 × 5min，目标 P95<2s（命中 LLM 缓存/ Mock LLM 时 <500ms）。
  2. **代码判分**：`POST /api/code/submit` 10 并发 × 5min，目标：沙箱不积压、CPU<70%、无残留容器。
  3. **资源生成**：`POST /api/resource/generate` 20 并发，观察 Sentinel 是否触发限流（429 比例在预期内）。
- 产出：`docs/benchmark/` 放 JMeter `.jmx` + 结果 `*.jtl` + 截图；README 写「压测命令 + 预期指标」。

## 3.4 CI 质量门

GitHub Actions / GitLab CI（`.github/workflows/ci.yml`）：
```yaml
jobs:
  build:
    steps:
      - run: mvn -q -B clean verify
      - run: mvn checkstyle:check pmd:check spotbugs:check
      - run: mvn jacoco:report
```
- **覆盖率门**：核心逻辑（code 判分/编译/沙箱、ai 检索）行覆盖 ≥ 60%，门禁不达标 PR 红。
- **代码规范门**：Checkstyle/PMD/SpotBugs 作为 `verify` 阶段强制（**code-service 自用这套检查，元闭环**——主蓝图 §11.1）。
- **契约测试**：Spring Cloud Contract（Pact）——ai-service 的 OpenAPI 与 resource/learning 的 Feign stub 互验，防前后端漂移（主蓝图 §5.5 / §11.3）。
- AI 侧：pytest 跑通（检索空上下文不崩、降级回退）。

## 3.5 演示脚本与文档清单

**演示脚本 `scripts/demo.sh`（一键）**
```bash
docker-compose up -d
sleep 30
TOKEN=$(curl -s -XPOST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
        -d '{"username":"teststudent","password":"student123"}' | jq -r .data.token)
curl -s localhost:8080/api/ai/chat -H "Authorization: Bearer $TOKEN" \
     -H 'Content-Type: application/json' -d '{"userInput":"什么是多态","studentId":"1001"}'
curl -s -XPOST localhost:8080/api/code/submit -H "Authorization: Bearer $TOKEN" \
     -H 'Content-Type: application/json' \
     -d '{"studentId":1001,"language":"java","sourceCode":"public class Main{public static void main(String[]a){System.out.println(\"hi\");}}","expectedOutput":"hi\n"}'
curl -s -XPOST localhost:8080/api/ai/kb/rebuild -H "Authorization: Bearer $ADMIN" -d '{}'
```
**文档清单（交付物）**
- `README.md`：架构图、端口表、启动步骤、默认账号（admin/admin123、teststudent/student123、testteacher/teacher123）。
- `docs/architecture-overview.md`：服务依赖图、DB-per-service 边界。
- `docs/api/<service>.yaml`：各服务 OpenAPI（网关聚合）。
- `docs/benchmark/`：压测计划 + 结果。
- `docs/runbook.md`：常见故障（沙箱超时、Chroma 未连接、Sentinel 误杀）排查。

## 3.6 P4 验收（Definition of Done）

- [ ] Sentinel 网关+服务规则全部在 Nacos 固化，重启后规则不丢。
- [ ] SkyWalking 看板可见 `gateway→auth→...→code→ai` 全链路，自定义面板就位，告警规则生效。
- [ ] 压测三场景达标，报告归档。
- [ ] CI 全绿：Maven 构建 + 单测 + 覆盖率门 + Checkstyle/PMD/SpotBugs + 契约测试，任一失败阻断合并。
- [ ] `demo.sh` 一键跑通端到端，README/文档清单齐全。

---

# §4. 单体 → 微服务拆分指导

> 对象：`edu-agent-server/src/main/java/com/eduagent/**` 现有单体。目标：按主蓝图 §3.3 拆成 6 个 Spring Boot 应用（单仓多 Maven module）+ 1 个 Python ai-service。
> 本节能直接指导陈嘉成(resource)、陈海洋(learning)/吴友诚(teacher)动手；吴友诚的 code 已独立成 §2。

## 4.1 现有类归属总表（逐文件）

### 4.1.1 → auth-service（吴友诚/P0 已建，陈海洋迁移时注意）
| 现有类 | 动作 |
|--------|------|
| `controller/AuthController.java` | 迁 auth-service |
| `controller/UserController.java` | 迁 auth-service |
| `service/AuthService` + `impl/` | 迁 auth-service |
| `service/UserService` + `impl/` | 迁 auth-service |
| `security/JwtTokenProvider、UserDetailsServiceImpl、JwtAuthenticationFilter` | 迁 auth-service；**注意**：P0 已用 Gateway 过滤器替代 `JwtAuthenticationFilter`，单体 Filter 删除，改 `AuthContextFilter`（来自 common） |
| `entity/User.java` | 迁 auth_db（`users` 表，加 role 关联） |

### 4.1.2 → learning-service（陈海洋）
| 现有类 | 动作 |
|--------|------|
| `controller/ProfileController、LearningPathController、OnboardController、QuizController、DashboardController、ExplainController、TutorController` | 迁 learning-service |
| `service/StudentProfileService(+impl)、LearningPathService(+impl)、ExplainService(+impl)、TutorService(+impl)` | 迁 learning-service |
| `entity/StudentProfile、LearningPath、LearningPathHistory、LearningTask、Task、StudyLog、Conversation、QuizAnswer` | 迁 learning_db |
| `mapper/*` 对应上述 | 迁 learning-service |

> 注意：`ExplainController`/`TutorController` 现用 `agent/AiClient` 直连 AI → 改为 **Feign `AiServiceClient`**（§1.5.3）调 `lb://ai-service/api/ai/chat`。

### 4.1.3 → resource-service（陈嘉成）
| 现有类 | 动作 |
|--------|------|
| `controller/ResourceController` | 迁 resource-service（路径改 `/api/resource/**`，对齐网关） |
| `service/ResourceService(+impl)、ContentReviewService(+impl)` | 迁 resource-service（impl 里 `aiClient.generateResource` 改为 Feign 调 `/api/ai/resource/generate`，契约见 ai-service §1.3.2） |
| `entity/Resource` | 迁 resource_db（`resources` 表，列基本复用现有 `resources` DDL） |
| `mapper/ResourceMapper` | 迁 resource-service |
| `exercise_records`、`admin_stats_cache` 表 | 迁 resource_db（对应 `service/impl/ResourceServiceImpl` 写库逻辑保留） |

> 对齐：陈嘉成调用 AI 时，`generateResource` 的请求字段 `studentId/chapterName/topic/type/level` → ai `/resource/generate`（ai-service §1.3.2）；返回 `content` 字符串直接落 `resources.content`。

### 4.1.4 → teacher-service（吴友诚，原 report-service 8084）
| 现有类 | 动作 |
|--------|------|
| `controller/AdminController、ReportController` | 迁 teacher-service |
| `service/AdminService(+impl)、ReportService(+impl)` | 迁 teacher-service |
| `entity/Report` | 迁 teacher_db |
| `mapper/ReportMapper` | 迁 teacher-service |
| **新增**（陈海洋 P3 建）：`classes、class_students、questions、assignments、assignment_items、grades` | 见主蓝图 §4.3 DDL |
| 作业批改消费 `assignment.graded` 事件（§2.4.7） | 吴友诚在 teacher-service 加 MQ 消费者 |

### 4.1.5 → code-service（吴友诚，见 §2）
- 单体里**没有**代码相关类，全新增。前端的"代码题"由 teacher 的 `questions.type=code` + code-service 判分协作完成。

### 4.1.6 → ai-service（陈海洋，见 ai-service §1）— 非 Java
- `controller/JavaNotesController` + `service/JavaNotesService` + `entity/JavaNotes`：知识源**导出为 Markdown**（ai-service §1.4.1），不再作为 Java 服务表；管理端"知识库"走 ai-service `/kb/rebuild`。
- `entity/AgentConfig` + `service/AgentConfigService`：prompts 配置迁 ai-service（或 Nacos 动态配置，管理端可热更）。
- `entity/SystemSetting` + `service/SystemSettingService`：系统设置类配置迁 **Nacos 配置中心**（不再落业务库）。

## 4.2 迁移步骤（通用，每个服务照做）

1. **建 module**：在 `edu-agent-server/pom.xml` 父工程下加 `<module>code-service</module>`，各 module 独立 `Application`、独立端口（Nacos 配置或 `application.yml`）。
2. **改包名**：`com.eduagent.Xxx` → `com.eduagent.<service>.Xxx`（如 `com.eduagent.code`），IDE 全局重构 + 校验 `@SpringBootApplication(scanBasePackages="com.eduagent.code")`。
3. **依赖 common**：每个 module `pom.xml` 加 `com.eduagent:common`（P0 已发），移除单体里重复的 `Result/PageResult/GlobalExceptionHandler/JwtTokenProvider` 等（改引 common）。
4. **换鉴权**：删单体 `security/*` 的 `JwtAuthenticationFilter`（Gateway 已做）；加 common 的 `AuthContextFilter` + `AuthFeignInterceptor`（P0 §6.2），控制器用 `AuthContext.getUserId()` 取身份（替换 `SecurityContextHolder` 取 principal 的写法，见 `ResourceController.getCurrentUserId`）。
5. **换数据源**：每个服务配自己库（`auth_db/learning_db/resource_db/code_db/teacher_db`），删跨库 `@TableName` 引用。
6. **换 AI 调用**：`agent/AiClient`（RestTemplate 直连）改为 Feign `AiServiceClient`（ai-service §1.5.3 / §2.4.5）。
7. **跨服务调用**：本地 `service` 互调 → Feign/MQ（§4.3）。
8. **Flyway/Liquibase**：每服务独立 `db/migration/V1__init.sql`，从现有 `init.sql`/`edu_agent.sql` 按归属裁剪，**禁止共用单体 init.sql**。
9. **种子数据**：真实测试数据写进 `V2__seed.sql`（配合前端零 mock 策略，主蓝图 §5.5）。

## 4.3 常见坑（重点）

- **包名扫描遗漏**：`@MapperScan`、`@ComponentScan` 默认只扫启动类同级包；跨包 entity/mapper 要显式配 `basePackages`。
- **事务边界**：单体 `@Transactional` 跨多表在一个库 OK；拆库后**跨库 @Transactional 失效**。改为：每服务只管自己库的本地事务；跨服务写用 **MQ 事件 + 幂等消费**（如 §2.4.7 成绩落 teacher_db）。
- **分布式事务取舍**：**本期不引入 Seata/2PC**（主蓝图非目标）。采用「本地事务 + 发事件 + 最终一致 + 消费者幂等」。例：作业发布(teacher) 与 代码判分(code) 不保证原子，靠 `assignment.graded` 事件最终补齐 grades；若 code 判分失败，grades 留空，教师端可见"待判分"状态。
- **跨服务查询禁直连库**：learning 不能 `SELECT` resource 的表；要数据走 Feign（如 teacher 聚合班级学情 → Feign 调 learning + code 拿数据后自己拼）。
- **身份透传**：Feign 调下游必须走 `AuthFeignInterceptor`（P0 已固化为 common 默认配置），**不要**在 Feign 请求里手填 `X-User-*` 头（会被网关视为伪造，且破坏透传一致性）。
- **AI 路径前缀**：所有 Java 侧调用 ai 的路径都带 `/api/ai` 前缀（网关不 StripPrefix），与 ai-service §1.5.1 一致；否则 404。
- **Chroma 连接**：ai-service 用 service 名 `chroma`（edu-net）而非 `localhost`；本地裸跑 ai 时设 `CHROMA_HOST=host.docker.internal`。
- **端口冲突**：单体 `EduAgentApplication` 跑在某一端口；拆后各服务 8081–8085 + ai 8001，单体入口在拆分完成、各服务验证通过后**整体下线**，避免双实例抢 Nacos 服务名。
- **移除硬编码**：删除单体 `EduAgentApplication` 启动强制重置 admin 密码逻辑（P0 §4.4），改初始化脚本。

## 4.4 拆分验收（Definition of Done）

- [ ] 单体内相关类按 §4.1 归属表全部迁出，单体 `EduAgentApplication` 下线。
- [ ] 5 个 Java 服务 + ai-service 在 Nacos 全部注册，网关路由按 `/api/<服务>/**` 命中。
- [ ] 任意跨服务调用经 Feign 且 `X-User-*` 透传正确；无服务直连他库。
- [ ] 每服务独立库、独立 migration、独立种子数据；前端接真数据、组件内零 mock。
- [ ] 代码作业全链路：teacher 发布 → code 判分 → `assignment.graded` → teacher grades 落库，端到端跑通。

---

## 附录 A：AI `/code/analyze` 固定 System Prompt（供 code-service 对齐）

```
你是一位资深 Java 代码评审专家。请基于以下信息给出改进建议。
【输入】源码、编译结果、Checkstyle/PMD 静态检查结果、运行结果。
【要求】只返回严格 JSON（不要 markdown/代码块），结构如下：
{
  "suggestions": [{"severity":"info|warning|error","location":"文件:行号","title":"...","detail":"...","example":"..."}],
  "summary": "一句话总结",
  "overall_comment": "整体评价",
  "score_hint": 0-100
}
【范围】仅 JavaSE 基础，指出可读性/健壮性/规范问题，不引入 Spring 等框架。
```

## 附录 B：与外部契约方对齐清单

| 契约 | 提供方 | 消费方 | 对齐章节 |
|------|--------------|--------|---------|
| `/api/ai/chat` | ai-service | learning(陈海洋)/前端 | ai-service §1.3.1 |
| `/api/ai/resource/generate` | ai-service | resource(陈嘉成) | ai-service §1.3.2 |
| `/api/ai/path/generate` | ai-service | learning(陈海洋) | ai-service §1.3.3 |
| `/api/ai/code/analyze`（内网） | ai-service | code(吴友诚) | ai-service §1.3.4 / §2.4.5 |
| `/api/ai/kb/rebuild` | ai-service | admin/teacher(吴友诚) | ai-service §1.3.5 |
| `/api/code/submit` `/result/{id}` | code-service | teacher(吴友诚)/前端 | §2.3 |
| `assignment.graded`(MQ) | code-service | teacher(吴友诚) | §2.4.7 |

---

*吴友诚子 spec 结束。P1(learning/resource)、P3(teacher/admin) 见对应成员子 spec；本文件覆盖 code-service / ai-service / P4 加固 / 拆分指导。*
