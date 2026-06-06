def retrieval_agent(state: dict) -> dict:
    return {"final_answer": state.get("user_input", "")}

def retrieve_knowledge(state: dict) -> dict:
    return state
