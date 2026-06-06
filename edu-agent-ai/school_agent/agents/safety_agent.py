def safety_precheck(state: dict) -> dict:
    return {"safety_report": {"passed": True}}

def safety_postcheck(state: dict) -> dict:
    return {}
def route_after_safety(state: dict) -> str:
    return "continue"
