import copy
from typing import Any, Dict, List

from school_agent.services.profile_store import load_student_profile, save_student_profile
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.time_utils import now_iso


def _infer_topic(text: str) -> str:
    candidates = ["ArrayList", "LinkedList", "HashMap", "HashSet", "List", "Map", "Set", "集合", "字符串", "异常", "多线程", "泛型", "IO流", "Socket", "反射", "注解", "抽象类", "接口", "继承", "多态", "封装", "数组", "Lambda", "Stream", "Java"]
    for item in candidates:
        if item.lower() in text.lower():
            return item
    return "Java 基础"


def _infer_weaknesses(text: str, topic: str) -> List[str]:
    weaknesses = []
    for item in ["语法", "编译错误", "空指针", "类型转换", "逻辑错误", "API使用", "概念混淆", "代码实现"]:
        if item in text:
            weaknesses.append(item)
    if any(word in text for word in ["不会", "不懂", "薄弱", "错", "看不懂"]):
        weaknesses.append(topic)
    return list(dict.fromkeys(weaknesses or [topic]))


def _build_profile_from_input(text: str) -> Dict[str, Any]:
    topic = _infer_topic(text)
    return {
        "major": "计算机相关专业" if "计算机" in text or "Java" in text else "信息类相关专业",
        "grade": "大二" if "大二" in text else "未知年级",
        "course": "Java 程序设计",
        "topic": topic,
        "learning_goal": "期末考 85 分" if "85" in text else "掌握并能应用当前知识点",
        "knowledge_base": "有一定编程基础" if any(w in text for w in ["复习", "大二", "Java"]) else "基础未知",
        "cognitive_style": "偏好图解、代码案例和练习题" if any(w in text for w in ["图解", "代码", "练习"]) else "偏好结构化讲解",
        "weaknesses": _infer_weaknesses(text, topic),
        "mistake_patterns": [],
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
            },
        ),
    }


def update_profile(state: dict) -> dict:
    """画像更新智能体范例：根据评估报告回写薄弱点和建议。"""
    student_id = state.get("student_id", "student_001")
    profile = copy.deepcopy(state.get("profile", {}))
    # Always refresh topic from latest user input
    user_input = state.get("user_input", "")
    if user_input:
        fresh_topic = _infer_topic(user_input)
        if fresh_topic:
            profile["topic"] = fresh_topic
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

    patch = {
        "weaknesses": merged,
        "last_suggestion": profile["last_suggestion"],
        "last_score": profile["last_score"],
        "last_updated": profile["last_updated"],
    }

    save_student_profile(student_id, profile)

    return {
        "profile": profile,
        "profile_patch": patch,
        "agent_outputs": merge_agent_output(
            state,
            "profile_update_agent",
            {"status": "success", "patch": patch},
        ),
    }
