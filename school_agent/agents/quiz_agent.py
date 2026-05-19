import random
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import get_main_topic, to_text

def _get_difficulty_level(profile: dict) -> str:
    knowledge = profile.get("knowledge_base", "").lower()
    if "零基础" in knowledge or "没有基础" in knowledge:
        return "基础"
    if "熟练" in knowledge or "精通" in knowledge:
        return "提高"
    return "中等"

def _adjust_question_type(cognitive_style: str, topic: str, weaknesses: list) -> list:
    base_types = ["choice", "judge"]
    if "例子驱动" in cognitive_style:
        base_types.extend(["code_reading", "choice"])
    if "逻辑推导" in cognitive_style:
        base_types.extend(["short_answer", "practice"])
    if any("边界" in w for w in weaknesses):
        base_types.append("code_reading")
    if any("递归" in w for w in weaknesses):
        base_types.append("practice")
    seen = set()
    unique = []
    for t in base_types:
        if t not in seen:
            seen.add(t)
            unique.append(t)
    target = ["choice", "judge", "short_answer", "code_reading", "practice"]
    for t in target:
        if t not in unique:
            unique.append(t)
    return unique[:5]

def _build_dynamic_quiz(topic: str, profile: dict) -> list:
    weaknesses = profile.get("weaknesses", [])
    weaknesses_text = to_text(weaknesses)
    cognitive_style = profile.get("cognitive_style", "偏好结构化讲解")
    difficulty = _get_difficulty_level(profile)

    templates = {
        "choice": {
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
        "judge": {
            "question": f"判断：只要能背出 {topic} 的定义，就一定能解决相关编程题。",
            "answer": "错误",
            "analysis": "编程题还需要理解边界条件、输入输出和具体实现过程。",
        },
        "short_answer": {
            "question": f"请用自己的话解释 {topic} 解决了什么问题。",
            "answer": f"{topic} 的价值在于帮助我们组织或处理特定类型的数据与过程。",
            "analysis": "这道题检查是否真正理解概念，而不是机械背诵。",
        },
        "code_reading": {
            "question": f"阅读一段与 {topic} 相关的代码，指出可能遗漏的边界条件。",
            "answer": "需要检查空输入、极端规模、递归终止条件或索引越界等情况。",
            "analysis": "代码阅读题用于暴露常见错误模式。",
        },
        "practice": {
            "question": f"请写一个代码示例（Python/Java），演示 {topic} 的核心思想。",
            "answer": "答案应包含核心逻辑、注释说明和一个简单测试用例。",
            "analysis": "实践题用于把概念迁移到代码实现中。",
        },
    }

    if difficulty == "基础":
        templates["choice"]["options"] = ["基本定义", "死记硬背", "直接做题", "忽略概念"]
        templates["practice"]["question"] = f"请写一个最简单的代码片段，展示 {topic} 的基本用法。"
    elif difficulty == "提高":
        templates["practice"]["question"] = f"请实现一个包含异常处理和边界测试的 {topic} 相关功能。"

    chosen_types = _adjust_question_type(cognitive_style, topic, weaknesses)
    quiz = []
    for qtype in chosen_types[:5]:
        tpl = templates.get(qtype, templates["choice"])
        quiz.append({
            "type": qtype,
            "difficulty": difficulty,
            "question": tpl["question"],
            "options": tpl.get("options"),
            "answer": tpl["answer"],
            "analysis": tpl["analysis"],
        })
    return quiz

# ---------- 兼容 resource_agent 的静态题目生成 ----------
def _build_quiz(topic: str, weaknesses_text: str):
    return [
        {"type": "choice", "difficulty": "基础", "question": f"学习 {topic} 时，最应该先掌握哪一项？", "options": ["基本定义、使用场景和常见错误", "直接背诵全部代码", "跳过概念直接做难题", "只看答案不练习"], "answer": "基本定义、使用场景和常见错误", "analysis": f"当前薄弱点包括：{weaknesses_text}，应先建立概念框架。"},
        {"type": "judge", "difficulty": "基础", "question": f"判断：只要能背出 {topic} 的定义，就一定能解决相关编程题。", "answer": "错误", "analysis": "编程题还需要理解边界条件、输入输出和具体实现过程。"},
        {"type": "short_answer", "difficulty": "中等", "question": f"请用自己的话解释 {topic} 解决了什么问题。", "answer": f"{topic} 的价值在于帮助我们组织或处理特定类型的数据与过程。", "analysis": "这道题检查是否真正理解概念，而不是机械背诵。"},
        {"type": "code_reading", "difficulty": "中等", "question": f"阅读一段与 {topic} 相关的代码，指出可能遗漏的边界条件。", "answer": "需要检查空输入、极端规模、递归终止条件或索引越界等情况。", "analysis": "代码阅读题用于暴露常见错误模式。"},
        {"type": "practice", "difficulty": "提高", "question": f"请写一个代码示例（Python/Java），演示 {topic} 的核心思想。", "answer": "答案应包含核心逻辑、注释说明和一个简单测试用例。", "analysis": "实践题用于把概念迁移到代码实现中。"},
    ]

def quiz_agent(state: dict) -> dict:
    profile = state.get("profile", {})
    topic = get_main_topic(profile)
    quiz = _build_dynamic_quiz(topic, profile)

    lines = [f"## 个性化练习题：{topic}", ""]
    for idx, item in enumerate(quiz, start=1):
        lines.append(f"### {idx}. {item['type']}｜{item['difficulty']}")
        lines.append(f"**题目：** {item['question']}")
        if item.get("options"):
            lines.append("")
            for opt in item["options"]:
                lines.append(f"- {opt}")
        lines.append("")
        lines.append(f"**参考答案：** {item['answer']}")
        lines.append(f"**解析：** {item['analysis']}")
        lines.append("")

    return {
        "final_answer": "\n".join(lines),
        "agent_outputs": merge_agent_output(state, "quiz_agent", {"status": "success", "quiz_count": len(quiz)}),
    }