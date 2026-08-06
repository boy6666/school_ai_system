# 开发文档（子 Spec）：learning-service + ai-service

> 编写人：**陈海洋（成员C）** ｜ 阶段：**P1（learning-service）/ P2（ai-service）**
> 上游基线：主蓝图 `2026-07-31-edu-agent-platform-design.md`、P0 `2026-07-31-p0-infra-gateway.md`
> 协同方：吴友诚（code-service `/api/code/**`）、陈海洋（ai-service `/api/ai/**`）、陈嘉成（resource-service）、曾姿妍（前端）
> 文档粒度（对齐 P0）：**需求 → 接口契约 → 数据模型 → 关键实现 → 测试 → 验收**
> 端口：`learning-service`=8082（`/api/learning/**`，库 `learning_db`）；`teacher-service`=8084（`/api/teacher/**`，库 `teacher_db`）

---

> **统一约定（来自 P0）**
> - 身份：所有控制器通过 `com.eduagent.common.AuthContext.getUserId()` / `getRoles()` 取身份，**不自己解析 JWT**。
> - 角色常量：`RoleConstants.ROLE_STUDENT` / `ROLE_TEACHER` / `ROLE_ADMIN`（值 `ROLE_STUDENT`/`ROLE_TEACHER`/`ROLE_ADMIN`）。
> - 跨服务：Feign 调别的服务用 `common` 的 `AuthFeignInterceptor` 自动透传身份头，服务名走 Nacos。
> - 响应：`Result<T>` / `PageResult<T>`；异常抛 `BusinessException`，由 `GlobalExceptionHandler` 统一包装。
> - DB-per-service：**禁止跨库外键**。原单体里 `student_profiles`/`learning_*` 等表对 `users` 的 FK、以及 `quiz_answer.resource_id` 对 `resources` 的 FK，在独立库中**一律改逻辑引用（纯 BIGINT），不建外键**。
> - Feign 调 AI（Python，Nacos 名 `ai-service`）：路径**必须带 `/api/ai` 前缀**（如 `/api/ai/chat`），与网关转发路径完全一致（网关不 StripPrefix）；否则 Feign 直连会 404。规范见陈海洋 ai-service 子 spec §1.5.1。

---

# A. learning-service（P1，学情核心）

## A.1 需求与职责边界

### A.1.1 职责（本服务拥有）
学生学情全量数据，包含六大子域：
1. **学情画像（Profile）**：六维画像（知识掌握度 / 学习目标清晰度 / 认知风格适配 / 错误规避力 / 学习自主性 / 综合能力）、薄弱点、错误模式、资源偏好、学习目标等。
2. **学习路径（LearningPath）**：AI 生成路径、阶段/任务、整体进度；任务完成状态更新。
3. **学习日志（StudyLog）**：学习时长、模块、关联笔记/章节，用于看板统计。
4. **测验（Quiz）**：题目作答记录、错题、提交判分（judge）。
5. **学习报告（Report）**：AI 生成的学习总结/回顾/评价，存库复用。
6. **对话历史（Conversation）**：智能辅导 / 引导对话留存，供复盘与画像回写。

### A.1.2 边界（不属于本服务，防止越界）
- **资源生成** → `resource-service`（`/api/resource/**`）。learning-service 不生成 mindmap/quiz/reading/code 等素材；仅消费"测验判分"所需的 AI judge 能力（调 ai-service，不调 resource-service）。
- **代码编译/沙箱/判分** → `code-service`（`/api/code/**`）。
- **用户/角色/JWT** → `auth-service`（`/api/auth/**`）。learning-service 只认 `AuthContext` 里的 `userId`，不存用户密码。
- **班级/学生归属、题库、作业、成绩** → `teacher-service`（`/api/teacher/**`）。learning-service 不建 `classes` 表；但 `student_profiles` 增加 `class_id` 逻辑字段便于教师端按班聚合（外键不跨库）。

### A.1.3 迁移来源（单体 → 微服务）
| 单体来源 | 落点 |
|------|------|
| `ProfileController` / `StudentProfile` / `student_profiles` | learning-service profile 子域 |
| `LearningPathController` / `LearningPathService` / `learning_paths` / `learning_tasks` / `learning_path_history` | path 子域 |
| `DashboardController` 的 study_log + report 逻辑 / `study_logs` / `report` | studylog / report 子域 |
| `QuizController` / `quiz_answer` | quiz 子域 |
| `OnboardController` / `TutorController` / `conversation` | onboard / tutor（对话历史）子域 |
| `ReportController` / `report` | report 子域 |
| `AiClient`（RestTemplate → ai.base-url） | 替换为 `AiServiceClient`（Feign → `ai-service`）+ `AuthFeignInterceptor` 透传 |

---

## A.2 接口契约（`/api/learning/**`，端口 8082）

> 角色列：`S`=ROLE_STUDENT，`T`=ROLE_TEACHER，`A`=ROLE_ADMIN。无 JWT/无角色 → 网关 401/403。

### A.2.1 画像 Profile
| 方法 | 路径 | 角色 | 入参 | 出参 |
|------|------|------|------|------|
| GET | `/api/learning/profile` | S | —（userId 取 AuthContext） | `Result<ProfileVO>` |
| GET | `/api/learning/profile/{studentId}` | T,A | path `studentId` | `Result<ProfileVO>`（教师查看学生画像） |
| POST | `/api/learning/profile/save` | S | body：`pace,learningGoal,topic,course,knowledgeBase,cognitiveStyle,overallType,weaknesses[],resourcePreference[],mistakePatterns[]`（六维 `dimensions` 由 AI 合并进 `profile_data`，不在此 body） | `Result<{id,status}>` |
| POST | `/api/learning/profile/generate-suggestions` | S | body：`{userId?}`（默认取 AuthContext） | `Result<{suggestions:[]}>`（Feign→ai `/resource/generate` mode=suggestion） |

`ProfileVO`（出参，含六维）：
```json
{
  "studentId": 12, "classId": 3, "course": "JavaSE", "topic": "面向对象",
  "learningGoal": "...", "knowledgeBase": "...", "cognitiveStyle": "...",
  "pace": "medium", "weaknesses": ["..."], "mistakePatterns": ["..."],
  "resourcePreference": ["..."], "overallType": "稳定提升型",
  "lastScore": 72, "profileComplete": true,
  "dimensions": {
    "knowledge_mastery":   {"score": 70, "level": "level_2", "evidence": ["..."]},
    "learning_goal_clarity":{"score": 65, "level": "level_2", "evidence": ["..."]},
    "cognitive_adaptation": {"score": 60, "level": "level_2", "evidence": ["..."]},
    "mistake_avoidance":    {"score": 55, "level": "level_2", "evidence": ["..."]},
    "learning_autonomy":    {"score": 68, "level": "level_2", "evidence": ["..."]},
    "overall_level":        {"score": 66, "level": "level_2", "evidence": ["..."]}
  },
  "profileSuggestions": "..." , "lastSuggestion": "...", "updateTime": "..."
}
```
> 六维 `dimensions` 结构与 ai-service `school_agent/schemas/profile_schema.py` 的 `DimensionState`（score/level/evidence）**严格对齐**，`level` 取值 `level_1/level_2/level_3`，存 `profile_data` JSON。

### A.2.2 学习路径 Path
| 方法 | 路径 | 角色 | 入参 | 出参 |
|------|------|------|------|------|
| GET | `/api/learning/path/current` | S | — | `Result<LearningPathVO>` |
| POST | `/api/learning/path/generate` | S | —（读画像→Feign→ai `/path/generate`） | `Result<LearningPathVO>` |
| PUT | `/api/learning/path/task` | S | body：`{stageName, taskTitle, completed:bool}` | `Result<LearningPathVO>`（重算进度 + 发 `study.progress` 事件） |
| GET | `/api/learning/path/history` | S | — | `Result<List<PathHistoryVO>>` |

`LearningPathVO`（沿用单体结构，源自 `vo/LearningPathVO.java`）：
```json
{ "goal":"...", "targetMastery":"≥85%", "estimatedCompletion":"2026-08-30",
  "totalTasks":8, "completedTasks":3, "totalHours":24,
  "stages":[{"name":"今日计划","tasks":[{"id":1,"title":"...","duration":30,"status":0,"progress":0}]}],
  "suggestions":"...", "applicationAdvice":"...", "examAdvice":"...",
  "masteryRate":72, "learningRate":18, "unmasteredRate":10, "recommendTime":"每天19:00-21:00" }
```
> 字段来源（见《契约对齐决议》C9）：`POST /api/learning/path/generate` 调 ai-service `/path/generate` 后，**ai 返回** `goal / targetMastery / totalHours / masteryRate / stages[]`（含 tasks 的 title/duration/status/progress）/ `suggestions / applicationAdvice / examAdvice / recommendTime`；learning-service **自算补全** `totalTasks / completedTasks / learningRate / unmasteredRate`（基于 stages 与 `study_logs` 聚合），并为每个 `tasks[].id` 生成落库 ID，最后整体返回 `LearningPathVO`。

### A.2.3 学习日志 StudyLog（看板数据源）
| 方法 | 路径 | 角色 | 入参 | 出参 |
|------|------|------|------|------|
| POST | `/api/learning/study-log` | S | body：`{module, durationSec, chapterId?, noteId?}` | `Result<{id}>` |
| GET | `/api/learning/study-log/summary` | S | — | `Result<{today:[{module,total}], totalSec:int}>` |
| GET | `/api/learning/study-log/report` | S | — | `Result<{totalSec, modules:[{module,total}], trend:[{day,module,total}], score, profileData, weaknesses, goal, progress}>` |
| GET | `/api/learning/study-log/tasks` | S | — | `Result<List<{id,title,status,priority}>>` |
| GET | `/api/learning/study-log/path` | S | — | `Result<{goal,pace,progress,suggestions}>` |

`module` 取值：`mindmap`/`quiz`/`reading`/`code`（与 `study_logs.module` 一致）。

### A.2.4 测验 Quiz（作答 + 判分 judge）
| 方法 | 路径 | 角色 | 入参 | 出参 |
|------|------|------|------|------|
| GET | `/api/learning/quiz/answered` | S | query `resourceId` | `Result<List<{question,userAnswer,correctAnswer,isCorrect,explanation}>>` |
| GET | `/api/learning/quiz/wrong-questions` | S | — | `Result<List<错题>>` |
| GET | `/api/learning/quiz/wrong-questions/{id}` | S | path `id` | `Result<错题>` |
| POST | `/api/learning/quiz/judge` | S | body：`{resourceId?, question, questionType, userAnswer, correctAnswer, explanation?}` | `Result<{isCorrect, score, comment, savedId}>`（Feign→ai `/resource/generate` mode=judge） |

`POST /judge` 逻辑：把题目/标准答案/学生答案发给 ai-service（mode=`judge`），拿回 `{score(0|1), correct, comment}` → 写 `quiz_answer`（is_correct、explanation 用 AI/标准的 explanation）→ 返回。选择题可本地比对，简答/代码题走 AI judge。

### A.2.5 报告 Report
| 方法 | 路径 | 角色 | 入参 | 出参 |
|------|------|------|------|------|
| POST | `/api/learning/reports/generate` | S | body：`GenerateReportRequest{title, periodStart, periodEnd, metrics}` | `Result<ReportVO>`（Feign→ai `/chat` 生成，存 `report`） |
| GET | `/api/learning/reports` | S | query `page,size` | `Result<PageResult<ReportVO>>` |
| GET | `/api/learning/reports/{id}` | S | path `id` | `Result<ReportVO>` |
| DELETE | `/api/learning/reports/{id}` | S | path `id` | `Result<Void>` |
| POST | `/api/learning/dashboard/ai-summary` | S | — | `Result<Map>`（AI 学习总结，存 `report`） |
| GET | `/api/learning/dashboard/ai-summary` | S | — | `Result<Map>`（读最新 AI 总结） |
| POST | `/api/learning/dashboard/learning-review` | S | — | `Result<Map>`（AI 学习回顾，存 `report`） |
| GET | `/api/learning/dashboard/learning-review` | S | — | `Result<Map>` |
| GET | `/api/learning/dashboard/evaluation` | S | — | `Result<Map>`（六维画像 + 分数） |

> 说明：原 `DashboardController.learningReview()` 里"拉已发布资源"那段依赖 resource-service。为保持 learning-service **边界干净、不反向依赖 resource**，P1 版本**移除该段**（review 仅基于画像/路径/日志/错题）。如确需资源维度，后续经 Feign→resource-service 取轻量列表，不引入强耦合。

### A.2.6 引导 Onboard（画像采集）
| 方法 | 路径 | 角色 | 入参 | 出参 |
|------|------|------|------|------|
| POST | `/api/learning/onboard/chat` | S | body：`{message, sessionId, profile?}` | `Result<{finalAnswer, intent, profile, profileComplete, resourceDir}>`（Feign→ai `/chat`） |

### A.2.7 对话历史 Tutor（智能辅导）
| 方法 | 路径 | 角色 | 入参 | 出参 |
|------|------|------|------|------|
| GET | `/api/learning/tutor/sessions` | S | — | `Result<List<{sessionId,title,time}>>` |
| POST | `/api/learning/tutor/chat` | S | body：`{message, sessionId}` | `Result<TutorReplyVO>`（Feign→ai `/chat`，回写画像 + 落 `conversation`） |
| GET | `/api/learning/tutor/history` | S | query `sessionId?` | `Result<List<TutorReplyVO>>` |

`TutorReplyVO`：`{question, answer, intent, routeReason, evaluation, resourceDir}`，沿用 `vo/TutorReplyVO.java`。

### A.2.8 学情统计聚合（供 teacher-service 调用）
| 方法 | 路径 | 角色 | 入参 | 出参 |
|------|------|------|------|------|
| GET | `/api/learning/analytics/student/{studentId}` | T,A | path `studentId` | `Result<StudentAnalyticsVO>` |
| GET | `/api/learning/analytics/student/{studentId}/progress` | T,A | path `studentId` | `Result<{progress, completedTasks, totalTasks, masteryRate, lastScore, totalStudySec, wrongCount, pathGoal}>`（轻量，供看板逐生聚合） |

`StudentAnalyticsVO`（看板聚合，单学生）：
```json
{ "studentId":12, "classId":3, "course":"JavaSE", "topic":"面向对象",
  "lastScore":72, "profileComplete":true,
  "dimensions":{"knowledge_mastery":{"score":70,"level":"level_2"}, ... },
  "path":{"goal":"...","progress":40,"completedTasks":3,"totalTasks":8},
  "study":{"totalSec":5400,"modules":[{"module":"quiz","total":1800}]},
  "quiz":{"answered":35,"wrong":8,"accuracy":0.77},
  "recentReports":[{"id":1,"title":"AI学习总结","createTime":"..."}] }
```

---

## A.3 数据模型（`learning_db`，DDL）

> 原则：MySQL8 / InnoDB / utf8mb4；**去除所有跨库 FK**（原 `users` 外键、原 `resources` 外键）；表名沿用单体运行库（`edu_agent.sql`）实际使用的名字，降低迁移成本。
> 新增：`student_profiles.class_id`、`student_profiles.profile_complete`、`student_profiles.major/grade`（已在原表）、`study_logs` 沿用 `module/duration_sec/chapter_id/note_id/created_at`。

```sql
CREATE DATABASE IF NOT EXISTS learning_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE learning_db;

-- 1. 学生画像（新增 class_id / profile_complete；移除对 users 的 FK）
CREATE TABLE student_profiles (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL COMMENT '逻辑关联 auth_db.users.id（无 FK）',
  class_id BIGINT DEFAULT NULL COMMENT '逻辑归属班级 teacher_db.classes.id（无 FK）',
  major VARCHAR(100) DEFAULT NULL,
  grade VARCHAR(50) DEFAULT NULL,
  course VARCHAR(100) DEFAULT NULL,
  topic VARCHAR(100) DEFAULT NULL,
  learning_goal TEXT,
  knowledge_base TEXT,
  cognitive_style VARCHAR(50) DEFAULT NULL,
  pace VARCHAR(50) DEFAULT NULL,
  weaknesses JSON,
  mistake_patterns JSON,
  resource_preference JSON,
  overall_type VARCHAR(50) DEFAULT NULL,
  last_score INT DEFAULT NULL,
  profile_data JSON COMMENT '六维画像 JSON，结构对齐 ai-service profile_schema',
  profile_suggestions TEXT,
  last_suggestion TEXT,
  profile_complete TINYINT(1) NOT NULL DEFAULT 0 COMMENT '0未完成引导 1已完成',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_student_id (student_id),
  INDEX idx_class_id (class_id),
  INDEX idx_topic (topic)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生画像表';

-- 2. 学习路径
CREATE TABLE learning_paths (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  steps LONGTEXT COMMENT '学习路径 JSON（LearningPathVO 序列化）',
  progress INT DEFAULT 0 COMMENT '整体进度%',
  pace VARCHAR(50) DEFAULT 'medium',
  goal TEXT,
  suggestions TEXT,
  recommendations TEXT,
  exam_advice TEXT,
  status VARCHAR(20) DEFAULT 'active',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_student_status (student_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习路径表';

-- 3. 学习任务（status: todo/doing/done；stage: today/week/exam/practice）
CREATE TABLE learning_tasks (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  course_name VARCHAR(100) DEFAULT NULL,
  chapter_name VARCHAR(100) DEFAULT NULL,
  stage VARCHAR(20) DEFAULT 'today',
  start_time DATETIME DEFAULT NULL,
  end_time DATETIME DEFAULT NULL,
  priority ENUM('high','middle','low') DEFAULT 'middle',
  status ENUM('todo','doing','done') DEFAULT 'todo',
  progress INT DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_id (user_id),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习任务表';

-- 4. 学习路径历史
CREATE TABLE learning_path_history (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  goal TEXT,
  path_data JSON,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习路径历史表';

-- 5. 学习日志（沿用运行库字段）
CREATE TABLE study_logs (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  module VARCHAR(20) DEFAULT NULL COMMENT 'mindmap/quiz/reading/code',
  duration_sec INT DEFAULT 0,
  chapter_id INT DEFAULT NULL,
  note_id INT DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_student (student_id),
  INDEX idx_date (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习日志表';

-- 6. 测验作答（去除对 resources 的 FK，resource_id 仅逻辑引用）
CREATE TABLE quiz_answer (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  resource_id BIGINT DEFAULT NULL COMMENT '逻辑引用 resource_db（无 FK）',
  question TEXT,
  question_type VARCHAR(50) DEFAULT NULL,
  user_answer TEXT,
  correct_answer VARCHAR(500) DEFAULT NULL,
  is_correct TINYINT DEFAULT NULL,
  explanation TEXT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_student (student_id),
  INDEX idx_resource (resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测验作答表';

-- 7. 报告
CREATE TABLE report (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT DEFAULT NULL,
  title VARCHAR(200) DEFAULT NULL,
  content TEXT,
  period_start DATE DEFAULT NULL,
  period_end DATE DEFAULT NULL,
  metrics TEXT,
  create_time DATETIME DEFAULT NULL,
  INDEX idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习报告表';

-- 8. 对话历史
CREATE TABLE conversation (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  session_id VARCHAR(100) DEFAULT NULL,
  question TEXT NOT NULL,
  answer MEDIUMTEXT,
  intent VARCHAR(50) DEFAULT NULL,
  intent_confidence VARCHAR(20) DEFAULT NULL,
  evaluation_report TEXT,
  resource_dir VARCHAR(500) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_student (student_id),
  INDEX idx_session (student_id, session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话历史表';
```

> **种子数据（Flyway/seed.sql，不进前端）**：注入 1~2 个测试学生的画像、1 条 learning_path、若干 learning_tasks、若干 study_logs、若干 quiz_answer（含错题），供教师端看板联调。

---

## A.4 关键实现

### A.4.1 分层结构
```
learning-service/
├── LearningServiceApplication.java
├── controller/   ProfileController, LearningPathController, StudyLogController,
│                QuizController, ReportController, DashboardController,
│                OnboardController, TutorController, AnalyticsController
├── service/      ProfileService, LearningPathService, StudyLogService,
│                QuizService, ReportService, TutorService, AnalyticsService
├── service/impl/ *ServiceImpl
├── mapper/       *Mapper（MyBatis-Plus）
├── entity/       StudentProfile, LearningPath, LearningTask, LearningPathHistory,
│                StudyLog, QuizAnswer, Report, Conversation
├── vo/           ProfileVO, LearningPathVO, ReportVO, TutorReplyVO, StudentAnalyticsVO
├── dto/          SaveProfileRequest, GenerateReportRequest, JudgeRequest, OnboardRequest, TutorRequest
├── feign/        AiServiceClient            （→ ai-service，AuthFeignInterceptor 透传）
├── mq/           StudyProgressEvent, StudyProgressPublisher, StudyProgressConsumer(无)
├── config/       FeignConfig(继承 common), RabbitConfig, MybatisPlusConfig
└── resources/    application.yml（Nacos）, mapper/*.xml（少量自定义 SQL）
```

### A.4.2 身份与鉴权
```java
// 每个 controller 内：
Long studentId = AuthContext.getUserId();
List<String> roles = AuthContext.getRoles();
```
角色校验用 `@PreAuthorize("hasRole('ROLE_TEACHER')")` 或手动 `if (!roles.contains(ROLE_TEACHER)) throw new BusinessException(403,"无权限");`（按 P0 约定，不引 Security 表达式也行，统一用 AuthContext 工具方法 `AuthContext.requireRole(...)`）。

### A.4.3 画像提取逻辑（与 ai `profile_extractor` 对齐）
- **引导/辅导回写画像**：`OnboardController`/`TutorController` 调 `AiServiceClient.chat(...)` 拿到 `AiChatResult.profile`（六维 + 辅助字段）。
- 落库时把 AI 返回的 profile 合并进 `student_profiles.profile_data`，结构**严格等同于** ai-service `apply_profile_changes` 产出的 `DimensionState`（score/level/evidence）。learning-service 侧提供 `ProfileMapper.mergeAiProfile(Long studentId, Map profile)`：
  - 读现有 `profile_data` JSON → 合并 `topic/learning_goal/cognitive_style/overall_type`；
  - 合并 `weaknesses`/`mistake_patterns`（去重，限长）；
  - 用 AI 的六维 `score` 经指数移动平均（旧0.6/新0.4）写入；
  - 引导完成时置 `profile_complete=1`。
- 这样 learning-service 是 ai `profile_extractor` 的**唯一落库方**，避免双写漂移。

### A.4.4 学习路径进度更新 + 事件
`LearningPathServiceImpl.updateTaskStatus`：
1. 按 `studentId` 查 `learning_tasks`，匹配 `title` 更新 `status/progress`；
2. 重算 `completedTasks/totalTasks` → 更新 `learning_paths.progress`；
3. 发布 RabbitMQ 事件 `study.progress`（供 teacher-service 看板消费）：
```java
// mq/StudyProgressEvent.java
@Data
public class StudyProgressEvent {
    private Long studentId;
    private Long classId;          // 来自 profile.class_id
    private Integer progress;      // 路径整体进度
    private Integer completedTasks;
    private Integer totalTasks;
    private Integer masteryRate;   // 六维平均
    private LocalDateTime eventTime;
}
// 发送：RabbitTemplate.convertAndSend("study.progress","study.progress", event);  // exchange 名 = 事件名（与 resource.generate 风格统一，见《契约对齐决议》C12）
```

### A.4.5 跨服务调用（仅 Feign → ai-service）
```java
// feign/AiServiceClient.java  → ai-service（路径带 /api/ai 前缀，与网关一致，不 StripPrefix）
@FeignClient(name = "ai-service", url = "${ai.base-url:}", path = "/api/ai") // 陈海洋确保 ai-service 在 Nacos 可达；url 兜底
public interface AiServiceClient {
    @PostMapping("/chat")                Result<AiChatResult> chat(@RequestBody AiChatRequest req);
    @PostMapping("/path/generate")       Result<LearningPathVO> generatePath(@RequestBody AiPathRequest req);
    @PostMapping("/resource/generate")   Result<Map<String,Object>> generateResource(@RequestBody AiResourceRequest req);
}
// generateResource 的 AiResourceRequest 必须带 mode 字段（见《契约对齐决议》C3），按 mode 解析：
//   mode=suggestion → 解析 {suggestions:[]}                              （profile/generate-suggestions A.2.1）
//   mode=judge      → 解析 {score(0|1), correct, comment, explanation?}  （quiz/judge A.2.4）
//   mode=resource   → 解析 {content, resourceType, chapter}               （默认，资源生成）
// 结果 Map 按 mode 反序列化为对应结构，再包装成各端点出参。
// AiChatRequest{ userInput, studentId, sessionId, profile }
// AiChatResult{ intent, finalAnswer, profile, resources, learningPath, safetyReport,
//               evaluationReport, resourceDir, profileComplete }
```
> 注：路径/资源/判分都复用 ai-service 既有端点；learning-service **不新增** ai 端点。原 `AiClient`（RestTemplate）废弃。

### A.4.6 配置骨架
```yaml
# application.yml（Nacos 配置中心）
server:
  port: 8082
spring:
  application:
    name: learning-service
  cloud:
    nacos:
      discovery:
        server-addr: nacos:8848
      config:
        server-addr: nacos:8848
        namespace: edu-dev
        group: learning-group
  datasource:
    url: jdbc:mysql://mysql:3306/learning_db?useSSL=false&serverTimezone=Asia/Shanghai
    username: ${DB_USER}
    password: ${DB_PWD}
    driver-class-name: com.mysql.cj.jdbc.Driver
mybatis-plus:
  mapper-locations: classpath*:mapper/*.xml
  global-config:
    db-config:
      id-type: auto
rabbitmq:             # 来自 common 的 RabbitConfig
  host: rabbitmq
  port: 5672
ai:
  base-url: http://ai-service:8001   # 兜底直连，优先 Nacos 服务发现
logging:
  pattern: ...
```
> 入口：`@SpringBootApplication` + `@EnableDiscoveryClient` + `@EnableFeignClients` + `@MapperScan("com.eduagent.learning.mapper")`。

---

## A.5 测试

### A.5.1 单测（JUnit5 + Mockito）
- `ProfileServiceImpl`：保存画像字段映射、AI profile 合并（指数移动平均、去重）正确。
- `LearningPathServiceImpl`：任务状态更新后 `progress` 重算正确；AI 返回异常时 fallback 路径非空。
- `QuizService.judge`：选择题本地比对 + 简答调 `AiServiceClient`（mock）返回 `correct/score/comment`，落库 `is_correct`。
- `StudyLogService`：`moduleSummary`/`dailyTrend` 聚合 SQL 正确（@DataJpaTest 或内存 H2 等价 SQL）。

### A.5.2 集成（Testcontainers）
- MySQL 容器起 `learning_db` → 跑 Flyway → 启 learning-service → `MockMvc/WebTestClient` 断言：
  - 无 token 经网关 → 401；带学生 token → `/api/learning/profile` 200。
  - `POST /api/learning/path/generate` 用 mock 的 `ai-service` 容器返回路径 JSON → 断言写库 + 返回 `LearningPathVO`。
  - `PUT /api/learning/path/task` 后断言 `study.progress` 事件进入 RabbitMQ 测试队列。

### A.5.3 契约
- 导出 learning-service OpenAPI（`/v3/api-docs`）作为前端（曾姿妍）与 teacher-service（吴友诚自己）的契约基线。

---

## A.6 验收（DoD）
- [ ] `learning_db` 建库建表完成，去除了跨库 FK；种子数据可灌入。
- [ ] 全部 `/api/learning/**` 端点按契约跑通；无 JWT→401，错误角色→403。
- [ ] 画像六维 `profile_data` 与 ai-service `profile_schema` 结构一致，引导完成置 `profile_complete=1`。
- [ ] 路径生成/任务完成正确落库，任务完成发布 `study.progress` 事件。
- [ ] `AiClient`(RestTemplate) 已替换为 `AiServiceClient`(Feign)，经 `AuthFeignInterceptor` 透传身份。
- [ ] `GET /api/learning/analytics/student/{id}` 返回教师看板所需聚合（teacher-service P3 联调通过）。
- [ ] 单测 + Testcontainers 集成测试绿；OpenAPI 契约发布。

---

# §1. ai-service 开发文档（Python · 端口 8001 · RAG 引擎）

## 1.1 需求

把现有 `edu-agent-ai/`（FastAPI + LangGraph 多智能体）改造为**独立微服务**，挂在网关 `/api/ai/**` 下，提供：

1. 多智能体对话 `POST /api/ai/chat`（接入 RAG）。
2. 资源生成 `POST /api/ai/resource/generate`（mindmap/quiz/reading/code/…）。
3. 学习路径生成 `POST /api/ai/path/generate`。
4. **代码质量分析 `POST /api/ai/code/analyze`**（供 code-service 经 Feign 调用，★内网专用，不暴露给前端）。
5. **知识库重建 `POST /api/ai/kb/rebuild`**（供管理端治理页触发，重建 Chroma 索引）。
6. RAG 检索增强：java_notes → 切分 → Embedding → Chroma（持久化容器）→ 检索 top-k → 注入 Agent prompt。

**非目标**：不做微调（主蓝图 §0.2 已排除）；不引入 Dify/Spring AI；AI 仍是单 Python 进程（主蓝图 §6.1 明确不拆 ai-chat/ai-code 两个服务，共享一份 Chroma）。

## 1.2 现状对齐（现有代码可复用部分）

| 现有文件 | 复用方式 |
|---------|---------|
| `edu-agent-ai/api.py` | FastAPI 入口；改为带 `/api/ai` 前缀的 router，新增 `/code/analyze`、`/kb/rebuild` |
| `edu-agent-ai/school_agent/graph.py` | `SimpleGraph` / LangGraph `StateGraph` 原样复用为 `/chat` 编排 |
| `edu-agent-ai/school_agent/agents/*.py` | 11 个 Agent 节点原样复用 |
| `edu-agent-ai/school_agent/services/llm_client.py` | `call_llm` / `call_llm_json` 复用；补 embedding 客户端 |
| `edu-agent-ai/school_agent/utils/code_fixer.py` | 中文关键字修复器，被 `/code/analyze` 复用 |
| `edu-agent-ai/school_agent/config.py` | 扩展：加 `CHROMA_HOST/PORT/COLLECTION`、`EMBEDDING_MODEL` |

> 现有 `retrieval_agent.py` 的 `retrieve_knowledge` 是**空壳**（只置 `retrieved_context=""`）。本 spec 要求把它落地为真实 Chroma 检索（§1.4）。

## 1.3 接口契约（请求/响应 JSON 形状——陈嘉成/陈海洋据此对齐）

所有端点统一包 `common.Result` 风格。**网关鉴权已在前置完成**，ai-service 收到的请求里已带网关注入的 `X-User-Id`/`X-User-Roles`（仅用于审计/限流，不用于业务鉴权，因为 AI 不持有用户库）。

> **契约字段命名（C4 / C20）**：本文契约（**wire JSON 与 Java DTO**）字段统一为 **camelCase**（如 `userInput` / `studentId` / `sessionId` / `sourceCode` / `finalAnswer` / `learningPath` / `resourceDir` / `profileComplete` / `targetMastery` / `masteryRate` / `knowledgeBase` 等）；Python 源码块（`api.py` 内部变量）可保留 snake（Python 惯例），但 **`api.py` 序列化须输出 camelCase（与 Java/前端一致）**。画像 `profile` 对象字段同样 camelCase，且含 **`course`** 字段（camelCase）——三跳一致透传（learning/resource → ai → 回传），ai 用于 **JavaSE 课程内章节定位**。

### 1.3.1 `POST /api/ai/chat`

**请求**
```json
{
  "userInput": "什么是 Java 的多态？",
  "studentId": "1001",
  "sessionId": "sess_abc",
  "profile": { "course": "JavaSE", "topic": "面向对象", "weaknesses": [], "knowledgeBase": "零基础" }
}
```

**响应（200）**
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "intent": "explain",
    "finalAnswer": "多态是指……（注入 RAG 上下文后的讲解）",
    "profile": { "topic": "面向对象", "course": "JavaSE", "_onboarding_phase": "done" },
    "resources": null,
    "learningPath": null,
    "safety_report": { "passed": true },
    "evaluation_report": { "mastery": 72 },
    "resourceDir": null,
    "profileComplete": true
  }
}
```

### 1.3.2 `POST /api/ai/resource/generate`

**请求**
```json
{
  "chapter": "第三章",
  "topic": "继承与多态",
  "resourceType": "mindmap",
  "level": "basic",
  "mode": "resource",
  "prompt": "为薄弱学生生成思维导图"
}
```
> 说明：`resourceType` 取值沿用现有 `api.py` 的 `role_prompts` 键集合；新增的 `level` 映射到原 `difficulty`（`easy`→basic）。陈嘉成(resource-service)调用时传 `studentId/chapter/topic/resourceType/level`，由 ai-service 内部拼 prompt（保持与现有 `AiClient.buildResourcePrompt` 一致的角色 prompt）。
> **`mode` 参数（见 C3）**：默认 `"resource"`，兼容现有 mindmap/quiz/reading/code/learning_path 生成；用于复用同一端点承载多类语义，`suggestion/judge/evaluation` 归 `mode` 轴而非 `resourceType` 取值（见 C19）。响应结构随 `mode` 不同而不同（见下）。

**响应（200）** —— 结构随 `mode` 不同而不同：

- **`mode=resource`（默认）**：`{content, resourceType, chapter}`（现状不变）：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "content": "{\"id\":\"root\",\"topic\":\"继承与多态\",\"children\":[...]}",
    "resourceType": "mindmap",
    "chapter": "第三章"
  }
}
```
> `content` 一律为**字符串**（可能是 JSON 文本或 Markdown），resource-service 落库 `resources.content` 后由前端按 `type` 解析。

- **`mode=judge`**（测验判分 / 作业判分）：`{score(0|1), correct, comment, explanation?}`：
```json
{ "code": 0, "message": "ok", "data": { "score": 1, "correct": true, "comment": "答对要点……", "explanation": "解释……" } }
```
- **`mode=suggestion`**（画像/学习建议）：`{suggestions:[]}`：
```json
{ "code": 0, "message": "ok", "data": { "suggestions": ["建议巩固……", "可尝试……"] } }
```
- **`mode=quiz`**（教师出题草稿）：`{items:[...]}`：
```json
{ "code": 0, "message": "ok", "data": { "items": [ { "stem": "……", "options": ["A.…","B.…"], "answer": "A" } ] } }
```
- **`mode=evaluation`**（成绩解读）：`{analysis}`：
```json
{ "code": 0, "message": "ok", "data": { "analysis": "本次成绩解读……" } }
```

### 1.3.3 `POST /api/ai/path/generate`

**请求**
```json
{ "studentId": "1001", "prompt": "根据画像规划 4 周学习路径", "profile": { "course": "JavaSE", "weaknesses": ["多线程"] } }
```
**响应（200）** —— 直接返回路径 JSON（保持现有行为，外层再包 `Result`）：
```json
{
  "code": 0, "message": "ok",
  "data": {
    "goal": "掌握课程核心知识并完成实践项目",
    "targetMastery": "≥85%",
    "totalHours": 24,
    "masteryRate": 50,
    "stages": [
      { "name": "本周路径", "tasks": [ { "title": "语法与基础练习", "duration": 60, "status": 0, "progress": 0 } ] }
    ],
    "suggestions": ["建议每天复习 30 分钟", "多线程建议结合案例练习"],
    "applicationAdvice": "可结合小型项目实践巩固面向对象与多线程",
    "examAdvice": "重点复习继承、多态与异常章节",
    "recommendTime": "4 周"
  }
}
```
> **字段分工（见 C9）**：`suggestions` / `applicationAdvice` / `examAdvice` / `recommendTime` 由 **ai 负责**（基于画像+路径生成）。其余聚合字段 `totalTasks` / `completedTasks` / `learningRate` / `unmasteredRate` / `tasks[].id` 由 **learning-service 自算/落库时生成**，不在此 ai 响应中。

### 1.3.4 `POST /api/ai/code/analyze` ★仅供内网（code-service 经 Feign 调用）

**请求**
```json
{
  "language": "java",
  "sourceCode": "public class Main { public static void main(String[] a){ System.out.println(\"hi\"); } }",
  "context": {
    "assignment_item_id": 12,
    "studentId": "1001",
    "compile_ok": 1,
    "checkstyle_errors": 0,
    "pmd_violations": 1,
    "run_passed": 1,
    "run_stdout": "hi\n",
    "expected_output": "hi\n"
  }
}
```
**响应（200）**
```json
{
  "code": 0, "message": "ok",
  "data": {
    "suggestions": [
      { "severity": "warning", "location": "Main.java:3", "title": "建议增加空指针判断", "detail": "……", "example": "if (a != null) {...}" }
    ],
    "summary": "代码可运行，存在 1 处 PMD 潜在空指针隐患，建议补充防御性判断。",
    "overall_comment": "整体良好，规范性待提升。",
    "score_hint": 88
  }
}
```
> `score_hint` 仅作参考，**最终分由 code-service 判分逻辑决定**（§2.6），ai 不强制。

### 1.3.5 `POST /api/ai/kb/rebuild` ★管理端治理触发

**请求**：`{}` 或 `{ "collection": "java_notes", "force": true }`（入参不变）
**响应（202 异步 / 或 200 + 进度）**
```json
{ "code": 0, "message": "ok", "data": { "task_id": "rebuild_20260731", "status": "started", "docs_indexed": 0 } }
```
> 重建为耗时操作，建议后台线程执行，接口立即返回 `task_id`；管理端轮询 `/api/ai/kb/status/{task_id}` 取进度。

**语义（见 C6）**：`/kb/rebuild` 不再重建本地 `java_notes` md，改为「触发从 resource-service 拉取 `kb_corpus`(status=0) → embed → 写 Chroma(collection) → 回调 `mark-indexed`」：
- **语料权威源 = `kb_corpus`**（resource-service 清洗流水线产出）；ai **不连、不写任何 MySQL 关系表**（含 `resource_db`），严守 DB-per-service 硬约束。
- ai 经 resource-service 暴露的 `GET /api/resource/kb/corpus?status=0` 拉取待向量化语料（出站客户端见 §1.3.5.1）。
- embed → 写 `Chroma(collection)` → 完成后**回调** `POST /api/resource/kb/mark-indexed`（body `{ids:[], collection}`），由 resource-service 把对应 `kb_corpus` 行 `status` 置 1。
- **ai 不持有任何关系表，不写 `resource_db`**；本地 `java_notes` md 仅作开发期种子/兜底，不进生产向量化主链路。

#### 1.3.5.1 resource 出站客户端（ai → resource-service，Feign/HTTP）
ai 新增一个出站客户端（Java 侧 `FeignClient`，或 Python 侧经 `requests`/service 名 `resource-service` 互访），封装：
- `GET /api/resource/kb/corpus?status=0`：拉取待向量化语料（`kb_corpus` 行，status=0）。
- `POST /api/resource/kb/mark-indexed`：body `{ids:[], collection}`，通知 resource-service 将对应 `kb_corpus` 行 `status` 置 1。

### 1.3.6 `GET /api/ai/health`
```json
{ "status": "ok", "chroma": "connected", "llm": "ok" }
```

## 1.4 数据模型 / RAG 架构

```
java_notes(知识源) ──导出──▶ kb/docs/*.md
                              │ loader 切分(chunk≈500字, overlap=50)
                              ▼
                       Embedding(OpenAI兼容 embed) ──▶ Chroma(collection=java_notes, 持久化卷)
                                                                    ▲ 检索
                                                                    │ top-k=5, score>0.3
                                                       Agent 调 call_llm 前注入 prompt
```

### 1.4.1 知识源迁移（java_notes → 文件）
原 `java_notes` 表 24 篇笔记内容（见 `edu_agent.sql`）**导出为 Markdown 文件**，置于 `edu-agent-ai/school_agent/kb/docs/<category>/<title>.md`。提供一次性导出脚本 `scripts/export_notes.py`（用 `pymysql` 读 `java_notes` → 写 md），跑一次即可，之后 AI 不再依赖 MySQL（满足"ai 无关系表"约束）。管理端编辑知识库时走"编辑→`/kb/rebuild`"流程。

### 1.4.2 Chroma 容器连接（关键配置）
```python
# school_agent/config.py 新增
import os
CHROMA_HOST   = os.getenv("CHROMA_HOST", "chroma")
CHROMA_PORT   = int(os.getenv("CHROMA_PORT", "8000"))
CHROMA_COLLECTION = os.getenv("CHROMA_COLLECTION", "java_notes")
CHROMA_PERSIST = os.getenv("CHROMA_PERSIST", "/chroma-data")
EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "text-embedding-3-small")
```

```python
# school_agent/kb/chroma_client.py
from chromadb import Client
from chromadb.config import Settings
from school_agent.config import CHROMA_HOST, CHROMA_PORT, CHROMA_COLLECTION

def get_client():
    return Client(Settings(
        chroma_server_host=CHROMA_HOST,
        chroma_server_http_port=CHROMA_PORT,
        persist_directory=None,
    ))

def get_collection():
    return get_client().get_or_create_collection(
        name=CHROMA_COLLECTION,
        metadata={"hnsw:space": "cosine"},
    )
```

### 1.4.3 检索节点落地（替换现有空壳 `retrieve_knowledge`）
```python
# school_agent/kb/retriever.py
def retrieve_knowledge(state: dict, top_k: int = 5) -> dict:
    user_input = state.get("user_input", "")
    profile = state.get("profile", {})
    topic = profile.get("topic", "")
    query = f"{topic} {user_input}".strip()
    try:
        col = get_collection()
        res = col.query(query_texts=[query], n_results=top_k)
        docs = (res.get("documents") or [[]])[0]
        ctx = "\n\n".join(docs)
    except Exception as e:
        ctx = ""
        print(f"[RAG] 检索失败: {e}")
    state["retrieved_context"] = ctx
    return state
```
`graph.py` 的 `retrieve_knowledge` 节点改为调用上述 `retriever.retrieve_knowledge`；各 Agent 在 `call_llm` 前把 `state["retrieved_context"]` 拼进 system prompt 的「知识库参考」段。

### 1.4.4 Embedding / 索引流水线
```python
# school_agent/kb/pipeline.py
from school_agent.kb.chroma_client import get_collection
from school_agent.services.embed_client import embed

def index_documents(docs: list[dict]):
    """docs: [{"id":..., "text":..., "metadata":{...}}]"""
    col = get_collection()
    col.upsert(
        ids=[d["id"] for d in docs],
        documents=[d["text"] for d in docs],
        metadatas=[d.get("metadata", {}) for d in docs],
        embeddings=[embed(d["text"]) for d in docs],
    )
```
> 首次部署跑一次 `pipeline.index_all()`；管理端 `/kb/rebuild` 调同一函数（带 `force` 先 `col.delete` 再重建）。

## 1.5 关键实现

### 1.5.1 入口与路由前缀
`app.py` 改为：
```python
from fastapi import FastAPI
from school_agent.routers import chat, resource, path, code_analyze, kb
app = FastAPI(title="EduAgent AI Service", version="1.0.0")
app.include_router(chat.router,    prefix="/api/ai", tags=["chat"])
app.include_router(resource.router,prefix="/api/ai", tags=["resource"])
app.include_router(path.router,    prefix="/api/ai", tags=["path"])
app.include_router(code_analyze.router, prefix="/api/ai", tags=["code"])
app.include_router(kb.router,      prefix="/api/ai", tags=["kb"])
@app.get("/api/ai/health")
def health(): ...
```
> 网关 `/api/ai/**` 路由**不 StripPrefix**（full path 直达 ai-service）。`/code/analyze` 由网关**排除在外**（见 §1.7 安全）。

### 1.5.2 `/code/analyze` 内部逻辑
1. 调 `code_fixer.fix_code` 清洗中文关键字（沿用现有 `utils/code_fixer.py`）。
2. 拼 prompt：你是 Java 代码评审专家（固定 system prompt 见附录 A）。
3. `call_llm_json` → 结构化建议；失败回退模板（保证不阻塞判分）。
4. 返回 §1.3.4 形状。

### 1.5.3 Nacos 注册（让 Java 经 `lb://ai-service` 发现）
Python 进程在启动时向 Nacos OpenAPI 注册自身：
```python
# scripts/nacos_register.py（进程启动时执行一次 + 心跳）
import os, requests
NACOS = os.getenv("NACOS_ADDR", "http://nacos:8848")
SERVICE = "ai-service"; IP = os.getenv("POD_IP", "ai-service"); PORT = 8001

def register():
    requests.post(f"{NACOS}/nacos/v2/ns/instance", params={
        "serviceName": SERVICE, "ip": IP, "port": PORT,
        "namespaceId": os.getenv("NACOS_NAMESPACE", "edu-dev"),
        "healthy": True, "ephemeral": True,
    }, timeout=5)
```
> 备选（更简单稳健，竞赛推荐先用）：Java 侧 Feign client 直接配 `url: http://ai-service:8001`（docker 网络内 service 名解析），不依赖 Nacos 注册 Python。两种都写进 §1.6 compose，二选一即可，**默认推荐 Feign 直连 service 名**（少一个故障点）。

### 1.5.4 降级
保留 `USE_MOCK_LLM=1`：LLM 不可用时 `call_llm` 回退本地模板输出（沿用现有降级思路），保证 P0/P1 联调不受外部 API 限流影响。

## 1.6 部署：Dockerfile + compose 片段

**`edu-agent-ai/Dockerfile`**（多阶段）
```dockerfile
FROM python:3.11-slim AS builder
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

FROM python:3.11-slim
WORKDIR /app
ENV PYTHONUNBUFFERED=1 CHROMA_HOST=chroma CHROMA_PORT=8000
COPY --from=builder /usr/local/lib/python3.11/site-packages /usr/local/lib/python3.11/site-packages
COPY . .
EXPOSE 8001
CMD ["sh", "-c", "python scripts/nacos_register.py & uvicorn app:app --host 0.0.0.0 --port 8001"]
```
`requirements.txt` 追加：`chromadb`、`sentence-transformers`（可选本地 embed）、`pymysql`（一次性导出用）、`requests`（Nacos 注册）。

**compose 片段（在 P0 的 `docker-compose.yml` 追加）**
```yaml
  ai-service:
    build: ./edu-agent-ai
    container_name: edu-ai
    networks: [edu-net]
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - OPENAI_BASE_URL=${OPENAI_BASE_URL}
      - LLM_MODEL=${LLM_MODEL:-gpt-4o-mini}
      - CHROMA_HOST=chroma
      - CHROMA_PORT=8000
      - CHROMA_COLLECTION=java_notes
      - USE_MOCK_LLM=${USE_MOCK_LLM:-0}
    depends_on: [chroma, nacos]
    ports: ["8001:8001"]
    healthcheck:
      test: ["CMD", "python", "-c", "import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://localhost:8001/api/ai/health').status==200 else 1)"]
      interval: 30s
```

## 1.7 安全（内网隔离）

- `/api/ai/**` 由网关鉴权后转发；ai-service **不持有用户库**，仅信任网关注入的 `X-User-*` 头做审计。
- **`/api/ai/code/analyze` 不进网关路由表**（仅 code-service 内网调用）。网关路由只挂：`/api/ai/chat`、`/api/ai/resource/generate`、`/api/ai/path/generate`、`/api/ai/kb/rebuild`、`/api/ai/health`。
- 生产环境 `ai-service.ports` 不映射到宿主，仅靠 `edu-net` 内 `ai-service:8001` 互访。
- 网关对 `/api/ai/**` 限流（QPS 较低，LLM 为稀缺资源），规则见 P4 §3.1。

## 1.8 测试

- 单测：`retriever` 在 Chroma 起不来时返回空上下文不抛异常；`index_documents` 幂等（upsert）。
- 集成：Testcontainers 起 Chroma → 灌 3 条 doc → query 命中 top-1 → 断言 `retrieved_context` 非空。
- 契约：`/api/ai/resource/generate` 与 `/api/ai/chat` 的 OpenAPI 作为 resource(陈嘉成)/learning(陈海洋) 的 Feign 契约基线（Spring Cloud Contract 在 P4 接入）。

## 1.9 验收

- [ ] `docker-compose up` 后 `ai-service` healthy，`/api/ai/health` 返回 `chroma:connected`。
- [ ] 首次 `/kb/rebuild` 完成，Chroma `java_notes` collection 有数据。
- [ ] `/chat` 未检索时也能答，检索开启后回答贴 JavaSE 知识库（RAG 生效，对比实验可见差异）。
- [ ] resource-service 调 `/resource/generate` 拿到 `content` 字符串并落库。
- [ ] code-service 调 `/code/analyze` 拿回 `suggestions` JSON。
- [ ] `/code/analyze` 经网关 404（内网专用验证）。

---

## C. 跨服务契约总览（陈海洋侧需要别人提供的）

| 我方(消费) | 提供方 | 端点 | 关键字段 | 状态 |
|------|------|------|------|------|
| learning-service | ai-service(陈海洋) | `POST /chat`,`/path/generate`,`/resource/generate` | 见 A.4.5 | 复用既有，已对齐 |
| teacher-service | learning-service(自己) | `GET /analytics/student/{id}`,`/progress`,`/profile/{id}`,`POST /profile/{id}/class` | 见 A.2.8 | 本 spec 定义 |
| teacher-service | code-service(吴友诚) | `POST /api/code/submit` | 见 B.4.2 | **待吴友诚 code 子 spec 对齐确认** |
| teacher-service | ai-service(陈海洋) | `POST /chat`,`/resource/generate` | 见 B.4.3 | 复用既有 |

> 唯一待锁定的外部契约是 **B.4.2 code-service 判分接口**；其余均复用 ai-service 既有端点或本 spec 自闭环。

*（陈海洋 · learning-service + ai-service 子 spec 结束）*

More Actions契约提供方消费方对齐章节`/api/ai/chat`ai-servicelearning(陈海洋)/前端ai-service §1.3.1`/api/ai/resource/generate`ai-serviceresource(陈嘉成)ai-service §1.3.2`/api/ai/path/generate`ai-servicelearning(陈海洋)ai-service §1.3.3`/api/ai/code/analyze`（内网）ai-servicecode(吴友诚)ai-service §1.3.4 / §2.4.5`/api/ai/kb/rebuild`ai-serviceadmin/teacher(吴友诚)ai-service §1.3.5
