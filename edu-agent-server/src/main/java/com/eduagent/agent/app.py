import json
from pathlib import Path

from school_agent.agent import graph


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
    """回显课程讲解文档。"""
    content = read_text_file(resource_dir / "course_explanation.md")

    if not content:
        # 如果没有单独 md，就从 resources.json 里读
        resources = read_json_file(resource_dir / "resources.json")
        if resources:
            content = resources.get("course_explanation", "")

    print_block("专业课程讲解文档", content)


def render_mindmap(resource_dir: Path) -> None:
    """回显 Mermaid 思维导图。"""
    content = read_text_file(resource_dir / "mindmap.mmd")

    if not content:
        resources = read_json_file(resource_dir / "resources.json")
        if resources:
            content = resources.get("mindmap", "")

    print_block("知识点思维导图 Mermaid", content)


def render_quiz(resource_dir: Path) -> None:
    """回显练习题。"""
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
    """回显拓展阅读。"""
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
    """回显代码实操案例。"""
    content = read_text_file(resource_dir / "code_practice.py")

    if not content:
        resources = read_json_file(resource_dir / "resources.json")
        if resources:
            content = resources.get("code_practice", "")

    print_block("代码类实操案例", content)


def render_learning_path(resource_dir: Path) -> None:
    """回显学习路径。"""
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
    """
    从 data/resources/student_xxx/ 读取生成文件，并合理回显到屏幕。
    """
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


def run_once(user_input: str, student_id: str = "student_001") -> None:
    """
    运行一次智能体流程。
    """
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
    print("输入学生学习情况，系统会生成学生画像、学习资源和学习路径。")
    print("输入 exit / quit / q 退出。\n")

    student_id = input("请输入学生ID，直接回车默认 student_001：").strip()

    if not student_id:
        student_id = "student_001"

    while True:
        user_input = input("\n请输入学生学习情况：\n> ").strip()

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
            print("1. vLLM 是否正在运行")
            print("2. 输入是否过长")
            print("3. kb.py / guards.py 是否有函数缺失")
            print("4. Qwen 服务地址是否是 http://localhost:8000/v1\n")


if __name__ == "__main__":
    main()