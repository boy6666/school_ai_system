def reading_agent(state: dict) -> dict:
    title = state.get("profile", {}).get("topic", "") or state.get("user_input", "")
    content = f"## {title} - 拓展阅读\n\n### 推荐阅读材料\n- Java 官方文档\n- 《Java 核心技术》相关章节\n\n### 学习建议\n结合本章知识点动手练习。"
    return {"resources": {"reading": content}, "final_answer": content}
