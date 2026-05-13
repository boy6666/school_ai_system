from school_agent.agents.quiz_agent import quiz_agent


def test_quiz_agent():
    state = {
        "profile": {
            "topic": "递归",
            "weaknesses": ["递归终止条件"],
        }
    }
    result = quiz_agent(state)
    assert "final_answer" in result
    assert "quiz_agent" in result["agent_outputs"]
