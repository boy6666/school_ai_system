import json
from pathlib import Path

from school_agent.graph import graph


def print_block(title: str, content: str) -> None:
    """统一打印一个内容块。"""
    print("\n" + "=" * 70)
    print(f"【{title}】")
    print("=" * 70)
    print(content.strip() if content else "暂无内容")


def read_text_file(path: Path) -> str:
    """安全读取文本文件。"""
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8", errors="ignore")


def read_json_file(path: Path):
    """安全读取 JSON 文件。"""
    if not path.exists():
        return None
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return None


def render_profile(resource_dir: Path) -> None:
    """回显学生画像。"""
    profile = read_json_file(resource_dir / "profile.json")
    if not profile:
        print_block("学生画像", "未找到 profile.json")
        return
    lines = [
        f"专业：{profile.get('major')}",
        f"课程：{profile.get('course')}",
        f"学习目标：{profile.get('learning_goal')}",
        f"知识基础：{profile.get('knowledge_base')}",
        f"认知风格：{profile.get('cognitive_style')}",
        f"薄弱点：{profile.get('weaknesses')}",
        f"易错倾向：{profile.get('mistake_preference')}",
        f"资源偏好：{profile.get('resource_preference')}",
    ]
    print_block("学生画像", "\n".join(lines))


def render_course_explanation(resource_dir: Path) -> None:
    content = read_text_file(resource_dir / "course_explanation.md")
    if not content:
        resources = read_json_file(resource_dir / "resources.json")
        if resources:
            content = resources.get("course_explanation", "")
    print_block("专业课程讲解文档", content)


def render_mindmap(resource_dir: Path) -> None:
    content = read_text_file(resource_dir / "mindmap.mmd")
    if not content:
        resources = read_json_file(resource_dir / "resources.json")
        if resources:
            content = resources.get("mindmap", "")
    print_block("知识点思维导图 Mermaid", content)


def render_quiz(resource_dir: Path) -> None:
    quiz = read_json_file(resource_dir / "quiz.json")
    if quiz is None:
        resources = read_json_file(resource_dir / "resources.json")
        if resources:
            quiz = resources.get("quiz", [])
    if not quiz:
        print_block("练习题", "暂无练习题")
        return
    lines = []
    for index, item in enumerate(quiz, start=1):
        lines.append(f"{index}. 题型：{item.get('type')}")
        lines.append(f"   题目：{item.get('question')}")
        options = item.get("options")
        if options:
            lines.append("   选项：")
            for opt in options:
                lines.append(f"   - {opt}")
        lines.append(f"   参考答案：{item.get('answer')}")
        lines.append("")
    print_block("练习题", "\n".join(lines))


def render_extended_reading(resource_dir: Path) -> None:
    content = read_text_file(resource_dir / "extended_reading.md")
    if not content:
        resources = read_json_file(resource_dir / "resources.json")
        if resources:
            readings = resources.get("extended_reading", [])
            if isinstance(readings, list):
                content = "\n".join(f"- {item}" for item in readings)
            else:
                content = str(readings)
    print_block("拓展阅读材料", content)


def render_code_practice(resource_dir: Path) -> None:
    content = read_text_file(resource_dir / "code_practice.py")
    if not content:
        resources = read_json_file(resource_dir / "resources.json")
        if resources:
            content = resources.get("code_practice", "")
    print_block("代码类实操案例", content)


def render_learning_path(resource_dir: Path) -> None:
    path_data = read_json_file(resource_dir / "learning_path.json")
    if not path_data:
        print_block("个性化学习路径", "暂无学习路径")
        return
    lines = []
    for item in path_data:
        lines.append(f"{item.get('step')}. {item.get('title')}")
        lines.append(f"   使用资源：{item.get('resource')}")
        lines.append(f"   推荐原因：{item.get('reason')}")
        lines.append("")
    print_block("个性化学习路径", "\n".join(lines))


def render_saved_outputs(resource_dir: str) -> None:
    resource_path = Path(resource_dir)
    if not resource_path.exists():
        print_block("资源回显", f"资源目录不存在：{resource_path}")
        return
    print("\n\n" + "#" * 70)
    print("已生成资源回显")
    print("#" * 70)
    print(f"资源目录：{resource_path}")
    render_profile(resource_path)
    render_course_explanation(resource_path)
    render_mindmap(resource_path)
    render_quiz(resource_path)
    render_extended_reading(resource_path)
    render_code_practice(resource_path)
    render_learning_path(resource_path)
    print("\n" + "#" * 70)
    print("资源回显结束")
    print("#" * 70 + "\n")


def is_question_mode(user_input: str) -> bool:
    """
    判断用户输入是否为题目讲解模式。
    启发式规则：包含典型题目标志，并且长度较短（通常题目不会太长）。
    """
    indicators = ["?", "？", "题", "题目", "讲解", "解答", "如何解", "步骤", "求", "帮我做"]
    # 如果包含这些词且长度小于 500 字符，视为题目
    if any(ind in user_input for ind in indicators) and len(user_input) < 500:
        return True
    # 也可以支持显式命令
    if user_input.strip().startswith("/explain"):
        return True
    return False


def run_once(user_input: str, student_id: str = "student_001") -> None:
    """
    运行一次智能体流程，自动判断是题目讲解还是资源生成。
    """
    if is_question_mode(user_input):
        # 题目讲解模式
        result = graph.invoke(
            {
                "student_id": student_id,
                "user_input": user_input,
                "question": user_input,   # 明确传入题目
            },
            config={"configurable": {"thread_id": f"{student_id}_thread"}},
        )
        print("\n" + "=" * 70)
        print("【题目讲解】")
        print("=" * 70)
        print(result.get("explanation", "未生成讲解"))
    else:
        # 原有的资源生成模式
        result = graph.invoke(
            {
                "student_id": student_id,
                "user_input": user_input,
            },
            config={"configurable": {"thread_id": f"{student_id}_thread"}},
        )
        print("\n" + "=" * 70)
        print("【智能体最终总结】")
        print("=" * 70)
        print(result.get("final_answer", "暂无最终回答"))
        resource_dir = result.get("resource_dir")
        if resource_dir:
            render_saved_outputs(resource_dir)
        else:
            print("\n未找到 resource_dir，无法回显本地资源文件。")


def main():
    """
    控制台交互入口。
    """
    print("个性化学习智能体已启动。")
    print("支持两种模式：")
    print("  - 输入学习情况（如'我是计算机专业，学习数据结构...'）→ 生成学习资源与路径")
    print("  - 输入题目（包含'题'、'？'、'讲解'等关键词）→ 获得个性化讲解")
    print("输入 exit / quit / q 退出。\n")

    student_id = input("请输入学生ID，直接回车默认 student_001：").strip()
    if not student_id:
        student_id = "student_001"

    while True:
        user_input = input("\n请输入内容：\n> ").strip()
        if user_input.lower() in {"exit", "quit", "q"}:
            print("已退出。")
            break
        if not user_input:
            print("输入不能为空，请重新输入。")
            continue
        try:
            run_once(user_input, student_id)
        except Exception as e:
            print("\n运行出错：")
            print(e)
            print("请检查：")
            print("1. API 服务是否正常运行")
            print("2. 输入是否过长")
            print("3. 知识库目录是否存在")


if __name__ == "__main__":
    main()