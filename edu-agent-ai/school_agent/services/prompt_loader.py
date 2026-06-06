import os
from school_agent.config import KNOWLEDGE_BASE_DIR

def load_prompt(name: str) -> str:
    path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "school_agent", "prompts", name)
    if os.path.exists(path):
        with open(path, "r", encoding="utf-8") as f:
            return f.read()
    return ""
