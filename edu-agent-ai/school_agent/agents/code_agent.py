def code_agent(state: dict) -> dict:
    title = state.get("profile", {}).get("topic", "") or state.get("user_input", "")
    clean = title.replace(" ", "").replace(".", "").replace("-", "")
    code = f"// {title} - 代码示例\npublic class {clean}Example {{\n    public static void main(String[] args) {{\n        System.out.println(\"{title} 示例代码\");\n    }}\n}}"
    return {"resources": {"code": code}, "final_answer": code}
