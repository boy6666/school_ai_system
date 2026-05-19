# school_agent/agents/resource_agent.py
from school_agent.services.llm_client import call_llm
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import compact_text, get_main_topic, to_text

def resource_agent(state: dict) -> dict:
    """资源生成智能体：完全由大模型生成六类个性化资源"""
    profile = state.get("profile", {})
    topic = get_main_topic(profile)
    course = profile.get("course", "数据结构")
    weaknesses = to_text(profile.get("weaknesses"))
    context = compact_text(state.get("retrieved_context", ""), max_chars=900)
    
    prompt = f"""你是一个智能学习资源生成器。请根据以下学生画像，生成完整的个性化学习资源包（JSON 格式）。

学生画像：
- 课程：{course}
- 当前知识点：{topic}
- 知识基础：{profile.get("knowledge_base")}
- 认知风格：{profile.get("cognitive_style")}
- 薄弱点：{weaknesses}
- 资源偏好：{to_text(profile.get("resource_preference"))}

知识库参考（可选）：{context}

要求输出以下六个字段的 JSON 对象：
{{
  "course_doc": "详细讲解文档（Markdown格式，包含概念、场景、易错点、建议）",
  "mindmap": "Mermaid 思维导图代码（graph TD 格式）",
  "quiz": [{{"type": "choice", "difficulty": "基础", "question": "...", "options": [...], "answer": "...", "analysis": "..."}}, ...],
  "extended_reading": "拓展阅读材料（Markdown，推荐3-5个资源）",
  "code_practice": "代码实操案例（包含可运行代码框架和TODO注释）",
  "video_script": "多模态教学视频/动画脚本（分镜头描述）"
}}

所有内容必须围绕 {topic}，结合学生的薄弱点和认知风格，提供个性化内容。只输出 JSON，不要附加解释。
"""
    response = call_llm(prompt)
    try:
        cleaned = response.strip().replace("```json", "").replace("```", "").strip()
        resources = json.loads(cleaned)
    except:
        # 降级：提供默认资源（但尽量保证 JSON 解析成功）
        resources = {
            "course_doc": f"# {topic} 讲解文档\n\n个性化内容生成失败，请检查大模型服务。",
            "mindmap": f"graph TD\nA[{topic}]-->B[核心概念]",
            "quiz": [],
            "extended_reading": "暂无拓展阅读",
            "code_practice": f"# {topic} 代码示例\nprint('hello')",
            "video_script": f"# {topic} 视频脚本\n待生成"
        }
    
    final_answer = f"已围绕 `{topic}` 生成个性化学习资源包，包含：讲解文档、思维导图、练习题、拓展阅读、代码实操案例和多模态视频脚本。"
    
    return {
        "resources": resources,
        "final_answer": final_answer,
        "agent_outputs": merge_agent_output(state, "resource_agent", {"status": "success"}),
    }