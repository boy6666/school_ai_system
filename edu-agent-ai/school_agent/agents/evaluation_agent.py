from school_agent.services.log_store import append_learning_log
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import get_main_topic
from school_agent.utils.time_utils import now_iso


def evaluate_learning(state: dict) -> dict:
    """学习效果评估智能体范例。"""
    profile = state.get("profile", {})
    topic = get_main_topic(profile)
    user_input = state.get("user_input", "")

    weak_signals = ["不会", "不懂", "错", "看不懂", "没理解", "还是不会"]
    has_weak_signal = any(word in user_input for word in weak_signals)

    if has_weak_signal:
        score = 55
        weak_points = [topic]
        suggestion = f"建议降低难度，优先学习 {topic} 的基础图解和最小代码案例。"
    elif state.get("intent") == "quiz":
        score = 70
        weak_points = profile.get("weaknesses", [])
        suggestion = "建议完成题目后反馈错题，系统会进一步更新画像。"
    else:
        score = 78
        weak_points = profile.get("weaknesses", [])
        suggestion = f"建议继续完成 {topic} 的练习题和代码实操。"

    report = {
        "understanding_score": score,
        "weak_points": weak_points,
        "suggestion": suggestion,
        "evaluated_at": now_iso(),
    }

    return {
        "evaluation_report": report,
        "agent_outputs": merge_agent_output(
            state,
            "evaluation_agent",
            {"status": "success", "report": report},
        ),
    }


def log_interaction(state: dict) -> dict:
    """学习日志节点。"""
    student_id = state.get("student_id", "student_001")
    log_path = append_learning_log(
        student_id,
        {
            "session_id": state.get("session_id"),
            "user_input": state.get("user_input"),
            "intent": state.get("intent"),
            "profile_patch": state.get("profile_patch"),
            "evaluation_report": state.get("evaluation_report"),
        },
    )

    return {
        "log_path": log_path,
        "agent_outputs": merge_agent_output(
            state,
            "log_store",
            {"status": "success", "log_path": log_path},
        ),
    }
