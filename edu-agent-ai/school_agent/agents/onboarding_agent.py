"""引导智能体 — 自然对话式画像采集。

通过自然聊天逐步了解学生，推断六维画像：
  knowledge_mastery / learning_goal_clarity / cognitive_adaptation /
  mistake_avoidance / learning_autonomy / overall_level

不与已有智能体共享逻辑，独立管理自身状态（写入 profile 文件）。
"""
import json
import copy
from school_agent.services.flow_logger import log
from school_agent.services.llm_client import call_llm
from school_agent.services.profile_store import load_student_profile, save_student_profile
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.time_utils import now_iso
from school_agent.agents.path_agent import path_agent
from school_agent.agents.resource_agent import resource_agent
from school_agent.services.resource_store import save_resources

ONBOARDING_SYSTEM = """你是"小航"，一个友好、耐心的学习伙伴。你的任务是通过自然聊天大致了解学生。

## 核心规则
1. **第一句话**: 热情打招呼 + 简单介绍自己能做什么 + 自然地开启话题
2. **自然对话**: 像真人朋友一样聊天，不要用"第一个问题""第二个问题"这种生硬方式
3. **不要追问细节**: 学生回答什么就是什么，不需要为了把某个维度搞清楚而反复追问
4. **不能教学**: 不要讲解知识点、不要出题、不要给学习建议——你只在搜集信息阶段
5. **回复简短**: 每次 1-2 句话，轻松自然
6. **覆盖全面但不要深入**: 聊 2-3 轮把 6 个维度大致覆盖到即可，不求精确

## 六维画像（只需大致分层，不用精确分数）

| 维度 | level_1 | level_2 | level_3 |
|------|---------|---------|---------|
| knowledge_mastery 知识掌握度 | 了解概念 | 熟练应用 | 深入精通 |
| learning_goal_clarity 学习目标清晰度 | 方向模糊 | 目标明确 | 系统规划 |
| cognitive_adaptation 认知风格适配 | 有待观察 | 初显偏好 | 策略自驱 |
| mistake_avoidance 错误规避力 | 易重复错 | 能自查纠 | 主动预防 |
| learning_autonomy 学习自主性 | 被动等待 | 主动提问 | 自主深耕 |
| overall_level 综合能力 | 基础补齐 | 稳步提升 | 拔尖拓展 |

## 输出格式
严格返回 JSON（不要其他文字）：
{
    "reply": "你的回复内容",
    "dimensions": {
        "knowledge_mastery": {"score": 35, "new_level": "level_1"},
        "learning_goal_clarity": {"score": 45, "new_level": "level_2"},
        "cognitive_adaptation": {"score": 30, "new_level": "level_1"},
        "mistake_avoidance": {"score": 30, "new_level": "level_1"},
        "learning_autonomy": {"score": 30, "new_level": "level_1"},
        "overall_level": {"score": 30, "new_level": "level_1"}
    },
    "inferred_profile": {
        "topic": "从对话中推断的学习主题",
        "course": "从对话中推断的课程名",
        "knowledge_base": "从对话中推断的基础水平",
        "pace": "从对话中推断的学习节奏（慢速/中速/快速）"
    },
    "complete": false
}

## 何时标记 complete=true
- 大致聊了 2-3 轮，对 4 个以上维度有了基本判断
- 不需要每个维度都很精确，不需要 evidence
- 不需要把所有维度都问到 level_3，level_1 也是有效判断
"""


# 首次对话的默认回复（不走LLM，直接返回）
WELCOME_MESSAGE = """👋 你好呀！我是 **小航**，你的专属学习伙伴～

我可以帮你分析学习情况、定制学习计划、生成学习资料。

先跟我说说，你最近在学什么吧？😊"""

DIMENSION_KEYS = [
    "knowledge_mastery",
    "learning_goal_clarity",
    "cognitive_adaptation",
    "mistake_avoidance",
    "learning_autonomy",
    "overall_level",
]


def _build_dimension_context(profile: dict) -> str:
    """构建当前画像摘要，供LLM参考"""
    parts = []
    for dim_key in DIMENSION_KEYS:
        dim = profile.get(dim_key, {})
        if isinstance(dim, dict):
            level = dim.get("level", "level_1")
            score = dim.get("score", 30)
            evidence = dim.get("evidence", [])
            if evidence:
                ev = "; ".join(evidence[-2:])
                parts.append(f"- {dim_key}: {level} ({score}分), 证据: {ev}")
            else:
                parts.append(f"- {dim_key}: {level} ({score}分), 尚无证据")
    return "\n".join(parts) if parts else "尚无画像数据"


def _merge_dimensions(profile: dict, dimension_changes: dict) -> dict:
    """将LLM返回的维度变化合并到画像中"""
    profile = copy.deepcopy(profile)
    for dim_key in DIMENSION_KEYS:
        change = dimension_changes.get(dim_key)
        if not change:
            continue
        if dim_key not in profile or not isinstance(profile.get(dim_key), dict):
            profile[dim_key] = {"level": "level_1", "score": 30, "evidence": [], "last_updated": None}

        new_score = change.get("score", profile[dim_key].get("score", 30))
        new_level = change.get("new_level", profile[dim_key].get("level", "level_1"))
        profile[dim_key]["score"] = max(0, min(100, new_score))
        profile[dim_key]["level"] = new_level

        # evidence 为可选（新 prompt 不要求输出），有则累加
        new_evidence = change.get("evidence", [])
        if new_evidence:
            existing_ev = profile[dim_key].get("evidence", [])
            profile[dim_key]["evidence"] = (existing_ev + new_evidence)[-10:]

        profile[dim_key]["last_updated"] = now_iso()
    return profile


def _check_complete(dimension_changes: dict, profile: dict) -> bool:
    """判断是否有足够维度有了大致判断（不需全部精确）"""
    filled = 0
    for dim_key in DIMENSION_KEYS:
        dim = profile.get(dim_key, {}) if isinstance(profile.get(dim_key), dict) else {}
        score = dim.get("score", 30)
        if score >= 35:
            filled += 1
    # 6 个维度中 4 个以上有大致的分数即可
    return filled >= 4


def onboarding_agent(state: dict) -> dict:
    user_input = state.get("user_input", "")
    student_id = state.get("student_id", "student_001")

    # 从文件读取自身状态（覆盖 init_profile 加载的版本）
    profile = load_student_profile(student_id)
    if not profile:
        profile = state.get("profile", {})

    onboarding_phase = profile.get("_onboarding_phase", "")

    # Phase 1: 首次对话 → 欢迎语
    if not onboarding_phase:
        log("ONBOARD", "Phase 1: welcome")
        profile["_onboarding_phase"] = "started"
        profile["_onboarding_complete"] = False
        profile["initialized"] = True
        save_student_profile(student_id, profile)

        return {
            "final_answer": WELCOME_MESSAGE,
            "profile": profile,
            "is_onboarding": True,
            "agent_outputs": merge_agent_output(state, "onboarding_agent", {"phase": "welcome"}),
        }

    # Phase 2+ : 自然对话 + 画像推断
    # 构建对话历史上下文（从 profile 中读取）
    conversation_log = profile.get("_onboarding_log", [])
    if not isinstance(conversation_log, list):
        conversation_log = []

    dim_context = _build_dimension_context(profile)

    # 构造 LLM prompt
    prompt = f"""{dim_context}

# 对话历史（最近5轮）
"""
    recent = conversation_log[-5:] if len(conversation_log) > 5 else conversation_log
    for i, turn in enumerate(recent, 1):
        prompt += f"用户: {turn['user']}\n小航: {turn['ai']}\n\n"

    prompt += f"学生最新说：{user_input}\n\n请分析并返回JSON。"

    resp = call_llm(prompt, system=ONBOARDING_SYSTEM)

    # 解析 LLM 输出
    try:
        raw = resp.strip()
        if raw.startswith("```"):
            raw = raw.split("\n", 1)[-1].rsplit("```", 1)[0]
        result = json.loads(raw)
    except json.JSONDecodeError:
        log("ONBOARD", "LLM output parse failed, using fallback")
        result = {
            "reply": "嗯，继续说，我在听～",
            "dimensions": {},
            "complete": False,
        }

    reply = result.get("reply", "嗯，继续说，我在听～")
    dimension_changes = result.get("dimensions", {})

    # 合并维度更新到画像
    if dimension_changes:
        profile = _merge_dimensions(profile, dimension_changes)

    # 合并 flat 字段（供 Java MySQL 同步使用）
    inferred = result.get("inferred_profile", {})
    if inferred:
        for key in ["topic", "course", "knowledge_base", "pace"]:
            if inferred.get(key):
                profile[key] = inferred[key]

    # 记录对话历史
    conversation_log.append({"user": user_input[:300], "ai": reply[:300]})
    if len(conversation_log) > 20:
        conversation_log = conversation_log[-20:]
    profile["_onboarding_log"] = conversation_log
    profile["_onboarding_phase"] = "collecting"

    # 判断是否完成
    is_complete = result.get("complete", False) or _check_complete(dimension_changes, profile)

    if is_complete:
        log("ONBOARD", "Phase 3: complete")
        profile["_onboarding_phase"] = "complete"
        profile["_onboarding_complete"] = True
        # 清理临时字段
        profile.pop("_onboarding_log", None)
        save_student_profile(student_id, profile)

        # 自动生成学习路径（含应用建议）
        path_result = path_agent({"profile": profile})
        learning_path = path_result.get("learning_path", [])
        suggestions = path_result.get("suggestions", [])

        # 确保 topic 有值，否则从对话历史推断
        if not profile.get("topic"):
            history = profile.get("_onboarding_log", [])
            for turn in (history or []):
                if turn.get("user"):
                    profile["topic"] = turn["user"][:50]
                    break
            if not profile.get("topic"):
                profile["topic"] = "编程学习"

        # 自动生成资源
        res_result = resource_agent({"profile": profile, "retrieved_context": ""})
        resources = res_result.get("resources", {})
        resource_dir = save_resources(
            student_id=student_id,
            session_id=state.get("session_id", "default"),
            profile=profile,
            resources=resources,
            learning_path=learning_path,
        )

        # 写入 Java 数据库（用内置 urllib，避免依赖外部 requests 库）
        import json as _json, urllib.request as _urllib
        db_type_map = {
            "course_doc": "文档", "mindmap": "思维导图",
            "quiz": "题库", "extended_reading": "拓展阅读",
            "code_practice": "代码案例",
        }
        for res_key, db_type in db_type_map.items():
            content = resources.get(res_key) or res_result.get(res_key)
            if content:
                payload = {
                    "title": f"{db_type} - {profile.get('topic', '') or '通用'}",
                    "type": db_type,
                    "description": db_type,
                    "content": str(content)[:5000],
                    "studentId": int(student_id) if student_id.isdigit() else None,
                    "fileUrl": resource_dir or "",
                    "courseName": profile.get("course", ""),
                }
                try:
                    data = _json.dumps(payload).encode("utf-8")
                    req = _urllib.Request(
                        "http://localhost:8080/resources",
                        data=data,
                        headers={"Content-Type": "application/json"},
                    )
                    _urllib.urlopen(req, timeout=5)
                    log("ONBOARD", f"DB save success: {res_key}")
                except Exception as e:
                    log("ONBOARD", f"DB save failed: {res_key} - {e}")

        return {
            "final_answer": reply + "\n\n画像采集完成，正在为你生成学习路径和学习资源...",
            "profile": profile,
            "learning_path": learning_path,
            "suggestions": suggestions,
            "resources": resources,
            "resource_dir": resource_dir,
            "is_onboarding": False,
            "profile_complete": True,
            "silent_tasks": ["update_profile", "init_path"],
            "agent_outputs": merge_agent_output(state, "onboarding_agent", {"phase": "complete"}),
        }

    # 持久化
    save_student_profile(student_id, profile)

    return {
        "final_answer": reply,
        "profile": profile,
        "is_onboarding": True,
        "agent_outputs": merge_agent_output(state, "onboarding_agent", {"phase": "collecting"}),
    }
