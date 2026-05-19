# school_agent/agents/router_agent.py
import json
from school_agent.constants import (
    INTENT_EXPLAIN,
    INTENT_QUIZ,
    INTENT_RETRIEVE,
    INTENT_RESOURCE,
    INTENT_TUTOR,
)
from school_agent.services.llm_client import call_llm
from school_agent.utils.json_utils import merge_agent_output

def classify_intent(state: dict) -> dict:
    """AI 智能体版意图分类：调用大模型判断用户意图"""
    user_input = state.get("user_input", "")
    
    prompt = f"""你是一个智能意图分类器。请分析以下用户输入，判断其最可能属于哪一类意图。
意图类型：
- explain: 用户希望获得知识点讲解、概念解释
- quiz: 用户要求出题、练习、测试
- resource: 用户希望生成学习资源（文档、思维导图、代码案例等）
- retrieve: 用户希望检索资料、查找文档
- tutor: 用户需要辅导、答疑、错题解析

用户输入：{user_input}

请只输出一个 JSON 对象，格式：{{"intent": "意图类型", "confidence": 0.0-1.0, "reason": "简短理由"}}
不要输出其他内容。
"""
    response = call_llm(prompt)
    try:
        cleaned = response.strip().replace("```json", "").replace("```", "").strip()
        data = json.loads(cleaned)
        intent = data.get("intent", INTENT_EXPLAIN)
        confidence = data.get("confidence", 0.7)
        reason = data.get("reason", "AI 判断")
    except:
        intent = INTENT_EXPLAIN
        confidence = 0.6
        reason = "JSON 解析失败，默认讲解"
    
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