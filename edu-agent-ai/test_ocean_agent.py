"""
测试 Ocean 开发的学生画像识别 Agent
"""
# -*- coding: utf-8 -*-
import sys
sys.stdout.reconfigure(encoding='utf-8')
import json
from school_agent.agents.profile_agent import (
    build_profile,
    update_profile,
    classify_student_profile,
    build_profile_suggestions,
    _build_profile_from_input,
)


def print_section(title, data):
    print(f"\n{'='*60}")
    print(f"  {title}")
    print(f"{'='*60}")
    if isinstance(data, (dict, list)):
        print(json.dumps(data, ensure_ascii=False, indent=2))
    else:
        print(data)


def test_build_profile():
    """测试构建学生画像"""
    print_section("测试1: 构建学生画像", "开始测试...")

    # 测试用例1: 大二学生，递归和二叉树薄弱
    test_cases = [
        {
            "student_id": "test_student_001",
            "user_input": "我是计算机专业大二学生，正在学习 Java 数据结构。递归和二叉树一直不太懂，希望用图解、代码案例和练习题复习，目标是期末考 85 分。"
        },
        {
            "student_id": "test_student_002",
            "user_input": "我是零基础开始学习，对链表和数组都不会，希望能有基础讲解"
        },
        {
            "student_id": "test_student_003",
            "user_input": "我已经掌握了基础数据结构，想要进阶学习，提升综合能力"
        },
    ]

    for i, test_case in enumerate(test_cases, 1):
        print(f"\n--- 测试用例 {i} ---")
        print(f"输入: {test_case['user_input'][:50]}...")

        result = build_profile(test_case)
        profile = result["profile"]

        print(f"[OK] 主题识别: {profile.get('topic')}")
        print(f"[OK] 薄弱点: {profile.get('weaknesses')}")
        print(f"[OK] 画像类型: {profile.get('overall_type')}")
        print(f"[OK] 学习建议数: {len(profile.get('profile_suggestions', []))}")

        # 验证基本字段
        assert profile.get('topic'), "主题不能为空"
        assert profile.get('overall_type'), "画像类型不能为空"
        assert profile.get('profile_suggestions'), "学习建议不能为空"

        print_section(f"完整画像 {i}", profile)


def test_classify_student_profile():
    """测试学生画像分类"""
    print_section("测试2: 学生画像分类", "开始测试...")

    # 模拟三种类型的学生
    test_profiles = [
        {
            "knowledge_base": "零基础",
            "weaknesses": ["递归", "二叉树", "链表"],
            "mistake_patterns": ["概念混淆", "不会用"],
            "learning_goal": "期末考 60 分",
            "cognitive_style": "偏好图解",
            "pace": "中速"
        },
        {
            "knowledge_base": "有一定编程基础",
            "weaknesses": ["复杂度"],
            "mistake_patterns": ["边界条件遗漏"],
            "learning_goal": "期末考 80 分",
            "cognitive_style": "偏好代码案例",
            "pace": "中速"
        },
        {
            "knowledge_base": "基础扎实",
            "weaknesses": [],
            "mistake_patterns": [],
            "learning_goal": "期末考 95 分",
            "cognitive_style": "偏好项目实战",
            "pace": "快速"
        }
    ]

    expected_types = ["基础补齐型", "稳定提升型", "进阶拓展型"]

    for i, profile in enumerate(test_profiles):
        result_type = classify_student_profile(profile)
        expected = expected_types[i]
        print(f"\n画像 {i+1}: {result_type}")
        assert result_type == expected, f"期望 {expected}, 实际 {result_type}"
        print(f"[OK] 分类正确")


def test_build_profile_suggestions():
    """测试学习建议生成"""
    print_section("测试3: 学习建议生成", "开始测试...")

    profile_types = ["基础补齐型", "稳定提升型", "进阶拓展型"]

    for profile_type in profile_types:
        suggestions = build_profile_suggestions(profile_type)
        print(f"\n{profile_type} 的建议:")
        for suggestion in suggestions:
            print(f"  - {suggestion}")

        assert len(suggestions) >= 3, "每种类型至少有3条建议"


def test_update_profile():
    """测试更新学生画像"""
    print_section("测试4: 更新学生画像", "开始测试...")

    # 模拟评估报告
    state = {
        "student_id": "test_student_001",
        "profile": {
            "weaknesses": ["递归"],
            "overall_type": "稳定提升型",
            "profile_suggestions": ["原有建议"]
        },
        "evaluation_report": {
            "weak_points": ["二叉树", "栈"],
            "suggestion": "需要重点练习二叉树遍历",
            "understanding_score": 65
        }
    }

    result = update_profile(state)
    updated_profile = result["profile"]

    print(f"更新后薄弱点: {updated_profile.get('weaknesses')}")
    print(f"更新后分数: {updated_profile.get('last_score')}")
    print(f"[OK] 薄弱点已合并")

    # 验证薄弱点合并
    expected_weaknesses = ["递归", "二叉树", "栈"]
    assert all(w in updated_profile.get('weaknesses', []) for w in expected_weaknesses)


def test_integration():
    """测试完整的流程集成"""
    print_section("测试5: 完整流程集成", "开始测试...")

    state = {
        "student_id": "integration_test_001",
        "session_id": "test_session",
        "user_input": "我是计算机大二学生，递归和二叉树不懂，喜欢图解和代码"
    }

    # 调用 build_profile
    result = build_profile(state)

    print(f"[OK] 画像构建成功")
    print(f"  - 主题: {result['profile']['topic']}")
    print(f"  - 类型: {result['profile']['overall_type']}")
    print(f"  - 薄弱点: {result['profile']['weaknesses']}")

    # 模拟评估并更新
    result.update({
        "evaluation_report": {
            "weak_points": ["栈"],
            "suggestion": "需要加强栈的操作练习",
            "understanding_score": 70
        }
    })

    # 调用 update_profile
    update_result = update_profile(result)

    print(f"[OK] 画像更新成功")
    print(f"  - 薄弱点: {update_result['profile']['weaknesses']}")
    print(f"  - 类型: {update_result['profile']['overall_type']}")


def main():
    print("\n" + "="*60)
    print("  Ocean Agent 画像识别模块测试")
    print("="*60)

    try:
        test_build_profile()
        test_classify_student_profile()
        test_build_profile_suggestions()
        test_update_profile()
        test_integration()

        print_section("测试结果", "[SUCCESS] 所有测试通过！Ocean 的画像识别 Agent 运行正常！")

    except AssertionError as e:
        print(f"\n[ERROR] 测试失败: {e}")
    except Exception as e:
        print(f"\n[ERROR] 运行出错: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    main()