# Edu-Agent 平台化演进总体设计（主蓝图）

> 版本：v1.0 ｜ 日期：2026-07-31 ｜ 状态：已批准，待逐模块细化
> 本文档是**总体蓝图**。每个板块都已尽量写细，作为后续「逐模块开发文档（子 spec）」的直接底稿。
> 各阶段/各服务的详细开发文档见文末「子 spec 索引」，按需逐个展开。

---

## 0. 文档说明与范围

### 0.1 目标（Goals）
将现有「比赛用单体系统」演进为**公司级、全真实场景的多角色智能教育平台**：
- 架构：完整 Spring Cloud 微服务（Gateway + 5 个 Java 业务服务 + 1 个 Python AI 服务）。
- 角色：学生 / 教师（新增）/ 管理员（治理增强）三角色。
- 能力：在现有 AI 多智能体基础上引入 **RAG 检索增强**；**新增代码编译+静态检查+安全沙箱+AI 判分**能力。
- 技术观感：「让人一眼看出用过公司级技术」——Nacos / Gateway / Sentinel / SkyWalking / RabbitMQ / Redis / Docker / 向量库 全栈亮相。

### 0.2 非目标（Non-Goals，本期不做）
- **不做 LLM 微调（LoRA/全量）**：AI 仅做 RAG，不做训练/微调（算力与数据成本考量，且 RAG 落地稳、面试可讲全链路）。
- **不引入 Dify / n8n / Spring AI 等新框架**：AI 引擎继续用 Python + LangGraph + Chroma，与现有代码完全一致，零新框架。
- **不拆分多 git 仓库**：采用单仓多 Maven 模块，降低协作成本（后期可无损抽成独立仓库）。
- **本期不引入 K8s**：用 Docker Compose 编排，K8s 留作后续演进。

### 0.3 术语
- **服务(Service)**：一个独立可部署的 Spring Boot 应用（或 Python 应用），有独立端口、独立库表、独立 Dockerfile、独立 Nacos 配置。
- **模块(Module)**：单仓内的 Maven module；也是「按模块优化中间件」中的粒度单位。服务 = 模块这一级。
- **P0–P4**：实施阶段编号（见 §12）。

---

## 1. 依赖与关键路径框架

> 本节回答「哪些技术必须建在哪些技术之上」，是排期与分工的总依据。

### 1.1 技术依赖分层（自底向上）

```
[第0层·运行基座]  Docker / docker-compose
        │
[第1层·基础设施容器]  MySQL · Redis · RabbitMQ · Chroma(向量) · Nacos · SkyWalking
        │   （均只需 Docker，彼此独立，可一同拉起）
        ▼
[第2层·公共与身份]  common 公共模块 · auth-service(发JWT)
        │
[第3层·入口]  API Gateway（依赖 Nacos 发现服务 + auth-service 的 JWT 方案）
        │
[第4层·业务核心]  learning-service（学生学情核心）
        │
[第4.5层·AI 引擎]  ai-service（Python+LangGraph+RAG；依赖 Chroma+LLM；RAG 检索依赖"嵌入索引任务"先完成）
        │
[第5层·业务服务]  resource-service（依赖 ai 生成 + Redis 缓存）
        │            code-service（依赖 Docker守护 + ai /code/analyze）
        │
[第6层·聚合服务]  teacher-service（依赖 learning 学情 + code 判分 + ai 助教）
        │
[第7层·前端/治理]  前端三角色壳（依赖 网关 + 各服务API契约）
        │            管理端治理页（依赖 各自被治理对象 已存在）
        ▼
[横切增强]  Sentinel限流 · SkyWalking链路 · RabbitMQ异步  （均"贴"在已存在的服务上，不阻断主链）
```

### 1.2 技术 → 必须在其基础上才能实现（映射表）

| 要做的 | 必须已经存在（前置） |
|------|------|
| Docker / compose 编排 | 无（最底层） |
| MySQL/Redis/RabbitMQ/Chroma/Nacos/SkyWalking | 仅 Docker |
| `common` 公共模块 | 无（纯代码，最先写） |
| **auth-service** | MySQL + Nacos + common |
| **API Gateway** | Nacos（服务发现）+ auth-service（JWT 方案先定） |
| **learning-service** | MySQL + Nacos + common + Gateway 路由 + auth |
| **ai-service (RAG)** | Chroma + LLM API；**RAG 检索**还需"java_notes 嵌入索引任务"先跑完 |
| **resource-service** | learning 层基础设施 + **ai-service**（生成）+ Redis（缓存） |
| **code-service** | Docker 守护进程（沙箱）+ **ai-service** `/code/analyze` + MySQL |
| **teacher-service** | learning + code + ai 三者 + MySQL + Nacos + Gateway + auth |
| **RabbitMQ 异步**（资源生成/事件） | resource/teacher 先存在 + RabbitMQ 基础设施 |
| **Sentinel 限流** | 网关 + 被保护服务先存在 |
| **SkyWalking 可观测** | 各 Java 服务先存在 + SkyWalking 基础设施（agent 挂载） |
| **前端三角色** | Gateway（统一入口）+ 各服务 **API 契约**（契约定了 UI 可并行，集成依赖网关） |
| **管理端治理页** | AI治理→ai-service；监控→SkyWalking/Redis/MQ；班级治理→teacher；审计→审计日志先埋 |

### 1.3 阶段关键路径（P0–P4，标注每阶段前置）

| 阶段 | 内容 | 前置依赖（必须已完成） |
|------|------|------|
| **P0 骨架** | Docker 编排 + MySQL/Redis/RabbitMQ/Chroma/Nacos/SkyWalking + common + auth + Gateway 端到端 | 仅 Docker |
| **P1 学生纵切** | learning-service + resource-service + 学生前端 + RAG 接入 ai-service | P0（Gateway/auth/Nacos/MySQL/Chroma） |
| **P2 代码+AI** | code-service(沙箱/检查) + ai-service RAG 增强 + 教师代码作业判分 | P0 + ai-service 基础(P1) + code 需 Docker 守护 |
| **P3 教师+管理** | teacher-service + 教师前端 + 管理端治理/监控/审计页 + RabbitMQ 异步 + Sentinel | P1(learning/resource) + P2(code/ai) |
| **P4 打磨** | Sentinel 规则固化、SkyWalking 看板、压测、CI 质量门、文档/演示 | 全部前置 |

**关键路径（最长链）**：Docker → Nacos → auth → Gateway → learning → ai(RAG) → resource → code → teacher → 前端/管理。链上任一环卡住，后面全堵。

### 1.4 并行与风险
- **可并行起点**：P0 里"基础设施容器"彼此独立，可一人一次性 compose 拉起；`common` 模块可单人先写供全员依赖。
- **前端可早并行**：API 契约（OpenAPI）在 P0 末敲定后，前端 UI 壳/页面能与后端并行开发，最后接网关集成（见 §5.5 零 mock 策略）。
- **最大风险点**：① Gateway+auth 的 **JWT 跨服务透传**（P0 必须验证，否则后面全乱）；② RAG **嵌入索引任务**（P1）决定 ai 质量，要早跑通；③ code-service **沙箱依赖宿主机 Docker 守护**，容器里再跑 Docker 需用 docker.sock 挂载或 dind，是 P2 硬坑，需提前验证。

---

## 2. 技术栈总表（公司级）

| 层 | 技术 | 用途 | 部署形态 |
|----|------|------|---------|
| 运行基座 | **Docker / docker-compose** | 全部中间件与服务编排 | 容器（本机/单台云 VM） |
| 服务注册/配置 | **Nacos** | 服务发现 + 统一配置（standalone 模式） | 1 容器 |
| API 网关 | **Spring Cloud Gateway** | 路由 + JWT 鉴权过滤 + Sentinel 限流 | 1 容器 |
| 服务调用 | **OpenFeign** + Spring Cloud LoadBalancer | 服务间通信（经 Nacos 发现） | 应用内依赖 |
| 容错 | **Sentinel** | 熔断 / 降级 / 限流 | 依赖 + 可选 dashboard 容器 |
| 异步 | **RabbitMQ** | 资源生成、批改、学情事件解耦 | 1 容器 |
| 缓存 | **Redis** | 热点资源 / 会话 / 限流计数 | 1 容器 |
| 可观测 | **SkyWalking** | APM + 分布式链路追踪 | OAP + UI 2 容器 |
| 向量库(RAG) | **Chroma**(standalone) | 知识库嵌入与语义检索（Milvus 为升级路径） | 1 容器 |
| LLM | OpenAI 兼容(base_url 可换 Spark/DeepSeek) | Agent 推理 | 外部 API |
| AI 引擎 | **Python + FastAPI + LangGraph** | 多智能体工作流 + RAG | 1 容器 |
| 后端 | **Java 17 + Spring Boot 3 + MyBatis-Plus** | 各业务服务 | 各 1 容器 |
| 前端 | **Vue3 + TS + Vite + Element Plus + Pinia + vue-router + ECharts + Monaco** | 三角色单工程 | 1 容器(Nginx) |
| 质量 | **Checkstyle / PMD / SpotBugs / JUnit5 / Testcontainers / Vitest / ESLint** | 测试与质量门 | CI 内 |

> 选型原则：**匹配当前技术流**——AI 维持 Python+LangGraph；后端维持 Spring Boot；新增项均为业界主流且彼此正交的中间件，不引入与现有栈冲突的框架（见 §0.2 非目标）。

---

## 3. 服务拆分方案

### 3.1 组织结构：单仓多 Maven 模块
```
edu-agent-server/                # 单 git 仓
├── pom.xml                      # 父 pom（dependencyManagement）
├── common/                      # 公共模块（被所有服务依赖）
├── gateway-service/
├── auth-service/
├── learning-service/
├── resource-service/
├── code-service/
└── teacher-service/
edu-agent-ai/                    # 独立（Python）仓/目录，ai-service
edu-agent-web/                   # 前端单工程
```
- 每个服务 module：独立 `Application` 入口、独立端口、独立 `application.yml`（或 Nacos 配置）、独立 Dockerfile、独立 Nacos namespace/group。
- `common` 抽取所有服务共性，避免重复。

### 3.2 common 公共模块内容
- `Result` / `PageResult` / `BusinessException` / `GlobalExceptionHandler`
- JWT 工具（`JwtUtil`：签发/解析/校验，算法与 Gateway 过滤器一致）
- Feign 全局配置（拦截器注入网关注入的 user/role 头，避免被覆盖）
- Sentinel / MyBatis-Plus / Redis / RabbitMQ 公共配置与自动装配
- 统一请求/响应模型（`AuthContext`：当前 userId/role 从请求头解析）

### 3.3 服务清单（职责 / 端口 / 归属库表）

| 服务 | 端口 | 职责 | 归属库表(schema) |
|------|------|------|------|
| **gateway-service** | 8080(对前端) | 路由 / JWT 过滤 / Sentinel 限流 / 转发 | 无 |
| **auth-service** | 8081 | 注册 / 登录 / JWT / RBAC(STUDENT/TEACHER/ADMIN) | `auth_db`: users, roles, role_user |
| **learning-service** | 8082 | 学生画像 / 学习路径 / 任务 / 学习日志 / 测验 / 对话 / Dashboard | `learning_db`: student_profiles, learning_paths, learning_tasks, study_logs, quiz_records, conversations |
| **resource-service** | 8083 | 资源 CRUD / AI 生成(Feign→ai) / Redis 缓存 / 内容审核流 | `resource_db`: learning_resources, exercise_records, admin_stats_cache |
| **code-service** 🆕 | 8085 | 编译 + 静态检查 + Docker 沙箱 + AI 分析 + 代码作业判分 | `code_db`: code_submissions, code_check_reports |
| **teacher-service** 🆕 | 8084 | 班级/学生 / 题库 / 作业布置 / 批改编排 / 班级学情 / 资源发布 / AI 助教 | `teacher_db`: classes, class_students, questions, assignments, assignment_items, grades |
| **ai-service** (Py) | 8001 | LangGraph 多智能体 + RAG（chat/resource/path/code-analyze/kb-rebuild） | 向量库(Chroma)，无关系表 |

### 3.4 中间件 per-service 配置策略（支持后期按模块独享实例）
- **Redis**：每服务 key 前缀隔离（`resource:*` / `auth:*` / `code:*`），每服务独立 TTL、连接池、序列化；配置走 Nacos，可逐服务调整。后期某服务吃紧 → 单独分一个 Redis 实例，业务代码零改动。
- **RabbitMQ**：每服务独立 exchange/queue（`resource.generate` / `assignment.graded` / `study.progress`），可独立 vhost。
- **Chroma**：按课程/知识库独立 collection，ai-service 内隔离；后期可多实例分片。
- **Nacos**：每服务独立 namespace/group，配置互不干扰。
- **SkyWalking**：每服务独立 agent + 采样率 + 看板 + 告警规则。
> 演进路径：开发期**共用一个实例**（靠命名空间/前缀区分），上线后按瓶颈**逐服务拆分实例/集群**。这是 DB-per-service + 配置-per-service 带来的核心红利。

---

## 4. 数据库边界与表设计

### 4.1 DB-per-service
每个服务独立 schema，开发期可共用一个 MySQL 实例的不同 schema，生产各自实例。禁止跨服务直连对方库，跨服务数据一律经 API/Feign/MQ。

### 4.2 现有表迁移映射
| 现有表(单体) | 归属服务 | 动作 |
|------|------|------|
| users | auth-service | 迁移 + 增加 role 关联 |
| student_profiles | learning-service | 迁移 + 加 class_id |
| learning_paths / learning_tasks / study_logs | learning-service | 迁移 |
| quiz_records / conversations | learning-service | 迁移 + quiz_records 加 assignment_id |
| learning_resources / exercise_records / admin_stats_cache | resource-service | 迁移 |

### 4.3 新增表 DDL 草图（MySQL 8 / InnoDB / utf8mb4）

**auth_db**
```sql
CREATE TABLE roles (id BIGINT PRIMARY KEY, code VARCHAR(32) UNIQUE, name VARCHAR(32));
CREATE TABLE role_user (user_id BIGINT, role_id BIGINT, PRIMARY KEY(user_id, role_id));
-- users 增加 role 字段或在 role_user 维护；保留 BCrypt 密码
```

**teacher_db**
```sql
CREATE TABLE classes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(64), teacher_id BIGINT,
  course VARCHAR(64), semester VARCHAR(32), created_at DATETIME
);
CREATE TABLE class_students (
  class_id BIGINT, student_id BIGINT, PRIMARY KEY(class_id, student_id)
);
CREATE TABLE questions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, type VARCHAR(16),           -- choice/code/blank
  chapter VARCHAR(64), topic VARCHAR(64),
  content TEXT, options JSON, answer TEXT, explanation TEXT,
  difficulty VARCHAR(8), creator_id BIGINT, created_at DATETIME
);
CREATE TABLE assignments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, class_id BIGINT, title VARCHAR(128),
  type VARCHAR(16),                         -- homework/code
  deadline DATETIME, status TINYINT, creator_id BIGINT, created_at DATETIME
);
CREATE TABLE assignment_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, assignment_id BIGINT, question_id BIGINT, score INT
);
CREATE TABLE grades (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, assignment_id BIGINT, student_id BIGINT,
  item_id BIGINT, submission TEXT, run_result JSON, static_report JSON,
  ai_report JSON, score INT, graded_at DATETIME
);
```

**code_db**
```sql
CREATE TABLE code_submissions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, student_id BIGINT, assignment_item_id BIGINT NULL,
  language VARCHAR(16), source_code LONGTEXT, status TINYINT,            -- 0待运行1成功2失败3超时
  stdout LONGTEXT, stderr LONGTEXT, run_time_ms INT, created_at DATETIME
);
CREATE TABLE code_check_reports (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, submission_id BIGINT,
  compile_ok TINYINT, compile_msg LONGTEXT,
  checkstyle JSON, pmd JSON, ai_suggestion LONGTEXT, overall_score INT, created_at DATETIME
);
```

### 4.4 初始化与种子数据
- 各服务独立初始化脚本（**Flyway 或 Liquibase**，或 per-service init sql），禁止共用单体 init.sql。
- **种子数据(Seed)**：用 Flyway 的 seed / 独立 `seed.sql` 灌入**真实形态测试数据**（学生/班级/题目/笔记），供前端联调，**种子数据放 SQL 脚本、不进前端组件**（见 §5.5）。
- 移除现有 `EduAgentApplication` 里 admin 密码硬编码重置，改为初始化数据/脚本（安全与可维护性）。

---

## 5. 前端架构

### 5.1 单工程三角色
一个 Vue3 + TS 工程，三套布局 + 路由守卫：
- `/student/*`（现有迁移复用）
- `/teacher/*`（🆕 全套）
- `/admin/*`（现有隔离保留 + 🆕 治理/监控/审计页）
- 守卫：解析 JWT 中 role，越权访问直接跳登录/403。

### 5.2 目录结构（建议）
```
src/
├── layouts/        StudentLayout / TeacherLayout / AdminLayout
├── router/         index.ts（三角色路由 + 守卫）
├── stores/         pinia: auth(token/role/user) / 各业务
├── api/            auth/ learning/ resource/ code/ teacher/ ai（按服务分模块，baseURL→网关）
├── views/
│   ├── student/    （现有迁移）
│   ├── teacher/    ClassManage / QuestionBank / Assignment / Grade / Analytics / AiTutor / ResourcePublish
│   └── admin/      Dashboard / UserManage / ResourceManage / ContentReview / Statistics
│       └── govern/ AiAgent / Monitor / ClassGovern / Audit   （🆕 治理页）
├── components/     MonacoEditor 封装 / 通用卡片 / ECharts 封装
└── utils/          request(axios, baseURL=网关) / auth
```

### 5.3 API 层
- 按服务分模块；所有请求 `baseURL` 指向**网关**（统一入口），由网关按路径路由到各服务。
- API 契约（请求/响应结构）在 P0 末由后端定稿（OpenAPI），前端据此开发，避免漂移。

### 5.4 状态与 Monaco
- Pinia：`auth` 存 token/role/用户信息；多角色切换不刷新页面（路由 + 守卫控制）。
- **Monaco Editor** 封装为组件，用于：学生代码练习/作业作答、教师代码题布置、代码报告展示（配合 markmap/highlight.js 已有依赖）。

### 5.5 零 mock 调试策略（重点：避免魔法数字残留）
根因：mock 写在组件里 → 遗忘删除 → 魔法数字。对策：**假数据永不进组件**。
1. **主：早起真后端 + DB 种子数据**。P0 骨架一好前端即调真接口；"像样数据"由 Flyway seed 灌入真实 DB（在 SQL 里，不在前端），可审可清。
2. **兜底：网络层 stub**。后端某服务未好时，用 **Prism / Mockoon** 按 OpenAPI 起独立 stub HTTP 服务，前端只切 `baseURL`，**组件零改动、零 mock 代码**。
3. **防漂移：契约测试**（Pact / Spring Cloud Contract），前端期望响应自动比对后端契约。
4. **硬规则**：组件内禁止出现 `const mockData = [...]`；假数据只在 DB 种子 / 独立 stub 服务 / 网络拦截层，删除=关开关或不跑种子，不可能漏到业务代码。CI/prod 环境强制关闭 stub。

---

## 6. RAG 方案（AI 引擎）

### 6.1 ai-service 架构（维持 Python + LangGraph + Chroma）
```
FastAPI
 ├─ LangGraph StateGraph（沿用 graph.py 编排）
 │    nodes: init_profile → classify_intent → safety_precheck → retrieve_knowledge(RAG)
 │         → route_by_intent → {chat/explain/quiz/resource/path/tutor/onboarding}
 │         → safety_postcheck → evaluate → extract_profile → log → finalize
 ├─ RAG 层：kb/loader.py（加载切分）→ embed → Chroma；kb/retriever.py（语义检索 top-k）
 └─ LLM 调用：services/llm_client.py（OpenAI 兼容）
```
- **不拆 ai-chat/ai-code 两个服务**：保留为单个 Python ai-service，共享 Chroma 向量库，避免两进程各持一份向量库。代码分析作为 `/code/analyze` 端点。

### 6.2 嵌入/索引流水线
- 数据源：`java_notes` 等课程知识库（复用现有 `kb/`）。
- **数据归属（重要）**：RAG 所需的**基础语料（寻找/采集/清洗/去重/分块/落库）由陈嘉成（resource-service）端到端负责**，清洗后的干净语料再交付给 ai-service 做嵌入；吴友诚的 ai-service 只负责 embed + Chroma 检索，不做语料清洗（职责边界见 §12.1）。这是 data-centric AI 的关键环节——检索质量上限由清洗语料决定。
- 流程：`loader` 加载（陈嘉成清洗后的语料）→ 按语义/固定窗口切分(chunk) → Embedding（OpenAI 兼容 embed 接口或本地模型）→ 写入 Chroma collection（按课程/知识库隔离）。
- 触发：管理端"知识库管理"页调用 `POST /kb/rebuild` 重建索引；首次部署跑一次初始化嵌入任务。

### 6.3 检索注入
- 各 Agent 在 `call_llm` 前先 `retrieve_knowledge`：语义检索 top-k → 拼为上下文 → 注入 prompt system 段。
- 收益：稳定 JSON 输出、贴合教育场景、减少幻觉。

### 6.4 端点清单
| 端点 | 说明 |
|------|------|
| `POST /chat` | 多智能体对话（接入 RAG） |
| `POST /resource/generate` | 资源生成（mindmap/quiz/reading/code/review…），接入 RAG |
| `POST /path/generate` | 学习路径生成 |
| `POST /code/analyze` | **🆕** 代码质量分析（收代码+语言+上下文 → LLM 建议 JSON），供 code-service 调用 |
| `POST /kb/rebuild` | **🆕** 重建向量索引，供管理端治理页触发 |

### 6.5 其他
- **Prompt 集中管理**：统一放 `prompts/`，支持管理端动态配置（治理页可编辑后热更新）。
- **降级**：保留 `USE_MOCK_LLM=1`，LLM 不可用时回退本地模拟输出，保证流程跑通。

---

## 7. code-service 安全沙箱（核心难点 ★★★★★）

> 本模块是**技术最丰富 + 最难**双冠（详见 §10），也是全局唯一 🔴 高危安全点。

### 7.1 能力链路
```
Monaco 提交源码 → code-service
  ├─ javax.tools.JavaCompiler 内存编译（或写临时文件 javac）
  ├─ Checkstyle + PMD 静态检查 → 规范/潜在 Bug 报告
  ├─ Docker 沙箱执行（一次性容器，限制 CPU/内存/网络/文件系统，超时强杀）
  ├─ Feign → ai-service /code/analyze → LLM 代码质量建议
  └─ 综合判分（运行结果 + 静态检查 + AI 建议）→ 写 code_submissions / code_check_reports
```

### 7.2 编译与静态检查
- 编译：`javax.tools.JavaCompiler`（内存编译字符串源码）或落盘 `javac`。
- 静态检查：**Checkstyle**（规范）+ **PMD**（潜在 Bug），产出结构化 JSON 报告。

### 7.3 Docker 沙箱（安全红线）
- 用户代码**绝不在宿主机直接执行**。每个提交启动一次性容器：
  - 资源限制：`--memory=256m --cpus=1`
  - 网络隔离：`--network=none`
  - 文件系统：`--read-only` + 仅挂载代码目录(ro)
  - 超时：`waitFor(5s)`，超时 `destroyForcibly()`
  - 镜像：`openjdk:17-slim`
- **运行 Docker 的两种方式（P2 需提前验证）**：
  - (a) **挂载宿主 docker.sock**：`/var/run/docker.sock:/var/run/docker.sock`，code-service 容器内调用宿主 Docker 起子容器（简单，但给容器较高权限，需评估）。
  - (b) **Docker-in-Docker (dind)**：独立 dind 容器，code-service 连 dind 守护（隔离更好，运维更重）。
  - 推荐先验证 (a)，生产再评估 (b)。

### 7.4 AI 分析与判分
- `Feign` 调 `ai-service /code/analyze`，传源码+语言+上下文，得 LLM 建议 JSON。
- 判分综合：运行是否通过(权重) + 静态检查违规数 + AI 建议严重度 → `overall_score`。
- 教师端"代码作业"：提交 → code-service 判分 → 结果回写 `grades`，教师可复核/微调。

### 7.5 表结构
见 §4.3（`code_submissions` / `code_check_reports`）。

---

## 8. 异步解耦（RabbitMQ）

| 交换机/队列 | 生产者 | 消费者 | 用途 |
|------|------|------|------|
| `resource.generate` | resource-service(收请求) | resource-service(worker) | 异步调 ai 生成 → 写库 → 缓存 → 推进度 |
| `assignment.graded` | code-service(判分完) | teacher-service | 学情/成绩事件 |
| `study.progress` | learning-service | teacher-service | 学生进度同步到班级看板 |

- 可靠性：手动 ack、失败重试（有限次）、死信队列(DLQ) 收集异常。
- 幂等：消费者按 submission_id / assignment_id 去重，避免重复处理。

---

## 9. 可观测性

### 9.1 SkyWalking
- 每个 Java 服务挂载 SkyWalking agent（docker volume 挂 agent + JVM `-javaagent` 参数）。
- 采样率 per-service 配置；OAP 收集、UI 展示链路/耗时/错误率。
- 管理端"系统监控"页拉取 SkyWalking / Redis / RabbitMQ 指标，呈现服务健康与 MQ 积压。

### 9.2 Sentinel
- 网关层：按路由限流（如 `/api/code/**` 限流更严，防沙箱被打爆）。
- 服务层：敏感端点（生成、判分）熔断/降级；规则可在 Nacos 动态配置。

### 9.3 日志与审计
- 统一日志格式；竞赛期可简化（文件日志 + SkyWalking 追踪 ID 串联）。
- **审计日志**：关键操作（登录、资源审核、班级/作业增删、知识库重建）落 `audit_log` 表，供管理端"审计与运营"页展示（P3）。

---

## 10. 模块技术丰富度 × 难度矩阵（亮点与难度）

| 模块 | 涉及的主要技术 | 技术丰富度 | 难度/风险 |
|------|------|:--:|:--:|
| **code-service 🆕** | JavaCompiler · Checkstyle · PMD · **Docker 沙箱(docker.sock/dind)** · Feign→ai · RabbitMQ · MySQL · 资源限制/超时强杀/安全隔离 | ★★★★★ | ★★★★★ |
| **平台层 P0** | Gateway · Nacos · Sentinel · SkyWalking · OpenFeign · LoadBalancer · JWT 透传 | ★★★★★ | ★★★★ |
| **ai-service** | FastAPI · **LangGraph(工作流)** · Chroma · Embedding · RAG · OpenAI兼容LLM · Prompt 管理 | ★★★★ | ★★★ |
| **teacher-service 🆕** | MySQL(多表) · Feign×3(learning/code/ai) · RabbitMQ · 业务编排 · ECharts | ★★★★ | ★★★ |
| **resource-service** | MySQL · Feign→ai · Redis · RabbitMQ · 内容审核状态机 | ★★★ | ★★☆ |
| **前端** | Vue3 · Element Plus · Pinia · vue-router · **Monaco** · ECharts · 三角色守卫 | ★★★★ | ★★★ |
| **learning-service** | MySQL · MyBatisPlus · 聚合计算 | ★★☆ | ★★ |
| **auth-service** | JWT · BCrypt · RBAC · MySQL | ★★ | ★★ |

### 10.1 结论（叙事主线）
- **技术最丰富 + 最难 = `code-service`（双冠）**：最大技术亮点，简历核武——「自研安全代码沙箱：内存编译 + 静态检查 + Docker 隔离执行 + AI 代码分析 + 自动判分」。
- **中间件技术最密集 = 平台层 P0**：全 Spring Cloud 生态，是"公司级技术观感"主来源。
- **AI 工程最丰富 = `ai-service`**：LangGraph 工作流 + RAG + 向量库。
- **推荐答辩/简历叙事顺序**：`code-service`（沙箱+判分）→ 平台层（Spring Cloud 全家桶）→ `ai-service`（LangGraph+RAG）。
- 全局难点集中在两处：**code-service 的 Docker 沙箱**（隔离/限流/超时，容器里再起容器需 docker.sock/dind，P2 硬坑）与 **P0 的 JWT 跨服务透传**（一次性解决，全线受益）。

---

## 11. 测试与质量门

### 11.1 后端
- 单测：JUnit5 + Mockito，各服务核心逻辑覆盖率达标。
- 集成测试：**Testcontainers** 起 MySQL/Redis/RabbitMQ，验证跨服务（Feign/MQ）行为。
- **质量门（CI）**：**Checkstyle / PMD / SpotBugs** 作为 Java 构建质量门（不达标拦截 PR）；同时 code-service **自用**这套检查（元闭环，最硬核简历点）。

### 11.2 前端
- Vitest + @vue/test-utils 组件/store 测试（现有）。
- ESLint 强制；**组件内零 mock**（见 §5.5）。

### 11.3 CI / 契约
- CI（GitHub Actions / GitLab CI）：多模块 build + test；任一质量门失败阻断合并。
- 契约测试（Pact / Spring Cloud Contract）防前后端漂移。

---

## 12. 4 人分工 + P0–P4 分期

### 12.1 角色分工（已确认）
| 成员 | 职责 | 主导阶段 |
|------|------|------|
| **吴友诚（架构/地基）** | 基础设施 + common + auth + Gateway + JWT 透传 + **微服务拆分**（铺地基、定契约） | P0 |
| **吴友诚 → code-service + ai-service** | 地基交付后专注：**code-service**（最难，沙箱/检查/判分） + **ai-service**（AI 最丰富，RAG） | P2 |
| **陈嘉成（成员B）** | **resource-service**：①资源生成（调 ai，与吴友诚契约对齐）；②**RAG 知识库基础语料的采集与清洗**（java_notes 等基础数据的寻找→清洗→去重→分块→落库，交付吴友诚 ai-service 向量化；最轻，可支援 ai/code） | P1–P2 |
| **陈海洋（成员C）** | **learning-service + teacher-service**（学情核心 + 教师端后端） | P1 / P3 |
| **曾姿妍（成员D，前端）** | **全部前端**：现有学生端调整 → 教师端 → 管理端治理/监控/审计页 | 全程 |

> 耦合说明：吴友诚拥有 `code + ai` 两个皇冠模块（最难 + AI 最丰富），广度/深度最大化；陈嘉成的 resource 依赖 ai（吴友诚），契约对齐即可，且最轻可当副手；陈海洋的 teacher 调 learning(自己)/code(吴友诚)/ai(吴友诚)，聚合层天然跨人；曾姿妍纯前端，按契约接各服务。后端模块均**契约优先、领先前端开发**，不阻塞。

### 12.2 阶段任务 / 前置 / 产出 / 验收
| 阶段 | 任务 | 前置 | 关键产出 | 验收标准 |
|------|------|------|------|------|
| **P0** | compose 6 中间件 + common + auth + Gateway，JWT 透传端到端 | Docker | 可登录→网关→下游带身份；SkyWalking 有链路 | 一条跨服务请求全链路可追踪 |
| **P1** | learning/resource + 学生前端 + ai RAG 接入 + 种子数据 | P0 | 学生登录→对话(RAG)→资源生成跑通 | RAG 检索生效、前端接真数据 |
| **P2** | code-service(沙箱/检查) + ai /code/analyze + 代码作业判分 | P0+P1 | 提交代码→编译/检查/沙箱/AI 判分 | 沙箱隔离验证、判分合理 |
| **P3** | teacher-service + 教师前端 + 管理端治理/监控/审计 + MQ 异步 + Sentinel | P1+P2 | 三角色可用、治理页拉实时指标 | 教师全流程 + 监控/审计可见 |
| **P4** | Sentinel 规则固化、SkyWalking 看板、压测、CI 质量门、文档/演示 | 全 | 可演示系统 + 文档 | 质量门全绿、演示脚本通过 |

### 12.3 子 spec 索引（按阶段逐个展开为开发文档）

**阶段总索引**
- [x] `specs/2026-07-31-p0-infra-gateway.md`（基础设施 + 公共模块 + auth + Gateway + JWT 透传）【已完成】
- [ ] P1：learning/resource 服务 + 学生前端 + RAG 接入
- [ ] P2：code-service（沙箱/检查/判分）+ ai /code/analyze
- [ ] P3：teacher-service + 教师前端 + 管理端治理/监控/审计 + MQ/Sentinel
- [ ] P4：Sentinel/SkyWalking/压测/CI 质量门/文档

**按分工的开发文档（每人一份，覆盖其名下全部模块 / 阶段）**
- [x] `specs/2026-07-31-dev-wuyoucheng.md` — **吴友诚**：地基(见P0) + ai-service(RAG) + code-service(最难) + P4 加固 + 单体→微服务拆分指导【已完成】
- [x] `specs/2026-07-31-dev-chenjiacheng.md` — **陈嘉成**：resource-service（资源生成调 ai） + **RAG 知识库基础语料采集与清洗**（寻找→清洗→去重→分块→落库，交付 ai 向量化）【已完成】
- [x] `specs/2026-07-31-dev-chenhaiyang.md` — **陈海洋**：learning-service(学情核心) + teacher-service(教师端后端)【已完成】
- [x] `specs/2026-07-31-dev-zengziyan.md` — **曾姿妍**：全部前端（学生端调整 / 教师端 / 管理端治理·监控·审计；零 mock 策略）【已完成】
- [x] `specs/2026-07-31-contract-resolution.md` — **《契约对齐决议》**：跨文档契约冲突裁定（C1/C3/C4/C6 及卫星项；9 项纯一致性已落地，决策项待吴友诚确认）【已起草·待架构确认】

> 每份开发文档均按「需求 → 接口契约 → 数据模型 → 关键实现 → 测试 → 验收」粒度展开，并复用 P0 的 `common`（AuthContext / AuthFeignInterceptor）与 auth/Gateway 契约。

---

## 13. 部署与运维

- **docker-compose 全栈**：mysql / redis / rabbitmq / chroma / nacos / skywalking-oap / skywalking-ui / gateway / 各业务服务 / ai-service / web(nginx)。
- **Dockerfile**：各服务多阶段构建（Maven/`pip` 构建 → 精简运行镜像），SkyWalking agent 随镜像或 volume 挂载。
- **启动脚本**：更新 `start-docker.bat` 覆盖新服务；默认账号（admin / 各角色测试账号）由初始化脚本设定。
- **文档**：架构图、各服务端口与依赖、API 文档（按服务）、数据库设计、演示脚本。
- **资源建议**：本地/演示机 ≥16GB 内存（容器多）；8GB 偏紧。无需独立云服务器，廉价云 VM + Docker 即可对外演示。

---

*主蓝图结束。后续按 §12.3 子 spec 索引逐模块展开开发文档。*
