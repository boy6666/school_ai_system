from school_agent.agents.profile_extractor import (
    extract_profile_from_conversation,
    apply_profile_changes,
)
from school_agent.services.profile_store import save_student_profile
from school_agent.utils.time_utils import now_iso


def init_profile(state: dict) -> dict:
    """初始化画像：从 state 中取出 profile，没有则设为空"""
    profile = state.get("profile", {})
    if not profile:
        profile = {}
        state["profile"] = profile
    return {"profile": profile}


def extract_profile(state: dict) -> dict:
    """从本轮对话中提取画像变化并更新"""
    user_input = state.get("user_input", "")
    final_answer = state.get("final_answer", "")
    profile = state.get("profile", {})

    # 没有对话内容则跳过
    if not user_input or not final_answer:
        return {"profile_patch": {}}

    # 调用 LLM 从对话中提取画像变化
    changes = extract_profile_from_conversation(
        user_message=user_input,
        ai_response=final_answer,
        current_profile=profile,
    )

    if not changes:
        return {"profile_patch": {}}

    # 应用到画像
    updated_profile = apply_profile_changes(profile, changes)
    updated_profile["last_updated"] = now_iso()

    # 保存到 JSON 文件
    student_id = state.get("student_id", "")
    if student_id:
        try:
            save_student_profile(student_id, updated_profile)
        except Exception:
            pass

    return {"profile": updated_profile, "profile_patch": changes}
