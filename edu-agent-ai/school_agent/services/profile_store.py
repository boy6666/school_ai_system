import json, os
from school_agent.config import PROFILES_DIR

_PROFILES_CACHE: dict = {}

def load_profile(student_id: str) -> dict:
    if student_id in _PROFILES_CACHE:
        return _PROFILES_CACHE[student_id]
    path = os.path.join(PROFILES_DIR, f"{student_id}.json")
    if os.path.exists(path):
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
        _PROFILES_CACHE[student_id] = data
        return data
    return {}

def save_profile(student_id: str, profile: dict):
    path = os.path.join(PROFILES_DIR, f"{student_id}.json")
    os.makedirs(PROFILES_DIR, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(profile, f, ensure_ascii=False, indent=2)
    _PROFILES_CACHE[student_id] = profile
