from school_agent.services.llm_client import call_llm
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import compact_text, get_main_topic, to_text

def explain_agent(state: dict) -> dict:
    profile = state.get("profile", {})
    user_input = state.get("user_input", "")          # 用户具体问题
    topic = get_main_topic(profile)                   # 画像中的主题（备用）
    context = compact_text(state.get("retrieved_context", ""), max_chars=800)

    prompt = f"""
你是学生的专属编程辅导老师。请直接回答学生的问题。

学生的问题：{user_input}

参考信息：
- 学生画像中的知识点：{topic}
- 学生基础：{profile.get("knowledge_base")}
- 薄弱点：{to_text(profile.get("weaknesses"))}

知识库参考内容（可作为素材，但不要被它带偏）：
{context}

要求：
1. **首先直接回答学生的问题**，给出清晰的步骤、代码示例（如果需要）。
2. 如果问题涉及的知识点与 {topic} 相关，可以适当延伸讲解该知识点，但不要跑题到完全不相关的内容。
3. 代码示例必须正确、可运行，并附上必要的解释。
4. 最后给出一个简单的巩固练习或下一步学习建议。

输出格式：直接输出讲解内容，不用 JSON，不用额外标记。
"""
    llm_text = call_llm(prompt)
    answer = f"""
## 回答：{user_input}

### 📌 讲解内容
{llm_text}

### 📚 参考资料
{context[:500] if context else "（无额外参考）"}

### 💡 巩固建议
试着在 IDE 中运行上述代码，并修改输入数据观察输出变化。如有问题可继续提问。
""".strip()

    return {
        "final_answer": answer,
        "agent_outputs": merge_agent_output(state, "explain_agent", {"status": "success", "topic": topic, "user_question": user_input}),
    }