from school_agent.kb.retriever import build_context, search_documents
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import get_main_topic, to_text


def retrieve_knowledge(state: dict) -> dict:
    """知识库检索节点：只根据用户当前输入和画像主题检索，避免历史薄弱点污染。"""
    profile = state.get("profile", {})
    user_input = state.get("user_input", "")
    topic = profile.get("topic", "")   # 例如 "Java 基础语法"
    
    # 构建干净的查询：优先使用用户输入，如果用户输入太短则加上主题
    if len(user_input) < 5 and topic:
        query = topic
    else:
        query = f"{topic} {user_input}".strip()
    
    # 检索文档（最多5篇）
    docs = search_documents(query, top_k=5)
    context = build_context(docs)
    
    return {
        "retrieved_docs": docs,
        "retrieved_context": context,
        "agent_outputs": merge_agent_output(
            state,
            "retrieve_knowledge",
            {"status": "success", "doc_count": len(docs), "query": query},
        ),
    }


def retrieval_agent(state: dict) -> dict:
    """检索推荐智能体范例：把检索结果整理成用户可读推荐。"""
    profile = state.get("profile", {})
    topic = get_main_topic(profile)
    docs = state.get("retrieved_docs", [])

    lines = [f"## 课程资料推荐：{topic}", ""]
    if not docs:
        lines.append("当前知识库没有命中资料，建议补充课程文档。")
    else:
        lines.append("根据你的画像和问题，推荐优先阅读以下资料：")
        for idx, doc in enumerate(docs, start=1):
            lines.append(f"{idx}. **{doc.get('title')}**")
            lines.append(f"   - 课程：{doc.get('course')}")
            lines.append(f"   - 来源：{doc.get('relative_path')}")
            lines.append(f"   - 推荐理由：与当前知识点或薄弱点相关。")

    answer = "\n".join(lines)

    return {
        "final_answer": answer,
        "agent_outputs": merge_agent_output(
            state,
            "retrieval_agent",
            {"status": "success", "recommended_count": len(docs)},
        ),
    }
