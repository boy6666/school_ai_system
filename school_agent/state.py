from typing import TypedDict, Dict, Any, List, Optional


class StudentState(TypedDict, total=False):
    """
    LangGraph 在各个节点之间传递的状态对象。

    每个节点接收 state，然后返回一个 dict。
    LangGraph 会把返回的 dict 合并回 state。
    """
    #资源文件
    resource_dir: str
    # 用户输入
    user_input: str

    # 学生 ID，用来区分不同学生的画像和资源
    student_id: str

    # 学生画像，至少包含 6 个维度
    profile: Dict[str, Any]

    # 从知识库检索出来的上下文内容
    retrieved_context: str

    # 生成的学习资源，包括讲解文档、思维导图、题目、拓展阅读、代码案例
    resources: Dict[str, Any]

    # 个性化学习路径
    learning_path: List[Dict[str, Any]]

    # 最终给控制台展示的文本
    final_answer: str
    
    question: Optional[str]         # 待讲解的题目
    explanation: Optional[str]      # 生成的讲解