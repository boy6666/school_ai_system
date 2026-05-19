# school_agent/agents/quiz_agent.py
import json
from school_agent.services.llm_client import call_llm
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import get_main_topic, to_text

def quiz_agent(state: dict) -> dict:
    """AI 智能体版出题：完全由大模型动态生成个性化练习题"""
    profile = state.get("profile", {})
    topic = get_main_topic(profile)
    weaknesses = to_text(profile.get("weaknesses", []))
    cognitive_style = profile.get("cognitive_style", "偏好结构化讲解")
    knowledge_base = profile.get("knowledge_base", "基础一般")
    
    prompt = f"""你是一个智能出题老师。请根据以下学生画像，生成 3~5 道个性化的练习题（覆盖不同题型和难度）。

学生画像：
- 当前知识点：{topic}
- 知识基础：{knowledge_base}
- 认知风格：{cognitive_style}
- 薄弱点：{weaknesses}

要求：
1. 每道题包含：type（choice/judge/short_answer/code_reading/practice）、difficulty（基础/中等/提高）、question、options（仅选择题需要）、answer、analysis。
2. 题目要围绕 {topic}，并结合学生的薄弱点设计易错陷阱。
3. 输出格式：一个 JSON 数组。
只输出 JSON 数组，不要有其他说明。
"""
    response = call_llm(prompt)
    try:
        cleaned = response.strip().replace("```json", "").replace("```", "").strip()
        quiz = json.loads(cleaned)
        if not isinstance(quiz, list):
            quiz = []
    except:
        quiz = []
    
    # 如果生成失败，使用一个简单的降级题目（不依赖静态模板）
    if not quiz:
        quiz = [{
            "type": "choice",
            "difficulty": "基础",
            "question": f"关于 {topic}，以下哪项描述最准确？",
            "options": ["选项A", "选项B", "选项C", "选项D"],
            "answer": "选项A",
            "analysis": "请参考课程内容学习。"
        }]
    
    lines = [f"## 个性化练习题：{topic}", ""]
    for idx, item in enumerate(quiz, start=1):
        lines.append(f"### {idx}. {item['type']}｜{item.get('difficulty', '中等')}")
        lines.append(f"**题目：** {item['question']}")
        if item.get("options"):
            lines.append("")
            for opt in item["options"]:
                lines.append(f"- {opt}")
        lines.append("")
        lines.append(f"**参考答案：** {item.get('answer', '无')}")
        lines.append(f"**解析：** {item.get('analysis', '无')}")
        lines.append("")
    
    return {
        "final_answer": "\n".join(lines),
        "agent_outputs": merge_agent_output(state, "quiz_agent", {"status": "success", "quiz_count": len(quiz)}),
    }