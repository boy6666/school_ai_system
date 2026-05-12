from langgraph.graph import StateGraph, START, END
from langgraph.checkpoint.memory import InMemorySaver

from school_agent.state import StudentState
from school_agent.nodes import (
    build_profile,
    retrieve_knowledge,
    generate_resources,
    plan_learning_path,
    save_outputs,
)


builder = StateGraph(StudentState)

# 1. 构建学生画像
builder.add_node("build_profile", build_profile)

# 2. 检索知识库
builder.add_node("retrieve_knowledge", retrieve_knowledge)

# 3. 生成学习资源
builder.add_node("generate_resources", generate_resources)

# 4. 规划学习路径
builder.add_node("plan_learning_path", plan_learning_path)

# 5. 保存结果并生成最终回答
builder.add_node("save_outputs", save_outputs)


builder.add_edge(START, "build_profile")
builder.add_edge("build_profile", "retrieve_knowledge")
builder.add_edge("retrieve_knowledge", "generate_resources")
builder.add_edge("generate_resources", "plan_learning_path")
builder.add_edge("plan_learning_path", "save_outputs")
builder.add_edge("save_outputs", END)


graph = builder.compile(checkpointer=InMemorySaver())