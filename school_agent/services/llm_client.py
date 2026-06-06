from typing import Optional

from school_agent.config import (
    API_KEY,
    BASE_URL,
    MAX_TOKENS,
    MODEL_NAME,
    TEMPERATURE,
    USE_MOCK_LLM,
)


_client = None


def _mock_llm(prompt: str, system: Optional[str] = None) -> str:
    """没有 API Key 时的本地模拟输出。"""
    snippet = (prompt or "").strip().replace("\n", " ")
    if len(snippet) > 220:
        snippet = snippet[:220] + "..."
    return (
        "【模拟大模型输出】\n"
        "系统已根据学生画像、课程知识库和任务意图生成内容。\n"
        f"参考输入：{snippet}"
    )


def _get_openai_client():
    global _client
    if _client is not None:
        return _client

    try:
        from openai import OpenAI
        _client = OpenAI(
            base_url=BASE_URL,
            api_key=API_KEY,
            timeout=60,
        )
    except Exception:
        _client = None
    return _client


def call_llm(prompt: str, system: Optional[str] = None) -> str:
    if USE_MOCK_LLM or not API_KEY:
        return _mock_llm(prompt, system)

    client = _get_openai_client()
    if client is None:
        return _mock_llm(prompt, system)

    messages = []
    if system:
        messages.append({"role": "system", "content": system})
    messages.append({"role": "user", "content": prompt})

    try:
        resp = client.chat.completions.create(
            model=MODEL_NAME,
            messages=messages,
            temperature=TEMPERATURE,
            max_tokens=MAX_TOKENS,
        )
        return resp.choices[0].message.content or ""
    except Exception:
        return _mock_llm(prompt, system)
