import json
from school_agent.constants import (
    INTENT_CHAT, INTENT_EXPLAIN, INTENT_ONBOARDING,
    INTENT_QUIZ, INTENT_RETRIEVE, INTENT_RESOURCE, INTENT_TUTOR,
)
from school_agent.services.llm_client import call_llm


def classify_intent(state: dict) -> dict:
    session_id = state.get("session_id", "")
    profile_from_state = state.get("profile", {})
    onboarding_phase = profile_from_state.get("_onboarding_phase", "")
    text = state.get("user_input", "")

    # ===== DEBUG: 意图分类 =====
    print(f"\n  [DEBUG router] session_id: {session_id}")
    print(f"  [DEBUG router] session_id.startswith('onboard_'): {session_id.startswith('onboard_')}")
    print(f"  [DEBUG router] profile keys: {list(profile_from_state.keys())[:10]}...")
    print(f"  [DEBUG router] _onboarding_phase: '{onboarding_phase}'")
    print(f"  [DEBUG router] user_input: '{text[:50]}'")
    print(f"  [DEBUG router] 走 onboard?: {session_id.startswith('onboard_') and onboarding_phase != 'complete'}")

    # 引导会话走 onboarding（不受 LLM 影响）
    if session_id.startswith("onboard_") and onboarding_phase != "complete":
        print(f"  [DEBUG router] → 返回 INTENT_ONBOARDING")
        return {"intent": INTENT_ONBOARDING, "intent_confidence": 1.0, "route_reason": "session_id=onboard_"}

    # Step 1: LLM 意图分类
    llm_intent, llm_reason, llm_confidence = _classify_by_llm(text)

    # Step 2: LLM 置信度够高，直接用它
    if llm_confidence >= 0.6:
        return {"intent": llm_intent, "intent_confidence": llm_confidence, "route_reason": f"LLM: {llm_reason}"}

    # Step 3: LLM 置信度低，用关键词兜底
    kw_intent, kw_reason = _classify_by_keywords(text)
    if kw_intent:
        return {"intent": kw_intent, "intent_confidence": 0.7, "route_reason": f"关键词: {kw_reason}"}

    # Step 4: 全都不行，默认讲解
    return {"intent": INTENT_EXPLAIN, "intent_confidence": 0.6, "route_reason": "默认讲解"}


def _classify_by_llm(text: str):
    """用 LLM 判断用户意图"""
    prompt = f"""判断用户消息的意图，只返回 JSON。

消息：{text}

可选意图：
- chat: 问候、闲聊、表达情感（你好、你是谁、谢谢、拜拜、哈哈）
- explain: 请求讲解知识点（讲解、解释、什么是、怎么理解）
- quiz: 要求出题练习（出题、练习、考我、刷题）
- tutor: 请求答疑辅导（为什么错、不会、帮我看、答疑）
- retrieve: 查找资源（查找、推荐资料、有哪些文档）
- resource: 生成学习资源包（生成资源、学习方案、思维导图）

返回格式（只返回 JSON，不要其他文字）：
{{"intent":"意图名称","confidence":0.0到1.0之间的小数,"reason":"简短判断理由"}}"""

    try:
        resp = call_llm(prompt)
        result = json.loads(resp.strip().strip("```json").strip("```").strip())
        intent = result.get("intent", "")
        confidence = float(result.get("confidence", 0))
        reason = result.get("reason", "")

        valid_intents = {INTENT_CHAT, INTENT_EXPLAIN, INTENT_QUIZ, INTENT_TUTOR, INTENT_RETRIEVE, INTENT_RESOURCE}
        if intent not in valid_intents:
            return INTENT_EXPLAIN, "LLM 返回了无效意图", 0

        return intent, reason, confidence
    except Exception:
        return INTENT_EXPLAIN, "LLM 分类失败", 0


def _classify_by_keywords(text: str):
    """关键词兜底分类"""
    rules = [
        (INTENT_RESOURCE, ["生成资源", "学习资料", "资源包", "学习方案", "思维导图", "拓展阅读", "代码案例", "生成一套"]),
        (INTENT_QUIZ, ["出题", "题目", "练习", "测试", "选择题", "刷题", "考我"]),
        (INTENT_RETRIEVE, ["查找", "检索", "推荐资料", "找资料", "有哪些资料", "文档"]),
        (INTENT_TUTOR, ["错在哪", "为什么错", "不会", "不懂", "答疑", "帮我看", "解释一下这题"]),
        (INTENT_EXPLAIN, ["讲解", "解释", "说明", "教我", "怎么理解", "原理"]),
        (INTENT_CHAT, ["你好", "你是谁", "我喜欢你", "早上好", "晚上好", "嗨", "hello", "hi",
                       "在吗", "谢谢", "感谢", "拜拜", "再见", "晚安", "哈哈"]),
    ]
    for intent, keywords in rules:
        for kw in keywords:
            if kw in text:
                return intent, kw
    return None, None


def route_by_intent(state: dict) -> str:
    return state.get("intent", INTENT_EXPLAIN)
