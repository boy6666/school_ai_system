from school_agent.constants import INTENT_EXPLAIN, INTENT_ONBOARDING
from school_agent.services.flow_logger import log

def classify_intent(state: dict) -> dict:
    session_id = state.get("session_id", "")
    user_input = state.get("user_input", "")[:50]
    profile = state.get("profile", {})
    onboarding_phase = profile.get("_onboarding_phase", "")

    print(f"  [ROUTER] 收到输入: \"{user_input}\"")
    print(f"  [ROUTER] session_id: {session_id}")
    print(f"  [ROUTER] _onboarding_phase: {onboarding_phase}")

    # 检测是否为引导会话
    if session_id.startswith("onboard_"):
        print(f"  [ROUTER] → 判定: INTENT_ONBOARDING (session_id 以 onboard_ 开头)")
        return {"intent": INTENT_ONBOARDING, "intent_confidence": 1.0}

    print(f"  [ROUTER] → 判定: INTENT_EXPLAIN (默认)")
    return {"intent": INTENT_EXPLAIN, "intent_confidence": 0.8}

def route_by_intent(state: dict) -> str:
    intent = state.get("intent", INTENT_EXPLAIN)
    print(f"  [ROUTER] 路由到: {intent}")
    return intent

def route_after_safety(state: dict) -> str:
    return "continue"
