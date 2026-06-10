"""AI 层 Agent 测试"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))


class TestGraph:
    """测试图结构"""

    def test_graph_exists(self):
        from school_agent.graph import graph
        assert graph is not None

    def test_graph_has_nodes(self):
        from school_agent.graph import graph
        nodes = list(graph.nodes)
        assert len(nodes) > 0

    def test_graph_has_start_node(self):
        from school_agent.graph import graph
        assert "__start__" in graph.nodes


class TestPromptLoader:
    """测试 Prompt 加载器"""

    def test_load_prompt_function_exists(self):
        from school_agent.services.prompt_loader import load_prompt
        assert callable(load_prompt)


class TestSchemaImports:
    """测试 Schema 模块可导入"""

    def test_profile_schema_imports(self):
        from school_agent.schemas.profile_schema import (
            DimensionLevel, DimensionState, StudentProfile
        )
        assert DimensionLevel is not None
        assert DimensionState is not None
        assert StudentProfile is not None


class TestServices:
    """测试 Service 模块"""

    def test_append_log_exists(self):
        from school_agent.services.log_store import append_log
        assert callable(append_log)

    def test_profile_store_functions(self):
        from school_agent.services.profile_store import load_profile, save_profile
        assert callable(load_profile)
        assert callable(save_profile)

    def test_save_resources_exists(self):
        from school_agent.services.resource_store import save_resources
        assert callable(save_resources)
