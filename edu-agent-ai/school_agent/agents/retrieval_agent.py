import json
from school_agent.services.llm_client import call_llm
from school_agent.utils.text_utils import get_main_topic, to_text


def retrieve_knowledge(state: dict) -> dict:
    """知识库检索节点：为后续智能体提供上下文"""
    if "retrieved_context" not in state:
        state["retrieved_context"] = ""
    return state


def retrieval_agent(state: dict) -> dict:
    """检索推荐 — 根据画像和学习进度推荐资源"""
    profile = state.get("profile", {})
    user_input = state.get("user_input", "")
    topic = get_main_topic(profile)
    course = profile.get("course", "Java 程序设计")
    weaknesses = to_text(profile.get("weaknesses"))
    knowledge_base = profile.get("knowledge_base", "未知")
    wrong_questions = profile.get("wrong_questions", [])
    context = state.get("retrieved_context", "")

    wrong_text = ""
    if wrong_questions:
        items = [f"  {wq.get('question')}（正确答案：{wq.get('correctAnswer')}）" for wq in wrong_questions[:3]]
        wrong_text = "近期错题相关知识点：\n" + "\n".join(items)

    prompt = f"""你是高校课程学习资源推荐专家。根据学生画像推荐最合适的学习资源。

学生需求：{user_input}
当前知识点：{topic}
课程：{course}
学生基础：{knowledge_base}
薄弱点：{weaknesses or '暂无记录'}

{wrong_text}

知识库内容：
{context[:600] if context else '暂无知识库内容'}

要求：
1. 推荐3-5个最相关的学习资源
2. 每个资源说明推荐理由（针对画像）
3. 优先推荐能解决薄弱点和错题的资源
4. 资源类型包括：讲解文档、练习题、代码案例、思维导图、拓展阅读

直接返回 JSON，不要额外说明：
{{"type":"retrieval","recommendations":[{{"title":"资源标题","type":"文档/练习/代码/导图/阅读","reason":"推荐理由","priority":"高/中/低"}}],"summary":"整体建议"}}"""

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
                result = {"type": "retrieval", "recommendations": [{"title": llm_text, "type": "文档", "reason": "", "priority": "中"}], "summary": ""}
        else:
            result = {"type": "retrieval", "recommendations": [{"title": llm_text, "type": "文档", "reason": "", "priority": "中"}], "summary": ""}

    return {"final_answer": json.dumps(result, ensure_ascii=False)}
