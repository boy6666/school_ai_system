import json
from school_agent.services.llm_client import call_llm
from school_agent.utils.text_utils import get_main_topic, to_text


def tutor_agent(state: dict) -> dict:
    """智能辅导 — 根据画像和错题进行分步骤答疑"""
    profile = state.get("profile", {})
    user_input = state.get("user_input", "")
    topic = get_main_topic(profile)
    course = profile.get("course", "Java 程序设计")
    weaknesses = to_text(profile.get("weaknesses"))
    knowledge_base = profile.get("knowledge_base", "未知")
    wrong_questions = profile.get("wrong_questions", [])
    tasks = profile.get("tasks", {})
    context = state.get("retrieved_context", "")

    wrong_text = ""
    if wrong_questions:
        items = [f"  题目：{wq.get('question')}，你的答案：{wq.get('userAnswer')}，正确答案：{wq.get('correctAnswer')}，讲解：{wq.get('explanation', '')}" for wq in wrong_questions[:3]]
        wrong_text = "相关错题记录：\n" + "\n".join(items)

    task_text = ""
    if tasks:
        task_text = f"学习进度：已完成 {tasks.get('completed', 0)}/{tasks.get('total', 0)} 个任务"

    prompt = f"""你是高校课程智能辅导导师。请根据学生的问题和画像进行分步骤辅导。

学生问题：{user_input}
知识点：{topic}
课程：{course}
学生基础：{knowledge_base}
薄弱点：{weaknesses or '暂无记录'}

{wrong_text}
{task_text}

知识库参考：
{context[:500] if context else '暂无'}

要求：
1. 先分析学生可能卡在什么地方
2. 分步骤引导解决问题（不要直接给答案）
3. 结合薄弱点和错题进行针对性提示
4. 最后给出总结和下一步练习建议

直接返回 JSON，不要额外说明：
{{"type":"tutor","steps":["步骤1：...","步骤2：..."],"summary":"总结和建议","weaknesses_touched":["涉及薄弱点"]}}"""

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
                result = {"type": "tutor", "steps": [llm_text], "summary": "", "weaknesses_touched": []}
        else:
            result = {"type": "tutor", "steps": [llm_text], "summary": "", "weaknesses_touched": []}

    return {"final_answer": json.dumps(result, ensure_ascii=False)}
