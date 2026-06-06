from typing import List, Dict, Any


def merge_agent_output(state: dict, node_name: str, output: dict) -> dict:
    """将当前节点输出合并到 agent_outputs 列表"""
    outputs: List[Dict[str, Any]] = state.get("agent_outputs", [])
    outputs.append({"node": node_name, "output": output})
    return {"agent_outputs": outputs}
