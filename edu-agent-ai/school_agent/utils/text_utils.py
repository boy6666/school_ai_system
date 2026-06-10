import re
from typing import Any, Dict


def extract_code_blocks(text: str) -> list:
    """从文本中提取代码块"""
    return re.findall(r'```(\w*)\n(.*?)```', text, re.DOTALL)


def truncate_text(text: str, max_len: int = 2000) -> str:
    if len(text) <= max_len:
        return text
    return text[:max_len] + "\n...(截断)"


def to_text(value: Any) -> str:
    if value is None:
        return ""
    if isinstance(value, list):
        return "、".join(str(x) for x in value)
    if isinstance(value, dict):
        return "；".join(f"{k}: {v}" for k, v in value.items())
    return str(value)


def get_main_topic(profile: Dict[str, Any], fallback: str = "当前知识点") -> str:
    topic = profile.get("topic")
    if isinstance(topic, str) and topic.strip():
        return topic.strip()
    weaknesses = profile.get("weaknesses", [])
    if isinstance(weaknesses, list) and weaknesses:
        return str(weaknesses[0])
    course = profile.get("course")
    if isinstance(course, str) and course.strip():
        return course.strip()
    return fallback


def compact_text(text: str, max_chars: int = 1200) -> str:
    text = re.sub(r"\s+", " ", text or "").strip()
    if len(text) <= max_chars:
        return text
    return text[:max_chars].rstrip() + "..."
