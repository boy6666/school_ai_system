import re
from pathlib import Path
from typing import Dict, List

from school_agent.config import KNOWLEDGE_BASE_DIR
from school_agent.utils.text_utils import compact_text


_unicode_name_pattern = re.compile(r"#U([0-9a-fA-F]{4})")


def decode_escaped_name(name: str) -> str:
    return _unicode_name_pattern.sub(lambda m: chr(int(m.group(1), 16)), name)


def load_documents(limit: int = 500) -> List[Dict[str, str]]:
    docs: List[Dict[str, str]] = []

    if not KNOWLEDGE_BASE_DIR.exists():
        return docs

    for path in KNOWLEDGE_BASE_DIR.rglob("*"):
        if not path.is_file():
            continue
        if path.suffix.lower() not in {".md", ".txt"}:
            continue

        try:
            content = path.read_text(encoding="utf-8", errors="ignore")
        except Exception:
            continue

        rel = path.relative_to(KNOWLEDGE_BASE_DIR)
        title = decode_escaped_name(path.stem)
        course = decode_escaped_name(rel.parts[1]) if len(rel.parts) > 2 and rel.parts[0] == "courses" else "通用课程"

        docs.append(
            {
                "title": title,
                "course": course,
                "path": str(path),
                "relative_path": str(rel),
                "content": compact_text(content, max_chars=2500),
            }
        )

        if len(docs) >= limit:
            break

    return docs
