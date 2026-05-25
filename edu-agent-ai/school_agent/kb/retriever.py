from functools import lru_cache
from typing import Dict, List

from school_agent.config import MAX_KB_CHARS
from school_agent.kb.loader import load_documents
from school_agent.utils.text_utils import compact_text, keyword_score


@lru_cache(maxsize=1)
def _cached_docs() -> tuple:
    return tuple(load_documents())


def search_documents(query: str, top_k: int = 5) -> List[Dict[str, str]]:
    docs = list(_cached_docs())
    if not docs:
        return []

    scored = []
    for doc in docs:
        haystack = f"{doc.get('title', '')} {doc.get('course', '')} {doc.get('content', '')}"
        score = keyword_score(query, haystack)
        if score > 0:
            scored.append((score, doc))

    if not scored:
        return []

    scored.sort(key=lambda item: item[0], reverse=True)
    results = []
    for score, doc in scored[:top_k]:
        item = dict(doc)
        item["score"] = score
        results.append(item)
    return results


def build_context(docs: List[Dict[str, str]], max_chars: int = MAX_KB_CHARS) -> str:
    parts = []
    for index, doc in enumerate(docs, start=1):
        parts.append(
            f"[{index}] {doc.get('title')}\n"
            f"课程：{doc.get('course')}\n"
            f"来源：{doc.get('relative_path')}\n"
            f"内容摘要：{doc.get('content', '')}"
        )
    return compact_text("\n\n".join(parts), max_chars=max_chars)
