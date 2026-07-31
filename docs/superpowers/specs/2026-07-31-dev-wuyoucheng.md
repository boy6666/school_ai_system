# 吴友诚 开发文档（子 spec）：ai-service · code-service · P4 加固 · 拆分指导

> 阶段：P0（已交付，见 `2026-07-31-p0-infra-gateway.md`）→ P2（ai/code）→ P4（加固）
> 负责人：**吴友诚（架构/地基）** ｜ 日期：2026-07-31 ｜ 状态：开发文档（可直接落地）
> 本文把主蓝图 §6 / §7 / §9 / §10 / §11 / §12 中**吴友诚名下**的部分展开为
> 「需求 → 接口契约 → 数据模型 → 关键实现 → 测试 → 验收」粒度的开发文档。
> **本文不重复 P0**：common / auth / Gateway / JWT 透传一律引用 P0，仅对齐接口与契约。

---

## 0. 吴友诚职责总览

主蓝图 §12.1 已确认：吴友诚拥有**地基（P0，已建）+ code-service（最难）+ ai-service（AI 最丰富）** 两块皇冠模块，并负责 P4 全栈加固与「单体→微服务」拆分指导。

| 板块 | 本文章节 | 端口/库 | 对接人 |
|------|---------|--------|--------|
| 微服务地基、common、auth、Gateway、JWT 透传 | ——（引用 P0） | 见 P0 | 全员基线 |
| **ai-service**（Python FastAPI + LangGraph + Chroma RAG） | §1 | `8001`，无关系表 | 陈嘉成(resource) / 陈海洋(learning/teacher) |
| **code-service**（Java：编译+静态检查+Docker 沙箱+AI 判分） | §2 | `8085`，`code_db` | 陈海洋(teacher 作业批改) |
| **P4 加固**（Sentinel / SkyWalking / 压测 / CI 质量门 / 文档演示） | §3 | 全服务 | 全员 |
| **单体→微服务拆分指导** | §4 | `edu-agent-server` 现有类→各服务 | 陈嘉成 / 陈海洋 |

> 命名/对齐硬约束（来自主蓝图 + P0）：
> - 服务名：`auth-service` `gateway-service` `learning-service` `resource-service` `code-service` `teacher-service` `ai-service`。
> - 网关路由前缀：`/api/<服务>/**`。前端与 Feign 都只认网关前缀。
> - ai-service 经 Nacos 注册；Java 经 **OpenFeign + LoadBalancer** 调 `lb://ai-service/...`。
> - 全部 DB-per-service，跨服务**一律走 Feign/MQ**，禁止直连对方库（§4.3 详述）。

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

### 1.3.1 `POST /api/ai/chat`

**请求**
```json
{
  "user_input": "什么是 Java 的多态？",
  "student_id": "1001",
  "session_id": "sess_abc",
  "profile": { "course": "JavaSE", "topic": "面向对象", "weaknesses": [], "knowledge_base": "零基础" }
}
```

**响应（200）**
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "intent": "explain",
    "final_answer": "多态是指……（注入 RAG 上下文后的讲解）",
    "profile": { "topic": "面向对象", "_onboarding_phase": "done" },
    "resources": null,
    "learning_path": null,
    "safety_report": { "passed": true },
    "evaluation_report": { "mastery": 72 },
    "resource_dir": null,
    "profile_complete": true
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
  "prompt": "为薄弱学生生成思维导图"
}
```
> 说明：`resourceType` 取值沿用现有 `api.py` 的 `role_prompts` 键集合；新增的 `level` 映射到原 `difficulty`（`easy`→basic）。陈嘉成(resource-service)调用时传 `student_id/chapter/topic/resourceType/level`，由 ai-service 内部拼 prompt（保持与现有 `AiClient.buildResourcePrompt` 一致的角色 prompt）。

**响应（200）**
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

### 1.3.3 `POST /api/ai/path/generate`

**请求**
```json
{ "student_id": "1001", "prompt": "根据画像规划 4 周学习路径", "profile": { "course": "JavaSE", "weaknesses": ["多线程"] } }
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
    ]
  }
}
```

### 1.3.4 `POST /api/ai/code/analyze` ★仅供内网（code-service 经 Feign 调用）

**请求**
```json
{
  "language": "java",
  "source_code": "public class Main { public static void main(String[] a){ System.out.println(\"hi\"); } }",
  "context": {
    "assignment_item_id": 12,
    "student_id": "1001",
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

**请求**：`{}` 或 `{ "collection": "java_notes", "force": true }`
**响应（202 异步 / 或 200 + 进度）**
```json
{ "code": 0, "message": "ok", "data": { "task_id": "rebuild_20260731", "status": "started", "docs_indexed": 0 } }
```
> 重建为耗时操作，建议后台线程执行，接口立即返回 `task_id`；管理端轮询 `/api/ai/kb/status/{task_id}` 取进度。

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
7. 结果经 MQ 事件 `assignment.graded` 通知 teacher-service（陈海洋作业批改）。

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

### 2.3.2 `GET /api/code/result/{id}`
```json
{
  "code": 0, "message": "ok",
  "data": {
    "submissionId": 1024,
    "status": 1,
    "language": "java",
    "stdout": "hi\n", "stderr": "",
    "runTimeMs": 42,
    "report": {
      "compileOk": 1, "compileMsg": "",
      "checkstyle": { "errorCount": 0, "warningCount": 1,
        "violations": [ { "file":"Main.java","line":1,"severity":"warning","message":"缺少 Javadoc","source":"JavadocMethod","rule":"JavadocMethod" } ] },
      "pmd": { "violationCount": 0, "violations": [] },
      "aiSuggestion": "整体良好，建议补充方法注释。",
      "overallScore": 96,
      "scoreDetail": { "compile":40, "checkstyle":-1, "pmd":0, "run":57 }
    }
  }
}
```

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
`CodeAnalyzeRequest` 字段见 §1.3.4。**容错**：AI 调用必须设短超时 + fallback（返回空建议），绝不能因 AI 抖动导致判分失败（Sentinel/超时在 P4 §3.1 固化）。

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

### 2.4.7 与 teacher-service 对接（陈海洋 assignment 调 code）
```
teacher-service 发布代码作业(assignment_items.type=code)
   → 学生前端调用 code-service /api/code/submit(assignmentItemId=?)
   → code-service 判分完 发 MQ 事件 assignment.graded
   → teacher-service 消费：按 (assignment_id, student_id, item_id) 幂等写 grades
       grades.submission=report, grades.run_result=..., grades.static_report=...,
       grades.ai_report=ai_suggestion, grades.score=overall_score
   → 教师在 teacher 端"批改/复核"页可微调 score（人工覆盖）
```
> code-service **只写 code_db**；成绩归属 teacher_db.grades，由 teacher 消费事件写入（跨服务最终一致，避免分布式事务，见 §4.3）。`assignment.graded` 事件体：
```json
{ "assignmentId":5, "assignmentItemId":12, "studentId":1001,
  "submissionId":1024, "overallScore":96, "status":"graded",
  "runPassed":true, "aiSuggestion":"整体良好" }
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
     -H 'Content-Type: application/json' -d '{"user_input":"什么是多态","student_id":"1001"}'
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
> 本节能直接指导陈嘉成(resource)、陈海洋(learning/teacher)动手；吴友诚的 code 已独立成 §2。

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
| `service/ResourceService(+impl)、ContentReviewService(+impl)` | 迁 resource-service（impl 里 `aiClient.generateResource` 改为 Feign 调 `/api/ai/resource/generate`，契约见 §1.3.2） |
| `entity/Resource` | 迁 resource_db（`resources` 表，列基本复用现有 `resources` DDL） |
| `mapper/ResourceMapper` | 迁 resource-service |
| `exercise_records`、`admin_stats_cache` 表 | 迁 resource_db（对应 `service/impl/ResourceServiceImpl` 写库逻辑保留） |

> 对齐：陈嘉成调用 AI 时，`generateResource` 的请求字段 `studentId/chapterName/topic/type/level` → ai `/resource/generate`（§1.3.2）；返回 `content` 字符串直接落 `resources.content`。

### 4.1.4 → teacher-service（陈海洋，原 report-service 8084）
| 现有类 | 动作 |
|--------|------|
| `controller/AdminController、ReportController` | 迁 teacher-service |
| `service/AdminService(+impl)、ReportService(+impl)` | 迁 teacher-service |
| `entity/Report` | 迁 teacher_db |
| `mapper/ReportMapper` | 迁 teacher-service |
| **新增**（陈海洋 P3 建）：`classes、class_students、questions、assignments、assignment_items、grades` | 见主蓝图 §4.3 DDL |
| 作业批改消费 `assignment.graded` 事件（§2.4.7） | 陈海洋在 teacher-service 加 MQ 消费者 |

### 4.1.5 → code-service（吴友诚，见 §2）
- 单体里**没有**代码相关类，全新增。前端的"代码题"由 teacher 的 `questions.type=code` + code-service 判分协作完成。

### 4.1.6 → ai-service（吴友诚，见 §1）— 非 Java
- `controller/JavaNotesController` + `service/JavaNotesService` + `entity/JavaNotes`：知识源**导出为 Markdown**（§1.4.1），不再作为 Java 服务表；管理端"知识库"走 ai-service `/kb/rebuild`。
- `entity/AgentConfig` + `service/AgentConfigService`：prompts 配置迁 ai-service（或 Nacos 动态配置，管理端可热更）。
- `entity/SystemSetting` + `service/SystemSettingService`：系统设置类配置迁 **Nacos 配置中心**（不再落业务库）。

## 4.2 迁移步骤（通用，每个服务照做）

1. **建 module**：在 `edu-agent-server/pom.xml` 父工程下加 `<module>code-service</module>`，各 module 独立 `Application`、独立端口（Nacos 配置或 `application.yml`）。
2. **改包名**：`com.eduagent.Xxx` → `com.eduagent.<service>.Xxx`（如 `com.eduagent.code`），IDE 全局重构 + 校验 `@SpringBootApplication(scanBasePackages="com.eduagent.code")`。
3. **依赖 common**：每个 module `pom.xml` 加 `com.eduagent:common`（P0 已发），移除单体里重复的 `Result/PageResult/GlobalExceptionHandler/JwtTokenProvider` 等（改引 common）。
4. **换鉴权**：删单体 `security/*` 的 `JwtAuthenticationFilter`（Gateway 已做）；加 common 的 `AuthContextFilter` + `AuthFeignInterceptor`（P0 §6.2），控制器用 `AuthContext.getUserId()` 取身份（替换 `SecurityContextHolder` 取 principal 的写法，见 `ResourceController.getCurrentUserId`）。
5. **换数据源**：每个服务配自己库（`auth_db/learning_db/resource_db/code_db/teacher_db`），删跨库 `@TableName` 引用。
6. **换 AI 调用**：`agent/AiClient`（RestTemplate 直连）改为 Feign `AiServiceClient`（§1.5.3 / §2.4.5）。
7. **跨服务调用**：本地 `service` 互调 → Feign/MQ（§4.3）。
8. **Flyway/Liquibase**：每服务独立 `db/migration/V1__init.sql`，从现有 `init.sql`/`edu_agent.sql` 按归属裁剪，**禁止共用单体 init.sql**。
9. **种子数据**：真实测试数据写进 `V2__seed.sql`（配合前端零 mock 策略，主蓝图 §5.5）。

## 4.3 常见坑（重点）

- **包名扫描遗漏**：`@MapperScan`、`@ComponentScan` 默认只扫启动类同级包；跨包 entity/mapper 要显式配 `basePackages`。
- **事务边界**：单体 `@Transactional` 跨多表在一个库 OK；拆库后**跨库 @Transactional 失效**。改为：每服务只管自己库的本地事务；跨服务写用 **MQ 事件 + 幂等消费**（如 §2.4.7 成绩落 teacher_db）。
- **分布式事务取舍**：**本期不引入 Seata/2PC**（主蓝图非目标）。采用「本地事务 + 发事件 + 最终一致 + 消费者幂等」。例：作业发布(teacher) 与 代码判分(code) 不保证原子，靠 `assignment.graded` 事件最终补齐 grades；若 code 判分失败，grades 留空，教师端可见"待判分"状态。
- **跨服务查询禁直连库**：learning 不能 `SELECT` resource 的表；要数据走 Feign（如 teacher 聚合班级学情 → Feign 调 learning + code 拿数据后自己拼）。
- **身份透传**：Feign 调下游必须走 `AuthFeignInterceptor`（P0 已固化为 common 默认配置），**不要**在 Feign 请求里手填 `X-User-*` 头（会被网关视为伪造，且破坏透传一致性）。
- **AI 路径前缀**：所有 Java 侧调用 ai 的路径都带 `/api/ai` 前缀（网关不 StripPrefix），与 §1.5.1 一致；否则 404。
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

| 契约 | 提供方(吴友诚) | 消费方 | 对齐章节 |
|------|--------------|--------|---------|
| `/api/ai/chat` | ai-service | learning(陈海洋)/前端 | §1.3.1 |
| `/api/ai/resource/generate` | ai-service | resource(陈嘉成) | §1.3.2 |
| `/api/ai/path/generate` | ai-service | learning(陈海洋) | §1.3.3 |
| `/api/ai/code/analyze`（内网） | ai-service | code(吴友诚) | §1.3.4 / §2.4.5 |
| `/api/ai/kb/rebuild` | ai-service | admin/teacher(陈海洋) | §1.3.5 |
| `/api/code/submit` `/result/{id}` | code-service | teacher(陈海洋)/前端 | §2.3 |
| `assignment.graded`(MQ) | code-service | teacher(陈海洋) | §2.4.7 |

---

*吴友诚子 spec 结束。P1(learning/resource)、P3(teacher/admin) 见对应成员子 spec；本文件覆盖 code-service / ai-service / P4 加固 / 拆分指导。*
