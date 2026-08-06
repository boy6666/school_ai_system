import json
from school_agent.services.llm_client import call_llm
from school_agent.utils.text_utils import get_main_topic, to_text


def resource_agent(state: dict) -> dict:
    """资源生成 — 根据画像生成个性化学习资源包"""
    profile = state.get("profile", {})
    user_input = state.get("user_input", "")
    topic = get_main_topic(profile)
    course = profile.get("course", "Java 程序设计")
    weaknesses = to_text(profile.get("weaknesses"))
    knowledge_base = profile.get("knowledge_base", "未知")
    wrong_questions = profile.get("wrong_questions", [])

    wrong_text = ""
    if wrong_questions:
        items = [f"  题目：{wq.get('question')}（正确答案：{wq.get('correctAnswer')}）" for wq in wrong_questions[:3]]
        wrong_text = "近期错题：\n" + "\n".join(items)

    prompt = f"""你是高校课程学习资源生成专家。根据学生画像生成个性化学习资源包。

【范围限定】仅限 JavaSE 基础内容（语法、面向对象、集合、IO、多线程、反射等），严禁涉及 JavaEE、Spring Boot、Spring Cloud、MyBatis 等企业级框架。

课程：{course}
知识点：{topic}
学生基础：{knowledge_base}
薄弱点：{weaknesses or '暂无记录'}
学生需求：{user_input}

{wrong_text}

请为这个学生生成 3-5 项学习资源推荐，包含：讲解文档、练习题、思维导图、代码案例、拓展阅读等。
每项资源要针对学生的薄弱点和错题进行定制。

直接返回 JSON，不要额外说明：
{{"resources":[{{"title":"资源标题","type":"文档/练习/导图/代码/阅读","description":"内容描述","reason":"针对该学生的推荐理由"}}],"summary":"整体学习建议"}}"""

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
                result = {"resources": [{"title": llm_text[:100], "type": "文档", "description": "", "reason": ""}], "summary": ""}
        else:
            result = {"resources": [{"title": llm_text[:100], "type": "文档", "description": "", "reason": ""}], "summary": ""}

    return {"resources": result.get("resources", []), "final_answer": json.dumps(result, ensure_ascii=False)}
