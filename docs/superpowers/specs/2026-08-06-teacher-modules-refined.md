# edu-agent-teacher 模块细化需求（构建基线 v1）

> 负责人：吴友诚 ｜ 基准 spec：`2026-07-31-dev-wuyoucheng.md §B` ｜ 状态：实现中
> 本文在 §B 之上做「可被直接编码」的细化：逐模块拆需求、落实现决策、对齐 framework 与跨服务现实。
> 代码以本文为准；与 §B 冲突处以本文（更新）为准。

---

## 0. 全局决策（相对 §B 的更新/澄清）

| # | 决策 | 说明 |
|---|------|------|
| D1 | **审计列统一 `create_time`/`update_time`** | V1 的 `created_at` 与 framework `BaseEntity`（createTime→create_time / updateTime→update_time，`AutoFillMetaObjectHandler` 自动填充）冲突。以 **V2 追加迁移**收敛（新老库都兼容，Flyway 校验和不受影响）。`class_students` 为复合主键纯关联表，不继承 BaseEntity，保留 `joined_at`。 |
| D2 | **实体一律 `extends BaseEntity`**（除 `ClassStudent`） | 获得 `id` 自增 + `createTime/updateTime` 自动填充 + `deleted` 逻辑删。业务层不手动赋审计字段。`Question.options`、`Grade.run_result/static_report/ai_report` 为 MySQL `JSON` 列 → Java 用 `String` 承载，出参时由 VO 解析。 |
| D3 | **角色守卫**：教师端点统一 `ROLE_TEACHER`，需 T 的端点校验 `AuthContext` 含 T；`submit` 仅 `ROLE_STUDENT`；查询端支持 T,S。无 token（无 X-User-*）→ 401。 |
| D4 | **跨服务 Feign 用 `path="/api/<svc>"` + `name="edu-agent-<svc>"`**，无需 `lb://`（对齐 CodeController/common 惯例）；URL 空则走 Nacos 发现（`url="${...:}"`）。自动套 `AuthFeignInterceptor` 透传身份。 |
| D5 | **代码判分异步两段式**（对齐《契约对齐决议》C1）：`submit` 不读回执落库，判分结果由 `assignment.graded` 事件回填 `grades`（方案 A，不轮询）。 |
| D6 | **看板聚合**：`List<CompletableFuture>` + `Semaphore(8)` 保护下游 `edu-agent-learning`；线程池用独立 `AsyncConfig` executor。 |

---

## 1. 模块细化

### 1.1 班级管理 Class（独立，无强跨服务依赖）
- 需求：教师的班级 CRUD 与成员管理；`teacher_id` 取自 `AuthContext.getUserId()`，**不信任请求体/前端**。
- 端点（全部 T，`/api/edu-agent-teacher/classes**`）：
  - `POST /classes` → 建班级（name/course/semester）。
  - `GET /classes` → 仅本人班级列表。
  - `GET /classes/{id}` → 详情（校验属主）。
  - `PUT /classes/{id}` → 更新。
  - `DELETE /classes/{id}` → 逻辑删。
  - `POST /classes/{id}/students` {studentId} → 写 class_students + Feign→learning `bindClass` 回写学生 `class_id`（T 可写）。
  - `DELETE /classes/{id}/students/{studentId}` → 移除成员。
  - `GET /classes/{id}/students` → 成员列表 `{studentId, joinedAt}`。
- 属主校验：`class.teacher_id == AuthContext.userId`，否则 403。
- D7（细化）：加/删学生须**幂等**（重复加入不报错，`INSERT IGNORE` / 先查后插）；删除班级是逻辑删，不级联物理删成员关系（成员关系仍可查历史）。

### 1.2 题库 Question（独立）
- 需求：题库 CRUD；`creator_id` 取 AuthContext.userId。
- 端点（全 T）：`POST /questions`、`GET /questions`（chapter/topic/type/difficulty 过滤）、`GET /questions/{id}`、`PUT /questions/{id}`、`DELETE /questions/{id}`、`POST /questions/generate`（→ ai `/resource/generate` mode=quiz 出草稿，教师确认后落库）。
- `type ∈ {choice, code, blank}`；choice 的 `options` 为 JSON 数组字符串。
- D8（细化）：`answer` 存明文参考答案（教务场示意可），正式生产需按 role 控制 exposure（`answer` 仅 T 可读，S 不可见）——**本期仅在查询 VO 上对 code 题隐藏 answer**（选择题在校验时由服务内部比对，不吐给学生）。

### 1.3 作业 Assignment
- 需求：作业 + 题目项 CRUD；发布后发 `assignment.published` 事件（exchange=事件名，`{assignmentId, classId, title, type, deadline}`）。
- 端点：`POST /assignments`（含 items）、`GET /assignments?classId=`、`GET /assignments/{id}`（AssignmentDetailVO：items + question + submittedCount/gradedCount）、`PUT /assignments/{id}`、`DELETE /assignments/{id}`、`POST /assignments/{id}/items`、`POST /assignments/{id}/publish`。
- `type ∈ {homework, code}`（code=含代码题）。
- D9（细化）：建作业（草稿 status=0）不发事件；仅 `publish` 或建时显式 `status=1` 才发 `assignment.published`。
- D10（细化）：`submittedCount/gradedCount` 由 `grades` 统计（按 assignment + status 判定）。

### 1.4 提交与批改 Grade（核心跨服务）
- 需求：学生提交 → 逐 item 判分落 `grades`；code 题走异步两段式。
- 端点：
  - `POST /assignments/{id}/submit`（S）：`{items:[{itemId, submission, language?}]}`，studentId=AuthContext。
    - choice/blank：服务内比对 `questions.answer` 打分，落 `grades.status=1`。
    - code：写 `grades`(status=0 待批) 后 Feign→code `POST /api/edu-agent-code/submit` 拿受理回执 `{submissionId,status}`；**不读全量报告落库**——判分完成后由 `assignment.graded` 事件回填（方案 A）。
    - 返回 `List<GradeVO>`。
  - `GET /assignments/{id}/grades?studentId=`（T）：班级/某生成绩。
  - `GET /grades/{gradeId}`（T,S）：GradeDetailVO（含 run/static/ai report + comment）。
  - `PUT /grades/{gradeId}`（T）：教师复核 `{score?, comment?, aiReportOverride?}`。
  - `GET /students/{studentId}/assignments`（S,T）：某生全部作业含成绩。
- D11（细化）：`uk_stu_item(assignment_id, student_id, item_id)` 幂等 —— 提交 upsert：已存在同 item 成绩则覆盖不重建，保证重交安全。

### 1.5 学情看板 Analytics
- 需求：班级维度聚合 edu-agent-learning 逐生学情，ECharts 直接消费。
- 端点：`GET /classes/{id}/analytics`（全量 ClassAnalyticsVO）、`GET /classes/{id}/overview`（轻量均分/完成率/活跃度）。
- 实现：读 class_students → `CompletableFuture` + `Semaphore(8)` 并发 Feign→learning `/analytics/student/{id}/progress` → 聚合 avgMastery/masteryDist/dimensionAvg/taskCompletion/weakTopics/trend。
- D12（细化）：消费 `study.progress` 更新 Redis dashbboard 缓存 `teacher:class:{id}:dashboard`，看板读缓存合并 → 近实时且避免每次全量重算。Redis 未就绪时降级直接聚合（try/catch，不阻塞）。

### 1.6 AI 助教 AiTutor
- 端点：
  - `POST /ai/ask` {message, classId?, context?}（T）→ Feign→ai `/chat` → `{answer, intent, references?}`。
  - `POST /ai/explain-grade` {studentId, assignmentId}（T）→ 拉学情+成绩→ Feign→ai `/resource/generate` mode=evaluation → `{analysis}`。
  - `POST /questions/generate`（T，见 1.2）→ mode=quiz。
- D13（细化）：`AiServiceClient.generate` 的 `AiResourceRequest` **必须带 `mode` 字段**，按 mode 解析返回结构（quiz→items / evaluation→analysis / resource→content,resourceType,chapter）。

---

## 2. 跨服务契约（当前实现侧状态）

| 下游 | 本服务 Feign 方法 | 端点（路径前缀） | 现实状态 |
|------|------|------|------|
| edu-agent-learning | `getProgress/getAnalytics/getProfile/bindClass` | `/api/edu-agent-learning/...` | learning 尚未提供这些端点（陈海洋 P1）；本期先定义契约，依赖注入 `url` 空串走 Nacos，端点缺失时调用失败由 `@Sentinel`/异常处理兜底（看板降级返回空聚合）。 |
| edu-agent-code | `submit` | `POST /api/edu-agent-code/submit` → 202 受理回执 | **code 目前无 `/submit`**（吴友诚后续 §2 实现）；本期先按 C1 契约定义，并在 `assignment.graded` 事件落地为先。 |
| edu-agent-ai | `chat` / `generate` | `POST /api/edu-agent-ai/...` | ai 为 Python 且根路径挂载，**当前未走 Nacos `lb://` 也未带 `/api/edu-agent-ai` 前缀**（§B 路由表已注明现实）。本期 Feign 定义为契约态；联调时按网关实际配置核对。 |

> 原则：本期把 teacher 侧代码与契约写全，跨服务端点是否 ready 不阻塞本服务编译与单测（Feign 接口 + MQ 消费者先行）。

---

## 3. 目录与产出（以本节为准落地）
```
edu-agent-teacher/src/main/java/com/eduagent/teacher/
├── TeacherApplication.java        （已存在：@EnableDiscoveryClient + @EnableFeignClients(com.eduagent) + @MapperScan）
├── controller/  Class, Question, Assignment, Grade, Analytics, AiTutor
├── service/     ClassService, QuestionService, AssignmentService, GradeService, AnalyticsService, AiTutorService
├── service/impl/ 对应 *ServiceImpl
├── mapper/      ClassesMapper, ClassStudentMapper, QuestionMapper, AssignmentMapper, AssignmentItemMapper, GradeMapper
├── entity/      Classes, ClassStudent, Question, Assignment, AssignmentItem, Grade
├── vo/          ClassVO, QuestionVO, AssignmentVO, AssignmentDetailVO, GradeVO, GradeDetailVO,
│                ClassAnalyticsVO, ClassOverviewVO, StudentProgressVO
├── dto/         CreateClassRequest, CreateQuestionRequest, CreateAssignmentRequest, SubmitAssignmentRequest, AiAskRequest
├── feign/       LearningServiceClient, CodeServiceClient, AiServiceClient
├── mq/          AssignmentPublishedEvent, StudyProgressEvent, AssignmentGradedEvent,
│                AssignmentPublishedPublisher, StudyProgressConsumer, AssignmentGradedConsumer
├── config/      AsyncConfig, RabbitConfig, FeignConfig
└── resources/   application.yml（已存在）
```

## 4. 顺序交付与完成状态
| # | 模块/任务 | 状态 |
|---|------|------|
| 1 | 骨架对齐（V2 迁移 + 6 实体 + 6 mapper + AsyncConfig/RabbitConfig） | ✅ |
| 2 | 班级管理 Class（controller→service→impl→mapper→vo→dto，含 best-effort bindClass） | ✅ |
| 3 | 题库 Question（CRUD + `/questions/generate` AI 出题草稿） | ✅ |
| 4 | 作业 Assignment（含 `AssignmentPublishedPublisher`，publish 发事件） | ✅ |
| 5 | 提交批改 Grade（choice/blank 本地判分 + code 异步两段式 + `AssignmentGradedConsumer` 回填 + 教师复核） | ✅ |
| 6 | 学情看板 Analytics（`Semaphore(8)`+CompletableFuture 聚合 + `StudyProgressConsumer`→进程内缓存） | ✅ |
| 7 | AI 助教 AiTutor（`/ai/ask`、`/ai/explain-grade`） | ✅ |
| 8 | 单测 + 集成（B.5）+ 端到端联调（Nacos/Rabbit/MySQL 拉起） | ⏳ 待做 |

> 说明：所有模块已编译通过；`mvn -pl edu-agent-teacher compile` 绿色。`study.progress` 缓存本期用进程内 `DashboardCache`（无 Redis 依赖），生产可平替 Redis。跨服务端点（learning 的 progress、code 的 submit、ai 的 chat/generate）按目标契约声明，未就绪时降级/捕获，不阻塞。

*构建基线 v1 结束。*
