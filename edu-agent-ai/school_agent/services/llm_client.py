import os
from school_agent.config import LLM_MODEL, LLM_TEMPERATURE, LLM_MAX_TOKENS, OPENAI_API_KEY, OPENAI_BASE_URL

def _log(msg: str):
    """打印日志，兼容 Windows GBK 终端"""
    try:
        print(msg.encode('gbk', errors='replace').decode('gbk'))
    except Exception:
        print(msg.encode('ascii', errors='replace').decode('ascii'))

def call_llm(prompt: str, system_prompt: str = "") -> str:
    _log(f"[LLM] ===== 调用 LLM =====")
    _log(f"[LLM] model={LLM_MODEL}, temperature={LLM_TEMPERATURE}, max_tokens={LLM_MAX_TOKENS}")
    _log(f"[LLM] base_url={OPENAI_BASE_URL}")
    _log(f"[LLM] 有 system_prompt: {'是' if system_prompt else '否'}")
    _log(f"[LLM] prompt(前200字): {prompt[:200] if prompt else '空'}...")
    try:
        from openai import OpenAI
        api_key = OPENAI_API_KEY or os.getenv("OPENAI_API_KEY", "")
        base_url = OPENAI_BASE_URL or os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1")
        client = OpenAI(api_key=api_key, base_url=base_url)
        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        messages.append({"role": "user", "content": prompt})
        _log(f"[LLM] 🚀 发送请求...")
        resp = client.chat.completions.create(
            model=LLM_MODEL, messages=messages,
            temperature=LLM_TEMPERATURE, max_tokens=LLM_MAX_TOKENS,
        )
        content = resp.choices[0].message.content or ""
        usage = resp.usage
        _log(f"[LLM] ✅ 请求成功")
        _log(f"[LLM]    prompt_tokens={usage.prompt_tokens if usage else 'N/A'}, "
             f"completion_tokens={usage.completion_tokens if usage else 'N/A'}, "
             f"total_tokens={usage.total_tokens if usage else 'N/A'}")
        _log(f"[LLM]    返回内容长度: {len(content)} 字")
        _log(f"[LLM]    返回内容(前200字): {content[:200] if content else '空'}...")
        _log(f"[LLM] ===== LLM 调用结束 =====")
        return content
    except Exception as e:
        _log(f"[LLM] ❌ 调用失败: {e}")
        _log(f"[LLM] ===== LLM 调用异常结束 =====")
        return f"LLM 调用失败: {e}"

def call_llm_json(prompt: str, system_prompt: str = "") -> dict:
    import json
    text = call_llm(prompt, system_prompt)
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        return {"raw": text}
