from school_agent.services.llm_client import call_llm
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import compact_text, get_main_topic, to_text


def explain_agent(state: dict) -> dict:
    """个性化讲解——根据画像分层教学"""
    profile = state.get("profile", {})
    user_input = state.get("user_input", "")
    context = compact_text(state.get("retrieved_context", ""), max_chars=1000)

    topic = get_main_topic(profile)
    weaknesses = profile.get("weaknesses", [])
    if isinstance(weaknesses, str):
        weaknesses = [weaknesses] if weaknesses else []

    # 从画像提取真实分层依据
    knowledge_base = profile.get("knowledge_base", "")
    pace = profile.get("pace", "")
    preferences = profile.get("resource_preference", [])
    
    # 构建分层指令
    level_instruction = ""
    if knowledge_base and ("差" in knowledge_base or "弱" in knowledge_base or "未知" in knowledge_base):
        level_instruction = "学生基础薄弱，请从最基础的概念讲起，多用比喻和生活例子，避免跳步。"
    elif knowledge_base:
        level_instruction = f"学生基础：{knowledge_base}。请在此基础上有针对性地讲解，适当拓展。"
    
    if pace:
        level_instruction += f" 学习节奏：{pace}。"

    if weaknesses:
        level_instruction += f" 当前薄弱点：{', '.join(weaknesses)}。请重点讲解这些内容。"

    if preferences:
        level_instruction += f" 学生偏好：{', '.join(preferences)}。"

    prompt = f"""你是高校课程个性化讲解智能体。
请根据学生画像和课程知识库，对知识点进行结构化讲解。

当前问题：{user_input}
知识点：{topic}

学生画像：
- 课程：{profile.get('course', 'Java 程序设计')}
- 已有基础：{knowledge_base or '暂无记录'}
- 薄弱点：{', '.join(weaknesses) if weaknesses else '暂无记录'}
- 学习偏好：{', '.join(preferences) if preferences else '暂无记录'}

知识库参考：
{context or '暂无知识库内容'}

教学要求：
{level_instruction if level_instruction else '根据学生当前问题进行清晰的结构化讲解。'}
1. 围绕学生当前问题展开，不要偏离。
2. 用通俗语言解释核心概念，配代码示例。
3. 如果有薄弱点记录，重点突破这些薄弱点。
4. 给出下一步学习建议。
"""
    llm_text = call_llm(prompt)

    answer = f"""## 个性化讲解：{topic}

### 1. 当前画像依据

- 课程：{profile.get("course", "")}
- 知识基础：{knowledge_base or "暂无记录"}
- 薄弱点：{", ".join(weaknesses) if weaknesses else "暂无记录"}
- 学习偏好：{", ".join(preferences) if preferences else "暂无记录"}

### 2. 讲解内容

{llm_text}

### 3. 知识库参考

{context or "当前未检索到足够知识库内容，建议补充课程资料。"}

### 4. 下一步建议

请根据讲解内容，先复述一遍核心概念，再完成相关练习题巩固。
""".strip()

    return {
        "final_answer": answer,
        "agent_outputs": merge_agent_output(
            state,
            "explain_agent",
            {"status": "success", "topic": topic},
        ),
    }
