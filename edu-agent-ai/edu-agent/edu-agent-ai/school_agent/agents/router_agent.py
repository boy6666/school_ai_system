from school_agent.constants import (
    INTENT_EXPLAIN,
    INTENT_QUIZ,
    INTENT_RETRIEVE,
    INTENT_RESOURCE,
    INTENT_TUTOR,
)
from school_agent.utils.json_utils import merge_agent_output


def classify_intent(state: dict) -> dict:
    """意图分类智能体。

    第一版使用规则，优点是稳定、可解释；后续可以替换为 LLM 分类。
    """
    text = state.get("user_input", "")

    rules = [
        # 资源包类请求优先级最高，避免“学习资料 + 练习题”被误判成单纯出题。
        (INTENT_RESOURCE, ["生成资源", "学习资料", "资源包", "学习方案", "思维导图", "拓展阅读", "实操案例", "代码案例", "生成一套"]),
        (INTENT_QUIZ, ["出题", "题目", "练习", "测试", "选择题", "判断题", "填空题", "刷题", "考我"]),
        (INTENT_RETRIEVE, ["查找", "检索", "推荐资料", "找资料", "有哪些资料", "文档"]),
        (INTENT_TUTOR, ["错在哪", "为什么错", "不会", "不懂", "答疑", "帮我看", "解释一下这题"]),
        (INTENT_EXPLAIN, ["讲解", "解释", "说明", "教我", "怎么理解", "原理"]),
    ]

    intent = INTENT_EXPLAIN
    confidence = 0.65
    reason = "未命中明确意图，默认进入个性化讲解"

    for candidate, keywords in rules:
        hit = [word for word in keywords if word in text]
        if hit:
            intent = candidate
            confidence = min(0.95, 0.78 + 0.03 * len(hit))
            reason = f"命中关键词：{', '.join(hit)}"
            break

    return {
        "intent": intent,
        "intent_confidence": confidence,
        "route_reason": reason,
        "agent_outputs": merge_agent_output(
            state,
            "router_agent",
            {
                "status": "success",
                "intent": intent,
                "confidence": confidence,
                "reason": reason,
            },
        ),
    }


def route_by_intent(state: dict) -> str:
    return state.get("intent", INTENT_EXPLAIN)
