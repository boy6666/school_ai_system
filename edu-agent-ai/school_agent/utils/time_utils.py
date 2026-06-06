from datetime import datetime, timedelta


def format_duration(seconds: int) -> str:
    h, m = divmod(seconds // 60, 60)
    parts = []
    if h: parts.append(f"{h}小时")
    if m: parts.append(f"{m}分钟")
    return "".join(parts) if parts else "0分钟"

def now_iso() -> str:
    return datetime.now().isoformat()
