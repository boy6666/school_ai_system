import copy
from typing import Any, Dict, List

from school_agent.services.profile_store import load_student_profile, save_student_profile
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.time_utils import now_iso


def _infer_topic(text: str) -> str:
    candidates = ["递归", "二叉树", "链表", "数组", "栈", "队列", "哈希表", "字符串", "排序", "Java"]
    for item in candidates:
        if item in text:
            return item
    return "数据结构基础"


def _infer_weaknesses(text: str, topic: str) -> List[str]:
    weaknesses = []
    for item in ["递归", "二叉树", "链表", "复杂度", "代码实现", "边界条件"]:
        if item in text:
            weaknesses.append(item)
    if any(word in text for word in ["不会", "不懂", "薄弱", "错", "看不懂"]):
        weaknesses.append(topic)
    return list(dict.fromkeys(weaknesses or [topic]))

def _to_text(value: Any) -> str:
    if value is None:
        return ""
    if isinstance(value, list):
        return " ".join(str(item) for item in value)
    if isinstance(value, dict):
        return " ".join(f"{k} {v}" for k, v in value.items())
    return str(value)


def classify_student_profile(profile: Dict[str, Any]) -> str:
    """
    根据学生画像将学生分为三类：
    1. 基础补齐型
    2. 稳定提升型
    3. 进阶拓展型
    """
    knowledge_base = _to_text(profile.get("knowledge_base"))
    weaknesses = profile.get("weaknesses", [])
    mistake_patterns = _to_text(profile.get("mistake_patterns"))
    learning_goal = _to_text(profile.get("learning_goal"))
    cognitive_style = _to_text(profile.get("cognitive_style"))
    pace = _to_text(profile.get("pace"))

    text = f"{knowledge_base} {_to_text(weaknesses)} {mistake_patterns} {learning_goal} {cognitive_style} {pace}"

    score = 0

    # 1. 知识基础评分
    if any(word in text for word in ["没基础", "零基础", "基础薄弱", "基础未知", "不会", "不懂", "看不懂"]):
        score += 1
    elif any(word in text for word in ["有一定基础", "有点基础", "复习", "学过", "中速"]):
        score += 2
    elif any(word in text for word in ["基础扎实", "熟练", "掌握较好", "进阶"]):
        score += 3
    else:
        score += 2

    # 2. 薄弱点评分
    if isinstance(weaknesses, str):
        weaknesses = [weaknesses]

    if len(weaknesses) >= 3:
        score += 1
    elif len(weaknesses) >= 1:
        score += 2
    else:
        score += 3

    # 3. 易错模式评分
    if any(word in text for word in ["概念混淆", "边界条件遗漏", "不会用", "不知道", "错"]):
        score += 1
    else:
        score += 2

    if score <= 4:
        return "基础补齐型"
    if score <= 7:
        return "稳定提升型"
    return "进阶拓展型"


def build_profile_suggestions(overall_type: str) -> List[str]:
    if overall_type == "基础补齐型":
        return [
            "建议先补充前置知识，降低学习难度，从基础概念和简单例题开始。",
            "建议采用分步骤讲解，每次只聚焦一个知识点。",
            "建议多使用图解、代码示例和基础练习帮助学生建立信心。",
        ]

    if overall_type == "稳定提升型":
        return [
            "建议围绕薄弱点进行查漏补缺。",
            "建议增加变式练习，提升知识迁移能力。",
            "建议定期复盘错题，形成稳定的解题方法。",
        ]

    return [
        "建议增加综合题、项目任务和跨知识点应用训练。",
        "建议引导学生进行自主总结和拓展学习。",
        "建议提供更高阶的挑战任务，提升实践能力。",
    ]

def _build_profile_from_input(text: str) -> Dict[str, Any]:
    topic = _infer_topic(text)
    return {
        "major": "计算机相关专业" if "计算机" in text or "Java" in text else "信息类相关专业",
        "grade": "大二" if "大二" in text else "未知年级",
        "course": "Java 数据结构" if "Java" in text or "数据结构" in text else "数据结构",
        "topic": topic,
        "learning_goal": "期末考 85 分" if "85" in text else "掌握并能应用当前知识点",
        "knowledge_base": "有一定编程基础" if any(w in text for w in ["复习", "大二", "Java"]) else "基础未知",
        "cognitive_style": "偏好图解、代码案例和练习题" if any(w in text for w in ["图解", "代码", "练习"]) else "偏好结构化讲解",
        "weaknesses": _infer_weaknesses(text, topic),
        "mistake_patterns": ["概念混淆", "边界条件遗漏"],
        "resource_preference": ["讲解文档", "思维导图", "练习题", "代码案例"],
        "pace": "中速",
        "last_updated": now_iso(),
    }


def build_profile(state: dict) -> dict:
    """画像智能体范例：加载历史画像，并根据本轮输入做轻量补全。"""
    student_id = state.get("student_id", "student_001")
    user_input = state.get("user_input", "")

    old_profile = load_student_profile(student_id)
    inferred = _build_profile_from_input(user_input)

    profile = {**inferred, **old_profile}
    # 本轮输入里的 topic/weaknesses 优先级更高，避免历史画像挡住当前需求。
    profile["topic"] = inferred["topic"]
    profile["weaknesses"] = list(dict.fromkeys(old_profile.get("weaknesses", []) + inferred["weaknesses"]))
    profile["resource_preference"] = list(
        dict.fromkeys(old_profile.get("resource_preference", []) + inferred["resource_preference"])
    )
    profile["last_updated"] = now_iso()

    overall_type = classify_student_profile(profile)
    profile["overall_type"] = overall_type
    profile["profile_suggestions"] = build_profile_suggestions(overall_type)

    return {
        "profile_before": copy.deepcopy(old_profile),
        "profile": profile,
        "agent_outputs": merge_agent_output(
            state,
            "profile_agent",
            {
                "status": "success",
                "loaded_existing_profile": bool(old_profile),
                "topic": profile.get("topic"),
                 "overall_type": overall_type,
            },
        ),
    }


def update_profile(state: dict) -> dict:
    """画像更新智能体范例：根据评估报告回写薄弱点和建议。"""
    student_id = state.get("student_id", "student_001")
    profile = copy.deepcopy(state.get("profile", {}))
    evaluation = state.get("evaluation_report", {})

    weak_points = evaluation.get("weak_points", [])
    old_weaknesses = profile.get("weaknesses", [])
    if isinstance(old_weaknesses, str):
        old_weaknesses = [old_weaknesses]

    merged = list(dict.fromkeys(old_weaknesses + weak_points))
    profile["weaknesses"] = merged
    profile["last_suggestion"] = evaluation.get("suggestion", "")
    profile["last_score"] = evaluation.get("understanding_score")
    profile["last_updated"] = now_iso()
    overall_type = classify_student_profile(profile)
    profile["overall_type"] = overall_type
    profile["profile_suggestions"] = build_profile_suggestions(overall_type)

    patch = {
        "weaknesses": merged,
        "last_suggestion": profile["last_suggestion"],
        "last_score": profile["last_score"],
        "overall_type": profile["overall_type"],
        "profile_suggestions": profile["profile_suggestions"],
        "last_updated": profile["last_updated"],
    }

    save_student_profile(student_id, profile)

    return {
        "profile": profile,
        "profile_patch": patch,
        "agent_outputs": merge_agent_output(
            state,
            "profile_update_agent",
                        {
                "status": "success",
                "overall_type": overall_type,
                "patch": patch,
            },
        ),
    }
