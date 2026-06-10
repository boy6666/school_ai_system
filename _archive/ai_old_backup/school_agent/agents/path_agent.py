from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import get_main_topic


def path_agent(state: dict) -> dict:
    """学习路径规划智能体范例。"""
    profile = state.get("profile", {})
    topic = get_main_topic(profile)

    learning_path = [
        {
            "step": 1,
            "title": f"建立 {topic} 的核心概念",
            "resource": "course_doc",
            "reason": "先理解概念，降低后续题目和代码练习难度。",
        },
        {
            "step": 2,
            "title": "查看知识点思维导图",
            "resource": "mindmap",
            "reason": "用结构化方式建立知识框架。",
        },
        {
            "step": 3,
            "title": "完成基础与中等练习题",
            "resource": "quiz",
            "reason": "检测概念理解和常见易错点。",
        },
        {
            "step": 4,
            "title": "完成 Java 代码实操案例",
            "resource": "code_practice",
            "reason": "把知识迁移到代码实现中。",
        },
        {
            "step": 5,
            "title": "观看或生成多模态讲解视频",
            "resource": "video_script",
            "reason": "通过动画和画面强化过程理解。",
        },
    ]

    return {
        "learning_path": learning_path,
        "agent_outputs": merge_agent_output(
            state,
            "path_agent",
            {"status": "success", "step_count": len(learning_path)},
        ),
    }
