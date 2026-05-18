from school_agent.agents.quiz_agent import _build_quiz
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import compact_text, get_main_topic, to_text


def resource_agent(state: dict) -> dict:
    """资源生成智能体范例：生成至少 5 类个性化资源。"""
    profile = state.get("profile", {})
    topic = get_main_topic(profile)
    course = profile.get("course", "数据结构")
    context = compact_text(state.get("retrieved_context", ""), max_chars=900)
    weaknesses = to_text(profile.get("weaknesses"))
    preference = to_text(profile.get("resource_preference"))

    course_doc = f"""
# {course} 个性化讲解文档：{topic}

## 一、学生情况

- 已有基础：{profile.get("knowledge_base")}
- 薄弱点：{weaknesses}
- 资源偏好：{preference}

## 二、学习目标

掌握 `{topic}` 的核心定义、典型场景、易错点和代码实现方法。

## 三、核心讲解

`{topic}` 是 {course} 中的重要知识点。学习时建议先理解它解决的问题，再观察它在代码中的表现形式，最后通过练习题和实操案例巩固。

## 四、易错点提醒

1. 只背定义，不知道什么时候使用。
2. 代码实现时遗漏边界条件。
3. 对输入、处理过程和输出关系理解不清楚。

## 五、知识库参考

{context or "当前知识库未命中足够内容。"}
""".strip()

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

    quiz = _build_quiz(topic, weaknesses)

    extended_reading = f"""
# 拓展阅读材料：{topic}

1. **基础概念复习**：阅读 `{topic}` 的定义、使用场景和基本例题。
2. **易错点专题**：重点关注 `{weaknesses}` 相关问题。
3. **代码实战材料**：结合 Java 示例完成最小可运行程序。
4. **综合训练**：完成选择题、简答题、代码阅读题和实操题。
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
