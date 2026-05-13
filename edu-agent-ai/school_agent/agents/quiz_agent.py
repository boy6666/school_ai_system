from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import get_main_topic, to_text


def _build_quiz(topic: str, weaknesses_text: str):
    return [
        {
            "type": "choice",
            "difficulty": "基础",
            "question": f"学习 {topic} 时，最应该先掌握哪一项？",
            "options": [
                "基本定义、使用场景和常见错误",
                "直接背诵全部代码",
                "跳过概念直接做难题",
                "只看答案不练习",
            ],
            "answer": "基本定义、使用场景和常见错误",
            "analysis": f"当前薄弱点包括：{weaknesses_text}，应先建立概念框架。",
        },
        {
            "type": "judge",
            "difficulty": "基础",
            "question": f"判断：只要能背出 {topic} 的定义，就一定能解决相关编程题。",
            "answer": "错误",
            "analysis": "编程题还需要理解边界条件、输入输出和具体实现过程。",
        },
        {
            "type": "short_answer",
            "difficulty": "中等",
            "question": f"请用自己的话解释 {topic} 解决了什么问题。",
            "answer": f"{topic} 的价值在于帮助我们组织或处理特定类型的数据与过程。",
            "analysis": "这道题检查是否真正理解概念，而不是机械背诵。",
        },
        {
            "type": "code_reading",
            "difficulty": "中等",
            "question": f"阅读一段与 {topic} 相关的代码，指出可能遗漏的边界条件。",
            "answer": "需要检查空输入、极端规模、递归终止条件或索引越界等情况。",
            "analysis": "代码阅读题用于暴露常见错误模式。",
        },
        {
            "type": "practice",
            "difficulty": "提高",
            "question": f"请写一个 Java 小例子，演示 {topic} 的核心思想。",
            "answer": "答案应包含核心逻辑、注释说明和一个简单测试用例。",
            "analysis": "实践题用于把概念迁移到代码实现中。",
        },
    ]


def quiz_agent(state: dict) -> dict:
    """出题与解析智能体范例。"""
    profile = state.get("profile", {})
    topic = get_main_topic(profile)
    weaknesses_text = to_text(profile.get("weaknesses"))
    quiz = _build_quiz(topic, weaknesses_text)

    lines = [f"## 个性化练习题：{topic}", ""]
    for index, item in enumerate(quiz, start=1):
        lines.append(f"### {index}. {item['type']}｜{item['difficulty']}")
        lines.append(f"**题目：** {item['question']}")
        if item.get("options"):
            lines.append("")
            for option in item["options"]:
                lines.append(f"- {option}")
        lines.append("")
        lines.append(f"**参考答案：** {item['answer']}")
        lines.append(f"**解析：** {item['analysis']}")
        lines.append("")

    return {
        "final_answer": "\n".join(lines),
        "agent_outputs": merge_agent_output(
            state,
            "quiz_agent",
            {"status": "success", "quiz_count": len(quiz), "quiz": quiz},
        ),
    }
