import json
from school_agent.services.llm_client import call_llm
from school_agent.utils.text_utils import get_main_topic, to_text
from school_agent.utils.time_utils import now_iso


def evaluate_learning(state: dict) -> dict:
    """学习效果评估 — 根据对话内容评估学生理解程度"""
    profile = state.get("profile", {})
    user_input = state.get("user_input", "")
    final_answer = state.get("final_answer", "")

    if not user_input or not final_answer:
        return {"evaluation_report": {"status": "skipped", "reason": "无对话内容"}}

    topic = get_main_topic(profile)
    weaknesses = to_text(profile.get("weaknesses"))

    prompt = f"""你是学习评估专家。根据学生的提问和AI的回答，评估学生的理解程度。

知识点：{topic}
薄弱点：{weaknesses or '暂无记录'}

学生提问：{user_input}
AI回答：{final_answer[:500]}

请评估：
1. 学生的理解程度（0-100分）
2. 暴露的薄弱点
3. 学习建议

直接返回 JSON，不要额外说明：
{{"understanding_score":分数,"weak_points":["薄弱点列表"],"suggestion":"学习建议","evaluated_at":"评估时间"}}"""

    llm_text = call_llm(prompt)

    try:
        result = json.loads(llm_text)
    except json.JSONDecodeError:
        import re
        match = re.search(r'\{.*\}', llm_text, re.DOTALL)
        if match:
            try:
                result = json.loads(match.group(0))
            except json.JSONDecodeError:
                result = {"understanding_score": 60, "weak_points": [], "suggestion": "继续学习"}
        else:
            result = {"understanding_score": 60, "weak_points": [], "suggestion": "继续学习"}

    result["evaluated_at"] = now_iso()
    return {"evaluation_report": result}


def log_interaction(state: dict) -> dict:
    """记录本轮交互到 JSON 日志文件"""
    student_id = state.get("student_id", "")
    session_id = state.get("session_id", "")
    user_input = state.get("user_input", "")
    final_answer = state.get("final_answer", "")
    intent = state.get("intent", "")

    if not student_id:
        outputs = state.get("agent_outputs")
        if outputs is None:
            outputs = {}
            state["agent_outputs"] = outputs
        outputs["log_interaction"] = {"status": "skipped", "reason": "no student_id"}
        return {"agent_outputs": outputs}

    try:
        from school_agent.services.log_store import append_log
        log_entry = {
            "session_id": session_id,
            "user_input": user_input[:200],
            "final_answer": final_answer[:200] if final_answer else "",
            "intent": intent,
            "time": now_iso(),
        }
        append_log(student_id, session_id or "default", log_entry)
        status = "logged"
    except Exception as e:
        status = f"failed: {e}"

    outputs = state.get("agent_outputs")
    if outputs is None:
        outputs = {}
        state["agent_outputs"] = outputs
    outputs["log_interaction"] = {"status": status}
    return {"agent_outputs": outputs}
