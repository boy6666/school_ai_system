import sys
try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

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
    """判断画像是否完整。只有 LLM 明确标记 complete 时才通过，
    此函数作为兜底——只有当大部分维度达到中高置信度时才返回 True。
    """
    filled = 0
    for dim_key in DIMENSION_KEYS:
        dim = profile.get(dim_key, {}) if isinstance(profile.get(dim_key), dict) else {}
        score = dim.get("score", 30)
        # 需要 ≥55 分才算有效判断（避免初始 35 分就提前结束）
        if score >= 55:
            filled += 1
    # 6 个维度中至少 5 个达到中高置信度
    return filled >= 5


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
        print(f"\n  [ONBOARD] === Phase 1: 欢迎语 ===")
        print(f"  [ONBOARD] 用户首次进入引导，返回预设欢迎语（不走 LLM）")
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

    print(f"\n  [ONBOARD] === Phase 2: 调用 LLM ===")
    print(f"  [ONBOARD] 当前画像摘要:")
    print(f"  [ONBOARD] {dim_context[:200]}")
    print(f"  [ONBOARD] 发送给 LLM 的 prompt (前300字):")
    print(f"  [ONBOARD] {prompt[:300]}")
    print(f"  [ONBOARD] 完整 prompt 长度: {len(prompt)} 字")

    resp = call_llm(prompt, system_prompt=ONBOARDING_SYSTEM)

    print(f"  [ONBOARD] LLM 原始返回:")
    print(f"  [ONBOARD] {resp[:500]}")

    # 解析 LLM 输出
    try:
        raw = resp.strip()
        if raw.startswith("```"):
            raw = raw.split("\n", 1)[-1].rsplit("```", 1)[0]
        result = json.loads(raw)
        print(f"  [ONBOARD] JSON 解析成功")
        print(f"  [ONBOARD] reply: {result.get('reply', '')[:100]}")
        print(f"  [ONBOARD] complete: {result.get('complete')}")
        dims = result.get('dimensions', {})
        if dims:
            print(f"  [ONBOARD] 更新维度: { {k: v.get('score') for k, v in dims.items()} }")
    except json.JSONDecodeError:
        print(f"  [ONBOARD] ❌ JSON 解析失败, 使用 fallback")
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
        print(f"\n  [ONBOARD] === Phase 3: 画像采集完成 ===")
        print(f"  [ONBOARD] 最终维度:")
        for dim_key in DIMENSION_KEYS:
            d = profile.get(dim_key, {})
            if isinstance(d, dict):
                print(f"  [ONBOARD]   {dim_key}: level={d.get('level')}, score={d.get('score')}")
        print(f"  [ONBOARD] 画像已保存，生成逻辑交给前端逐步调 API")
        log("ONBOARD", "Phase 3: profile complete")
        profile["_onboarding_phase"] = "complete"
        profile["_onboarding_complete"] = True
        # 清理临时字段
        profile.pop("_onboarding_log", None)
        save_student_profile(student_id, profile)

        # 确保 topic 有值
        if not profile.get("topic"):
            profile["topic"] = "编程学习"

        return {
            "final_answer": reply + "\n\n画像采集完成！正在为你生成学习方案...",
            "profile": profile,
            "profile_complete": True,
            "is_onboarding": False,
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
