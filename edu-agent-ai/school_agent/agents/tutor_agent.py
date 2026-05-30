from school_agent.services.llm_client import call_llm
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import compact_text, get_main_topic, to_text

TUTOR_SYSTEM_PROMPT = """你是一个友善、耐心的大学课程辅导老师，名字叫"小航"。

## 你的风格
- 用自然、口语化的方式与学生对话，像朋友一样交流
- 先理解学生的问题，再用通俗的语言解释
- 鼓励学生思考，而不是直接给答案
- 适当追问，引导学生深入理解
- 如果学生说"不会"或"不懂"，先安抚情绪，再耐心讲解
- 用具体的例子帮助学生理解抽象概念
- 回答简洁有力，不要像在写教科书

## 你需要了解的学生信息
系统会在每次对话中提供学生的画像信息（知识薄弱点、学习偏好等），请据此调整讲解方式和难度。

## 重要
- 不要说"根据你的画像"、"系统显示"这类机械的话
- 自然地融入对学生情况的了解
- 像真正关心学生的老师一样对话"""


def tutor_agent(state: dict) -> dict:
    """智能辅导智能体——答疑、纠错、学习引导。

    当学生提问中包含"不会""不懂""错在哪""为什么错"等信号时触发，
    进行自然的一对一辅导对话。
    """
    profile = state.get("profile", {})
    user_input = state.get("user_input", "")
    topic = get_main_topic(profile)
    context = compact_text(state.get("retrieved_context", ""), max_chars=1500)

    # 构建画像摘要（自然地融入 prompt，不要让 AI 机械地复述）
    weaknesses = profile.get("weaknesses", [])
    mistake_patterns = profile.get("mistake_patterns", [])
    cognitive_style = profile.get("cognitive_style", "")
    learning_goal = profile.get("learning_goal", "")
    grade = profile.get("grade", "")
    overall_type = profile.get("overall_type", "")

    profile_context_parts = []
    if grade:
        profile_context_parts.append(f"学生年级：{grade}")
    if learning_goal:
        profile_context_parts.append(f"学习目标：{learning_goal}")
    if topic and topic != "当前知识点":
        profile_context_parts.append(f"当前学习主题：{topic}")
    if weaknesses:
        profile_context_parts.append(f"已知薄弱点：{', '.join(weaknesses[:5])}")
    if mistake_patterns:
        profile_context_parts.append(f"常见错误模式：{', '.join(mistake_patterns[:3])}")
    if cognitive_style:
        profile_context_parts.append(f"学习偏好：{cognitive_style}")
    if overall_type:
        profile_context_parts.append(f"学生类型：{overall_type}")

    profile_summary = "\n".join(profile_context_parts) if profile_context_parts else "暂无学生画像信息"

    prompt = f"""## 学生背景信息
{profile_summary}

## 知识库参考
{context if context else "暂无相关知识库内容"}

## 学生的问题
{user_input}

请以辅导老师"小航"的身份，自然地回答学生的问题。记住：
- 像朋友聊天一样，不要用模板化的结构
- 如果学生表现出困惑（说"不会""不懂"），先安抚再讲解
- 结合学生已知的薄弱点，在讲解中有针对性地加强
- 适当追问以确认学生是否理解"""

    try:
        llm_answer = call_llm(prompt, system=TUTOR_SYSTEM_PROMPT)
    except Exception:
        llm_answer = (
            f"我理解你在 {topic} 上遇到了困难。"
            f"让我们一起来分析一下这个问题。"
            f"你可以先说说你已经理解的部分，我再帮你补充不清楚的地方。"
        )

    return {
        "final_answer": llm_answer,
        "agent_outputs": merge_agent_output(
            state,
            "tutor_agent",
            {"status": "success", "topic": topic},
        ),
    }
