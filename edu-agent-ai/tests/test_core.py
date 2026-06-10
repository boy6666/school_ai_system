"""AI 层核心模块测试"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))


class TestConfig:
    """测试配置常量"""

    def test_api_key_defined(self):
        import school_agent.config as cfg
        assert hasattr(cfg, "OPENAI_API_KEY")
        assert hasattr(cfg, "LLM_MODEL")
        assert hasattr(cfg, "LLM_TEMPERATURE")

    def test_data_dirs_defined(self):
        import school_agent.config as cfg
        assert hasattr(cfg, "DATA_DIR")
        assert hasattr(cfg, "KNOWLEDGE_BASE_DIR")
        assert hasattr(cfg, "PROFILES_DIR")
        assert hasattr(cfg, "RESOURCES_DIR")


class TestState:
    """测试 StudentState 类型定义"""

    def test_state_keys(self):
        from school_agent.state import StudentState

        required = ["student_id", "session_id", "user_input", "intent", "final_answer"]
        for field in required:
            assert field in StudentState.__annotations__, f"缺少字段: {field}"


class TestTextUtils:
    """测试文本工具函数"""

    def test_truncate_long_text(self):
        from school_agent.utils.text_utils import truncate_text

        text = "A" * 1000
        result = truncate_text(text, max_len=100)
        # truncate_text 可能保留完整句子或添加 ...，长度接近 max_len 即可
        assert len(result) <= 120

    def test_truncate_short_text(self):
        from school_agent.utils.text_utils import truncate_text

        text = "Hello World"
        result = truncate_text(text, max_len=100)
        assert result == text

    def test_to_text(self):
        from school_agent.utils.text_utils import to_text

        assert to_text("hello") == "hello"
        assert to_text(42) == "42"
        assert to_text(None) == ""

    def test_get_main_topic(self):
        from school_agent.utils.text_utils import get_main_topic

        result = get_main_topic({"topic": "Java"}, fallback="默认")
        assert result is not None

    def test_compact_text(self):
        from school_agent.utils.text_utils import compact_text

        text = "A" * 2000
        result = compact_text(text, max_chars=100)
        # compact 后长度应明显缩短
        assert len(result) < 200


class TestTimeUtils:
    """测试时间工具函数"""

    def test_now_iso(self):
        from school_agent.utils.time_utils import now_iso

        result = now_iso()
        assert isinstance(result, str)
        assert len(result) > 0

    def test_format_duration_zero(self):
        from school_agent.utils.time_utils import format_duration

        result = format_duration(0)
        assert result is not None
        assert isinstance(result, str)

    def test_format_duration_positive(self):
        from school_agent.utils.time_utils import format_duration

        result = format_duration(3600)  # 1 hour
        assert result is not None
        assert isinstance(result, str)


class TestLLMClient:
    """测试 LLM 客户端"""

    def test_call_llm_exists(self):
        from school_agent.services.llm_client import call_llm
        assert callable(call_llm)

    def test_call_llm_json_exists(self):
        from school_agent.services.llm_client import call_llm_json
        assert callable(call_llm_json)


class TestPromptLoader:
    """测试 Prompt 加载"""

    def test_load_prompt_exists(self):
        from school_agent.services.prompt_loader import load_prompt
        assert callable(load_prompt)


class TestProfileSchema:
    """测试画像 Schema"""

    def test_dimension_level_exists(self):
        from school_agent.schemas.profile_schema import DimensionLevel
        assert DimensionLevel is not None

    def test_dimension_state_exists(self):
        from school_agent.schemas.profile_schema import DimensionState
        assert DimensionState is not None

    def test_student_profile_exists(self):
        from school_agent.schemas.profile_schema import StudentProfile
        assert StudentProfile is not None
