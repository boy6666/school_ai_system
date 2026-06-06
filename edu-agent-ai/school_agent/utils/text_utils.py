def extract_code_blocks(text: str) -> list:
    """从文本中提取代码块"""
    import re
    return re.findall(r'```(\w*)\n(.*?)```', text, re.DOTALL)

def truncate_text(text: str, max_len: int = 2000) -> str:
    if len(text) <= max_len:
        return text
    return text[:max_len] + "\n...(截断)"
