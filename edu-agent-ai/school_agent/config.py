import os

# OpenAI / LLM 配置
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "")
OPENAI_BASE_URL = os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1")
LLM_MODEL = os.getenv("LLM_MODEL", "gpt-4o-mini")
LLM_TEMPERATURE = float(os.getenv("LLM_TEMPERATURE", "0.7"))
LLM_MAX_TOKENS = int(os.getenv("LLM_MAX_TOKENS", "2048"))

# 数据路径
DATA_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "data")
KNOWLEDGE_BASE_DIR = os.path.join(DATA_DIR, "knowledge_base")
LEARNING_LOGS_DIR = os.path.join(DATA_DIR, "learning_logs")
PROFILES_DIR = os.path.join(DATA_DIR, "profiles")
QUESTION_BANK_DIR = os.path.join(DATA_DIR, "question_bank")
RESOURCES_DIR = os.path.join(DATA_DIR, "resources")

# 日志
LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
