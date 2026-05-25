import json
from pathlib import Path
from typing import Any, Dict


def safe_json_loads(text: str, default: Any = None) -> Any:
    if default is None:
        default = {}
    if not text:
        return default

    cleaned = text.strip().replace("```json", "").replace("```", "").strip()

    try:
        return json.loads(cleaned)
    except Exception:
        pass

    start = cleaned.find("{")
    end = cleaned.rfind("}")
    if start != -1 and end != -1 and end > start:
        try:
            return json.loads(cleaned[start:end + 1])
        except Exception:
            return default

    return default


def read_json(path: Path, default: Any = None) -> Any:
    if default is None:
        default = {}
    if not path.exists():
        return default
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return default


def write_json(path: Path, data: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")


def merge_agent_output(state: Dict[str, Any], agent_name: str, payload: Dict[str, Any]) -> Dict[str, Any]:
    outputs = dict(state.get("agent_outputs", {}))
    outputs[agent_name] = payload
    return outputs
