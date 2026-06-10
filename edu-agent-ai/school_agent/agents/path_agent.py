import json
from school_agent.services.llm_client import call_llm
from school_agent.utils.text_utils import get_main_topic, to_text


def path_agent(state: dict) -> dict:
    """学习路径规划 — 根据画像生成个性化学习路径"""
    profile = state.get("profile", {})
    topic = get_main_topic(profile)
    course = profile.get("course", "Java 程序设计")
    weaknesses = to_text(profile.get("weaknesses"))
    knowledge_base = profile.get("knowledge_base", "未知")
    tasks = profile.get("tasks", {})

    task_text = ""
    if tasks:
        task_text = f"当前进度：已完成 {tasks.get('completed', 0)}/{tasks.get('total', 0)} 个任务（{tasks.get('progress', 0)}%）"

    prompt = f"""你是高校课程学习路径规划专家。根据学生画像生成个性化学习路径。

课程：{course}
知识点：{topic}
学生基础：{knowledge_base}
薄弱点：{weaknesses or '暂无记录'}
{task_text}

请生成 3-5 步学习路径，每步包含：步骤名、建议学习内容、推荐资源类型、预计时长（分钟）。
要优先安排薄弱点的学习内容。

直接返回 JSON，不要额外说明：
{{"learning_path":[{{"step":1,"title":"步骤名","content":"学习内容","resource":"推荐资源类型","duration":45,"reason":"推荐理由"}}],"total_hours":0,"summary":"整体规划说明"}}"""

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
                result = {"learning_path": [], "total_hours": 0, "summary": llm_text[:200]}
        else:
            result = {"learning_path": [], "total_hours": 0, "summary": llm_text[:200]}

    return {
        "learning_path": result.get("learning_path", []),
        "final_answer": json.dumps(result, ensure_ascii=False),
    }
