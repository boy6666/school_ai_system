RISKY_INPUT_WORDS = [
    "代考", "作弊", "买答案", "窃取", "盗号", "木马", "病毒",
    "绕过监控", "攻击系统", "破解系统", "密码",
]
RISKY_OUTPUT_WORDS = ["窃取密码", "绕过监控", "攻击系统", "木马", "病毒"]


def safety_precheck(state: dict) -> dict:
    """输入安全检查 — 检测敏感词"""
    text = state.get("user_input", "")
    issues = [word for word in RISKY_INPUT_WORDS if word in text]
    passed = len(issues) == 0

    report = {
        "stage": "precheck",
        "passed": passed,
        "risk_level": "high" if issues else "low",
        "issues": issues,
    }

    if not passed:
        from school_agent.constants import INTENT_REJECT
        state["intent"] = INTENT_REJECT
        state["final_answer"] = "该请求可能涉及不合规内容，请重新提问。"
        state["safety_report"] = report
        return {"safety_report": report, "intent": INTENT_REJECT, "final_answer": state["final_answer"]}

    return {"safety_report": report}


def route_after_safety(state: dict) -> str:
    """安全后路由"""
    report = state.get("safety_report", {})
    if not report.get("passed", True):
        return "reject"
    return "continue"


def safety_postcheck(state: dict) -> dict:
    """输出安全检查 — 检测 AI 回复中的风险内容"""
    final_answer = state.get("final_answer", "")
    issues = [word for word in RISKY_OUTPUT_WORDS if word in (final_answer or "")]
    passed = len(issues) == 0

    report = {
        "stage": "postcheck",
        "passed": passed,
        "risk_level": "medium" if issues else "low",
        "issues": issues,
    }

    if issues:
        state["final_answer"] = "抱歉，回复内容包含不合规词汇，已拦截。"

    return {"safety_report": report}
