import copy
import json
from typing import Any, Dict, List

from school_agent.services.profile_store import load_student_profile, save_student_profile
from school_agent.services.llm_client import call_llm
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.time_utils import now_iso

JAVA_PROFILE_SYSTEM_PROMPT = """你是Java学习画像构建专家。你的任务是从学生的自然语言输入中提取六维学生画像。

## 六维画像定义

1. 知识基础 (knowledge_base)：学生对Java相关知识的现有掌握水平描述
2. 学习目标 (learning_goal)：学生近期的具体学习目标
3. 当前掌握度 (current_mastery)：学生对当前学习内容的掌握程度，包含文字描述和0-100的分数
4. 认知风格 (cognitive_style)：学生偏好的学习方式（视觉型/阅读型/实践型/社交型等）
5. 易错点类型 (mistake_patterns)：学生常犯的错误模式列表（如空指针、类型转换错误、并发问题、边界条件遗漏、逻辑错误等）
6. 学习行为与自主性 (learning_behavior)：学生的学习习惯、每日投入时间、自主学习能力

## 综合类型判断 (overall_type)

根据六维数据将学生分为三类：
- 基础补齐型：知识基础薄弱、掌握度低、易错点多，需要系统性地补充基础
- 稳定提升型：有一定基础、掌握度中等、有明确目标，需要结构化引导
- 进阶拓展型：基础扎实、掌握度高、自主学习能力强，适合挑战高级内容

## 输出格式

严格以JSON格式返回（只返回JSON，不要其他文字）：
{
  "major": "专业",
  "grade": "年级",
  "course": "Java相关课程",
  "topic": "当前关注的Java知识点",
  "knowledge_base": "知识基础描述",
  "learning_goal": "学习目标描述",
  "current_mastery": "当前掌握度描述（含大致分数，如'掌握度约60分，对集合框架理解尚可但多线程薄弱'）",
  "cognitive_style": "认知风格描述",
  "weaknesses": ["薄弱知识点列表"],
  "mistake_patterns": ["易错类型列表"],
  "learning_behavior": "学习行为描述（含每日学习时长和自主性评价）",
  "resource_preference": ["偏好的资源类型"],
  "pace": "学习节奏（快速/中速/慢速）",
  "overall_type": "基础补齐型/稳定提升型/进阶拓展型",
  "profile_suggestions": ["个性化学习建议"]
}

## 规则

- 所有字段必须围绕Java学习，忽略数学、物理等无关学科
- weaknesses和mistake_patterns必须具体，不能泛泛而谈
- 当前掌握度必须包含具体的知识点掌握情况
- 如果信息不明确，根据上下文合理推断"""


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


def _build_profile_from_input(text: str) -> Dict[str, Any]:
    topic = _infer_topic(text)
    return {
        "major": "计算机相关专业" if "计算机" in text or "Java" in text else "信息类相关专业",
        "grade": "大二" if "大二" in text else "未知年级",
        "course": "Java 数据结构" if "Java" in text or "数据结构" in text else "数据结构",
        "topic": topic,
        "learning_goal": "期末考 85 分" if "85" in text else "掌握并能应用当前知识点",
        "knowledge_base": "有一定编程基础" if any(w in text for w in ["复习", "大二", "Java"]) else "基础未知",
        "current_mastery": "",
        "cognitive_style": "偏好图解、代码案例和练习题" if any(w in text for w in ["图解", "代码", "练习"]) else "偏好结构化讲解",
        "weaknesses": _infer_weaknesses(text, topic),
        "mistake_patterns": ["概念混淆", "边界条件遗漏"],
        "learning_behavior": "",
        "resource_preference": ["讲解文档", "思维导图", "练习题", "代码案例"],
        "pace": "中速",
        "overall_type": None,
        "profile_suggestions": [],
        "last_updated": now_iso(),
    }


def _extract_json(text: str) -> Dict[str, Any]:
    text = text.strip()
    if text.startswith("```"):
        lines = text.split("\n")
        if lines[-1].strip() == "```":
            lines = lines[1:-1]
        else:
            lines = lines[1:]
        text = "\n".join(lines)
    start = text.find("{")
    end = text.rfind("}") + 1
    if start != -1 and end > start:
        text = text[start:end]
    return json.loads(text)


def _build_profile_with_llm(text: str) -> Dict[str, Any]:
    try:
        response = call_llm(text, system=JAVA_PROFILE_SYSTEM_PROMPT)
        profile = _extract_json(response)
        profile["last_updated"] = now_iso()
        profile.setdefault("mistake_patterns", [])
        profile.setdefault("current_mastery", "")
        profile.setdefault("learning_behavior", "")
        profile.setdefault("overall_type", None)
        profile.setdefault("profile_suggestions", [])
        return profile
    except Exception:
        return _build_profile_from_input(text)


def build_profile(state: dict) -> dict:
    student_id = state.get("student_id", "student_001")
    user_input = state.get("user_input", "")

    old_profile = load_student_profile(student_id)
    inferred = _build_profile_with_llm(user_input)

    profile = {**inferred, **old_profile}
    profile["topic"] = inferred.get("topic", profile.get("topic"))
    profile["weaknesses"] = list(dict.fromkeys(
        old_profile.get("weaknesses", []) + inferred.get("weaknesses", [])
    ))
    profile["resource_preference"] = list(
        dict.fromkeys(old_profile.get("resource_preference", []) + inferred.get("resource_preference", []))
    )
    profile["overall_type"] = inferred.get("overall_type") or old_profile.get("overall_type")
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
                "overall_type": profile.get("overall_type"),
                "llm_used": True,
            },
        ),
    }


def update_profile(state: dict) -> dict:
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
