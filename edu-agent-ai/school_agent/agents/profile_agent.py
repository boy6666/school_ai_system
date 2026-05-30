import copy
from typing import Any, Dict

from school_agent.agents.profile_extractor import (
    apply_profile_changes,
    extract_profile_from_conversation,
)
from school_agent.schemas.profile_schema import DimensionLevel, DimensionState, StudentProfile
from school_agent.services.profile_store import load_student_profile, save_student_profile
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.time_utils import now_iso


def _default_dimension() -> Dict[str, Any]:
    return DimensionState().model_dump()


def init_profile(state: dict) -> dict:
    """首次使用时创建默认level_1画像，无需用户填写任何信息。"""
    student_id = state.get("student_id", "student_001")
    existing = load_student_profile(student_id)

    if existing and existing.get("knowledge_mastery"):
        return {
            "profile": existing,
            "profile_before": copy.deepcopy(existing),
            "agent_outputs": merge_agent_output(
                state,
                "profile_agent",
                {"status": "loaded", "new_profile": False},
            ),
        }

    now = now_iso()
    profile = StudentProfile(
        created_at=now,
        last_updated=now,
        conversation_count=0,
    ).model_dump()

    save_student_profile(student_id, profile)

    return {
        "profile": profile,
        "profile_before": {},
        "agent_outputs": merge_agent_output(
            state,
            "profile_agent",
            {"status": "created", "new_profile": True},
        ),
    }


def extract_profile(state: dict) -> dict:
    """在对话末尾从对话历史中提取画像变化并更新。

    不再依赖用户主动填写，而是从自然辅导对话中后台分析。
    支持多轮对话上下文的累积分析。
    """
    student_id = state.get("student_id", "student_001")
    user_input = state.get("user_input", "")
    final_answer = state.get("final_answer", "")
    current_profile = state.get("profile", {})

    if not user_input or not final_answer:
        return {
            "profile": current_profile,
            "profile_changes": {"changed_dimensions": [], "has_changes": False},
            "agent_outputs": merge_agent_output(
                state,
                "profile_agent",
                {"status": "skipped", "reason": "no content to extract"},
            ),
        }

    # 累积对话历史（最近10轮）
    conversation_log = current_profile.get("_conversation_log", [])
    if isinstance(conversation_log, str):
        conversation_log = []
    conversation_log.append({
        "user": user_input[:300],
        "ai": final_answer[:300],
    })
    # 只保留最近10轮
    if len(conversation_log) > 10:
        conversation_log = conversation_log[-10:]

    # 构建对话历史摘要
    recent_context = ""
    if len(conversation_log) >= 2:
        recent_turns = conversation_log[-5:]  # 最近5轮
        recent_context = "## 近期对话历史\n"
        for i, turn in enumerate(recent_turns, 1):
            recent_context += f"第{i}轮 - 学生: {turn['user']}\nAI: {turn['ai']}\n\n"

    # 从对话中提取画像变化（传入对话历史）
    changes = extract_profile_from_conversation(
        user_message=user_input,
        ai_response=final_answer,
        current_profile=current_profile,
        conversation_context=recent_context,
    )

    # 应用变化
    result = apply_profile_changes(current_profile, changes)
    updated_profile = result["profile"]

    # 保存对话历史到画像中（供下次使用）
    updated_profile["_conversation_log"] = conversation_log

    # 持久化
    save_student_profile(student_id, updated_profile)

    return {
        "profile": updated_profile,
        "profile_before": copy.deepcopy(current_profile),
        "profile_changes": {
            "changed_dimensions": result["changed_dimensions"],
            "has_changes": result["has_changes"],
        },
        "agent_outputs": merge_agent_output(
            state,
            "profile_agent",
            {
                "status": "success",
                "changed_dimensions": result["changed_dimensions"],
                "has_changes": result["has_changes"],
            },
        ),
    }


def get_profile_snapshot(profile: dict) -> dict:
    """将画像数据转为前端可展示的快照，包含每个维度的层次标签。"""
    snapshot = {
        "major": profile.get("major", ""),
        "grade": profile.get("grade", ""),
        "course": profile.get("course", ""),
        "topic": profile.get("topic", ""),
        "learning_goal": profile.get("learning_goal", ""),
        "knowledge_base": profile.get("knowledge_base", ""),
        "current_mastery": profile.get("current_mastery", ""),
        "cognitive_style": profile.get("cognitive_style", ""),
        "weaknesses": profile.get("weaknesses", []),
        "mistake_patterns": profile.get("mistake_patterns", []),
        "learning_behavior": profile.get("learning_behavior", ""),
        "resource_preference": profile.get("resource_preference", []),
        "pace": profile.get("pace", ""),
        "overall_type": profile.get("overall_type"),
        "profile_suggestions": profile.get("profile_suggestions", []),
        "conversation_count": profile.get("conversation_count", 0),
        "last_updated": profile.get("last_updated"),
        "created_at": profile.get("created_at"),
    }

    for dim_name, dim_label in [
        ("knowledge_mastery", "知识掌握度"),
        ("learning_goal_clarity", "学习目标清晰度"),
        ("cognitive_adaptation", "认知风格适配"),
        ("mistake_avoidance", "错误规避力"),
        ("learning_autonomy", "学习自主性"),
        ("overall_level", "综合能力"),
    ]:
        dim = profile.get(dim_name)
        if isinstance(dim, dict):
            level_val = dim.get("level", "level_1")
            level_label = DimensionLevel.label(DimensionLevel(level_val))
            snapshot[dim_name] = {
                "level": level_val,
                "level_label": level_label,
                "level_number": 1 if level_val == "level_1" else 2 if level_val == "level_2" else 3,
                "score": dim.get("score", 30),
                "evidence": dim.get("evidence", []),
            }
            snapshot[f"{dim_name}_label"] = dim_label
        else:
            snapshot[dim_name] = {
                "level": "level_1",
                "level_label": "入门",
                "level_number": 1,
                "score": 30,
                "evidence": [],
            }
            snapshot[f"{dim_name}_label"] = dim_label

    return snapshot
