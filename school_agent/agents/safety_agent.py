import json

from school_agent.constants import INTENT_REJECT
from school_agent.utils.json_utils import merge_agent_output


RISKY_INPUT_WORDS = [
    "代考",
    "作弊",
    "买答案",
    "窃取",
    "盗号",
    "木马",
    "病毒",
    "绕过监控",
    "攻击系统",
    "破解系统",
]

RISKY_OUTPUT_WORDS = [
    "窃取密码",
    "绕过监控",
    "攻击系统",
    "木马",
    "病毒",
]


def safety_precheck(state: dict) -> dict:
    """输入安全检查智能体。"""
    text = state.get("user_input", "")
    issues = [word for word in RISKY_INPUT_WORDS if word in text]

    report = {
        "stage": "precheck",
        "passed": not issues,
        "risk_level": "high" if issues else "low",
        "issues": issues,
    }

    result = {
        "safety_report": report,
        "agent_outputs": merge_agent_output(
            state,
            "safety_precheck",
            {"status": "success", "report": report},
        ),
    }

    if issues:
        result.update(
            {
                "intent": INTENT_REJECT,
                "final_answer": (
                    "该请求可能涉及不合规内容，我不能直接生成相关方案。"
                    "如果你是在课程学习中遇到问题，可以改为询问概念讲解、合法实验流程或安全规范。"
                ),
            }
        )

    return result


def route_after_safety(state: dict) -> str:
    report = state.get("safety_report", {})
    if report.get("passed", True) is False or state.get("intent") == INTENT_REJECT:
        return "reject"
    return "continue"


def safety_postcheck(state: dict) -> dict:
    """输出安全与基础质量检查智能体。"""
    final_answer = state.get("final_answer", "")
    resources = state.get("resources", {})
    output_text = final_answer + json.dumps(resources, ensure_ascii=False)

    issues = []
    if not final_answer and not resources:
        issues.append("输出为空")

    for word in RISKY_OUTPUT_WORDS:
        if word in output_text:
            issues.append(f"输出包含风险词：{word}")

    report = {
        "stage": "postcheck",
        "passed": not issues,
        "risk_level": "medium" if issues else "low",
        "issues": issues,
    }

    return {
        "safety_report": report,
        "agent_outputs": merge_agent_output(
            state,
            "safety_postcheck",
            {"status": "success", "report": report},
        ),
    }
