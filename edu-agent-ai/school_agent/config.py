import os
from pathlib import Path

try:
    from dotenv import load_dotenv
except Exception:  # pragma: no cover
    load_dotenv = None


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DATA_DIR = PROJECT_ROOT / "data"
PROFILE_DIR = DATA_DIR / "profiles"
KNOWLEDGE_BASE_DIR = DATA_DIR / "knowledge_base"
QUESTION_BANK_DIR = DATA_DIR / "question_bank"
LEARNING_LOG_DIR = DATA_DIR / "learning_logs"
RESOURCE_DIR = DATA_DIR / "resources"
PROMPT_DIR = Path(__file__).resolve().parent / "prompts"

if load_dotenv:
    load_dotenv(PROJECT_ROOT / ".env")

MODEL_NAME = os.getenv("MODEL_NAME", "星火API测试体验")
BASE_URL = os.getenv("BASE_URL", "https://maas-api.cn-huabei-1.xf-yun.com/v2")
API_KEY = os.getenv("API_KEY", "")

TEMPERATURE = float(os.getenv("TEMPERATURE", "0.75"))
MAX_TOKENS = int(os.getenv("MAX_TOKENS", "4096"))
MAX_KB_CHARS = int(os.getenv("MAX_KB_CHARS", "1800"))

# 没有 API_KEY 时默认 mock，保证演示和测试流程不会卡住。
USE_MOCK_LLM = os.getenv("USE_MOCK_LLM", "1" if not API_KEY else "0") == "1"
