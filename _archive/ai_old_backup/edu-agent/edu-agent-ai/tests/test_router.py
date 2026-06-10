from school_agent.agents.router_agent import classify_intent
from school_agent.constants import INTENT_QUIZ, INTENT_RESOURCE


def test_classify_quiz():
    result = classify_intent({"user_input": "帮我出几道递归练习题"})
    assert result["intent"] == INTENT_QUIZ


def test_classify_resource():
    result = classify_intent({"user_input": "帮我生成学习资料和思维导图"})
    assert result["intent"] == INTENT_RESOURCE
