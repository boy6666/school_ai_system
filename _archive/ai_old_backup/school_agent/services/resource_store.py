from pathlib import Path
from typing import Any, Dict, List

from school_agent.config import RESOURCE_DIR
from school_agent.utils.json_utils import write_json


def _safe_name(value: str) -> str:
    keep = []
    for ch in value:
        if ch.isalnum() or ch in {"_", "-"}:
            keep.append(ch)
    return "".join(keep) or "student"


def get_student_resource_dir(student_id: str, session_id: str = "default") -> Path:
    folder = f"{_safe_name(student_id)}_{_safe_name(session_id)}"
    return RESOURCE_DIR / folder


def save_resources(
    student_id: str,
    session_id: str,
    profile: Dict[str, Any],
    resources: Dict[str, Any],
    learning_path: List[Dict[str, Any]],
) -> str:
    output_dir = get_student_resource_dir(student_id, session_id)
    output_dir.mkdir(parents=True, exist_ok=True)

    write_json(output_dir / "profile.json", profile)
    write_json(output_dir / "resources.json", resources)
    write_json(output_dir / "learning_path.json", learning_path)

    if "course_doc" in resources:
        (output_dir / "course_doc.md").write_text(str(resources["course_doc"]), encoding="utf-8")

    if "mindmap" in resources:
        (output_dir / "mindmap.mmd").write_text(str(resources["mindmap"]), encoding="utf-8")

    if "quiz" in resources:
        write_json(output_dir / "quiz.json", resources["quiz"])

    if "extended_reading" in resources:
        (output_dir / "extended_reading.md").write_text(str(resources["extended_reading"]), encoding="utf-8")

    if "code_practice" in resources:
        (output_dir / "code_practice.java").write_text(str(resources["code_practice"]), encoding="utf-8")

    if "video_script" in resources:
        (output_dir / "video_script.md").write_text(str(resources["video_script"]), encoding="utf-8")

    return str(output_dir)
