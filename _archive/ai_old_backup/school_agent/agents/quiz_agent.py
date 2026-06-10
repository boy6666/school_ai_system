from school_agent.services.llm_client import call_llm
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import compact_text, get_main_topic, to_text

QUIZ_SYSTEM_PROMPT = """你是编程课程出题专家。请根据学生的知识薄弱点和知识库内容，生成有针对性的练习题。

## 出题要求
- 题目必须与学生的薄弱点和当前学习主题相关
- 难度递进：基础→进阶→提高
- 每道题都要有答案解析，指出易错点
- 题目类型可以多样化：选择题、判断题、代码阅读题、简答题、编码实践题
- 如果知识库提供了相关内容，优先基于知识库出题

## 输出格式
严格返回JSON：
{
  "quiz": [
    {
      "type": "choice",
      "difficulty": "基础",
      "question": "题目标题",
      "options": ["A. xxx", "B. xxx", "C. xxx", "D. xxx"],
      "answer": "B. xxx",
      "analysis": "解析说明，包含易错提示"
    }
  ]
}"""


def _build_fallback_quiz(topic: str, weaknesses_text: str):
    """LLM 不可用时的兜底模板。"""
    return [
        {
            "type": "choice",
            "difficulty": "基础",
            "question": f"学习 {topic} 时，最应该先掌握哪一项？",
            "options": ["基本定义和使用场景", "直接背诵代码", "跳过概念做难题", "只看答案不练习"],
            "answer": "基本定义和使用场景",
            "analysis": f"当前薄弱点：{weaknesses_text}，应先建立概念框架。",
        },
        {
            "type": "short_answer",
            "difficulty": "中等",
            "question": f"请用自己的话解释 {topic} 解决了什么问题，并举一个实际场景。",
            "answer": f"{topic} 的核心价值在于解决特定类型的数据组织/处理问题。",
            "analysis": "检查是否真正理解概念而非机械记忆。",
        },
        {
            "type": "code_practice",
            "difficulty": "提高",
            "question": f"请用 Java 写一个最小示例，演示 {topic} 的典型用法，包含边界条件处理。",
            "answer": "代码应包含核心逻辑、注释说明和边界测试。",
            "analysis": "实践题用于把概念迁移到代码实现中。",
        },
    ]


def quiz_agent(state: dict) -> dict:
    """出题与解析智能体——LLM 驱动，结合知识库和画像生成个性化练习题。"""
    profile = state.get("profile", {})
    user_input = state.get("user_input", "")
    topic = get_main_topic(profile)
    context = compact_text(state.get("retrieved_context", ""), max_chars=2000)
    weaknesses = profile.get("weaknesses", [])
    mistake_patterns = profile.get("mistake_patterns", [])
    overall_type = profile.get("overall_type", "")

    quiz = None
    try:
        prompt = f"""## 学生情况
- 当前学习主题：{topic}
- 薄弱点：{', '.join(weaknesses[:5]) if weaknesses else '暂无'}
- 易错模式：{', '.join(mistake_patterns[:3]) if mistake_patterns else '暂无'}
- 学生类型：{overall_type or '未知'}

## 知识库参考
{context if context else '暂无相关知识库内容，请根据通用编程知识出题'}

## 学生请求
{user_input}

请生成3-5道与当前主题和薄弱点紧密相关的练习题，难度递进。"""
        import json as _json
        response = call_llm(prompt, system=QUIZ_SYSTEM_PROMPT)
        # 解析 JSON
        text = response.strip()
        if text.startswith("```"):
            lines = text.split("\n")
            text = "\n".join(lines[1:-1] if lines[-1].strip() == "```" else lines[1:])
        start = text.find("{")
        end = text.rfind("}") + 1
        if start != -1 and end > start:
            result = _json.loads(text[start:end])
            quiz = result.get("quiz", [])
    except Exception:
        pass

    if not quiz:
        quiz = _build_fallback_quiz(topic, to_text(weaknesses))

    lines = [f"## 个性化练习题：{topic}", ""]
    for index, item in enumerate(quiz, start=1):
        qtype = item.get("type", "题")
        diff = item.get("difficulty", "")
        lines.append(f"### {index}. {qtype}｜{diff}")
        lines.append(f"**题目：** {item.get('question', '')}")
        if item.get("options"):
            lines.append("")
            for option in item["options"]:
                lines.append(f"- {option}")
        lines.append("")
        lines.append(f"**参考答案：** {item.get('answer', '')}")
        lines.append(f"**解析：** {item.get('analysis', '')}")
        lines.append("")

    return {
        "final_answer": "\n".join(lines),
        "agent_outputs": merge_agent_output(
            state,
            "quiz_agent",
            {"status": "success", "quiz_count": len(quiz), "quiz": quiz},
        ),
    }
