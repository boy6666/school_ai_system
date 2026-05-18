# school_agent/nodes.py
# 统一节点入口：整合所有智能体节点与核心处理函数
# 同时保持与旧版本 graph 定义的兼容性

from typing import Any, Dict, List

from school_agent.agents.evaluation_agent import evaluate_learning, log_interaction
from school_agent.agents.explain_agent import explain_agent
from school_agent.agents.path_agent import path_agent
from school_agent.agents.profile_agent import build_profile, update_profile
from school_agent.agents.quiz_agent import quiz_agent
from school_agent.agents.resource_agent import resource_agent
from school_agent.agents.retrieval_agent import retrieval_agent, retrieve_knowledge
from school_agent.agents.router_agent import classify_intent, route_by_intent
from school_agent.agents.safety_agent import route_after_safety, safety_postcheck, safety_precheck
from school_agent.agents.tutor_agent import tutor_agent
from school_agent.constants import INTENT_REJECT
from school_agent.services.resource_store import save_resources
from school_agent.state import StudentState
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import compact_text, get_main_topic, to_text


# ========== 原有核心节点（已适配新 state） ==========
def finalize_output(state: dict) -> dict:
    """最终输出节点：保存资源，并整理最终回答（与 graph.py 中的一致）。"""
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


def generate_resources(state: dict) -> dict:
    """
    通用资源生成节点（保留原有逻辑，与新 resource_agent 并存）。
    注意：在最新的 graph.py 中，此节点不再被使用，resource_agent 已替代。
    此处保留是为了兼容仍引用此函数的旧图定义。
    """
    from school_agent.agents.resource_agent import resource_agent
    return resource_agent(state)


def plan_learning_path(state: dict) -> dict:
    """
    学习路径规划节点（保留原有逻辑，与新 path_agent 并存）。
    在最新 graph 中，path_agent 在 resource_agent 之后自动调用。
    """
    from school_agent.agents.path_agent import path_agent
    return path_agent(state)


def save_outputs(state: dict) -> dict:
    """
    保存输出节点（兼容旧版，新版使用 finalize_output）。
    """
    return finalize_output(state)


def explain_question(state: dict) -> dict:
    """
    题目讲解节点（独立于 explain_agent，保留原有 prompt 风格）。
    新版系统中，题目讲解推荐使用 explain_agent（通过意图分类触发）。
    """
    # 从 state 中获取题目（兼容新旧两种字段）
    question = state.get("question") or state.get("user_input", "")
    if not question:
        return {"explanation": "没有收到题目，请提供需要讲解的题目。"}

    profile = state.get("profile", {})
    if not profile.get("cognitive_style"):
        default_profile = {
            "knowledge_base": "基础一般",
            "cognitive_style": "逻辑推导型",
            "weaknesses": [],
            "mistake_patterns": "概念混淆"
        }
        for k, v in default_profile.items():
            if k not in profile or not profile[k]:
                profile[k] = v

    knowledge_base = to_text(profile.get("knowledge_base", "基础一般"))
    cognitive_style = to_text(profile.get("cognitive_style", "逻辑推导型"))
    weaknesses = to_text(profile.get("weaknesses", []))
    mistake_preference = to_text(profile.get("mistake_patterns", "概念混淆"))

    # 使用知识库检索相关信息
    from school_agent.kb import search_knowledge_base
    context = search_knowledge_base(question, max_chars=800)

    from school_agent.config import model  # 需要在 config 中暴露 model 实例
    prompt = f"""
你是学生的一位智能辅导老师。请根据以下学生画像，对下方【题目】进行一步一步的个性化讲解。

学生画像：
- 知识基础：{knowledge_base}
- 认知风格：{cognitive_style}
- 薄弱点：{weaknesses}
- 易错倾向：{mistake_preference}

知识库相关内容（供参考）：
{context}

讲解要求（严格遵守）：
1. 先明确指出本题考察的核心知识点。
2. 如果学生对该知识点薄弱，应先补充该知识点的核心定义或关键公式（不超过3句话）。
3. 分成 2~4 个逻辑步骤讲解解题过程，每步说明“为什么”这样做。
4. 在容易出错的步骤处，增加一个【⚠️常见错误提醒】框，结合学生的易错倾向。
5. 最后用“一句话总结”或“记忆口诀”帮助记忆。
6. 整体语言风格：若学生是“例子驱动型”，多用生活例子；若“逻辑推导型”，多用符号推导。

【题目】
{question}

请输出讲解内容（纯文本，不要JSON）：
"""
    response = model.invoke(prompt)
    explanation = response.content

    return {
        "explanation": explanation,
        "final_answer": explanation,
        "agent_outputs": merge_agent_output(
            state,
            "explain_question",
            {"status": "success", "question": question}
        ),
    }


# ========== 为了兼容性，将 agents 中的函数重新导出 ==========
__all__ = [
    "build_profile",
    "classify_intent",
    "evaluate_learning",
    "explain_agent",
    "explain_question",
    "finalize_output",
    "generate_resources",
    "log_interaction",
    "merge_agent_output",        # 工具函数
    "path_agent",
    "plan_learning_path",
    "quiz_agent",
    "resource_agent",
    "retrieval_agent",
    "retrieve_knowledge",
    "route_after_safety",
    "route_by_intent",
    "safety_postcheck",
    "safety_precheck",
    "save_outputs",
    "tutor_agent",
    "update_profile",
]