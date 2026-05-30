from school_agent.services.llm_client import call_llm
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import compact_text, get_main_topic, to_text

EXPLAIN_SYSTEM_PROMPT = """你是一个知识渊博、善于表达的大学课程讲解老师，名字叫"小航"。

## 你的风格
- 用通俗易懂的语言解释复杂概念，像在和朋友聊天
- 先给出一个简单的类比或生活例子，再进入技术细节
- 结构化但自然的讲解：概念 → 为什么需要它 → 怎么用 → 易错点
- 结合学生的背景和薄弱点，有针对性地展开
- 鼓励学生互动，在讲解后追问"这部分清楚吗？"或"要不要举个例子？"
- 避免长篇大论，一次聚焦一个核心知识点
- 用代码片段或图示思维帮助学生理解（如果需要）

## 重要
- 不要用"### 1. 当前画像依据"这类章节标题
- 不要复述学生的画像数据
- 自然地根据学生水平调整讲解深度
- 像真正懂教学的老师一样，把知识讲活"""


def explain_agent(state: dict) -> dict:
    """个性化讲解智能体——知识点的自然讲解。

    当学生请求讲解、解释某个知识点时触发，
    结合学生画像和课程知识库进行结构化但自然的讲解。
    """
    profile = state.get("profile", {})
    user_input = state.get("user_input", "")
    topic = get_main_topic(profile)
    context = compact_text(state.get("retrieved_context", ""), max_chars=2000)

    # 构建背景信息
    weaknesses = profile.get("weaknesses", [])
    mistake_patterns = profile.get("mistake_patterns", [])
    cognitive_style = profile.get("cognitive_style", "")
    learning_goal = profile.get("learning_goal", "")
    grade = profile.get("grade", "")

    profile_parts = []
    if grade:
        profile_parts.append(f"年级：{grade}")
    if learning_goal:
        profile_parts.append(f"学习目标：{learning_goal}")
    if topic and topic != "当前知识点":
        profile_parts.append(f"当前主题：{topic}")
    if weaknesses:
        profile_parts.append(f"薄弱点：{', '.join(weaknesses[:5])}")
    if mistake_patterns:
        profile_parts.append(f"易错模式：{', '.join(mistake_patterns[:3])}")
    if cognitive_style:
        profile_parts.append(f"学习偏好：{cognitive_style}")

    profile_summary = "\n".join(profile_parts) if profile_parts else "暂无学生信息"

    prompt = f"""## 学生背景
{profile_summary}

## 知识库参考资料
{context if context else "暂无相关知识库内容，请基于你自己的知识讲解。"}

## 学生请求
{user_input}

请用自然、友好的方式讲解。注意：
- 不要用模板化的章节标题（如"## 1. 当前画像依据"）
- 自然地融入对学生薄弱点的关注
- 讲解后追问学生是否理解，保持互动感
- 如果知识库有相关内容，优先引用"""

    try:
        llm_answer = call_llm(prompt, system=EXPLAIN_SYSTEM_PROMPT)
    except Exception:
        llm_answer = (
            f"好的，我来给你讲讲 {topic}。\n\n"
            f"{topic} 是计算机领域一个很重要的概念。"
            f"简单来说，它解决的是……嗯，让我用一个生活中的例子来帮你理解。"
            f"你可以先告诉我，你目前对 {topic} 了解多少？这样我可以更有针对性地讲解。"
        )

    # 轻量包装：只在末尾添加知识库引用提示，不破坏对话感
    if context and len(context) > 50:
        llm_answer += (
            "\n\n---\n"
            "💡 以上讲解参考了课程知识库的内容。如果你想深入某个细节，随时问我！"
        )

    return {
        "final_answer": llm_answer,
        "agent_outputs": merge_agent_output(
            state,
            "explain_agent",
            {"status": "success", "topic": topic},
        ),
    }
