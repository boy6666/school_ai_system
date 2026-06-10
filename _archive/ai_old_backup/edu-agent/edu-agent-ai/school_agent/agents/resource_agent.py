from school_agent.agents.quiz_agent import _build_quiz
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import compact_text, get_main_topic, to_text
from school_agent.services.llm_client import call_llm


def resource_agent(state: dict) -> dict:
    """资源生成智能体——调用LLM生成5种个性化资源"""
    profile = state.get("profile", {})
    topic = get_main_topic(profile)
    course = profile.get("course", "Java 程序设计")
    context = compact_text(state.get("retrieved_context", ""), max_chars=900)
    weaknesses = to_text(profile.get("weaknesses"))
    preference = to_text(profile.get("resource_preference"))
    kb = profile.get("knowledge_base", "未知")

    # ===== 1. 讲解文档 — LLM 生成 =====
    course_doc = call_llm(
        f"你是Java课程讲解专家。学生画像：课程={course}, 基础={kb}, 薄弱点={weaknesses}。"
        f"请为知识点「{topic}」生成一篇800字结构化讲解文档。要求："
        f"(1)通俗解释核心概念 (2)配2个可运行Java代码示例 (3)列出3-5个易错点 (4)给出学习建议。"
        f"参考知识库：{context or '无'}。直接返回纯文本，不要使用Markdown格式，不要额外说明。"
    )
    if not course_doc or len(course_doc) < 50:
        course_doc = f"# {topic} 讲解文档\n\nAI生成暂不可用，请稍后重试。"

    # ===== 2. 思维导图 — LLM 生成 =====
    mindmap = call_llm(
        f"请为知识点「{topic}」生成Mermaid思维导图代码。要求："
        f"(1)使用graph TD布局 (2)5-8个主节点，各挂2-3子节点 (3)节点用中文 (4)体现知识逻辑层次。"
        f"只返回以graph TD开头的Mermaid代码，不要额外说明。"
    )
    if not mindmap or not mindmap.strip().startswith("graph"):
        mindmap = f"""graph TD
A[{topic}] --> B[核心概念]
A --> C[应用场景]
A --> D[易错点]
A --> E[进阶延伸]
B --> B1[定义]
B --> B2[关键特性]
C --> C1[代码示例]
C --> C2[实际项目]
D --> D1[常见误区]
D --> D2[边界问题]
E --> E1[相关知识点]
E --> E2[推荐阅读]
"""

    # ===== 3. 练习题目 — 模板（不变）=====
    quiz = _build_quiz(topic, weaknesses)

    # ===== 4. 拓展阅读 — LLM 生成 =====
    extended_reading = call_llm(
        f"请为学习「{topic}」的学生生成拓展阅读材料(约400字)。要求："
        f"(1)推荐2-3个与{topic}相关的进阶概念 (2)列出实际项目应用场景 (3)推荐进一步学习资源方向。"
        f"直接返回纯文本，不要使用Markdown格式，不要额外说明。"
    )
    if not extended_reading or len(extended_reading) < 30:
        extended_reading = f"# 拓展阅读：{topic}\n\n1. 官方文档：查阅Java {topic}相关API\n2. 实战项目：在项目中练习{topic}\n3. 进阶：了解{topic}底层实现"

    # ===== 5. 代码案例 — 模板（不变）=====
    code_practice = f"""public class {topic.replace(" ", "")}Practice {{
    public static void main(String[] args) {{
        System.out.println("学习主题：{topic}");
        System.out.println("请你根据下方注释补充代码逻辑：");
        // TODO: 实现 {topic} 的核心操作
        // TODO: 添加边界条件检查
        // TODO: 测试并输出结果
    }}
}}
"""

    # 组装资源清单
    resources_meta = [
        {"title": f"{topic} - 讲解文档", "type": "文档", "file": "course_doc.md"},
        {"title": f"{topic} - 思维导图", "type": "思维导图", "file": "mindmap.mmd"},
        {"title": f"{topic} - 练习题目", "type": "题库", "file": "quiz.json"},
        {"title": f"{topic} - 拓展阅读", "type": "拓展阅读", "file": "extended_reading.md"},
        {"title": f"{topic} - 代码案例", "type": "代码案例", "file": "code_practice.java"},
    ]

    return {
        "course_doc": course_doc,
        "mindmap": mindmap,
        "quiz": quiz,
        "extended_reading": extended_reading,
        "code_practice": code_practice,
        "resources_meta": resources_meta,
        "resources": {
            "course_doc": course_doc,
            "mindmap": mindmap,
            "quiz": quiz,
            "extended_reading": extended_reading,
            "code_practice": code_practice,
        },
        "agent_outputs": merge_agent_output(
            state,
            "resource_agent",
            {"status": "success", "topic": topic, "generated_count": 5},
        ),
    }