from school_agent.config import LLM_MODEL, LLM_TEMPERATURE, LLM_MAX_TOKENS, OPENAI_API_KEY, OPENAI_BASE_URL

def call_llm(prompt: str, system_prompt: str = "") -> str:
    try:
        from openai import OpenAI
        client = OpenAI(api_key=OPENAI_API_KEY, base_url=OPENAI_BASE_URL)
        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        messages.append({"role": "user", "content": prompt})
        resp = client.chat.completions.create(
            model=LLM_MODEL, messages=messages,
            temperature=LLM_TEMPERATURE, max_tokens=LLM_MAX_TOKENS,
        )
        return resp.choices[0].message.content or ""
    except Exception as e:
        return f"LLM 调用失败: {e}"

def call_llm_json(prompt: str, system_prompt: str = "") -> dict:
    import json
    text = call_llm(prompt, system_prompt)
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        return {"raw": text}
