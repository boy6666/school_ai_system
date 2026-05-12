import json
import re
from pathlib import Path
from typing import Dict, Any, List, Tuple

from school_agent.config import MAX_KB_CHARS


KB_DIR = Path("data/knowledge_base")


def _parse_front_matter(block: str) -> Dict[str, Any]:
    """
    解析 Markdown 顶部的 YAML-like 元信息。

    支持：
    ---
    id: java_018
    title: 字符串
    course: Java编程
    topic: 字符串
    tags:
      - Java
      - 字符串
    ---
    """
    meta: Dict[str, Any] = {}

    lines = block.splitlines()
    in_meta = False
    current_key = None

    for line in lines:
        line = line.rstrip()

        if line.strip() == "---":
            if not in_meta:
                in_meta = True
                continue
            else:
                break

        if not in_meta:
            continue

        if not line.strip():
            continue

        # tags:
        if re.match(r"^[a-zA-Z_]+:\s*$", line):
            key = line.split(":", 1)[0].strip()
            meta[key] = []
            current_key = key
            continue

        #   - Java
        if line.strip().startswith("-") and current_key:
            value = line.strip()[1:].strip()
            if isinstance(meta.get(current_key), list):
                meta[current_key].append(value)
            continue

        # key: value
        if ":" in line:
            key, value = line.split(":", 1)
            key = key.strip()
            value = value.strip()
            meta[key] = value
            current_key = key

    return meta


def _split_multi_markdown(text: str) -> List[str]:
    """
    把一个包含多个 --- id: ... 的 Markdown 文件切成多个知识块。

    你的 java_knowagle_qwq.md 就是这种情况：
    一个文件里有 Java程序结构、变量、常量、数据类型、字符串、方法等多个知识点。
    """
    parts = re.split(r"(?=\n?---\s*\nid:)", text)

    blocks = []
    for part in parts:
        part = part.strip()
        if part:
            blocks.append(part)

    return blocks


def _load_json_like_md(path: Path) -> List[Dict[str, Any]]:
    """
    处理 JSON 风格的 .md 文件。

    例如：
    {
      "title": "Java 教程",
      "text": "...",
      "codes": [...],
      "url": "..."
    }
    """
    text = path.read_text(encoding="utf-8", errors="ignore").strip()

    try:
        obj = json.loads(text)
    except Exception:
        return []

    title = obj.get("title", path.stem)
    body = obj.get("text", "")
    codes = obj.get("codes", [])
    url = obj.get("url", "")

    if codes:
        body += "\n\n代码示例：\n"
        for code in codes:
            body += f"\n```text\n{code}\n```\n"

    if url:
        body += f"\n\n来源：{url}"

    return [
        {
            "path": str(path),
            "title": title,
            "course": "Java编程",
            "topic": title,
            "tags": ["Java"],
            "content": body,
        }
    ]


def _load_markdown_blocks(path: Path) -> List[Dict[str, Any]]:
    """
    加载一个 Markdown 文件。

    支持：
    1. JSON 风格 .md
    2. 一个 .md 中多个知识点
    3. 普通单知识点 Markdown
    """
    text = path.read_text(encoding="utf-8", errors="ignore")

    if text.strip().startswith("{"):
        return _load_json_like_md(path)

    blocks = _split_multi_markdown(text)

    if not blocks:
        blocks = [text]

    docs = []

    for block in blocks:
        meta = _parse_front_matter(block)

        title = meta.get("title") or path.stem
        course = meta.get("course") or ""
        topic = meta.get("topic") or title
        tags = meta.get("tags") or []

        if isinstance(tags, str):
            tags = [tags]

        docs.append(
            {
                "path": str(path),
                "title": title,
                "course": course,
                "topic": topic,
                "tags": tags,
                "content": block,
            }
        )

    return docs


def load_knowledge_documents() -> List[Dict[str, Any]]:
    """
    加载整个知识库。

    注意：
    一个 .md 文件可能被切成多个知识块。
    """
    docs = []

    for path in KB_DIR.rglob("*.md"):
        try:
            docs.extend(_load_markdown_blocks(path))
        except Exception as e:
            print(f"[KB WARNING] 读取失败：{path}，原因：{e}")

    return docs


def _score_document(query: str, doc: Dict[str, Any]) -> float:
    """
    给知识块打分。

    重点：
    不写死具体知识点。
    只做通用相关性判断：
    - title/topic 出现在 query 里，强相关
    - tags 出现在 query 里，较强相关
    - query 中的词出现在 title/topic/content 里，计分
    """
    query_lower = query.lower()

    title = str(doc.get("title", "")).lower()
    course = str(doc.get("course", "")).lower()
    topic = str(doc.get("topic", "")).lower()
    tags_list = doc.get("tags", []) or []
    tags = " ".join(str(x) for x in tags_list).lower()
    content = str(doc.get("content", "")).lower()

    score = 0.0

    # 文档元信息直接出现在用户输入中，权重最高
    if title and title in query_lower:
        score += 8

    if topic and topic in query_lower:
        score += 8

    if course and course in query_lower:
        score += 4

    for tag in tags_list:
        tag_text = str(tag).lower().strip()
        if tag_text and tag_text in query_lower:
            score += 4

    # query 中的词出现在文档字段中
    terms = re.split(r"[\s，。,.、:：;；！？!?]+", query_lower)
    terms = [x.strip() for x in terms if x.strip()]

    for term in terms:
        if term in title:
            score += 5
        if term in topic:
            score += 5
        if term in course:
            score += 3
        if term in tags:
            score += 3
        if term in content:
            score += 1

    # 中文短语反向匹配
    doc_keywords = [title, topic, course] + [str(x).lower() for x in tags_list]

    for kw in doc_keywords:
        kw = kw.strip()
        if len(kw) >= 2 and kw in query_lower:
            score += 5

    return score


def search_knowledge_base(query: str, max_chars: int = MAX_KB_CHARS) -> str:
    """
    统一知识库检索接口。

    输入：
    query = 用户输入 + course + topic

    输出：
    最相关的知识块内容。
    """
    docs = search_knowledge_documents(query=query, limit=3, min_score=3)

    if not docs:
        return "知识库中没有找到高度相关内容。可以继续生成通用学习资源，但建议补充对应知识点的 Markdown。"

    output = []

    for doc in docs:
        content = str(doc.get("content", ""))[:max_chars]

        output.append(
            f"来源：{doc.get('path')}\n"
            f"标题：{doc.get('title')}\n"
            f"课程：{doc.get('course')}\n"
            f"主题：{doc.get('topic')}\n"
            f"相关度：{doc.get('score')}\n"
            f"标签：{doc.get('tags')}\n\n"
            f"内容：\n{content}"
        )

    return "\n\n---\n\n".join(output)


def get_knowledge_catalog(query: str = "", limit: int = 25) -> str:
    """
    返回知识库目录，给 AI 判断 course/topic 用。

    如果传入 query，会优先返回和 query 相关的知识点。
    这样用户输入“复习字符串”时，目录里会优先出现“字符串 / 字符串方法 / StringBuilder”。
    """
    docs = load_knowledge_documents()

    if not docs:
        return "知识库为空"

    if query:
        scored = []
        for doc in docs:
            score = _score_document(query, doc)
            scored.append((score, doc))
        scored.sort(key=lambda x: x[0], reverse=True)
        docs = [doc for score, doc in scored if score > 0] + [
            doc for score, doc in scored if score <= 0
        ]

    items = []

    for doc in docs[:limit]:
        title = doc.get("title", "")
        course = doc.get("course", "")
        topic = doc.get("topic", "")
        tags = doc.get("tags", [])

        items.append(
            f"- course: {course}; topic: {topic}; title: {title}; tags: {tags}"
        )

    return "\n".join(items)
def search_knowledge_documents(
    query: str,
    limit: int = 3,
    min_score: float = 3,
) -> List[Dict[str, Any]]:
    """
    返回结构化知识库检索结果。

    这个函数给 nodes.py 用。
    它不仅返回文本，还返回 title/course/topic/tags/score。
    """
    docs = load_knowledge_documents()

    scored: List[Tuple[float, Dict[str, Any]]] = []

    for doc in docs:
        score = _score_document(query, doc)
        if score >= min_score:
            scored.append((score, doc))

    scored.sort(key=lambda x: x[0], reverse=True)

    results = []

    for score, doc in scored[:limit]:
        item = dict(doc)
        item["score"] = score
        results.append(item)

    return results


def get_top_knowledge_doc(query: str) -> Dict[str, Any]:
    """
    获取最相关的一个知识块。

    用途：
    当 AI 画像抽取失败时，用知识库命中的 metadata 修正 profile。
    """
    docs = search_knowledge_documents(query=query, limit=1, min_score=3)

    if not docs:
        return {}

    return docs[0]