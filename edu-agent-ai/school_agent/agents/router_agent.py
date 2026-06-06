from school_agent.constants import INTENT_EXPLAIN

def classify_intent(state: dict) -> dict:
    return {"intent": INTENT_EXPLAIN, "intent_confidence": 0.8}

def route_by_intent(state: dict) -> str:
    return state.get("intent", INTENT_EXPLAIN)
def route_after_safety(state: dict) -> str:
    return "continue"
