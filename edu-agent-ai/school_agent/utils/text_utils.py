import re
from typing import Any, Dict, Iterable, List


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


def keyword_score(query: str, text: str) -> int:
    words = [w for w in re.split(r"\s+|，|。|、|；|;|,|\.|/|\\|\(|\)|（|）", query.lower()) if w]
    target = (text or "").lower()
    score = 0
    for word in words:
        if word in target:
            score += len(word)
    return score
