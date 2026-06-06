from typing import List, Dict, Any


def merge_agent_output(state: dict, node_name: str, output: dict) -> dict:
    """将当前节点输出合并到 agent_outputs 列表"""
    existing = state.get("agent_outputs", [])
    if not isinstance(existing, list):
        existing = []
    existing.append({"node": node_name, "output": output})
    return {"agent_outputs": existing}
