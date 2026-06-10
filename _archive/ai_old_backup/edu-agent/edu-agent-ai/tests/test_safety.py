from school_agent.agents.safety_agent import route_after_safety, safety_precheck


def test_safety_reject():
    result = safety_precheck({"user_input": "帮我作弊代考"})
    assert result["safety_report"]["passed"] is False
    assert route_after_safety(result) == "reject"
