# 契约对齐决议（Contract Alignment Resolution）

> **用途**：把六份开发文档之间的跨服务契约冲突，按「推荐裁定方案」写死，作为后续写码的唯一依据。
> **状态**：推荐方案已写死，待 **吴友诚（架构）** 最终确认。确认后本文从【待确认】转【已生效】，各子 spec 据此落地（文中已标注对应项 Cx）。
> **配套**：9 项纯文档一致性修正已直接落地（见 §0），本文聚焦需架构拍板的契约决策（C1/C3/C4/C6 及其卫星项 C2/C7/C9/C20）。

---

## 0. 已落地的纯一致性修正（无需再确认）

| 项 | 修正内容 | 文件 |
|----|----------|------|
| C5 | 网关路由表由 `/api/ai/**` 通配改为显式白名单（chat/resource/generate/path/generate/kb/rebuild/health），`/code/analyze` 不进网关（内网 404） | p0-infra-gateway §5.2 |
| C8 | resource-service 画像字段注释由 snake 改为 camelCase，与 `LearningProfileVO` 一致 | dev-chenjiacheng §4.5 |
| C10 | 前端 dashboard 的 AI 类端点由 `/api/ai/dashboard/*` 改回 `/api/learning/dashboard/*`（归属 learning-service） | dev-zengziyan §3.2 |
| C11 | Nacos group `RESOURCE_GROUP` → `resource-group`（与 learning-group/teacher-group 统一 kebab） | dev-chenjiacheng 附录A |
| C12 | MQ exchange 名去 `.exchange` 后缀，统一为「exchange 名 = 事件名」（study.progress / assignment.published 对齐 resource.generate） | dev-chenhaiyang A.4.4/B.4.5 |
| C16 | resource-service 拉画像端点 `/api/learning/profile/me` → `/api/learning/profile`（learning 真实暴露的自我端点） | dev-chenjiacheng §4.5 |
| C17 | skywalking-ui 宿主端口 8081 → 18081（避免与 auth 文档端口 8081 同数字误导） | p0-infra-gateway §2.1 |
| C18 | `POST /profile/save` body 移除不存在的 `dimensions{}`（六维由 AI 合并进 `profile_data`） | dev-chenhaiyang A.2.1 |
| C19 | resourceType 枚举限定为生成产物类型（mindmap/quiz/reading/code/learning_path），suggestion/judge/evaluation 等归 `mode` 轴；`level` 对齐 ai 的 `basic` | dev-chenjiacheng §5.2 |

---

## 1. C1 / C2 / C7 · teacher↔code 判分链路：锁定「异步两段式」

**冲突**：吴友诚 `POST /api/code/submit` 返回 202 仅 `{submissionId,status}`，靠轮询 `GET /api/code/result/{id}` 或消费 `assignment.graded` 事件拿报告；陈海洋写成同步返回全量 `CodeSubmissionVO` 并直接落 `grades`，且 `CodeServiceClient` 无 result 轮询端点、请求体缺 `expectedOutput/className`、事件 payload 无报告体。

**裁定（推荐）**：
1. code-service `POST /api/code/submit` **保持 202 异步**，响应仅 `{submissionId, status}`。
2. 完整报告经 `GET /api/code/result/{id}` 获取，响应结构（与 `grades` 列一一对应）：
   `{ submissionId, status, stdout, runTimeMs, compileOk, checkstyle, pmd, aiSuggestion, overallScore }`。
3. code-service 判分完成后发 `assignment.graded` 事件，**payload 必须携带完整报告体**（不只 overallScore/aiSuggestion）：
   `{ assignmentId, assignmentItemId, studentId, submissionId, status, runPassed, compileOk, stdout, runTimeMs, checkstyle, pmd, aiSuggestion, overallScore }`。
4. teacher 落库策略（**推荐 A 事件驱动**）：`AssignmentGradedConsumer` 直接消费事件完整 payload 回填 `grades`（run_result/static_report/ai_report/score）；teacher 不轮询。退路 B：teacher `CodeServiceClient` 增 `GET /api/code/result/{id}`，submit 后轮询至 `status=done` 再落库。
5. 入参契约最终形态（两边对齐）：
   `{ studentId, assignmentId, assignmentItemId, language, sourceCode, expectedOutput, className }`
   —— teacher 补齐 `expectedOutput/className` 并保留 `assignmentId`；code 接收 `assignmentId`（用于事件回填关联），以 `expectedOutput` 参与判分权重。

**实现分工**：吴友诚（code）改 submit 为 202 + result 端点 + 事件带完整报告 + 收 assignmentId/expectedOutput/className；陈海洋（teacher）改 `CodeServiceClient`（202 形态 + 按 A/B 取结果）与落库逻辑。

---

## 2. C3 · `/resource/generate` 的 `mode` 多路复用

**冲突**：learning/teacher 用 `mode=suggestion/judge/quiz/evaluation` 四种语义，期望 judge 返回 `{score,correct,comment}`、suggestion 返回 `{suggestions[]}`；吴友诚契约只有 `resourceType`，任何调用只返回 `{content,resourceType,chapter}`，无 `mode`、无结构化结果。

**裁定（推荐）**：吴友诚在 ai-service 为 `POST /api/ai/resource/generate` **增加 `mode` 参数**（默认 `resource`，兼容现有生成），并按 mode 返回不同结构：
- `mode=resource`（默认）：`{content, resourceType, chapter}`（现状）。
- `mode=judge`：`{score(0|1), correct, comment, explanation?}`（测验判分 / 作业判分）。
- `mode=suggestion`：`{suggestions:[]}`（画像/学习建议）。
- `mode=quiz`：`{items:[...]}`（教师出题草稿）。
- `mode=evaluation`：`{analysis}`（成绩解读）。

**退路**：若吴友诚不愿扩 `mode`，则新增独立端点 `/api/ai/quiz/judge`、`/api/ai/suggest`、`/api/ai/evaluate`，learning/teacher 改调独立端点。

**实现分工**：吴友诚（ai）定 `mode` 语义与返回；陈海洋（learning/teacher）按 `mode` 解析对应 VO。
**注**：resource-service 的 generation 仍走 `mode=resource`（默认），`suggestion/judge/evaluation` **不**作为 resourceType 取值（已在 dev-chenjiacheng §5.2 修正）。

---

## 3. C4 · 字段命名 snake_case ↔ camelCase

**冲突**：吴友诚契约用 `user_input / student_id / session_id / source_code / final_answer / learning_path / resource_dir / profile_complete` 等蛇形；三个 Java 调用方 DTO 全驼峰 → 不统一则 Feign 字段全为 null。

**裁定（推荐）**：**吴友诚把 ai-service 全部契约字段统一为 camelCase**（与 Java/前端一致），即：
`userInput / studentId / sessionId / sourceCode / finalAnswer / learningPath / resourceDir / profileComplete / targetMastery / masteryRate / knowledgeBase / ...`
- **不采用**「Java 侧全局开 `PropertyNamingStrategies.SNAKE_CASE`」方案（会反向污染 Java 代码风格，且一次性改 ai 更省事）。
- 画像 `profile` 对象字段同样 camelCase，与 learning `ProfileVO`、resource `LearningProfileVO` 一致（呼应 C8）。

**实现分工**：吴友诚（ai）统一契约 + 修改 `api.py` 序列化；其余服务 DTO 无需改。

---

## 4. C6 · `kb_corpus` 归属与 ai 反向写库

**冲突**：陈嘉成期望 ai 读/写 `kb_corpus`、反向调 resource、回写 `status=1`；吴友诚只重建本地 `java_notes` md、无出站客户端、且不碰任何 MySQL 关系表（DB-per-service 硬约束）。两边说的"知识库"不是同一份。

**裁定（推荐）**：
1. **语料权威源 = `kb_corpus`**（陈嘉成 resource-service 清洗流水线产出）；吴友诚的 `java_notes` 本地 md 仅作开发期种子/兜底，不进生产向量化主链路。
2. **DB-per-service 硬约束保留**：ai-service **不连、不写任何 MySQL 关系表**（含 resource_db）。因此：
   - ai 读取语料：经 resource-service 暴露的 `GET /api/resource/kb/corpus?status=0`（陈嘉成 §8.9 已定义）拉取，ai 不直连库。
   - 「已向量化」状态维护：`kb_corpus.status` 由 **resource-service 维护**。ai 完成某批向量化后，**回调** resource-service 新增端点 `POST /api/resource/kb/mark-indexed`（body `{ids:[], collection}`），由 resource-service 把对应行 `status` 置 1。ai 不在自己侧维护关系表。
3. `POST /api/ai/kb/rebuild` 入参 `{}` 或 `{collection, force}` 不变；语义改为「触发从 resource-service 拉取 `kb_corpus`(status=0) → embed → 写 Chroma(collection) → 回调 `mark-indexed`」。

**实现分工**：吴友诚（ai）加 resource 出站客户端 + `mark-indexed` 回调；陈嘉成（resource）暴露 `kb/corpus` 拉取 + `kb/mark-indexed` 标记端点。

---

## 5. C9 · `/path/generate` 响应字段缺失

**冲突**：吴友诚只返回 `goal/targetMastery/totalHours/masteryRate/stages`；缺 `LearningPathVO` 的 `totalTasks/completedTasks/suggestions/applicationAdvice/examAdvice/learningRate/unmasteredRate/recommendTime/tasks[].id`。

**裁定（推荐）**：分级策略——
- **ai 负责补齐**可推导字段：`suggestions`、`applicationAdvice`、`examAdvice`、`recommendTime`（基于画像+路径生成）。
- **learning-service 自算**：`totalTasks/completedTasks/learningRate/unmasteredRate`（基于 stages 与 study_log 聚合）；`tasks[].id` 由 learning 落库时生成。
- ai 返回：`{goal, targetMastery, totalHours, masteryRate, stages[{name, tasks[{title, duration, status, progress}]}], suggestions, applicationAdvice, examAdvice, recommendTime}`；learning 补全其余后返回 `LearningPathVO`。

**实现分工**：吴友诚（ai）补 suggestions/advice/recommendTime；陈海洋（learning）自算聚合字段。

---

## 6. C20 · profile 含 `course` 字段

**冲突**：resource/learning 透传 `course`，吴友诚 profile 示例无 `course` 键。

**裁定（推荐）**：**profile 对象增加 `course` 字段**（camelCase），三跳一致透传；ai 用于 JavaSE 课程内章节定位（即便当前未强消费，也保留以对齐数据模型）。

**实现分工**：吴友诚（ai）在 profile schema 加 `course`；陈海洋/陈嘉成现有透传已含 `course`，无需改。

---

## 7. 生效与入口

- 本文为契约裁决最终依据；各子 spec 以「见《契约对齐决议》Cx」引用。
- 待吴友诚确认后：本文件状态【待确认】→【已生效】，并据此把 C1/C3/C4/C6/C9/C20 的修改落到对应子 spec（吴友诚/陈海洋/陈嘉成文档）。
- 主蓝图 §12.3 索引已追加本文件。

---

*契约对齐决议结束。确认前各服务写码应以本文推荐方案为临时依据，避免再次漂移。*
