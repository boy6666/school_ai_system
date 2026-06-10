"""通用对话智能体 — 处理问候、闲聊等非学习类对话"""
from school_agent.services.llm_client import call_llm


def chat_agent(state: dict) -> dict:
    """通用对话：处理问候、闲聊等非学习类消息，调用LLM自然回应"""
    user_input = state.get("user_input", "")
    profile = state.get("profile", {})
    topic = profile.get("topic", "")
    course = profile.get("course", "")

    prompt = f"""你是学生的学习伙伴，一个友好、热情的 AI 助教。

学生说：{user_input}

关于这个学生：
- 课程：{course or '未知'}
- 当前知识点：{topic or '未知'}

要求：
1. 如果学生在打招呼/闲聊/表达情感，用友好的语气自然回应
2. 如果学生问你是谁，介绍自己是学习助教
3. 可以在回应后自然引导回学习话题
4. 语气亲切，不要太长（不超过150字）

直接返回你的回复内容，不要额外说明。"""

    answer = call_llm(prompt)
    if not answer:
        answer = "你好呀！我是你的学习助教，有什么学习问题可以随时问我～"

    return {"final_answer": answer}
