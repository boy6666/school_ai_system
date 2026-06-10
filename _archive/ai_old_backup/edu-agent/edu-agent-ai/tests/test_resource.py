from school_agent.agents.resource_agent import resource_agent


def test_resource_agent():
    state = {
        "profile": {
            "course": "Java 数据结构",
            "topic": "二叉树",
            "weaknesses": ["遍历过程"],
            "resource_preference": ["思维导图", "代码案例"],
        },
        "retrieved_context": "二叉树包含根节点、左子树和右子树。",
    }
    result = resource_agent(state)
    assert "resources" in result
    assert "course_doc" in result["resources"]
    assert "mindmap" in result["resources"]
    assert "quiz" in result["resources"]
