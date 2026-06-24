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

    # ===== DEBUG extract_profile =====
    print(f"\n  [DEBUG extract_profile] 入口")
    print(f"  [DEBUG extract_profile] user_input: '{user_input[:50]}'")
    print(f"  [DEBUG extract_profile] profile keys: {list(profile.keys())[:10]}")
    print(f"  [DEBUG extract_profile] profile._onboarding_phase: '{profile.get('_onboarding_phase', 'NOT_FOUND')}'")
    # 检查 profile 是否被嵌套
    if "profile" in profile and isinstance(profile["profile"], dict):
        print(f"  [DEBUG extract_profile] ⚠️ profile 被嵌套! 内层 keys: {list(profile['profile'].keys())[:10]}")
        print(f"  [DEBUG extract_profile] ⚠️ 内层._onboarding_phase: '{profile['profile'].get('_onboarding_phase', 'NOT_FOUND')}'")

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
    apc_result = apply_profile_changes(profile, changes)
    # apply_profile_changes 返回 {"profile": 实际画像, "changed_dimensions": [...], "has_changes": ...}
    # 需要用 apc_result["profile"] 取出实际画像，否则 profile 被多包一层
    if isinstance(apc_result, dict) and "profile" in apc_result:
        updated_profile = apc_result["profile"]
        if "changed_dimensions" in apc_result:
            state["changed_dimensions"] = apc_result["changed_dimensions"]
        if "has_changes" in apc_result:
            state["has_profile_changes"] = apc_result["has_changes"]
    else:
        updated_profile = apc_result
    updated_profile["last_updated"] = now_iso()

    # 保存到 JSON 文件
    student_id = state.get("student_id", "")
    if student_id:
        try:
            # ===== DEBUG: 保存前的结构 =====
            save_data = updated_profile if isinstance(updated_profile, dict) else {}
            print(f"  [DEBUG extract_profile] 即将保存到文件, 键: {list(save_data.keys())[:10]}")
            print(f"  [DEBUG extract_profile] 保存时 _onboarding_phase: '{save_data.get('_onboarding_phase', 'NOT_FOUND')}'")
            # ===== END DEBUG =====
            save_student_profile(student_id, updated_profile)
        except Exception:
            pass

    return {"profile": updated_profile, "profile_patch": changes}
