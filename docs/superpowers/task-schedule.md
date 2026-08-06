# EduAgent 项目排期表（2026-08-01 起）

> **团队与模块归属**
> - 吴友诚（WYC）：架构 + Gateway + Common + auth-service + code-service（代码编译检查，新增模块）+ teacher-service（教师端，新增模块）
> - 陈嘉成（B）：resource-service（含 KB 知识库清洗流水线）
> - 陈海洋（C）：learning-service + ai-service（AI 最丰富，RAG，新增模块）
> - 曾姿妍（zzy/D）：前端（Vue3 三端：学生 / 教师 / 管理员）
>
> **特殊说明**：zzy 的新界面开发在 **2026-08-15 之后**才分配；其余三人无特殊说明，按节点立即开工。
> 三个待完善模块：code（新）、teacher（新）、admin（完善，主要为前端 + 既有服务聚合）。
>
> **表二「资料」列格式**：`spec 引用 ｜ DoD（验收标准） ｜ 前置（依赖的行号/条件）`。

---

## 表一：阶段性任务分配（粗略）

| 任务名称 | 周次 | 负责人 | 预计开始日期 | 是否完成 | 预计结束日期 | 详细内容 | 是否延期 |
|----------|------|--------|--------------|----------|--------------|----------|----------|
| 基础设施与网关搭建 | 第1周 | 吴友诚 | 2026-08-01 | 否 | 2026-08-07 | docker-compose、Gateway 白名单、Nacos、Common | 否 |
| 鉴权与用户体系 | 第1周 | 吴友诚 | 2026-08-01 | 否 | 2026-08-07 | JWT、三角色、X-User 透传 | 否 |
| 后端服务骨架与表结构 | 第1周 | 陈嘉成/陈海洋 | 2026-08-01 | 否 | 2026-08-07 | 三服务 DDL、CRUD、MQ | 否 |
| 代码判分服务核心 | 第2周 | 吴友诚 | 2026-08-08 | 否 | 2026-08-14 | submit/result、编译/Checkstyle/PMD、graded 事件 | 否 |
| 知识库清洗与向量化链路 | 第2周 | 陈嘉成 | 2026-08-08 | 否 | 2026-08-14 | kb_corpus 清洗、corpus 拉取、mark-indexed | 否 |
| 学习/教学核心逻辑 | 第2周 | 陈海洋/吴友诚 | 2026-08-08 | 否 | 2026-08-14 | path 生成、作业发布、消费 graded | 否 |
| AI 服务（RAG）完整对接 | 第2周 | 陈海洋 | 2026-08-08 | 否 | 2026-08-14 | generate(mode)、path、kb/rebuild、Chat | 否 |
| 契约测试与跨服务联调 | 第2-3周 | 吴友诚 | 2026-08-12 | 否 | 2026-08-20 | Contract/Pact、联调 | 否 |
| 前端基础框架（学生端优先） | 第3周 | 曾姿妍 | 2026-08-15 | 否 | 2026-08-21 | 脚手架、路由守卫、学生端对接 | 否 |
| 教师端前端开发 | 第4周 | 曾姿妍 | 2026-08-22 | 否 | 2026-08-28 | 布置/批改/学情看板 | 否 |
| 管理员端前端开发 | 第4周 | 曾姿妍 | 2026-08-22 | 否 | 2026-08-28 | 用户/课程/资源/统计 | 否 |
| 端到端集成与部署 | 第5周 | 全员 | 2026-08-29 | 否 | 2026-09-04 | 全链路、SkyWalking、竞赛材料 | 否 |

---

## 表二：详细工作安排（接口/文件/字段级，含 DoD 与前置依赖）

| 任务 | 负责人 | 资料（spec ｜ DoD ｜ 前置） | 附件 | 状态 | 紧急状态 | 开始时间 | 截止时间 |
|------|--------|------|------|------|----------|----------|----------|
| 编写 docker-compose.yml（MySQL8/Redis7/RabbitMQ3-mgmt/Chroma/Nacos/SkyWalking OAP+UI） | 吴友诚 | p0 §2、主蓝图 §11 ｜ DoD: 一条命令起全部容器且均 healthy ｜ 前置: 无 | docker-compose.yml | 未开始 | 高 | 2026-08-01 | 2026-08-02 |
| Gateway 启动类 + Nacos 注册（server.port=8080） | 吴友诚 | p0 §3 ｜ DoD: 8080 注册到 Nacos，/actuator 可访问 ｜ 前置: #1 | — | 未开始 | 高 | 2026-08-01 | 2026-08-02 |
| Gateway AuthGlobalFilter：验 JWT + 注入 X-User-Id/X-User-Roles | 吴友诚 | p0 §5.1、主蓝图 §9 ｜ DoD: 合法 JWT 注入 X-User-* 转发；无/失效返 401；/health 免鉴权 ｜ 前置: #2、JWT 格式定 | — | 未开始 | 高 | 2026-08-02 | 2026-08-03 |
| Gateway 路由显式白名单（ai: chat/resource/generate/path/generate/kb/rebuild/health；/code/analyze 网关 404） | 吴友诚 | p0 §5.2(C5) ｜ DoD: ai 仅白名单放行；/code/analyze 网关返 404；其余 /api/<svc>/** 透传 ｜ 前置: #2 | — | 未开始 | 高 | 2026-08-03 | 2026-08-03 |
| SkyWalking agent 接入各服务 + UI 端口 18081 | 吴友诚 | p0 §2.1(C17) ｜ DoD: 各服务有 trace，UI 18081 可看 ｜ 前置: #1 | — | 未开始 | 中 | 2026-08-04 | 2026-08-05 |
| Common 模块：Result<T>/异常枚举/GlobalExceptionHandler/JWT 工具/常量 | 吴友诚 | 主蓝图 §6 ｜ DoD: 统一格式被所有服务引用，编译通过 ｜ 前置: 无 | — | 未开始 | 中 | 2026-08-01 | 2026-08-04 |
| auth_db DDL（user/role/permission）+ MyBatis-Plus 实体 | 吴友诚 | dev-wuyoucheng 鉴权 ｜ DoD: 三表建好，脚本可重跑 ｜ 前置: #1 MySQL | user.sql | 未开始 | 高 | 2026-08-03 | 2026-08-05 |
| 登录/注册接口 + JWT 签发 | 吴友诚 | dev-wuyoucheng ｜ DoD: 登录返 JWT，刷新/过期正确，密码加密 ｜ 前置: #7 | — | 未开始 | 高 | 2026-08-05 | 2026-08-06 |
| 角色模型 ROLE_STUDENT/TEACHER/ADMIN + 权限注解 | 吴友诚 | 主蓝图 §8 ｜ DoD: 三角色可区分，@RoleRequired 拦越权 ｜ 前置: #7 | — | 未开始 | 高 | 2026-08-05 | 2026-08-07 |
| AuthContext（读 X-User-*）+ AuthFeignInterceptor（Feign 透传） | 吴友诚 | 主蓝图 §9 ｜ DoD: 下游读到 X-User-*，Feign 自动透传，单测覆盖 ｜ 前置: #3、#9 | — | 未开始 | 高 | 2026-08-06 | 2026-08-07 |
| code_db DDL（submissions/grades…）+ 实体 | 吴友诚 | dev-wuyoucheng §2 ｜ DoD: 两表建好，字段对齐 C1 报告体 ｜ 前置: #1 | code.sql | 未开始 | 高 | 2026-08-08 | 2026-08-09 |
| POST /api/code/submit 收 7 字段 + 返 202 {submissionId,status} | 吴友诚 | 契约 C1 ｜ DoD: 收 studentId/assignmentId/assignmentItemId/language/sourceCode/expectedOutput/className，落 submissions，校验生效 ｜ 前置: #11、#10 | — | 未开始 | 高 | 2026-08-08 | 2026-08-10 |
| 判分引擎：编译(javac/gcc)+Checkstyle+PMD+运行比对 expectedOutput | 吴友诚 | dev-wuyoucheng §2.3 ｜ DoD: 三项跑通，比对正确，超时可控 ｜ 前置: #12 | — | 未开始 | 高 | 2026-08-09 | 2026-08-12 |
| GET /api/code/result/{id} 扁平报告（stdout/runTimeMs/compileOk/checkstyle/pmd/aiSuggestion/overallScore） | 吴友诚 | 契约 C1 ｜ DoD: 返回全字段，未完成返 status=running ｜ 前置: #13 | — | 未开始 | 高 | 2026-08-10 | 2026-08-12 |
| AI 代码建议：调 /api/ai/code/analyze 取 suggestions | 吴友诚 | dev-chenhaiyang §1.3.4 ｜ DoD: 调 ai 拿 suggestions 并落库/返回 ｜ 前置: #22、#13 | — | 未开始 | 中 | 2026-08-11 | 2026-08-13 |
| 发 assignment.graded 事件（完整报告体：runPassed/compileOk/stdout/runTimeMs/checkstyle/pmd/aiSuggestion/overallScore…） | 吴友诚 | 契约 C1、dev-wuyoucheng §2.4.7 ｜ DoD: payload 含完整报告体，RabbitMQ 可消费 ｜ 前置: #13、#14 | — | 未开始 | 高 | 2026-08-10 | 2026-08-14 |
| FastAPI 骨架 + Chroma 客户端 + LangGraph 编排 | 陈海洋 | dev-chenhaiyang §1 ｜ DoD: 8001 起服，Chroma 连通，基础链路通 ｜ 前置: #1 Chroma | — | 未开始 | 中 | 2026-08-08 | 2026-08-09 |
| POST /api/ai/chat RAG 问答（wire camelCase: userInput/sessionId/knowledgeBase/finalAnswer） | 陈海洋 | 契约 C4、dev-chenhaiyang §1.3.1 ｜ DoD: RAG 召回正确，响应 wire 全 camelCase ｜ 前置: #17、#28 语料 | — | 未开始 | 中 | 2026-08-09 | 2026-08-11 |
| POST /api/ai/resource/generate 增 mode（resource/judge/suggestion/quiz/evaluation 五态返回） | 陈海洋 | 契约 C3、dev-chenhaiyang §1.3.2 ｜ DoD: 5 种 mode 返回各异且符合决议，默认 resource 兼容 ｜ 前置: #17 | — | 未开始 | 高 | 2026-08-08 | 2026-08-12 |
| POST /api/ai/path/generate 补 suggestions/applicationAdvice/examAdvice/recommendTime | 陈海洋 | 契约 C9、dev-chenhaiyang §1.3.3 ｜ DoD: 响应含补充字段，聚合字段不在 ai 侧 ｜ 前置: #17 | — | 未开始 | 高 | 2026-08-10 | 2026-08-13 |
| POST /api/ai/kb/rebuild 拉 kb/corpus + 回调 mark-indexed（新增 resource 出站客户端） | 陈海洋 | 契约 C6、dev-chenhaiyang §1.3.5 ｜ DoD: 拉 corpus?status=0→向量化→回调 mark-indexed 成功，ai 不写关系表 ｜ 前置: #17、#28、#29 | — | 未开始 | 高 | 2026-08-10 | 2026-08-14 |
| ai 序列化强制 camelCase（api.py 输出 userInput/studentId/sourceCode/learningPath/…） | 陈海洋 | 契约 C4 ｜ DoD: 所有 ai 响应键全 camelCase，Java DTO 不空 ｜ 前置: #17 | — | 未开始 | 高 | 2026-08-08 | 2026-08-14 |
| resource_db DDL（resources/kb_corpus…）+ 实体 | 陈嘉成 | dev-chenjiacheng §3 ｜ DoD: 两表建好，kb_corpus 含 status 列 ｜ 前置: #1 | resource.sql | 未开始 | 高 | 2026-08-01 | 2026-08-03 |
| 资源 CRUD + 分页 + Nacos group=resource-group | 陈嘉成 | dev-chenjiacheng §4、契约 C11 ｜ DoD: CRUD+分页可用，group 生效 ｜ 前置: #23 | — | 未开始 | 高 | 2026-08-03 | 2026-08-06 |
| 画像透传 LearningProfileVO（camelCase，C8） | 陈嘉成 | 契约 C8、dev-chenjiacheng §4.5 ｜ DoD: 字段 camelCase，与 learning 一致 ｜ 前置: #23 | — | 未开始 | 中 | 2026-08-04 | 2026-08-06 |
| GET /api/learning/profile（由 /profile/me 改，C16）透传 | 陈嘉成 | 契约 C16 ｜ DoD: 端点由 /profile/me 改为 /profile，learning 能拉到 ｜ 前置: #23、#33 | — | 未开始 | 中 | 2026-08-05 | 2026-08-06 |
| resourceType 枚举限定（mindmap/quiz/reading/code/learning_path；level=basic/intermediate/advanced） | 陈嘉成 | 契约 C19 ｜ DoD: 仅 5 种 type 合法，level 三档对齐 ai ｜ 前置: #23 | — | 未开始 | 中 | 2026-08-06 | 2026-08-08 |
| KB 采集清洗流水线（数据源/去重/分块） | 陈嘉成 | dev-chenjiacheng §8.1-8.6 ｜ DoD: 源→去重→分块→落 kb_corpus(status=0) 全跑通 ｜ 前置: #23 | — | 未开始 | 高 | 2026-08-08 | 2026-08-11 |
| GET /api/resource/kb/corpus?status=0 暴露待向量化语料 | 陈嘉成 | 契约 C6、dev-chenjiacheng §8.9 ｜ DoD: 返回待向量化语料，status 过滤正确 ｜ 前置: #30 | — | 未开始 | 高 | 2026-08-08 | 2026-08-12 |
| POST /api/resource/kb/mark-indexed 回调（置 status=1，幂等） | 陈嘉成 | 契约 C6、dev-chenjiacheng §8.7.1 ｜ DoD: 置 status=1，幂等（重复调用安全） ｜ 前置: #23 | — | 未开始 | 高 | 2026-08-09 | 2026-08-12 |
| resource 消费 resource.generate 事件 | 陈嘉成 | dev-chenjiacheng ｜ DoD: 收事件触发生成，幂等 ｜ 前置: #24、事件源 | — | 未开始 | 中 | 2026-08-10 | 2026-08-13 |
| learning_db/teacher_db DDL（profiles/study_logs/paths/classes/assignments/grades…） | 陈海洋/吴友诚 | dev-chenhaiyang §A / dev-wuyoucheng §B ｜ DoD: 两库表齐全，字段对齐 path/grades ｜ 前置: #1 | learning.sql/teacher.sql | 未开始 | 高 | 2026-08-01 | 2026-08-04 |
| learning 基础 CRUD + MyBatis-Plus + MQ 配置 | 陈海洋 | dev-chenhaiyang ｜ DoD: CRUD 可用，RabbitMQ 连接正常 ｜ 前置: #32 | — | 未开始 | 高 | 2026-08-01 | 2026-08-05 |
| teacher 基础 CRUD + 班级/学生管理 | 吴友诚 | dev-wuyoucheng §B ｜ DoD: 班级/学生管理可用 ｜ 前置: #32 | — | 未开始 | 高 | 2026-08-05 | 2026-08-08 |
| AI 出站客户端 AiServiceClient（带 mode：suggestion/judge/resource/quiz/evaluation） | 陈海洋 | 契约 C3、dev-chenhaiyang A.4.5 ｜ DoD: 请求带 mode，按 mode 解析对应 VO ｜ 前置: #19 | — | 未开始 | 高 | 2026-08-08 | 2026-08-10 |
| POST /api/learning/profile/generate-suggestions（调 ai mode=suggestion） | 陈海洋 | 契约 C3、A.2.1 ｜ DoD: 调 ai mode=suggestion 返 {suggestions:[]} 并落库 ｜ 前置: #35、#33 | — | 未开始 | 中 | 2026-08-09 | 2026-08-11 |
| POST /api/learning/quiz/judge（调 ai mode=judge，解析 {score,correct,comment}） | 陈海洋 | 契约 C3、A.2.4 ｜ DoD: 调 ai mode=judge 解析对应结构 ｜ 前置: #35、#33 | — | 未开始 | 中 | 2026-08-09 | 2026-08-11 |
| POST /api/learning/path/generate + LearningPathVO（自算 totalTasks/completedTasks/learningRate/unmasteredRate/tasks[].id，C9） | 陈海洋 | 契约 C9、A.2.2 ｜ DoD: ai 返基础字段，learning 自算聚合字段后返完整 VO ｜ 前置: #20、#33 | — | 未开始 | 高 | 2026-08-10 | 2026-08-14 |
| study.progress 事件发布（学习行为埋点，exchange 名=事件名，C12） | 陈海洋 | 契约 C12、A.4.4 ｜ DoD: exchange 名=事件名，事件可消费 ｜ 前置: #33 | — | 未开始 | 中 | 2026-08-08 | 2026-08-12 |
| POST /api/teacher/assignment/publish（发 assignment.published 事件，C12 去 .exchange 后缀） | 吴友诚 | 契约 C12、B.4.5 ｜ DoD: 发布作业并发 assignment.published（无 .exchange 后缀） ｜ 前置: #34、#39 | — | 未开始 | 高 | 2026-08-08 | 2026-08-11 |
| CodeServiceClient.submit（202 回执 CodeSubmitReceiptVO，非全量 VO） | 吴友诚 | 契约 C1、B.4.2/4.3 ｜ DoD: 调 code 收 202 回执，类型 CodeSubmitReceiptVO，不轮询 ｜ 前置: #12 | — | 未开始 | 高 | 2026-08-08 | 2026-08-10 |
| AssignmentGradedConsumer（消费 assignment.graded 完整体，回填 grades run_result/static_report/ai_report/score，方案 A 事件驱动） | 吴友诚 | 契约 C1、B.4.5 ｜ DoD: 消费完整体回填 grades 四列，幂等，事件驱动不轮询 ｜ 前置: #16、#32 | — | 未开始 | 高 | 2026-08-11 | 2026-08-14 |
| 前端技术预研/脚手架预备（非界面：Vite+Vue3+TS+ElementPlus+Pinia+ECharts+Monaco） | 曾姿妍 | dev-zengziyan §2 ｜ DoD: 技术栈验证过，环境就绪（无界面） ｜ 前置: 无 | — | 未开始 | 中 | 2026-08-01 | 2026-08-14 |
| 工程脚手架 + Pinia + vue-router + 三角色路由守卫（学生/教师/管理员隔离） | 曾姿妍 | dev-zengziyan §3、主蓝图 ｜ DoD: 三角色路由隔离，越权跳登录，Pinia 可用 ｜ 前置: #43、#9 | — | 未开始 | 中 | 2026-08-15 | 2026-08-18 |
| 封装 axios 拦截器（带 JWT、统一错误、零 mock 直连网关） | 曾姿妍 | dev-zengziyan ｜ DoD: 自动带 JWT，统一错误 toast，直连网关零 mock ｜ 前置: #44、#3 | — | 未开始 | 中 | 2026-08-15 | 2026-08-18 |
| 学生端：首页六维进度 + 引导 + profile | 曾姿妍 | dev-zengziyan §3.2 ｜ DoD: 六维进度渲染，引导完成 profile 非空 ｜ 前置: #44、#25/#26 | — | 未开始 | 中 | 2026-08-16 | 2026-08-19 |
| 学生端：对话/RAG 问答页（调 /api/ai/*） | 曾姿妍 | dev-zengziyan ｜ DoD: 问答调 /api/ai/chat 正常，loading 处理 ｜ 前置: #45、#18 | — | 未开始 | 中 | 2026-08-17 | 2026-08-20 |
| 学生端：学习路径 / 测验判分展示 | 曾姿妍 | dev-zengziyan ｜ DoD: 路径展示、测验判分结果渲染 ｜ 前置: #38、#37 | — | 未开始 | 中 | 2026-08-18 | 2026-08-21 |
| 学生端：代码作业提交 + Monaco 编辑器 + 判分结果（C1 链路） | 曾姿妍 | dev-zengziyan §3、契约 C1 ｜ DoD: 编辑+提交→拿判分结果展示 ｜ 前置: #12/#14/#16、#44 | — | 未开始 | 高 | 2026-08-18 | 2026-08-21 |
| 教师端：作业布置（含 code 题）+ assignment.published | 曾姿妍 | dev-zengziyan ｜ DoD: 建作业→发 assignment.published，列表可见 ｜ 前置: #40、#44 | — | 未开始 | 高 | 2026-08-22 | 2026-08-25 |
| 教师端：批改复核（读 assignment.graded 结果，可微调） | 曾姿妍 | dev-zengziyan、B.4.5 ｜ DoD: 读 graded 结果展示，可微调保存 ｜ 前置: #42、#50 | — | 未开始 | 高 | 2026-08-23 | 2026-08-26 |
| 教师端：班级学情看板（ECharts：完成率/平均分/薄弱点） | 曾姿妍 | dev-zengziyan §3、主蓝图 ｜ DoD: 三图基于 MQ 同步数据 ｜ 前置: #39、#44 | — | 未开始 | 中 | 2026-08-24 | 2026-08-28 |
| 管理员端：用户/角色/课程管理 | 曾姿妍 | dev-zengziyan ｜ DoD: 用户/角色/课程增删改查可用 ｜ 前置: #9、#44 | — | 未开始 | 中 | 2026-08-22 | 2026-08-26 |
| 管理员端：资源/统计看板（调 learning/resource 聚合） | 曾姿妍 | dev-zengziyan ｜ DoD: 资源列表/统计拉取 learning/resource 聚合 ｜ 前置: #24、#33、#44 | — | 未开始 | 中 | 2026-08-25 | 2026-08-28 |
| 契约测试脚手架（Contract/Pact）覆盖 C1/C3/C4/C6/C9/C20 | 吴友诚 | 契约决议、主蓝图 §10 ｜ DoD: 六契约用例绿，CI 可跑 ｜ 前置: #12/#19/#22/#21/#38/#26 | — | 未开始 | 中 | 2026-08-12 | 2026-08-18 |
| 跨服务联调（gateway→各服务→ai→mq） | 全员 | 各 spec ｜ DoD: 全链路通，无 500/超时 ｜ 前置: #4/#10 + 各服务完成 | — | 未开始 | 高 | 2026-08-18 | 2026-08-24 |
| docker-compose 全链路跑通 + SkyWalking 观测验证 | 吴友诚 | 主蓝图 §11/§13 ｜ DoD: 起全栈，SkyWalking 有各服务 trace ｜ 前置: #5、#56 | docker-compose.yml | 未开始 | 高 | 2026-08-29 | 2026-09-01 |
| 竞赛演示材料/截图/文档整理 | 全员 | 主蓝图 ｜ DoD: 演示脚本/截图/文档齐，可现场跑通 ｜ 前置: #57 | — | 未开始 | 中 | 2026-09-01 | 2026-09-04 |

---

### 备注
- 「资料」列引用的文档均在 `docs/superpowers/specs/` 下：主蓝图 `2026-07-31-edu-agent-platform-design.md`、契约决议 `2026-07-31-contract-resolution.md`、各人开发文档 `dev-wuyoucheng`/`dev-chenjiacheng`/`dev-chenhaiyang`/`dev-zengziyan` 及 `p0-infra-gateway`。
- 表二「前置」列中的 `#N` 指向本表内对应行号（自上而下 1-58）。
- zzy 在 8.15 前仅做技术预研与脚手架预备，**新界面开发严格 8.15 后启动**。
- 契约测试（C1/C3/C4/C6/C9/C20）以《契约对齐决议》为唯一裁定依据，先于前端联调。
- 本文件**仅存档，未提交 git**（遵循文档阶段全部写完再统一提交的约定）。
