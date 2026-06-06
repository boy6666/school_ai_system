from pathlib import Path
from typing import Any, Dict

from school_agent.config import PROFILE_DIR
from school_agent.utils.json_utils import read_json, write_json


def profile_path(student_id: str) -> Path:
    return PROFILE_DIR / f"{student_id}.json"


def load_student_profile(student_id: str) -> Dict[str, Any]:
    return read_json(profile_path(student_id), default={})


def save_student_profile(student_id: str, profile: Dict[str, Any]) -> None:
    write_json(profile_path(student_id), profile)
