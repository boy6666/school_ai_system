from school_agent.agents.profile_agent import build_profile


def test_build_profile():
    result = build_profile({
        "student_id": "test_profile_user",
        "user_input": "我是计算机大二学生，递归和二叉树不懂，喜欢图解和代码"
    })
    assert "profile" in result
    assert result["profile"]["topic"] in ["递归", "二叉树"]
    assert len(result["profile"]["weaknesses"]) >= 1
