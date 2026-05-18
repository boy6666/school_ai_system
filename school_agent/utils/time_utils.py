 # utils/time_utils.py
from datetime import datetime

def now_iso() -> str:
    """返回当前时间的 ISO 格式字符串，用于日志和评估"""
    return datetime.now().isoformat()