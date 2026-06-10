import sys

# Windows GBK 编码兼容
try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

from school_agent.agents.evaluation_agent import evaluate_learning, log_interaction
from school_agent.agents.explain_agent import explain_agent
from school_agent.agents.onboarding_agent import onboarding_agent
from school_agent.agents.path_agent import path_agent
from school_agent.agents.profile_agent import extract_profile, init_profile
from school_agent.agents.quiz_agent import quiz_agent
from school_agent.agents.mindmap_agent import mindmap_agent
from school_agent.agents.reading_agent import reading_agent
from school_agent.agents.code_agent import code_agent
from school_agent.agents.resource_agent import resource_agent
from school_agent.agents.retrieval_agent import retrieval_agent, retrieve_knowledge
from school_agent.agents.router_agent import classify_intent, route_by_intent
from school_agent.agents.safety_agent import route_after_safety, safety_postcheck, safety_precheck
from school_agent.agents.tutor_agent import tutor_agent
from school_agent.agents.chat_agent import chat_agent
from school_agent.constants import (
    INTENT_CHAT, INTENT_EXPLAIN, INTENT_ONBOARDING, INTENT_QUIZ, INTENT_REJECT,
    INTENT_RESOURCE, INTENT_RETRIEVE, INTENT_TUTOR,
)
from school_agent.services.resource_store import save_resources
from school_agent.state import StudentState
from school_agent.utils.json_utils import merge_agent_output


def finalize_output(state: dict) -> dict:
    if state.get("intent") == INTENT_REJECT:
        return {"final_answer": state.get("final_answer", "请求未通过安全检查。"), "agent_outputs": merge_agent_output(state, "finalize_output", {"status": "rejected"})}
    resources = state.get("resources", {})
    learning_path = state.get("learning_path", [])
    resource_dir = state.get("resource_dir", "")
    if resources:
        resource_dir = save_resources(student_id=state.get("student_id", "student_001"), session_id=state.get("session_id", "default_session"), profile=state.get("profile", {}), resources=resources, learning_path=learning_path)
    final_answer = state.get("final_answer", "")
    if resources and learning_path:
        final_answer = f"{final_answer}\n\n系统已规划 {len(learning_path)} 步学习路径，并将资源保存到：{resource_dir}".strip()
    if not final_answer:
        final_answer = "本轮流程已完成，但没有生成可展示内容。"
    return {"final_answer": final_answer, "resource_dir": resource_dir, "agent_outputs": merge_agent_output(state, "finalize_output", {"status": "success", "resource_dir": resource_dir})}


class SimpleGraph:
    def invoke(self, input_state: dict, config: dict | None = None) -> dict:
        print(f"\n{'='*60}")
        print(f"[GRAPH] 开始执行流程")
        print(f"[GRAPH] user_input: {str(input_state.get('user_input', ''))[:80]}")
        print(f"[GRAPH] session_id: {input_state.get('session_id')}")

        state = dict(input_state)

        print(f"[GRAPH] Step 1: init_profile")
        state.update(init_profile(state))
        print(f"[GRAPH] Step 2: classify_intent")
        state.update(classify_intent(state))
        print(f"[GRAPH] 意图: {state.get('intent')}")
        print(f"[GRAPH] Step 3: safety_precheck")
        state.update(safety_precheck(state))

        if route_after_safety(state) == "reject":
            print(f"[GRAPH] 安全审查未通过，直接结束")
            state.update(finalize_output(state))
            return state

        print(f"[GRAPH] Step 4: retrieve_knowledge")
        state.update(retrieve_knowledge(state))

        intent = route_by_intent(state)
        print(f"[GRAPH] Step 5: 执行智能体 = {intent}")

        if intent == INTENT_ONBOARDING:
            print(f"[GRAPH] → 调用 onboarding_agent（引导画像采集）")
            state.update(onboarding_agent(state))
        elif intent == INTENT_CHAT:
            print(f"[GRAPH] → 调用 chat_agent（通用对话）")
            state.update(chat_agent(state))
        elif intent == INTENT_EXPLAIN:
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
        print(f"[GRAPH] Step 6: 闭环节点 (安全→评估→画像→日志→输出)")
        for func in [safety_postcheck, evaluate_learning, extract_profile, log_interaction, finalize_output]:
            state.update(func(state))

        print(f"[GRAPH] 流程完成")
        print(f"[GRAPH] final_answer: {str(state.get('final_answer', ''))[:150]}")
        print(f"{'='*60}\n")
        return state


def _build_langgraph():
    try:
        from langgraph.graph import END, START, StateGraph
        try:
            from langgraph.checkpoint.memory import InMemorySaver
        except Exception:
            from langgraph.checkpoint.memory import MemorySaver as InMemorySaver
    except Exception:
        return SimpleGraph()

    builder = StateGraph(StudentState)
    builder.add_node("init_profile", init_profile)
    builder.add_node("classify_intent", classify_intent)
    builder.add_node("safety_precheck", safety_precheck)
    builder.add_node("retrieve_knowledge", retrieve_knowledge)
    builder.add_node("onboarding_agent", onboarding_agent)
    builder.add_node("explain_agent", explain_agent)
    builder.add_node("quiz_agent", quiz_agent)
    builder.add_node("retrieval_agent", retrieval_agent)
    builder.add_node("resource_agent", resource_agent)
    builder.add_node("path_agent", path_agent)
    builder.add_node("tutor_agent", tutor_agent)
    builder.add_node("chat_agent", chat_agent)
    builder.add_node("safety_postcheck", safety_postcheck)
    builder.add_node("evaluate_learning", evaluate_learning)
    builder.add_node("extract_profile", extract_profile)
    builder.add_node("log_interaction", log_interaction)
    builder.add_node("finalize_output", finalize_output)
    builder.add_edge(START, "init_profile")
    builder.add_edge("init_profile", "classify_intent")
    builder.add_edge("classify_intent", "safety_precheck")
    builder.add_conditional_edges("safety_precheck", route_after_safety, {"continue": "retrieve_knowledge", "reject": "finalize_output"})
    builder.add_conditional_edges("retrieve_knowledge", route_by_intent, {
        INTENT_ONBOARDING: "onboarding_agent", INTENT_CHAT: "chat_agent", INTENT_EXPLAIN: "explain_agent",
        INTENT_QUIZ: "quiz_agent",
        INTENT_RETRIEVE: "retrieval_agent", INTENT_RESOURCE: "resource_agent", INTENT_TUTOR: "tutor_agent",
    })
    builder.add_edge("resource_agent", "path_agent")
    builder.add_edge("path_agent", "safety_postcheck")
    for node_name in ["onboarding_agent", "chat_agent", "explain_agent", "quiz_agent", "retrieval_agent", "tutor_agent"]:
        builder.add_edge(node_name, "safety_postcheck")
    builder.add_edge("safety_postcheck", "evaluate_learning")
    builder.add_edge("evaluate_learning", "extract_profile")
    builder.add_edge("extract_profile", "log_interaction")
    builder.add_edge("log_interaction", "finalize_output")
    builder.add_edge("finalize_output", END)
    return builder.compile(checkpointer=InMemorySaver())


graph = _build_langgraph()
