from typing import TypedDict, Optional, List, Dict, Any


class StudentState(TypedDict, total=False):
    """学习状态类型"""
    student_id: str
    session_id: str
    user_input: str
    intent: str
    intent_confidence: float
    route_reason: str
    profile: Dict[str, Any]
    profile_patch: Dict[str, Any]
    agent_outputs: List[Dict[str, Any]]
    resources: Dict[str, Any]
    learning_path: Dict[str, Any]
    evaluation_report: Dict[str, Any]
    safety_report: Dict[str, Any]
    final_answer: str
    resource_dir: str
