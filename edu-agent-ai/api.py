import json
import os
import re
from dataclasses import dataclass
from typing import Optional

# 加载 .env 文件
try:
    from dotenv import load_dotenv
    env_path = os.path.join(os.path.dirname(__file__), '.env')
    if os.path.exists(env_path):
        load_dotenv(env_path)
except Exception:
    pass

try:
    from contextlib import asynccontextmanager

    from fastapi import APIRouter, Body, FastAPI
    from pydantic import BaseModel
except Exception:
    FastAPI = None
    Body = None
    BaseModel = object

# 服务发现 / 配置中心接入（详见 nacos_registry.py）。
# 必须先拉配置、再导入 school_agent：config.py 是导入期读环境变量的，顺序颠倒则下发值不生效。
import nacos_registry

nacos_registry.apply_config_to_env()

from school_agent.graph import graph

# 对外路径前缀：网关路由（/api/edu-agent-ai/**，无 StripPrefix）、Java 侧
# @FeignClient(name = "edu-agent-ai", path = "/api/edu-agent-ai") 三处必须一致。
API_PREFIX = "/api/edu-agent-ai"

# ══════════════════════════════════════════════════════════════════════
# Java ↔ Python 契约适配层
# ----------------------------------------------------------------------
# 本服务有两个消费方，入参形态不同，但业务逻辑只写一遍：
#   1) Java 侧 edu-agent-teacher 的 AiServiceClient（Feign）：
#      /chat              {message, context}
#      /resource/generate {mode, chapter, topic, type, difficulty, count, extra}
#   2) 前端/脚本直连（历史形态，保持行为不变）：
#      /chat              {user_input, student_id, session_id, profile}
#      /resource/generate {chapter, topic, resourceType, level, prompt}
# 分发方式：看原始 body 里出现哪个标志字段（message / mode），不做 header 协商、不引第二套协议。
# 响应：Java 形态一律包 Result 信封 {code, message, data}；直连形态维持原有裸 JSON。
# 两个端点都保持同步 def（FastAPI 会放进线程池执行）：graph.invoke / call_llm 是阻塞调用，
# 写成 async def 会占住事件循环，连 /health 一起卡死。
# ══════════════════════════════════════════════════════════════════════

FEIGN_CHAT_MARKER = "message"      # Java AiChatRequest 的字段，用它区分两种 chat 入参
FEIGN_RESOURCE_MARKER = "mode"     # Java AiResourceRequest 的字段，用它区分两种 resource 入参
LLM_FAILURE_MARKER = "LLM 调用失败:"  # llm_client.call_llm 失败时的返回文本前缀（它不抛异常，只回带错误文本）

# 各资源类型的角色 system prompt（按 resourceType / mode / type 取用）
ROLE_PROMPTS = {
    "mindmap": "你是一位思维导图设计大师。你的任务是将知识点转化为 JSON 树形结构数据。每个节点包含 id（唯一字符串）、topic（显示文本），children（子节点数组，可选）。只输出纯 JSON，不要 Markdown 标记、代码块或任何额外文字。\n【范围限定】仅限 JavaSE 基础内容（语法、面向对象、集合、IO、多线程、反射等），禁止涉及 JavaEE、Spring Boot、Spring Cloud、MyBatis 等企业级框架。",
    "quiz": "你是一位资深出题专家。你的任务是根据知识点设计高质量的练习题。必须返回 JSON 数组，每个元素包含 question（题目）、options（选项数组，选择题需要）、answer（正确答案）、explanation（解析）。\n【范围限定】仅限 JavaSE 基础内容，不得出现 JavaEE/Spring 等高级框架相关题目。",
    "reading": "你是一位教育内容创作专家。你的任务是为知识点撰写深入浅出的拓展阅读材料。使用 Markdown 格式，包含概念解释、应用场景和延伸阅读方向。\n【范围限定】仅限 JavaSE 范围，禁止涉及 JavaEE/Spring/MyBatis 等企业级框架内容。",
    "code": "你是一位高级Java编程导师。你的任务是为知识点编写教学级代码案例。代码必须完整、可运行、包含详细中文注释。使用 Markdown 代码块标注。\n【范围限定】仅限 JavaSE 知识点，代码案例中禁止出现 Spring Boot/Spring/JavaEE 相关注解或框架API。",
    "review": "你是一位学习回顾分析师。根据学生的学习数据（画像、学习时长、完成任务数、资源使用情况），生成结构化的学习回顾报告。包含进度总结、薄弱点分析、改进建议。返回 JSON 格式。",
    "summary": "你是一位学习总结专家。根据学生的学习画像、路径进度、学习时长和任务完成情况，生成全面的学习总结报告。包含综合评分、优点分析、不足分析、重点方向和学习建议。返回 JSON 格式。",
    "evaluation": "你是一位学习评估专家。根据学生的学习数据生成客观的学习评价，包含各维度的知识掌握度评分和综合评估。返回 JSON 格式。",
    "learning_path": "你是一位学习路径规划专家。根据学生画像生成个性化的学习路径规划。必须返回严格的 JSON 对象，包含 goal（学习目标）、stages（阶段数组），每个 stage 包含 name（阶段名）和 tasks（任务数组），每个 task 包含 title、duration（分钟）、status=0、progress=0。直接返回纯 JSON，不要 Markdown 标记或额外说明。",
    "suggestion": "你是一位学习建议导师。根据学生画像和薄弱点，生成具体可执行的个性化学习建议。返回 JSON 数组格式。",
    "explain": "你是一位个性化讲解导师。根据学生的做题情况（题目、用户答案、正确答案、对错）和学生画像（薄弱点、基础），生成针对性的纠错或拓展讲解。回答要结构清晰、有针对性。",
    "judge": "你是一位答题评判专家。根据题目、标准答案和用户答案，判断用户答案是否正确，并给出评分和简短的评判意见。返回 JSON 格式，包含 score（0或1）、correct（boolean）、comment（评判意见）。",
}
ROLE_PROMPTS_FALLBACK = "你是一位教育内容生成专家。根据要求生成高质量的学习内容。"

# mode（Java 语义：quiz / evaluation / resource）对应的任务指令，拼进 user prompt
MODE_INSTRUCTIONS = {
    "quiz": "请围绕上述知识点出题。只输出 JSON 数组，不要 Markdown 标记或额外说明，"
            "每个元素包含 question（题干）、options（选项数组，选择题必需）、answer（正确答案）、explanation（解析）。",
    "evaluation": "请基于上述数据给出学习评价。只输出 JSON 对象，包含 analysis（评价正文）。",
    "resource": "请生成对应的学习资源，内容直接可用。",
}


@dataclass
class ChatInput:
    """chat 的内部统一入参：两种外部形态都归一到这里。"""

    user_input: str = ""
    student_id: str = "student_001"
    session_id: str = "api_session"
    profile: Optional[dict] = None
    envelope: bool = False  # True：按 Java Result 信封返回


@dataclass
class ResourceInput:
    """resource/generate 的内部统一入参。"""

    resource_type: str = "mindmap"   # system prompt 角色键（ROLE_PROMPTS 的 key）
    prompt: str = ""                 # 真正发给 LLM 的 user prompt（extra.prompt 优先）
    chapter: str = ""
    topic: str = ""
    type: str = ""                   # Java 的题型（choice / judge / …）
    difficulty: str = ""
    count: Optional[int] = None
    level: str = "basic"
    mode: str = ""                   # Java 语义：quiz / evaluation / resource，决定响应结构
    extra: Optional[dict] = None
    envelope: bool = False


def _result_ok(data) -> dict:
    """等价于 Java 侧 Result.success(data)（edu-agent-common：code=0，message="success"）。"""
    return {"code": 0, "message": "success", "data": data}


def _result_fail(code: int, message: str) -> dict:
    """等价于 Java 侧 Result.fail(code, message)。

    Java 的 GlobalExceptionHandler 是 @ResponseBody 且不带 @ResponseStatus，
    即业务失败同样是 HTTP 200 携带非 0 code；这里保持一致，Feign 侧才能正常反序列化出 code。
    """
    return {"code": int(code), "message": message, "data": None}


def _is_llm_failure(text) -> bool:
    """call_llm 出错时不抛异常，而是返回 "LLM 调用失败: ..." 文本，这里识别它。"""
    return isinstance(text, str) and text.startswith(LLM_FAILURE_MARKER)


def _extract_json(text):
    """从 LLM 输出里尽力取出 JSON，返回 dict / list，取不到返回 None。

    依次尝试：整体解析 → ```json 代码块 → 从最早出现的 { 或 [ 起用 raw_decode 解析。
    最后一步用 raw_decode（而非正则贪匹配定界符）是必须的：散文里出现 [{...}] 这种
    对象数组时，{...} 正则会先咬住内层对象，把数组降级成单个元素。
    """
    if not text:
        return None
    try:
        return json.loads(text.strip())
    except (json.JSONDecodeError, TypeError):
        pass

    fenced = re.search(r"```(?:json)?\s*([\s\S]*?)```", text)
    if fenced:
        try:
            return json.loads(fenced.group(1).strip())
        except json.JSONDecodeError:
            pass

    decoder = json.JSONDecoder()
    for match in re.finditer(r"[\[{]", text):
        try:
            return decoder.raw_decode(text, match.start())[0]
        except json.JSONDecodeError:
            continue
    return None


def _chat_answer_and_extras(result: dict):
    """拆出 (answer 正文, 附带的其它字段)。

    部分意图的 final_answer 是 JSON 串（如 explain 返回 {"type","content","weaknesses_focus","suggestion"}），
    原样交给 Java 会被前端当正文渲染成一坨 JSON；这里取其中的 content 作为正文，
    其余字段交给 references，不丢信息。
    """
    raw = result.get("final_answer") or ""
    if not isinstance(raw, str) or not raw.lstrip().startswith("{"):
        return str(raw), {}
    parsed = _extract_json(raw)
    if not isinstance(parsed, dict) or not parsed.get("content"):
        return raw, {}
    extras = {k: v for k, v in parsed.items() if k != "content" and v}
    return str(parsed["content"]), extras


def _chat_references(result: dict, extras: Optional[dict] = None):
    """chat 响应的 references 字段（Java AiChatResult.references）。

    graph 目前没有 references 字段，先把它实际产出的辅助结果整体透传，
    避免 Java 侧拿到的永远是 null；将来 graph 直接给出 references 则优先使用。
    """
    explicit = result.get("references")
    if explicit:
        return explicit
    passthrough = {}
    for key in ("resources", "learning_path", "safety_report", "evaluation_report", "resource_dir"):
        value = result.get(key)
        if value:
            passthrough[key] = value
    if extras:
        passthrough.update(extras)
    return passthrough or None


def _build_resource_prompt(inp: ResourceInput) -> str:
    """把 Java 侧的结构化字段拼成真正可用的 user prompt。

    历史实现只把 req.prompt 喂给 LLM，而 Java 从不发 prompt，
    结果是空串发给模型；这里改由 chapter/topic/type/难度/数量/extra 拼装。
    """
    if inp.prompt:
        return inp.prompt
    lines = []
    if inp.chapter:
        lines.append(f"章节：{inp.chapter}")
    if inp.topic:
        lines.append(f"知识点：{inp.topic}")
    if inp.type:
        lines.append(f"题型/形式：{inp.type}")
    if inp.level and inp.level != "basic":
        lines.append(f"难度层级：{inp.level}")
    if inp.difficulty:
        lines.append(f"难度：{inp.difficulty}")
    if inp.count:
        lines.append(f"数量：{inp.count}")
    if inp.extra:
        lines.append("补充数据：" + json.dumps(inp.extra, ensure_ascii=False))
    lines.append(MODE_INSTRUCTIONS.get(inp.mode, MODE_INSTRUCTIONS["resource"]))
    return "\n".join(lines)


def _normalize_quiz_items(items, inp: ResourceInput) -> list:
    """把 LLM 出的题规范化成 Java QuestionServiceImpl 期望的字段。

    Java 逐个读取 type/chapter/topic/content/options/answer/explanation/difficulty（读的是 content）；
    Python 侧习惯叫 question，两个键都写，避免以后再漂。
    """
    normalized = []
    for raw in items:
        if not isinstance(raw, dict):
            continue
        question = (raw.get("question") or raw.get("content") or raw.get("stem")
                    or raw.get("title") or "")
        options = raw.get("options") or raw.get("choices") or []
        if not isinstance(options, list):
            options = [options]
        normalized.append({
            "type": str(raw.get("type") or inp.type or ""),
            "chapter": str(raw.get("chapter") or inp.chapter or ""),
            "topic": str(raw.get("topic") or inp.topic or ""),
            "question": str(question),
            "content": str(question),
            "options": [str(o) for o in options],
            "answer": str(raw.get("answer") or ""),
            "explanation": str(raw.get("explanation") or raw.get("analysis") or ""),
            "difficulty": str(raw.get("difficulty") or inp.difficulty or ""),
        })
    return normalized


def _log(msg: str):
    """打印日志，兼容 Windows GBK 终端"""
    try:
        print(msg.encode('gbk', errors='replace').decode('gbk'))
    except Exception:
        print(msg.encode('ascii', errors='replace').decode('ascii'))


if FastAPI is None:
    app = None
else:
    @asynccontextmanager
    async def lifespan(_app):
        # 启动即注册进 Nacos（服务发现），退出时注销，避免留下被网关 lb 转发的死节点。
        registrar = nacos_registry.start()
        try:
            yield
        finally:
            registrar.stop()

    app = FastAPI(title="Edu Agent AI", version="0.1.0", lifespan=lifespan)
    api = APIRouter(prefix=API_PREFIX)

    class ChatRequest(BaseModel):
        """直连形态入参（历史形态，字段名不变）。"""

        user_input: str = ""
        student_id: str = "student_001"
        session_id: Optional[str] = "api_session"
        profile: Optional[dict] = None

        def to_input(self) -> ChatInput:
            return ChatInput(
                user_input=self.user_input,
                student_id=self.student_id or "student_001",
                session_id=self.session_id or "api_session",
                profile=self.profile,
                envelope=False,
            )

    class ChatRequestFeign(BaseModel):
        """Java 侧 AiChatRequest 的镜像：{message, context}（Jackson 序列化出的 camelCase）。"""

        message: str = ""
        context: Optional[dict] = None

        def to_input(self) -> ChatInput:
            ctx = dict(self.context or {})
            student_id = ctx.pop("studentId", None) or ctx.pop("student_id", None)
            session_id = ctx.pop("sessionId", None) or ctx.pop("session_id", None)
            profile = ctx.pop("profile", None)
            if not isinstance(profile, dict):
                profile = ctx or None
            elif ctx:
                profile = {**profile, **ctx}  # context 里的其余键（班级、课程…）并进 profile
            return ChatInput(
                user_input=self.message,
                student_id=str(student_id) if student_id else "student_001",
                session_id=str(session_id) if session_id else "api_session",
                profile=profile,
                envelope=True,
            )

    @api.get("/health")
    def health():
        return {"status": "ok"}

    @api.post("/chat")
    def chat(payload: dict = Body(...)):
        if FEIGN_CHAT_MARKER in payload:  # Java Feign 形态：{message, context}
            feign = ChatRequestFeign(**payload)
            if not feign.message.strip():
                return _result_fail(400, "参数错误: message 不能为空")
            req = feign.to_input()
        else:  # 直连形态：{user_input, student_id, session_id, profile}
            req = ChatRequest(**payload).to_input()
        _log(f"\n{'='*60}")
        _log(f"[API] 收到请求")
        _log(f"[API] user_input: {req.user_input[:100]}")
        _log(f"[API] student_id: {req.student_id}")
        _log(f"[API] session_id: {req.session_id}")
        _log(f"[API] profile keys: {list(req.profile.keys()) if req.profile else 'none'}")
        if req.profile:
            _log(f"[API] profile._onboarding_phase: {req.profile.get('_onboarding_phase', 'not set')}")
            _log(f"[API] profile raw(前500字): {str(req.profile)[:500]}")

        initial_state = {
            "student_id": req.student_id,
            "session_id": req.session_id,
            "user_input": req.user_input,
        }
        if req.profile:
            initial_state["profile"] = req.profile

        result = graph.invoke(initial_state,
            config={"configurable": {"thread_id": f"{req.student_id}_{req.session_id}"}},
        )

        _log(f"[API] 返回结果")
        _log(f"[API] intent: {result.get('intent')}")
        _log(f"[API] final_answer: {str(result.get('final_answer', ''))[:150]}")
        _log(f"[API] profile._onboarding_phase: {result.get('profile', {}).get('_onboarding_phase', 'not set')}")
        ret_profile = result.get('profile', {})
        _log(f"[API] profile keys: {list(ret_profile.keys())[:15]}")
        if isinstance(ret_profile, dict) and 'profile' in ret_profile:
            inner = ret_profile['profile']
            if isinstance(inner, dict):
                _log(f"[API] ⚠️ profile 被嵌套! 内层 profile keys: {list(inner.keys())[:10]}")
                _log(f"[API] ⚠️ 内层 _onboarding_phase: {inner.get('_onboarding_phase', 'not set')}")
        _log(f"{'='*60}\n")

        if req.envelope:  # Java 形态：AiChatResult(answer, intent, references)
            answer, extras = _chat_answer_and_extras(result)
            return _result_ok({
                "answer": answer,
                "intent": result.get("intent"),
                "references": _chat_references(result, extras),
            })

        return {
            "intent": result.get("intent"),
            "final_answer": result.get("final_answer"),
            "profile": result.get("profile"),
            "resources": result.get("resources"),
            "learning_path": result.get("learning_path"),
            "safety_report": result.get("safety_report"),
            "evaluation_report": result.get("evaluation_report"),
            "resource_dir": result.get("resource_dir"),
            "profile_complete": result.get("profile_complete"),
        }

    class ResourceGenRequest(BaseModel):
        """直连形态入参（历史形态，字段名不变）。"""

        chapter: str = ""
        topic: str = ""
        resourceType: str = "mindmap"
        level: str = "basic"
        prompt: str = ""

        def to_input(self) -> ResourceInput:
            return ResourceInput(
                resource_type=self.resourceType or "mindmap",
                prompt=self.prompt,
                chapter=self.chapter,
                topic=self.topic,
                level=self.level,
                envelope=False,
            )

    class ResourceGenRequestFeign(BaseModel):
        """Java 侧 AiResourceRequest 的镜像：{mode, chapter, topic, type, difficulty, count, extra}。"""

        mode: str = ""
        chapter: str = ""
        topic: str = ""
        type: str = ""
        difficulty: str = ""
        count: Optional[int] = None
        extra: Optional[dict] = None

        def to_input(self) -> ResourceInput:
            extra = dict(self.extra or {})
            prompt = str(extra.pop("prompt", "") or "")
            mode = (self.mode or "").strip().lower()
            type_ = (self.type or "").strip().lower()
            # 角色选择：type 更贴近具体形式（mindmap/reading…），mode 是动作语义（quiz/evaluation）；
            # 优先取命中 ROLE_PROMPTS 的那个，都没有则退回 mode/type 本身。
            candidates = [c for c in (type_, mode) if c]
            role = next((c for c in candidates if c in ROLE_PROMPTS),
                        candidates[0] if candidates else "resource")
            return ResourceInput(
                resource_type=role,
                prompt=prompt,
                chapter=self.chapter,
                topic=self.topic,
                type=self.type or "",
                difficulty=self.difficulty or "",
                count=self.count,
                mode=mode,
                extra=extra or None,
                envelope=True,
            )

    class PathGenRequest(BaseModel):
        student_id: str = ""
        prompt: str = ""
        profile: Optional[dict] = None

    @api.post("/path/generate")
    def path_generate(req: PathGenRequest):
        _log(f"\n{'='*60}")
        _log(f"[路径规划AI] ===== 收到请求 =====")
        _log(f"[路径规划AI] student_id={req.student_id}")
        _log(f"[路径规划AI] profile keys: {list(req.profile.keys()) if req.profile else 'none'}")

        from school_agent.services.llm_client import call_llm

        system_prompt = """你是一个AI学习路径规划专家。根据学生画像生成个性化的学习路径规划。
你必须只返回一个严格的JSON对象，不要包含任何markdown标记、代码块或额外说明。
直接以{开头，以}结尾。"""

        full_prompt = system_prompt + "\n\n" + req.prompt

        _log(f"[路径规划AI] 🚀 正在调用 LLM...")
        text = call_llm(full_prompt)
        _log(f"[路径规划AI] 📥 LLM 原始返回(前300字): {text[:300] if text else '空'}...")

        # 尝试从返回中提取 JSON（对象/数组提取逻辑统一在 _extract_json）
        result = _extract_json(text)

        _log(f"[路径规划AI] 📥 最终返回 keys: {list(result.keys()) if isinstance(result, dict) else '空'}")
        if isinstance(result, dict) and result:
            _log(f"[路径规划AI] goal={result.get('goal')}, stages数={len(result.get('stages', []))}")
        else:
            _log(f"[路径规划AI] ⚠️ 返回空结果，使用默认数据")
            result = {
                "goal": "掌握课程核心知识并完成实践项目",
                "targetMastery": "≥85%",
                "estimatedCompletion": "2026-07-06",
                "totalHours": 24,
                "masteryRate": 50,
                "learningRate": 30,
                "unmasteredRate": 20,
                "suggestions": "建议从基础概念开始，逐步深入到实践项目",
                "applicationAdvice": "完成每章课后练习，并尝试独立完成小项目",
                "examAdvice": "每章结束后进行自测，重点复习错题",
                "recommendTime": "每天 19:00-21:00",
                "stages": [
                    {"name": "今日计划", "tasks": [{"title": "课程基础概念学习", "duration": 45, "status": 0, "progress": 0}, {"title": "核心知识点梳理", "duration": 30, "status": 0, "progress": 0}]},
                    {"name": "本周路径", "tasks": [{"title": "语法与基础练习", "duration": 60, "status": 0, "progress": 0}, {"title": "面向对象编程实践", "duration": 45, "status": 0, "progress": 0}]},
                    {"name": "考试冲刺", "tasks": [{"title": "模拟测试一", "duration": 60, "status": 0, "progress": 0}, {"title": "错题分析与针对性复习", "duration": 45, "status": 0, "progress": 0}]},
                    {"name": "实践提升", "tasks": [{"title": "综合项目实战", "duration": 90, "status": 0, "progress": 0}, {"title": "代码审查与优化", "duration": 60, "status": 0, "progress": 0}]}
                ]
            }
        _log(f"[路径规划AI] ===== 请求结束 =====\n{'='*60}")
        return result

    @api.post("/resource/generate")
    def resource_generate(payload: dict = Body(...)):
        import time as _time
        t0 = _time.time()
        if FEIGN_RESOURCE_MARKER in payload:  # Java Feign 形态：{mode, chapter, topic, type, difficulty, count, extra}
            req = ResourceGenRequestFeign(**payload).to_input()
        else:  # 直连形态：{chapter, topic, resourceType, level, prompt}
            req = ResourceGenRequest(**payload).to_input()
        _log(f"\n{'='*60}")
        _log(f"[资源生成AI] ===== 收到请求 =====")
        _log(f"[资源生成AI] chapter={req.chapter}, topic={req.topic}, "
             f"resourceType={req.resource_type}, mode={req.mode or '-'}, level={req.level}")

        from school_agent.services.llm_client import call_llm

        # 根据资源类型设置角色 system prompt
        system_prompt = ROLE_PROMPTS.get(req.resource_type, ROLE_PROMPTS_FALLBACK)
        # 真正发给 LLM 的 user prompt：Java 形态由结构化字段拼装，直连形态沿用其 prompt
        prompt = _build_resource_prompt(req)
        _log(f"[资源生成AI] prompt(前200字): {prompt[:200] if prompt else '空'}...")

        _log(f"[资源生成AI] 角色={req.resource_type}, 耗时={_time.time()-t0:.2f}s")

        # 传入 system_prompt + 用户 prompt
        _log(f"[资源生成AI] 🚀 正在调用 LLM...")
        t1 = _time.time()
        result = call_llm(prompt, system_prompt=system_prompt)
        t2 = _time.time()
        _log(f"[资源生成AI] 📥 LLM 返回, 耗时={t2-t1:.2f}s, 长度={len(result) if result else 0} 字")
        _log(f"[资源生成AI] 📥 LLM 返回(前200字): {result[:200] if result else '空'}...")
        _log(f"[资源生成AI] ===== 请求结束, 总耗时={t2-t0:.2f}s =====\n{'='*60}")

        # 直连形态：保持原有裸 JSON 响应（含 LLM 失败时的错误文本），行为不变
        if not req.envelope:
            return {
                "content": result,
                "resourceType": req.resource_type,
                "chapter": req.chapter,
            }

        # Java 形态：包 Result 信封，并按 mode 给出对方要读的结构
        if _is_llm_failure(result):
            _log("[资源生成AI] ⚠️ LLM 调用失败，返回失败信封")
            return _result_fail(500, result)

        if req.mode == "quiz":
            parsed = _extract_json(result)
            if isinstance(parsed, dict) and isinstance(parsed.get("items"), list):
                parsed = parsed["items"]
            if not isinstance(parsed, list):
                _log("[资源生成AI] ⚠️ LLM 返回不是合法 JSON 数组，无法产出 items")
                return _result_fail(500, "AI 返回内容不是合法的题目 JSON 数组")
            items = _normalize_quiz_items(parsed, req)
            _log(f"[资源生成AI] ✅ 解析出 {len(items)} 道题")
            return _result_ok({
                "items": items,
                "resourceType": req.resource_type,
                "chapter": req.chapter,
            })

        if req.mode == "evaluation":
            parsed = _extract_json(result)
            analysis = (str(parsed["analysis"]) if isinstance(parsed, dict) and parsed.get("analysis")
                        else result)
            return _result_ok({"analysis": analysis})

        return _result_ok({
            "content": result,
            "resourceType": req.resource_type,
            "chapter": req.chapter,
        })

    app.include_router(api)

    @app.get("/health", include_in_schema=False)
    def health_root():
        """根路径健康检查：供容器 healthcheck / 运维探针直连 :8001 使用，
        不属于对外 API 契约（对外统一在 API_PREFIX 下）。"""
        return {"status": "ok"}
