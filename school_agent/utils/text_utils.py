from typing import Any

def to_text(value: Any) -> str:
    if value is None:
        return ""
    if isinstance(value, list):
        return "、".join(str(x) for x in value)
    return str(value)

def compact_text(text: str, max_chars: int = 800) -> str:
    if len(text) <= max_chars:
        return text
    return text[:max_chars] + "..."

def get_main_topic(profile: dict) -> str:
    topic = profile.get("topic")
    if topic and topic != "信息不足":
        return topic
    weaknesses = profile.get("weaknesses", [])
    if weaknesses and weaknesses[0]:
        return weaknesses[0]
    return profile.get("course", "当前知识点")

def keyword_score(query: str, text: str) -> float:
    """简单的关键词匹配得分，用于文档检索排序。"""
    if not query or not text:
        return 0.0
    query_words = set(query.lower().split())
    text_lower = text.lower()
    matches = sum(1 for word in query_words if word in text_lower)
    total = len(query_words)
    return matches / total if total > 0 else 0.0