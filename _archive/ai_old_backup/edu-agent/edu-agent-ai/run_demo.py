import json
from pathlib import Path

from school_agent.graph import graph


DEFAULT_INPUT = (
    "我是计算机专业大二学生，正在学习 Java 数据结构。"
    "递归和二叉树一直不太懂，希望用图解、代码案例和练习题复习，目标是期末考 85 分。"
    "请帮我生成一套学习资料。"
)


def pretty(title: str, data) -> None:
    print("\n" + "=" * 80)
    print(f"【{title}】")
    print("=" * 80)
    if isinstance(data, (dict, list)):
        print(json.dumps(data, ensure_ascii=False, indent=2))
    else:
        print(data)


def main() -> None:
    result = graph.invoke(
        {
            "student_id": "student_001",
            "session_id": "demo_001",
            "user_input": DEFAULT_INPUT,
        },
        config={"configurable": {"thread_id": "student_001_demo_001"}},
    )

    pretty("最终回答", result.get("final_answer", ""))
    pretty("识别意图", {
        "intent": result.get("intent"),
        "confidence": result.get("intent_confidence"),
        "reason": result.get("route_reason"),
    })
    pretty("学生画像", result.get("profile", {}))
    pretty("安全报告", result.get("safety_report", {}))
    pretty("学习评估", result.get("evaluation_report", {}))
    pretty("画像更新", result.get("profile_patch", {}))

    resource_dir = result.get("resource_dir")
    if resource_dir:
        pretty("资源目录", resource_dir)
        path = Path(resource_dir)
        if path.exists():
            pretty("已生成文件", [p.name for p in path.iterdir() if p.is_file()])


if __name__ == "__main__":
    main()
