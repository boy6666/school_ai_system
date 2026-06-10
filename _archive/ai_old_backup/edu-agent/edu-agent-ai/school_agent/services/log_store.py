import json
from pathlib import Path
from typing import Any, Dict

from school_agent.config import LEARNING_LOG_DIR
from school_agent.utils.time_utils import now_iso


def append_learning_log(student_id: str, event: Dict[str, Any]) -> str:
    LEARNING_LOG_DIR.mkdir(parents=True, exist_ok=True)
    path = LEARNING_LOG_DIR / f"{student_id}.jsonl"

    payload = {
        "time": now_iso(),
        **event,
    }

    with path.open("a", encoding="utf-8") as f:
        f.write(json.dumps(payload, ensure_ascii=False) + "\n")

    return str(path)
