import json, os
from school_agent.config import PROFILES_DIR

_PROFILES_CACHE: dict = {}

def _ensure_profiles_dir():
    os.makedirs(PROFILES_DIR, exist_ok=True)

def load_student_profile(student_id: str) -> dict:
    if student_id in _PROFILES_CACHE:
        return _PROFILES_CACHE[student_id]
    _ensure_profiles_dir()
    path = os.path.join(PROFILES_DIR, f"{student_id}.json")
    if os.path.exists(path):
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
        _PROFILES_CACHE[student_id] = data
        return data
    return {}

def save_student_profile(student_id: str, profile: dict):
    _ensure_profiles_dir()
    path = os.path.join(PROFILES_DIR, f"{student_id}.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(profile, f, ensure_ascii=False, indent=2)
    _PROFILES_CACHE[student_id] = profile

# backward compat aliases
load_profile = load_student_profile
save_profile = save_student_profile
