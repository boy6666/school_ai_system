import json
from typing import Any, Dict, List, Optional

from school_agent.schemas.profile_schema import (
    DimensionLevel,
    DimensionState,
    StudentProfile,
)
from school_agent.services.llm_client import call_llm
from school_agent.utils.time_utils import now_iso

PROFILE_EXTRACTOR_SYSTEM = """你是学生画像后台分析专家。你的任务是从学生与AI辅导的对话中，自动提取六维画像变化。

## 六维定义与层次标准

### 1. 知识掌握度 (knowledge_mastery)
- 入门(level_1/0-40分)：学生对当前知识点仅有表面了解，能看懂代码但无法独立编写，频繁混淆基本概念
- 熟练(level_2/41-70分)：学生能独立应用知识点完成练习，能说出关键概念，偶尔有小错误
- 精通(level_3/71-100分)：学生能讲解原理、对比不同方案、优化代码，能发现并纠正错误

### 2. 学习目标清晰度 (learning_goal_clarity)
- 入门(level_1/0-40分)：学生没有明确学习目标，只是随意提问，问的问题零散不聚焦
- 熟练(level_2/41-70分)：学生有短期目标（如通过考试、完成作业、搞懂某个知识点）
- 精通(level_3/71-100分)：学生有长期规划（如职业发展、系统学习路径），提问有明确方向

### 3. 认知风格适配 (cognitive_adaptation)
- 入门(level_1/0-40分)：学生的认知偏好尚未显现，没有表现出特定学习方式偏好
- 熟练(level_2/41-70分)：能识别学生偏好（视觉型喜欢图解、实践型喜欢代码、阅读型喜欢文档）
- 精通(level_3/71-100分)：学生有明确的学习策略偏好，会主动选择适合自己的学习方式

### 4. 错误规避力 (mistake_avoidance)
- 入门(level_1/0-40分)：学生频繁重复犯同类错误，未意识到自己的错误模式
- 熟练(level_2/41-70分)：学生能识别部分错误模式，会主动询问"这样写对吗"
- 精通(level_3/71-100分)：学生能主动规避常见错误，有意识地检查和验证

### 5. 学习自主性 (learning_autonomy)
- 入门(level_1/0-40分)：被动学习，只是简单提问等待答案，不追问不思考
- 熟练(level_2/41-70分)：主动学习，自觉提问和追问，尝试自己解决问题后再求助
- 精通(level_3/71-100分)：自驱学习，主动拓展知识、深入追问原理、寻找额外资源

### 6. 综合能力 (overall_level)
- 入门(level_1/0-40分)：基础补齐型，知识薄弱需要系统性补充
- 熟练(level_2/41-70分)：稳定提升型，有一定基础按部就班提升
- 精通(level_3/71-100分)：进阶拓展型，基础扎实适合挑战高级内容

## 输出要求

严格返回JSON（只返回JSON，不要有其他文字）：
{
  "dimension_changes": {
    "knowledge_mastery": {
      "new_level": "level_1",
      "level_changed": false,
      "new_score": 35,
      "score_change_reason": "学生对基础概念理解有偏差，本次对话中出现了概念混淆",
      "evidence": ["学生询问了基本概念但理解有误——具体表现为..."],
      "reason": "摘要说明本次对话在该维度的表现"
    }
  },
  "weaknesses": ["从本次对话中新发现的知识薄弱点"],
  "mistake_patterns": ["从本次对话中新观察到的错误模式"],
  "topic": "当前讨论的知识点",
  "learning_goal": "从对话中推断的学习目标（如果有的话）",
  "cognitive_style": "从对话中推断的认知风格（如果有的话）",
  "overall_type": "基础补齐型/稳定提升型/进阶拓展型（如果有足够信息判断）",
  "suggestions": ["针对性的学习建议"]
}

## 核心规则
- **每个维度都必须输出**，即使没有变化也要给出当前评估
- 只更新有明确证据的维度，没有证据的维度保持原level，level_changed为false
- level_changed为true时表示层次确实发生了变化（升级或降级）
- **分数可以上下调整**：如果学生表现出色，分数应该上升；如果表现退步或概念混淆，分数应该下降
- evidence必须是从对话中提取的具体内容摘要，不能是泛泛而谈
- 从自然对话推断，不要假设不存在的信息
- **综合能力(overall_level)** 应综合其他五个维度给出整体判断
- 如果学生只是简单打了个招呼或闲聊（如"你好""在吗"），所有维度保持原样"""


def _parse_extractor_json(text: str) -> Dict[str, Any]:
    text = text.strip()
    if text.startswith("```"):
        lines = text.split("\n")
        if lines[-1].strip() == "```":
            lines = lines[1:-1]
        else:
            lines = lines[1:]
        text = "\n".join(lines)
    start = text.find("{")
    end = text.rfind("}") + 1
    if start != -1 and end > start:
        text = text[start:end]
    return json.loads(text)


# LLM 可能返回中文层次标签，映射回 level_1/2/3
_LEVEL_CN_MAP = {
    "入门": "level_1",
    "熟练": "level_2",
    "精通": "level_3",
    "level_1": "level_1",
    "level_2": "level_2",
    "level_3": "level_3",
}


def _normalize_level(raw: str) -> str:
    return _LEVEL_CN_MAP.get(raw, "level_1")


def _to_int(val, default: int = 30) -> int:
    """安全转换为 int，兼容 LLM 返回的字符串或浮点数。"""
    try:
        if isinstance(val, str):
            return int(float(val))
        if isinstance(val, (int, float)):
            return int(val)
    except (ValueError, TypeError):
        pass
    return default


def _build_default_changes() -> Dict[str, Any]:
    return {
        "dimension_changes": {},
        "weaknesses": [],
        "mistake_patterns": [],
        "topic": "",
        "learning_goal": "",
        "cognitive_style": "",
        "overall_type": None,
        "suggestions": [],
    }


def extract_profile_from_conversation(
    user_message: str,
    ai_response: str,
    current_profile: Optional[Dict[str, Any]] = None,
    conversation_context: str = "",
) -> Dict[str, Any]:
    """从一轮对话中提取画像变化。

    Args:
        user_message: 用户的消息
        ai_response: AI的回复（完整内容，不截断）
        current_profile: 当前画像数据（dict格式）
        conversation_context: 近期对话历史摘要（可选，用于多轮上下文分析）

    Returns:
        画像变化信息，包含各维度层次变化、证据等
    """
    current_state_desc = "暂无画像"
    if current_profile:
        parts = []
        for dim_name, dim_label in [
            ("knowledge_mastery", "知识掌握度"),
            ("learning_goal_clarity", "学习目标清晰度"),
            ("cognitive_adaptation", "认知风格适配"),
            ("mistake_avoidance", "错误规避力"),
            ("learning_autonomy", "学习自主性"),
            ("overall_level", "综合能力"),
        ]:
            dim = current_profile.get(dim_name)
            if isinstance(dim, dict):
                level = _normalize_level(dim.get("level", "level_1"))
                level_label = DimensionLevel.label(DimensionLevel(level), dim_name)
                score = dim.get("score", 30)
                evidence = dim.get("evidence", [])
                recent_evidence = evidence[-3:] if len(evidence) > 3 else evidence
                parts.append(
                    f"- {dim_label}: {level_label}({score}分)"
                )
                if recent_evidence:
                    parts.append(f"  近期证据: {'; '.join(recent_evidence)}")
        if parts:
            current_state_desc = "当前画像:\n" + "\n".join(parts)

    # 传入完整 AI 回复（不再截断到500字符）
    ai_summary = ai_response if len(ai_response) <= 1500 else ai_response[:1500] + "\n...(后续内容已省略)"

    prompt = f"""{current_state_desc}

---
{conversation_context if conversation_context else ""}
学生最新发言：
{user_message}

AI辅导回复摘要：
{ai_summary}

请根据以上对话内容，分析本轮对话中学生在各维度的表现，输出画像变化JSON。"""

    try:
        response = call_llm(prompt, system=PROFILE_EXTRACTOR_SYSTEM)
        result = _parse_extractor_json(response)
        # 确保 dimension_changes 至少有基本结构
        if "dimension_changes" not in result:
            result["dimension_changes"] = {}
        return result
    except Exception:
        return _build_default_changes()


def apply_profile_changes(
    profile_dict: Dict[str, Any],
    changes: Dict[str, Any],
) -> Dict[str, Any]:
    """将提取的画像变化应用到画像数据上。

    支持分数的双向调整（上升和下降），不再只升不降。

    Args:
        profile_dict: 当前画像数据
        changes: extract_profile_from_conversation 返回的变化数据

    Returns:
        更新后的画像数据和变更摘要
    """
    now = now_iso()
    changed_dimensions: List[Dict[str, Any]] = []
    dimension_changes = changes.get("dimension_changes", {})

    for dim_name in [
        "knowledge_mastery",
        "learning_goal_clarity",
        "cognitive_adaptation",
        "mistake_avoidance",
        "learning_autonomy",
        "overall_level",
    ]:
        dim_change = dimension_changes.get(dim_name)
        if not dim_change:
            continue

        # 初始化维度
        if dim_name not in profile_dict or not isinstance(profile_dict.get(dim_name), dict):
            profile_dict[dim_name] = DimensionState().model_dump()

        old_level = _normalize_level(profile_dict[dim_name].get("level", "level_1"))
        old_score = _to_int(profile_dict[dim_name].get("score", 30))
        new_level = _normalize_level(dim_change.get("new_level", old_level))
        new_score = _to_int(dim_change.get("new_score", old_score))
        evidence = dim_change.get("evidence", [])
        level_changed = dim_change.get("level_changed", False)

        # 对比 new_level 和 old_level 自动判断 level 是否真的变了
        if new_level != old_level:
            level_changed = True

        # 分数双向调整：使用指数移动平均，让分数平滑变化
        # 新分数权重0.4，旧分数权重0.6，避免单次对话造成剧烈波动
        if new_score != old_score:
            smoothed_score = round(old_score * 0.6 + new_score * 0.4)
            profile_dict[dim_name]["score"] = max(0, min(100, smoothed_score))
        else:
            profile_dict[dim_name]["score"] = old_score

        profile_dict[dim_name]["level"] = new_level
        profile_dict[dim_name]["evidence"] = (
            profile_dict[dim_name].get("evidence", []) + evidence
        )[-10:]  # 只保留最近10条证据
        profile_dict[dim_name]["last_updated"] = now

        from_label = DimensionLevel.label(DimensionLevel(old_level), dim_name)
        to_label = DimensionLevel.label(DimensionLevel(new_level), dim_name)

        if level_changed:
            changed_dimensions.append({
                "dimension": dim_name,
                "from_level": old_level,
                "to_level": new_level,
                "from_label": from_label,
                "to_label": to_label,
                "level_changed": True,
                "reason": dim_change.get("reason", ""),
            })
        elif abs(new_score - old_score) >= 5:
            changed_dimensions.append({
                "dimension": dim_name,
                "from_level": old_level,
                "to_level": new_level,
                "from_label": from_label,
                "to_label": to_label,
                "level_changed": False,
                "score_change": f"{old_score}→{profile_dict[dim_name]['score']}",
                "reason": dim_change.get("score_change_reason", dim_change.get("reason", "基于本轮对话调整")),
            })

    # 更新辅助字段
    if changes.get("topic"):
        profile_dict["topic"] = changes["topic"]
    if changes.get("learning_goal"):
        profile_dict["learning_goal"] = changes["learning_goal"]
    if changes.get("cognitive_style"):
        profile_dict["cognitive_style"] = changes["cognitive_style"]
    if changes.get("overall_type"):
        profile_dict["overall_type"] = changes["overall_type"]

    # 合并 weaknesses（去重，限制数量）
    old_weaknesses = profile_dict.get("weaknesses", [])
    if isinstance(old_weaknesses, str):
        old_weaknesses = [old_weaknesses]
    new_weaknesses = changes.get("weaknesses", [])
    profile_dict["weaknesses"] = list(dict.fromkeys(old_weaknesses + new_weaknesses))[:10]

    # 合并 mistake_patterns（去重，限制数量）
    old_mistakes = profile_dict.get("mistake_patterns", [])
    if isinstance(old_mistakes, str):
        old_mistakes = [old_mistakes]
    new_mistakes = changes.get("mistake_patterns", [])
    profile_dict["mistake_patterns"] = list(dict.fromkeys(old_mistakes + new_mistakes))[:8]

    # 合并 suggestions
    if changes.get("suggestions"):
        old_suggestions = profile_dict.get("profile_suggestions", [])
        new_suggestions = changes["suggestions"]
        profile_dict["profile_suggestions"] = (old_suggestions + new_suggestions)[-5:]

    profile_dict["conversation_count"] = profile_dict.get("conversation_count", 0) + 1
    profile_dict["last_updated"] = now

    return {
        "profile": profile_dict,
        "changed_dimensions": changed_dimensions,
        "has_changes": len(changed_dimensions) > 0,
    }
