import os
from pathlib import Path

try:
    from dotenv import load_dotenv
except Exception:
    load_dotenv = None

# ---------- 路径常量 ----------
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

# ---------- 模型配置 ----------
MODEL_NAME = os.getenv("MODEL_NAME", "lite")
BASE_URL = os.getenv("BASE_URL", "https://spark-api-open.xf-yun.com/v1")
API_KEY = os.getenv("API_KEY", "56aa698f0142261c450b32244b25d3b3:Y2IzOWYyYjFiNjE1N2RiZWMzNzI4ZTU1")

TEMPERATURE = float(os.getenv("TEMPERATURE", "1"))
MAX_TOKENS = int(os.getenv("MAX_TOKENS", "1024"))
MAX_KB_CHARS = int(os.getenv("MAX_KB_CHARS", "1800"))

USE_MOCK_LLM = False