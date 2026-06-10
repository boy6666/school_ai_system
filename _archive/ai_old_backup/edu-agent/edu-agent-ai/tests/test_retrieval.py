from school_agent.agents.profile_agent import build_profile
from school_agent.agents.retrieval_agent import retrieve_knowledge


def test_retrieve_knowledge():
    state = {
        "student_id": "test_retrieval_user",
        "user_input": "我想学习 Java 字符串",
    }
    state.update(build_profile(state))
    result = retrieve_knowledge(state)
    assert "retrieved_context" in result
    assert "retrieved_docs" in result
