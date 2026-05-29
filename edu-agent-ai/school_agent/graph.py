from typing import Callable, Dict

from school_agent.agents.evaluation_agent import evaluate_learning, log_interaction
from school_agent.agents.explain_agent import explain_agent
from school_agent.agents.path_agent import path_agent
from school_agent.agents.profile_agent import extract_profile, init_profile
from school_agent.agents.quiz_agent import quiz_agent
from school_agent.agents.resource_agent import resource_agent
from school_agent.agents.retrieval_agent import retrieval_agent, retrieve_knowledge
from school_agent.agents.router_agent import classify_intent, route_by_intent
from school_agent.agents.safety_agent import route_after_safety, safety_postcheck, safety_precheck
from school_agent.agents.tutor_agent import tutor_agent
from school_agent.constants import (
    INTENT_EXPLAIN,
    INTENT_QUIZ,
    INTENT_REJECT,
    INTENT_RESOURCE,
    INTENT_RETRIEVE,
    INTENT_TUTOR,
)
from school_agent.services.resource_store import save_resources
from school_agent.state import StudentState
from school_agent.utils.json_utils import merge_agent_output


def finalize_output(state: dict) -> dict:
    """最终输出节点：保存资源，并整理最终回答。"""
    if state.get("intent") == INTENT_REJECT:
        return {
            "final_answer": state.get("final_answer", "请求未通过安全检查。"),
            "agent_outputs": merge_agent_output(
                state,
                "finalize_output",
                {"status": "rejected"},
            ),
        }

    resources = state.get("resources", {})
    learning_path = state.get("learning_path", [])
    resource_dir = state.get("resource_dir", "")

    if resources:
        resource_dir = save_resources(
            student_id=state.get("student_id", "student_001"),
            session_id=state.get("session_id", "default_session"),
            profile=state.get("profile", {}),
            resources=resources,
            learning_path=learning_path,
        )

    final_answer = state.get("final_answer", "")
    if resources and learning_path:
        final_answer = (
            f"{final_answer}\n\n"
            f"系统已规划 {len(learning_path)} 步学习路径，并将资源保存到：{resource_dir}"
        ).strip()

    if not final_answer:
        final_answer = "本轮流程已完成，但没有生成可展示内容。"

    return {
        "final_answer": final_answer,
        "resource_dir": resource_dir,
        "agent_outputs": merge_agent_output(
            state,
            "finalize_output",
            {"status": "success", "resource_dir": resource_dir},
        ),
    }


class SimpleGraph:
    """LangGraph 未安装时的顺序执行兜底。

    正式比赛环境安装 langgraph 后，会自动使用真正的 StateGraph。
    这个类只用于本地快速测试流程。

    新流程：init_profile → classify → safety → retrieve → intent → ... → evaluate → extract_profile → finalize
    画像在后台自动生成/更新，不再需要用户主动填写。
    """

    def invoke(self, input_state: dict, config: dict | None = None) -> dict:
        state = dict(input_state)

        # 初始化/加载画像（后台操作，不打断对话）
        for func in [init_profile, classify_intent, safety_precheck]:
            state.update(func(state))

        if route_after_safety(state) == "reject":
            state.update(finalize_output(state))
            return state

        state.update(retrieve_knowledge(state))

        intent = route_by_intent(state)
        if intent == INTENT_EXPLAIN:
            state.update(explain_agent(state))
        elif intent == INTENT_QUIZ:
            state.update(quiz_agent(state))
        elif intent == INTENT_RETRIEVE:
            state.update(retrieval_agent(state))
        elif intent == INTENT_RESOURCE:
            state.update(resource_agent(state))
            state.update(path_agent(state))
        elif intent == INTENT_TUTOR:
            state.update(tutor_agent(state))
        else:
            state.update(explain_agent(state))

        # 闭环节点：安全复核 → 评估 → 后台提取画像 → 日志 → 输出
        for func in [safety_postcheck, evaluate_learning, extract_profile, log_interaction, finalize_output]:
            state.update(func(state))

        return state


def _build_langgraph():
    try:
        from langgraph.graph import END, START, StateGraph
        try:
            from langgraph.checkpoint.memory import InMemorySaver
        except Exception:  # pragma: no cover
            from langgraph.checkpoint.memory import MemorySaver as InMemorySaver
    except Exception:
        return SimpleGraph()

    builder = StateGraph(StudentState)

    # 前置节点：初始化画像（后台加载/创建，不对话）
    builder.add_node("init_profile", init_profile)
    builder.add_node("classify_intent", classify_intent)
    builder.add_node("safety_precheck", safety_precheck)
    builder.add_node("retrieve_knowledge", retrieve_knowledge)

    # 专业智能体节点
    builder.add_node("explain_agent", explain_agent)
    builder.add_node("quiz_agent", quiz_agent)
    builder.add_node("retrieval_agent", retrieval_agent)
    builder.add_node("resource_agent", resource_agent)
    builder.add_node("path_agent", path_agent)
    builder.add_node("tutor_agent", tutor_agent)

    # 统一闭环节点：安全 → 评估 → 画像提取 → 日志 → 输出
    builder.add_node("safety_postcheck", safety_postcheck)
    builder.add_node("evaluate_learning", evaluate_learning)
    builder.add_node("extract_profile", extract_profile)
    builder.add_node("log_interaction", log_interaction)
    builder.add_node("finalize_output", finalize_output)

    # 流程连接
    builder.add_edge(START, "init_profile")
    builder.add_edge("init_profile", "classify_intent")
    builder.add_edge("classify_intent", "safety_precheck")

    builder.add_conditional_edges(
        "safety_precheck",
        route_after_safety,
        {
            "continue": "retrieve_knowledge",
            "reject": "finalize_output",
        },
    )

    builder.add_conditional_edges(
        "retrieve_knowledge",
        route_by_intent,
        {
            INTENT_EXPLAIN: "explain_agent",
            INTENT_QUIZ: "quiz_agent",
            INTENT_RETRIEVE: "retrieval_agent",
            INTENT_RESOURCE: "resource_agent",
            INTENT_TUTOR: "tutor_agent",
        },
    )

    builder.add_edge("resource_agent", "path_agent")
    builder.add_edge("path_agent", "safety_postcheck")

    for node_name in ["explain_agent", "quiz_agent", "retrieval_agent", "tutor_agent"]:
        builder.add_edge(node_name, "safety_postcheck")

    builder.add_edge("safety_postcheck", "evaluate_learning")
    builder.add_edge("evaluate_learning", "extract_profile")
    builder.add_edge("extract_profile", "log_interaction")
    builder.add_edge("log_interaction", "finalize_output")
    builder.add_edge("finalize_output", END)

    return builder.compile(checkpointer=InMemorySaver())


graph = _build_langgraph()
