import json, os
from school_agent.config import LEARNING_LOGS_DIR
from datetime import datetime

def append_log(student_id: str, session_id: str, log_entry: dict):
    path = os.path.join(LEARNING_LOGS_DIR, f"{student_id}.jsonl")
    os.makedirs(LEARNING_LOGS_DIR, exist_ok=True)
    log_entry["timestamp"] = datetime.now().isoformat()
    with open(path, "a", encoding="utf-8") as f:
        f.write(json.dumps(log_entry, ensure_ascii=False) + "\n")
