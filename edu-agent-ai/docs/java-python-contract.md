# Java ↔ Python 对接契约（edu-agent-teacher ⇄ edu-agent-ai）

本文只讲一件事：**Java 侧 Feign 调用与 AI 服务之间的入参/出参约定**，以及这些约定被哪些测试钉住。
服务发现、网关路由、Nacos 注册见 `deploy/nacos-config/edu-agent-ai.yaml` 与 `docs/dev-prod-guide.md`。

## 1. 谁在调、走哪条路

| 项 | 值 |
|---|---|
| 调用方 | `edu-agent-teacher` → `feign/AiServiceClient.java` |
| 服务名 | `edu-agent-ai`（`@FeignClient(name = "edu-agent-ai", url = "${ai.base-url:}")`） |
| 路径 | `path = "/api/edu-agent-ai"`（**无 StripPrefix**，三段必须同名：Feign `path` = Python `API_PREFIX` = 网关 `Path=`） |
| 解析方式 | `url` 默认空 → 走 Nacos 服务发现（`lb://edu-agent-ai`） |
| 网关放行 | 逐条列举，无通配：`/health`、`/chat`、`/path/generate`、`/resource/generate`、`/kb/rebuild` |

## 2. 两种入参形态：为什么会有两套

`api.py` 同时服务两类调用方，业务逻辑只写一遍，差异在契约适配层（`api.py` 顶部 `FEIGN_*` 常量以下）：

| 调用方 | 入参形态 | 响应形态 |
|---|---|---|
| Java（Feign） | camelCase，`/chat` 带 `message`、`/resource/generate` 带 `mode` | `Result` 信封 `{code, message, data}` |
| 前端/脚本直连（历史） | snake_case 裸 JSON（`user_input` / `resourceType` / `prompt`） | 原有裸 JSON，**行为不变** |

分发规则：看原始 body 里出现哪个标志字段——`message` → Java 形态；`mode` → Java 形态；否则直连形态。
不用 header 协商，也不引第二套协议。

## 3. /chat

### 入参

```jsonc
// Java 形态（AiChatRequest）
{ "message": "什么是多态", "context": { "studentId": "s_1", "classId": "c_9" } }
// 直连形态（不变）
{ "user_input": "什么是多态", "student_id": "student_001", "session_id": "api_session", "profile": {} }
```

`context` 的处理：`studentId` → `student_id`（记忆线程按 `{student_id}_{session_id}` 隔离）；
`sessionId` → `session_id`；**其余键**（`classId`、`courseName`…）整体并入 `profile` 传给 graph。
`context.profile` 若存在则与之合并。

### 出参（Java 形态）

```jsonc
{ "code": 0, "message": "success",
  "data": { "answer": "多态是同一接口的不同实现。", "intent": "explain", "references": { /* 见下 */ } } }
```

`data` 恰好三个字段，对应 `AiChatResult(String answer, String intent, Object references)`。

- `answer`：取自 graph 的 `final_answer`。**explain 等意图的 `final_answer` 是 JSON 串**
  （`{"type","content","weaknesses_focus","suggestion"}`），直接给前端会渲染成一坨 JSON，
  故这里取其中 `content` 作正文；其余字段并入 `references`。
- `references`：graph 暂无 `references` 字段，先透传它实际产出的
  `resources` / `learning_path` / `safety_report` / `evaluation_report` / `resource_dir`，
  以及上面拆出来的 `weaknesses_focus` / `suggestion`。**任何一项都没有时为 `null`**。
  将来 graph 直接给出 `references` 则优先使用它。

## 4. /resource/generate

### 入参

```jsonc
// Java 形态（AiResourceRequest）
{ "mode": "quiz", "chapter": "第3章 集合", "topic": "HashMap",
  "type": "choice", "difficulty": "easy", "count": 5, "extra": { "prompt": "" } }
// 直连形态（不变）
{ "chapter": "第1章", "topic": "变量", "resourceType": "reading", "level": "basic", "prompt": "写拓展阅读" }
```

**prompt 拼装**（这是修掉的关键缺陷：Java 从不发 `prompt`，旧实现把空串发给了模型）：

1. `extra.prompt` 存在 → 直接用它；
2. 否则按 `章节：… / 知识点：… / 题型/形式：… / 难度：… / 数量：… / 补充数据：<extra 的 JSON>` 逐行拼；
3. 末尾追加 `mode` 对应的任务指令（quiz 要求 JSON 数组、evaluation 要求含 analysis 的 JSON 对象）。

**角色（system prompt）选择**：先看 `type`、再看 `mode`，取第一个命中角色表的（`mindmap`/`quiz`/`reading`/
`code`/`review`/`summary`/`evaluation`/`learning_path`/`suggestion`/`explain`/`judge`）；
都没命中则退回 `mode`/`type` 本身，最后是通用兜底。例：`mode=quiz` + `type=choice` → 命中 `quiz`（出题专家）。

### 出参（Java 形态，按 `mode` 分结构）

| `mode` | `data` 结构 | Java 侧读取点 |
|---|---|---|
| `quiz` | `{ "items": [ {…} ], "resourceType": …, "chapter": … }` | `QuestionServiceImpl`：`data.get("items") instanceof List` |
| `evaluation` | `{ "analysis": "评价正文" }` | `AiTutorServiceImpl.explainGrade`：`data.get("analysis")` |
| `resource` / 其它 | `{ "content": "…", "resourceType": …, "chapter": … }` | 同直连形态的键 |

`items[]` 每项的键（与 `QuestionServiceImpl` 的映射一一对应，请求级字段会回填以便兜底）：

| 键 | 来源 |
|---|---|
| `content` | 题干。**Java 读的是 `content`，不是 `question`**，故两个键都给 |
| `question` | 同 `content` |
| `options` | 模型给的数组，非数组会被包成单元素数组；缺省 `[]` |
| `answer` / `explanation` | 模型给的答案/解析（`explanation` 兼容模型的 `analysis`） |
| `type` / `chapter` / `topic` / `difficulty` | 模型给的值优先，缺失则回填请求里的同名字段 |

`analysis`：模型返回 JSON 且含 `analysis` 时取该字段，否则用模型原文。

## 5. 错误约定（很重要）

Python **不抛 500 页面**，而是与 Java 侧 `GlobalExceptionHandler` 同形：
`Result.fail(code, message)` 是 `@ResponseBody` 且不带 `@ResponseStatus`，
所以**业务失败也是 HTTP 200 携带非 0 code**。AI 侧照抄这个语义：

| 场景 | HTTP | body |
|---|---|---|
| 成功 | 200 | `{code: 0, message: "success", data: {…}}` |
| `/chat` `message` 为空白（对应 Java DTO 的 `@NotBlank`） | 200 | `{code: 400, message: "参数错误: message 不能为空", data: null}` |
| LLM 调用失败 | 200 | `{code: 500, message: "LLM 调用失败: …", data: null}` |
| `mode=quiz` 但模型输出不是合法 JSON 数组 | 200 | `{code: 500, message: "AI 返回内容不是合法的题目 JSON 数组", data: null}` |

注意：`llm_client.call_llm` **失败时不抛异常**，而是返回 `"LLM 调用失败: …"` 文本。
适配层靠这个前缀识别失败（`LLM_FAILURE_MARKER`）——否则会把错误文本当成一次「成功的生成」返回。

直连形态仍然把错误文本放在 `content` 里原样返回（历史行为，前端直接展示）。

**为什么 quiz 解析失败要显式报 500 而不是返回空 `items`**：Java 侧 `data == null` 或
`items` 不是 List 时会静默返回 `emptyList()`，出题功能"看起来成功但没有题"，无法定位。
宁可让调用方拿到一个明确的 500。

## 6. 已知缺口（本次未改 Java 一行，留待确认）

1. `explainGrade` 只发 `extra={studentId, assignmentId}`，**没有任何成绩/答题数据**。
   AI 拿不到学生实际作答内容，`analysis` 只能是泛泛评价。要么 Java 补传成绩明细，要么接受现状。
2. `AiResourceRequest.count` 只进了 prompt（要求模型出 N 道），Python 不做截断；
   模型多给几道会原样返回，需要严格数量的话由 Java 侧截断。
3. 旧的 `USE_MOCK_LLM` 环境变量**在 Python 代码里没有对应分支**，本服务无 mock 模式。
4. `/code/analyze` 在 **feat/ai 分支上**不存在（Python 无路由、Java 无调用），
   但在 **feat/code 分支上已完整实现**——详见 §8。
5. `/kb/rebuild`（契约 C6、规格 §1.3.5）**本分支未实现**：`school_agent/kb/` 只有一个空
   `__init__.py`，没有 `retriever` / `chroma_client` / `pipeline`，全服务也没有任何代码读取
   `CHROMA_HOST` / `CHROMA_PORT`。但网关的 ai 路由**按任务表已经放行**该路径
   （`task-schedule` 第 42 条明文要求白名单含 `kb/rebuild`），所以经网关访问是下游 404。
   这是**预期缺口，不是配置错误**：不要用「删网关枚举」的方式消掉它（违反 C6），
   实现归 AI 服务侧（拉 resource 的 `kb/corpus?status=0` → embed → 写 Chroma → 回调 `mark-indexed`）。

## 7. 验证范围

- `tests/test_java_contract.py`：37 条字段级断言，钉住入参解析、信封结构、`items`/`analysis` 键名、
  prompt 拼装、失败码、直连形态不变。改任何一侧的字段名这里会红。
- 端到端（假 Nacos + 假 OpenAI 兼容端点 + 真 uvicorn + 真 `api.py`）：24 项断言全通过，
  含真 graph 跑完一次 `/chat`、quiz/evaluation/失败路径、Nacos 注册与注销。
- **未覆盖**：容器内运行、经网关 `lb://edu-agent-ai` 转发、真实大模型输出质量（Docker 未起）。

## 8. `/code/analyze` 跨分支现状（feat/ai 上看不到，合并时会对撞）

feat/code 分支上这条链路已经完整实现（提交 `a679ef8`；其历史里包含 `7dcfd8d`，也就是 feat/ai 当前 HEAD，
所以这些提交在 feat/ai 上不可见）：

- Python：`school_agent/services/code_review.py`（206 行：附录 A 固定 system prompt、snake→camel 归一、
  三级 JSON 兜底）+ `api.py` 里的 `@app.post("/api/ai/code/analyze")`；单测 `tests/test_code_review.py`（8 个）。
- Java：`edu-agent-code/.../client/{AiServiceClient, CodeAnalyzeRequest, CodeAnalyzeData, AiFeignConfig, AiFeedbackService}.java`，
  由 `service/worker/JudgeWorker.java:114` 调用；AI 不可用时降级为空建议，不阻塞判分（这条容错是对的）。

并入 feat/ai 前必须处理的四件事（否则契约对不上）：

1. **前缀**：feat/code 挂在 `/api/ai/code/analyze`（application 级路由，不在 router 上），feat/ai 的统一前缀是
   `/api/edu-agent-ai`。合并后要挪到 `API_PREFIX` 的 router 下，否则 Feign `path`、网关断言、Nacos 注册名三处都对不上。
2. **服务名与地址**：feat/code 的 Feign 是 `name="ai-service"`、`url="${edu-agent.ai.base-url:http://ai-service:8001}"`，
   而 compose 的服务名是 `ai`、Nacos 注册名是 `edu-agent-ai`，且该 property 全仓无定义 →
   容器里 `ai-service` 解析不到，调用只会命中降级分支。应统一为
   `name="edu-agent-ai", url="${ai.base-url:}", path="/api/edu-agent-ai"`（与 teacher 的 `AiServiceClient` 一致）。
3. **失败语义**：`analyze_code` 在 LLM 故障时返回 `code=0` + 最小 data（空 `suggestions`），而本分支
   `/chat`、`/resource/generate` 故障时返回 `code=500`。analyze 属参考信息，`0` 更合适，但要写明这是有意差异。
4. **依赖与重复代码**：feat/code 的 `requirements.txt` 多了 `json_repair`（三级兜底的第三层），合并时别丢；
   另外 `code_review._extract_json` 用的是贪婪正则 `\{[\s\S]*\}`，与 `api.py` 里已换成
   `json.JSONDecoder().raw_decode` 的版本重复且更弱，建议合并时统一走 api.py 的 helper。

网关侧：feat/code 的 ai 路由用的是 `/api/edu-agent-ai/**` 通配；本分支已改为逐条枚举，
所以 analyze 挪到新前缀后**不会**被放行（过网关仍 404）。若改回通配，这个内网端点就会对任何已登录学生开放。

两条 spec 与实现的落差（spec 文本未同步，非本次改动）：

- §1.3.4 / 附录 A 定义的是 `overall_comment` / `score_hint`（snake_case），实现归一为 camelCase 对外，符合 C4。
- §1.5.2 要求 analyze 复用 `utils/code_fixer.py` 清洗中文关键字，实际 `code_review.py` 没有 import 它，
  `fix_code` 全仓无调用者（仍是死代码）。
