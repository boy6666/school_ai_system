from langgraph.graph import StateGraph, START, END
from langgraph.checkpoint.memory import InMemorySaver

from school_agent.state import StudentState
from school_agent.nodes import (
    build_profile,
    retrieve_knowledge,
    generate_resources,
    plan_learning_path,
    save_outputs,
    explain_question,      # 新增导入
)


builder = StateGraph(StudentState)

# 添加所有节点
builder.add_node("build_profile", build_profile)
builder.add_node("retrieve_knowledge", retrieve_knowledge)
builder.add_node("generate_resources", generate_resources)
builder.add_node("plan_learning_path", plan_learning_path)
builder.add_node("save_outputs", save_outputs)
builder.add_node("explain_question", explain_question)   # 新增节点


# 路由函数：判断是题目讲解还是资源生成
def route_after_profile(state: dict) -> str:
    if state.get("question"):
        return "explain_question"
    else:
        return "retrieve_knowledge"


# 构建图边
builder.add_edge(START, "build_profile")
builder.add_conditional_edges(
    "build_profile",
    route_after_profile,
    {
        "explain_question": "explain_question",
        "retrieve_knowledge": "retrieve_knowledge"
    }
)
builder.add_edge("retrieve_knowledge", "generate_resources")
builder.add_edge("generate_resources", "plan_learning_path")
builder.add_edge("plan_learning_path", "save_outputs")
builder.add_edge("save_outputs", END)
builder.add_edge("explain_question", END)   # 讲解完直接结束


graph = builder.compile(checkpointer=InMemorySaver())