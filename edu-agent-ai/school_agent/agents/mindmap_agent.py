def mindmap_agent(state: dict) -> dict:
    title = state.get("profile", {}).get("topic", "") or state.get("user_input", "")
    md = f"# {title} 思维导图\n\n## 核心概念\n\n## 主要特性\n\n## 使用方法\n\n## 注意事项"
    return {"resources": {"mindmap": md}, "final_answer": f"已生成 {title} 思维导图"}
