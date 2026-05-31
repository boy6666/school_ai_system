from school_agent.graph import graph


def test_graph_invoke():
    result = graph.invoke({
        "student_id": "test_graph_user",
        "session_id": "test",
        "user_input": "我是计算机大二学生，递归不懂，帮我生成学习资料和练习题",
    }, config={"configurable": {"thread_id": "test_graph_thread"}})
    assert "final_answer" in result
    assert "profile" in result
    assert "intent" in result
