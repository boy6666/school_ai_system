from school_agent.services.llm_client import call_llm
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import compact_text, get_main_topic, to_text

def explain_agent(state: dict) -> dict:
    profile = state.get("profile", {})
    topic = get_main_topic(profile)   # 强制使用画像中的主题
    context = compact_text(state.get("retrieved_context", ""), max_chars=800)

    prompt = f"""
你是高校课程个性化讲解智能体。
请直接围绕学生询问的 **{topic}** 进行结构化讲解，**不要跑题**。

学生画像：
{profile}

要求：
1. 用通俗语言解释 {topic} 的核心概念。
2. 说明典型使用场景。
3. 结合学生薄弱点列出易错点。
4. 给出下一步学习建议（包括练习题和代码实践）。
5. 不要提及与 {topic} 无关的知识，如“树结构”。

知识库参考（仅作补充，非必须）：
{context}
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
{context or "当前未检索到足够内容，以上讲解基于通用知识。"}

### 4. 下一步建议
先复述一遍 `{topic}` 的核心定义，再完成 2 道基础题和 1 个代码小练习。
""".strip()

    return {
        "final_answer": answer,
        "agent_outputs": merge_agent_output(state, "explain_agent", {"status": "success", "topic": topic}),
    }