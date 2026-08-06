import json
from school_agent.services.llm_client import call_llm
from school_agent.utils.text_utils import get_main_topic, to_text


def quiz_agent(state: dict) -> dict:
    """个性化出题 — 根据画像和错题生成针对性练习题"""
    profile = state.get("profile", {})
    topic = get_main_topic(profile)
    course = profile.get("course", "Java 程序设计")
    weaknesses = to_text(profile.get("weaknesses"))
    knowledge_base = profile.get("knowledge_base", "未知")
    wrong_questions = profile.get("wrong_questions", [])

    wrong_text = ""
    if wrong_questions:
        items = [f"  题目：{wq.get('question')}（你选错了，正确答案：{wq.get('correctAnswer')}）" for wq in wrong_questions[:3]]
        wrong_text = "近期错题：\n" + "\n".join(items)

    prompt = f"""你是高校课程出题专家。根据学生画像生成3-5道个性化练习题。

【范围限定】仅限 JavaSE 基础内容出题（语法、面向对象、集合、IO、多线程、反射等），禁止出现 JavaEE、Spring Boot、Spring Cloud、MyBatis 相关题目。

课程：{course}
知识点：{topic}
学生基础：{knowledge_base}
薄弱点：{weaknesses or '暂无记录'}

{wrong_text}

要求：
1. 包含选择题和简答题
2. 重点考察薄弱点
3. 结合错题相关知识点出题
4. 难度匹配学生基础
5. 每道题带详细解析

直接返回 JSON 数组，不要额外说明：
[{{"type":"choice","question":"题目","options":["A","B","C","D"],"answer":"正确答案","explanation":"解析"}},
 {{"type":"short_answer","question":"题目","answer":"参考答案","explanation":"解析"}}]"""

    llm_text = call_llm(prompt)

    try:
        questions = json.loads(llm_text)
        if not isinstance(questions, list):
            questions = [questions]
    except json.JSONDecodeError:
        import re
        match = re.search(r'\[.*\]', llm_text, re.DOTALL)
        if match:
            try:
                questions = json.loads(match.group(0))
            except json.JSONDecodeError:
                questions = [{"type": "choice", "question": llm_text, "options": [], "answer": "", "explanation": ""}]
        else:
            questions = [{"type": "choice", "question": llm_text, "options": [], "answer": "", "explanation": ""}]

    result = {"type": "quiz", "questions": questions}
    return {"final_answer": json.dumps(result, ensure_ascii=False)}
