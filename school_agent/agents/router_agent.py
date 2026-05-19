from school_agent.constants import (
    INTENT_EXPLAIN,
    INTENT_QUIZ,
    INTENT_RETRIEVE,
    INTENT_RESOURCE,
    INTENT_TUTOR,
)
from school_agent.utils.json_utils import merge_agent_output


def classify_intent(state: dict) -> dict:
    """增强版意图分类智能体。

    识别规则：
    - 优先匹配资源生成、出题、检索、辅导等明确关键词
    - 对讲解类意图，特别支持 "什么是python"、"java基础" 等自然语言
    """
    text = state.get("user_input", "").lower().strip()

    # 特殊处理：明确询问语言特性/语法 → 讲解意图
    explain_keywords = [
    "讲解", "解释", "说明", "教我", "怎么理解", "原理",
    "什么是", "是什么", "何为", "啥是", "哪是",
    "python", "java", "c++", "go", "rust", "javascript", "html", "css"
]
    quiz_keywords = ["出题", "题目", "练习", "测试", "选择题", "判断题", "填空题", "刷题", "考我", "做几道"]
    resource_keywords = ["生成资源", "学习资料", "资源包", "学习方案", "思维导图", "拓展阅读", "实操案例", "代码案例", "生成一套"]
    retrieve_keywords = ["查找", "检索", "推荐资料", "找资料", "有哪些资料", "文档"]
    tutor_keywords = ["错在哪", "为什么错", "不会", "不懂", "答疑", "帮我看", "解释一下这题"]

    # 优先级从高到低
    intent = INTENT_EXPLAIN
    confidence = 0.65
    reason = "未命中明确意图，默认进入个性化讲解"

    for keywords, candidate in [
        (resource_keywords, INTENT_RESOURCE),
        (quiz_keywords, INTENT_QUIZ),
        (retrieve_keywords, INTENT_RETRIEVE),
        (tutor_keywords, INTENT_TUTOR),
    ]:
        hit = [w for w in keywords if w in text]
        if hit:
            intent = candidate
            confidence = min(0.95, 0.8 + 0.03 * len(hit))
            reason = f"命中关键词：{', '.join(hit)}"
            break
    else:
        # 如果没有命中以上，检查是否命中讲解意图（包含语言名）
        hit = [w for w in explain_keywords if w in text]
        if hit:
            intent = INTENT_EXPLAIN
            confidence = min(0.9, 0.7 + 0.02 * len(hit))
            reason = f"命中讲解类关键词：{', '.join(hit)}"

    return {
        "intent": intent,
        "intent_confidence": confidence,
        "route_reason": reason,
        "agent_outputs": merge_agent_output(
            state,
            "router_agent",
            {"status": "success", "intent": intent, "confidence": confidence, "reason": reason},
        ),
    }


def route_by_intent(state: dict) -> str:
    return state.get("intent", INTENT_EXPLAIN)