from school_agent.services.llm_client import call_llm
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import compact_text, get_main_topic, to_text


def explain_agent(state: dict) -> dict:
    """个性化讲解智能体范例。"""
    profile = state.get("profile", {})
    topic = get_main_topic(profile)
    context = compact_text(state.get("retrieved_context", ""), max_chars=1000)

    prompt = f"""
你是高校课程个性化讲解智能体。
请结合学生画像和课程知识库，对知识点进行结构化讲解。

学生画像：
{profile}

知识点：
{topic}

知识库参考：
{context}

要求：
1. 先用通俗语言解释概念。
2. 再说明典型使用场景。
3. 结合学生薄弱点列出易错点。
4. 给出下一步学习建议。
"""
    llm_text = call_llm(prompt)

    answer = f"""
## 个性化讲解：{topic}

### 1. 当前画像依据

- 课程：{profile.get("course")}
- 知识基础：{profile.get("knowledge_base")}
- 薄弱点：{to_text(profile.get("weaknesses"))}
- 学习偏好：{to_text(profile.get("resource_preference"))}

### 2. 讲解内容

{llm_text}

### 3. 知识库参考

{context or "当前未检索到足够知识库内容，建议补充课程资料。"}

### 4. 下一步建议

先复述一遍 `{topic}` 的定义，再完成 2 道基础题和 1 个代码小练习。
""".strip()

    return {
        "final_answer": answer,
        "agent_outputs": merge_agent_output(
            state,
            "explain_agent",
            {"status": "success", "topic": topic},
        ),
    }
