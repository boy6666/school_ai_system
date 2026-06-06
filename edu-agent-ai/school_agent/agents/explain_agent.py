def explain_agent(state: dict) -> dict:
    return {"final_answer": state.get("user_input", "")}
