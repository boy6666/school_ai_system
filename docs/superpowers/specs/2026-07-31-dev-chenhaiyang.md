# 开发文档（子 Spec）：learning-service + teacher-service

> 编写人：**陈海洋（成员C）** ｜ 阶段：**P1（learning-service）/ P3（teacher-service）**
> 上游基线：主蓝图 `2026-07-31-edu-agent-platform-design.md`、P0 `2026-07-31-p0-infra-gateway.md`
> 协同方：吴友诚（code-service `/api/code/**`、ai-service `/api/ai/**`）、陈嘉成（resource-service）、曾姿妍（前端）
> 文档粒度（对齐 P0）：**需求 → 接口契约 → 数据模型 → 关键实现 → 测试 → 验收**
> 端口：`learning-service`=8082（`/api/learning/**`，库 `learning_db`）；`teacher-service`=8084（`/api/teacher/**`，库 `teacher_db`）

---

> **统一约定（来自 P0）**
> - 身份：所有控制器通过 `com.eduagent.common.AuthContext.getUserId()` / `getRoles()` 取身份，**不自己解析 JWT**。
> - 角色常量：`RoleConstants.ROLE_STUDENT` / `ROLE_TEACHER` / `ROLE_ADMIN`（值 `ROLE_STUDENT`/`ROLE_TEACHER`/`ROLE_ADMIN`）。
> - 跨服务：Feign 调别的服务用 `common` 的 `AuthFeignInterceptor` 自动透传身份头，服务名走 Nacos。
> - 响应：`Result<T>` / `PageResult<T>`；异常抛 `BusinessException`，由 `GlobalExceptionHandler` 统一包装。
> - DB-per-service：**禁止跨库外键**。原单体里 `student_profiles`/`learning_*` 等表对 `users` 的 FK、以及 `quiz_answer.resource_id` 对 `resources` 的 FK，在独立库中**一律改逻辑引用（纯 BIGINT），不建外键**。
> - Feign 调 AI（Python，Nacos 名 `ai-service`）：路径**必须带 `/api/ai` 前缀**（如 `/api/ai/chat`），与网关转发路径完全一致（网关不 StripPrefix）；否则 Feign 直连会 404。规范见吴友诚 ai-service 子 spec §1.5.1。

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
@FeignClient(name = "ai-service", url = "${ai.base-url:}", path = "/api/ai") // 吴友诚确保 ai-service 在 Nacos 可达；url 兜底
public interface AiServiceClient {
    @PostMapping("/chat")                Result<AiChatResult> chat(@RequestBody AiChatRequest req);
    @PostMapping("/path/generate")       Result<LearningPathVO> generatePath(@RequestBody AiPathRequest req);
    @PostMapping("/resource/generate")   Result<Map<String,Object>> generateResource(@RequestBody AiResourceRequest req);
}
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
- 导出 learning-service OpenAPI（`/v3/api-docs`）作为前端（曾姿妍）与 teacher-service（陈海洋自己）的契约基线。

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

# B. teacher-service（P3，教师端后端）

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

### B.4.2 与 code-service 的作业-判分对接契约（★需与吴友诚对齐）
teacher-service 提交代码题时调 `code-service`。**约定契约如下（吴友诚在 code-service 实现，路径前缀 `/api/code/**`，网关已路由）**：

**请求** `POST /api/code/submit`
```json
{ "studentId": 12,
  "language": "java",
  "sourceCode": "public class Main{...}",
  "assignmentItemId": 45,
  "assignmentId": 7 }
```
**响应** `Result<CodeSubmissionVO>`
```json
{ "code": 0, "data": {
    "submissionId": 901,
    "status": 1,                 // 0待运行 1成功 2失败 3超时
    "stdout": "...", "stderr": "...",
    "runTimeMs": 120,
    "compileOk": 1, "compileMsg": "",
    "checkstyle": { "violations": [...] },
    "pmd": { "violations": [...] },
    "aiSuggestion": "建议...",
    "overallScore": 85 },
  "message": "ok" }
```

**teacher-service 落库映射**（写 `grades`）：
```java
grade.setRunResult(Map.of("stdout", vo.stdout, "stderr", vo.stderr,
                          "runTimeMs", vo.runTimeMs, "status", vo.status));
grade.setStaticReport(Map.of("compileOk", vo.compileOk, "compileMsg", vo.compileMsg,
                              "checkstyle", vo.checkstyle, "pmd", vo.pmd));
grade.setAiReport(Map.of("aiSuggestion", vo.aiSuggestion));
grade.setScore(vo.overallScore);
```
> 选择题/填空题（choice/blank）：不调 code-service，本地比对 `questions.answer` 判分（或小题调 ai `/resource/generate` mode=judge）。仅 `code` 类型走 code-service。
> 该契约是**双向约定**：吴友诚的 code-service 必须提供 `POST /api/code/submit` 且响应字段如上；陈海洋侧 `CodeServiceClient` 按此签名实现。AI 分析底层复用 ai-service `/code/analyze`（由 code-service 内部调用，teacher-service 不直接调）。

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
@FeignClient(name = "code-service", url = "${code.base-url:}", path = "/api/code")
public interface CodeServiceClient {
    @PostMapping("/submit")
    Result<CodeSubmissionVO> submit(@RequestBody CodeSubmissionRequest req);
}

// feign/AiServiceClient.java  → AI 助教（路径带 /api/ai 前缀，与网关一致）
@FeignClient(name = "ai-service", url = "${ai.base-url:}", path = "/api/ai")
public interface AiServiceClient {
    @PostMapping("/chat") Result<AiChatResult> chat(@RequestBody AiChatRequest req);
    @PostMapping("/resource/generate") Result<Map<String,Object>> generate(@RequestBody AiResourceRequest req);
}
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
- `assignment.graded`（来自 code-service）：`AssignmentGradedConsumer` 接收后把判分结果回写 `grades`（status→1，score 填充），并触发教师端"待复核"提醒计数。
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

## C. 跨服务契约总览（陈海洋侧需要别人提供的）

| 我方(消费) | 提供方 | 端点 | 关键字段 | 状态 |
|------|------|------|------|------|
| learning-service | ai-service(吴友诚) | `POST /chat`,`/path/generate`,`/resource/generate` | 见 A.4.5 | 复用既有，已对齐 |
| teacher-service | learning-service(自己) | `GET /analytics/student/{id}`,`/progress`,`/profile/{id}`,`POST /profile/{id}/class` | 见 A.2.8 | 本 spec 定义 |
| teacher-service | code-service(吴友诚) | `POST /api/code/submit` | 见 B.4.2 | **待吴友诚 code 子 spec 对齐确认** |
| teacher-service | ai-service(吴友诚) | `POST /chat`,`/resource/generate` | 见 B.4.3 | 复用既有 |

> 唯一待锁定的外部契约是 **B.4.2 code-service 判分接口**；其余均复用 ai-service 既有端点或本 spec 自闭环。

*（陈海洋 · learning-service + teacher-service 子 spec 结束）*
