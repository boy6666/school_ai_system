from school_agent.agents.quiz_agent import _build_fallback_quiz
from school_agent.services.llm_client import call_llm
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import compact_text, get_main_topic, to_text

RESOURCE_DOC_SYSTEM = """你是课程资源生成专家。请根据学生的画像和知识库内容，撰写一份个性化的学习讲解文档。

## 文档要求
- 结构清晰：学生情况 → 学习目标 → 核心讲解 → 易错点 → 进阶建议
- 核心讲解部分要结合学生的薄弱点，有针对性
- 如果知识库提供了相关内容，一定要引用和展开
- 语言自然，像老师在给学生写辅导材料
- 适度使用代码示例（Java）帮助理解"""

RESOURCE_READING_SYSTEM = """你是课程拓展阅读推荐专家。根据学生的当前学习主题和薄弱点，推荐针对性的拓展学习路径。

严格返回JSON：
{
  "items": [
    {"title": "阅读主题", "why": "为什么推荐这个", "focus": "重点关注什么"}
  ]
}"""


def resource_agent(state: dict) -> dict:
    """资源生成智能体——LLM 驱动的个性化资源包。"""
    profile = state.get("profile", {})
    topic = get_main_topic(profile)
    course = profile.get("course", "数据结构")
    user_input = state.get("user_input", "")
    context = compact_text(state.get("retrieved_context", ""), max_chars=2500)
    weaknesses = profile.get("weaknesses", [])
    weaknesses_text = to_text(weaknesses)
    mistake_patterns = profile.get("mistake_patterns", [])
    preference = to_text(profile.get("resource_preference"))
    overall_type = profile.get("overall_type", "")

    # === 讲解文档：LLM 生成 ===
    course_doc = None
    try:
        doc_prompt = f"""## 学生画像
- 课程：{course}
- 学习主题：{topic}
- 薄弱点：{weaknesses_text or '暂无'}
- 易错模式：{', '.join(mistake_patterns[:3]) if mistake_patterns else '暂无'}
- 学生类型：{overall_type or '未知'}
- 学习偏好：{preference or '暂无'}

## 知识库参考
{context if context else '暂无知识库内容，请根据通用编程知识撰写'}

## 学生原始请求
{user_input}

请撰写一份针对该学生的个性化讲解文档。"""
        course_doc = call_llm(doc_prompt, system=RESOURCE_DOC_SYSTEM)
    except Exception:
        pass

    if not course_doc:
        course_doc = f"""# {course} 个性化讲解文档：{topic}

## 一、学生情况
- 薄弱点：{weaknesses_text}
- 学习偏好：{preference}

## 二、学习目标
掌握 `{topic}` 的核心定义、典型场景、易错点和代码实现方法。

## 三、核心讲解
`{topic}` 是 {course} 中的重要知识点。学习时建议先理解它解决的问题，再观察代码中的表现形式，最后通过练习和实操巩固。

## 四、易错提醒
1. 只背定义，不知道什么时候使用
2. 代码实现时遗漏边界条件
3. 对输入、处理过程和输出关系理解不清楚
"""

    # === 拓展阅读：LLM 生成 ===
    extended_reading = None
    try:
        read_prompt = f"""学生正在学习 {course} 中的 {topic}。
薄弱点：{weaknesses_text or '暂无'}
知识库参考：{context[:1000] if context else '无'}

请推荐3-4条拓展阅读路径。"""
        import json as _json
        resp = call_llm(read_prompt, system=RESOURCE_READING_SYSTEM)
        text = resp.strip()
        if text.startswith("```"):
            lines = text.split("\n")
            text = "\n".join(lines[1:-1] if lines[-1].strip() == "```" else lines[1:])
        start = text.find("{")
        end = text.rfind("}") + 1
        if start != -1 and end > start:
            data = _json.loads(text[start:end])
            items = data.get("items", [])
            parts = [f"# 拓展阅读材料：{topic}", ""]
            for i, item in enumerate(items, 1):
                parts.append(f"{i}. **{item.get('title', '')}**")
                parts.append(f"   - 推荐理由：{item.get('why', '')}")
                parts.append(f"   - 重点关注：{item.get('focus', '')}")
            extended_reading = "\n".join(parts)
    except Exception:
        pass

    if not extended_reading:
        extended_reading = f"""# 拓展阅读材料：{topic}
1. **基础概念复习**：阅读 `{topic}` 的定义、使用场景和基本例题。
2. **易错点专题**：重点关注 `{weaknesses_text}` 相关问题。
3. **代码实战**：结合 Java 示例完成最小可运行程序。
"""

    quiz = _build_fallback_quiz(topic, weaknesses_text)

    mindmap = f"""
graph TD
A[{course}] --> B[{topic}核心概念]
A --> C[典型应用场景]
A --> D[常见易错点]
A --> E[练习巩固]
B --> B1[定义]
B --> B2[关键特征]
C --> C1[代码案例]
C --> C2[题目应用]
D --> D1[边界条件]
D --> D2[概念混淆]
E --> E1[选择题]
E --> E2[简答题]
E --> E3[代码实操]
""".strip()

    code_practice = f"""
public class {topic.replace(" ", "")}Practice {{
    public static void main(String[] args) {{
        System.out.println("学习主题：{topic}");
        System.out.println("请在这里补充与 {topic} 相关的核心代码示例。");

        // TODO 1: 写出基本输入
        // TODO 2: 实现核心逻辑
        // TODO 3: 补充边界条件测试
    }}
}}
""".strip()

    video_script = f"""
# 多模态教学视频 / 动画脚本：{topic}

## 镜头 1：问题引入
画面展示学生在学习 `{topic}` 时遇到的典型困惑。

## 镜头 2：概念图解
用流程图或节点图展示 `{topic}` 的核心结构。

## 镜头 3：代码演示
展示 Java 代码片段，并高亮容易出错的位置。

## 镜头 4：互动练习
弹出一道基础题，让学生判断关键步骤。

## 镜头 5：总结
回顾定义、使用场景、易错点和下一步学习任务。
""".strip()

    resources = {
        "course_doc": course_doc,
        "mindmap": mindmap,
        "quiz": quiz,
        "extended_reading": extended_reading,
        "code_practice": code_practice,
        "video_script": video_script,
    }

    final_answer = (
        f"已围绕 `{topic}` 生成个性化学习资源包，包含：讲解文档、思维导图、练习题、拓展阅读、代码实操案例和多模态视频脚本。"
    )

    return {
        "resources": resources,
        "final_answer": final_answer,
        "agent_outputs": merge_agent_output(
            state,
            "resource_agent",
            {"status": "success", "resource_types": list(resources.keys())},
        ),
    }
