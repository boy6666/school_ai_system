from typing import Optional

from school_agent.config import (
    API_KEY,
    BASE_URL,
    MAX_TOKENS,
    MODEL_NAME,
    TEMPERATURE,
    USE_MOCK_LLM,
)


_model = None


def _mock_llm(prompt: str, system: Optional[str] = None) -> str:
    """没有 API Key 时的本地模拟输出。

    注意：这不是正式大模型能力，只用于跑通流程、写测试和前端联调。
    """
    snippet = (prompt or "").strip().replace("\n", " ")
    if len(snippet) > 220:
        snippet = snippet[:220] + "..."
    return (
        "【模拟大模型输出】\n"
        "系统已根据学生画像、课程知识库和任务意图生成内容。\n"
        f"参考输入：{snippet}"
    )


def get_model():
    global _model
    if _model is not None:
        return _model

    if USE_MOCK_LLM or not API_KEY:
        return None

    try:
        from langchain_openai import ChatOpenAI
    except Exception as exc:  # pragma: no cover
        raise RuntimeError("缺少 langchain-openai，请先安装 requirements.txt") from exc

    _model = ChatOpenAI(
        model=MODEL_NAME,
        base_url=BASE_URL,
        api_key=API_KEY,
        temperature=TEMPERATURE,
        max_tokens=MAX_TOKENS,
    )
    return _model


def call_llm(prompt: str, system: Optional[str] = None) -> str:
    model = get_model()
    if model is None:
        return _mock_llm(prompt, system)

    full_prompt = prompt if not system else f"{system}\n\n{prompt}"
    resp = model.invoke(full_prompt)
    return getattr(resp, "content", str(resp))
