import json, os
from school_agent.config import RESOURCES_DIR

def save_resources(student_id: str, session_id: str, profile: dict, resources: dict, learning_path: list) -> str:
    resource_dir = os.path.join(RESOURCES_DIR, student_id, session_id)
    os.makedirs(resource_dir, exist_ok=True)
    for key, val in resources.items():
        path = os.path.join(resource_dir, f"{key}.json")
        with open(path, "w", encoding="utf-8") as f:
            json.dump(val, f, ensure_ascii=False, indent=2)
    return resource_dir
