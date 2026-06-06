def init_profile(state: dict) -> dict:
    return {"profile": state.get("profile", {})}

def extract_profile(state: dict) -> dict:
    return {"profile_patch": {}}
