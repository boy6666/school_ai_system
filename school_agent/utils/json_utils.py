import json
from pathlib import Path
from typing import Any, Dict, Union

def merge_agent_output(state: dict, agent_name: str, data: dict) -> dict:
    if "agent_outputs" not in state:
        state["agent_outputs"] = {}
    state["agent_outputs"][agent_name] = data
    return state["agent_outputs"]

def safe_json_loads(text: str) -> Dict[str, Any]:
    if not text:
        return {}
    cleaned = text.strip().replace("```json", "").replace("```", "").strip()
    try:
        return json.loads(cleaned)
    except:
        start = cleaned.find("{")
        end = cleaned.rfind("}")
        if start != -1 and end != -1:
            try:
                return json.loads(cleaned[start:end+1])
            except:
                pass
    return {}

def read_json(file_path: Union[str, Path], default: Any = None) -> Any:
    path = Path(file_path)
    if not path.exists():
        return default if default is not None else {}
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)

def write_json(file_path: Union[str, Path], data: Any) -> None:
    path = Path(file_path)
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)