from typing import Any, Dict, List, TypedDict


class StudentState(TypedDict, total=False):
    """LangGraph 全局状态。

    所有智能体都只通过这个 state 传递数据：
    - 输入：用户请求、学生 ID、会话 ID
    - 中间态：画像、意图、知识库检索、安全报告、评估报告
    - 输出：最终回答、学习资源、学习路径、资源目录
    """

    # 基础信息
    user_input: str
    student_id: str
    session_id: str

    # 意图分类
    intent: str
    intent_confidence: float
    route_reason: str

    # 学生画像
    profile: Dict[str, Any]
    profile_before: Dict[str, Any]
    profile_patch: Dict[str, Any]

    # 知识库检索
    retrieved_context: str
    retrieved_docs: List[Dict[str, Any]]

    # 智能体输出
    agent_outputs: Dict[str, Any]

    # 生成资源和学习路径
    resources: Dict[str, Any]
    learning_path: List[Dict[str, Any]]

    # 安全、评估、日志
    safety_report: Dict[str, Any]
    evaluation_report: Dict[str, Any]
    log_path: str

    # 最终输出
    final_answer: str
    resource_dir: str
    error: str
