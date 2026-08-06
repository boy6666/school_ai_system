import json
from school_agent.services.llm_client import call_llm
from school_agent.utils.text_utils import get_main_topic, to_text


def explain_agent(state: dict) -> dict:
    """个性化讲解 — 根据画像、错题、学习进度生成针对性讲解"""
    profile = state.get("profile", {})
    user_input = state.get("user_input", "")
    topic = get_main_topic(profile)
    course = profile.get("course", "Java 程序设计")
    weaknesses = to_text(profile.get("weaknesses"))
    knowledge_base = profile.get("knowledge_base", "未知")
    wrong_questions = profile.get("wrong_questions", [])
    tasks = profile.get("tasks", {})
    learning_path = profile.get("learning_path", {})

    wrong_text = ""
    if wrong_questions:
        wrong_items = []
        for wq in wrong_questions[:3]:
            wrong_items.append(f"  题目：{wq.get('question')}，你的答案：{wq.get('userAnswer')}，正确答案：{wq.get('correctAnswer')}")
        wrong_text = "近期错题：\n" + "\n".join(wrong_items)

    task_text = ""
    if tasks:
        task_text = f"学习进度：已完成 {tasks.get('completed', 0)}/{tasks.get('total', 0)} 个任务（{tasks.get('progress', 0)}%）"

    prompt = f"""你是高校课程个性化讲解智能体。请根据学生画像进行针对性讲解。

【范围限定】仅限 JavaSE 基础内容。如果学生问及 JavaEE、Spring Boot、Spring Cloud、MyBatis 等超出 JavaSE 范围的内容，请回复"该知识点属于企业级框架范围，不在当前 JavaSE 课程中"并引导回到 JavaSE 基础。

当前问题：{user_input}
知识点：{topic}
课程：{course}
学生基础：{knowledge_base}
薄弱点：{weaknesses or '暂无记录'}

{wrong_text}

{task_text}

要求：
1. 围绕学生当前问题展开讲解
2. 重点突破薄弱点
3. 结合错题进行针对性分析
4. 用通俗语言，配代码示例
5. 给出下一步学习建议

请直接返回 JSON 格式，不要额外说明：
{{"type":"explain","content":"讲解内容（支持 Markdown）","weaknesses_focus":["薄弱点1","薄弱点2"],"suggestion":"下一步建议"}}"""

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
                result = {"type": "explain", "content": llm_text, "weaknesses_focus": [], "suggestion": ""}
        else:
            result = {"type": "explain", "content": llm_text, "weaknesses_focus": [], "suggestion": ""}

    return {"final_answer": json.dumps(result, ensure_ascii=False)}
