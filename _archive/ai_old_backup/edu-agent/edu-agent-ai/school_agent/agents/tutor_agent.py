from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import compact_text, get_main_topic, to_text


def tutor_agent(state: dict) -> dict:
    """智能辅导智能体范例。"""
    profile = state.get("profile", {})
    topic = get_main_topic(profile)
    context = compact_text(state.get("retrieved_context", ""), max_chars=800)

    answer = f"""
## 智能辅导：{topic}

你当前的问题是：

> {state.get("user_input", "")}

结合你的画像，你可能卡在以下位置：

- 薄弱点：{to_text(profile.get("weaknesses"))}
- 常见错误：{to_text(profile.get("mistake_patterns"))}

### 分步骤解决

1. 先说清 `{topic}` 解决什么问题。
2. 再画出输入、处理过程和输出。
3. 最后用一个最小代码例子验证理解。

### 知识库参考

{context or "当前知识库没有命中足够内容。"}

### 给你的下一步任务

请用一句话解释 `{topic}` 的核心作用。如果解释不清，系统会继续把它标记为薄弱点，并降低后续资源难度。
""".strip()

    return {
        "final_answer": answer,
        "agent_outputs": merge_agent_output(
            state,
            "tutor_agent",
            {"status": "success", "topic": topic},
        ),
    }
