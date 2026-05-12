"""
Guardrails 校验模块
确保学生画像、学习资源、学习路径的数据完整性与合法性
"""
from typing import Dict, Any, List

REQUIRED_PROFILE_FIELDS = [
    "major", "course", "learning_goal", "knowledge_base",
    "cognitive_style", "weaknesses", "mistake_preference", "resource_preference",
]

REQUIRED_RESOURCE_TYPES = [
    "course_explanation", "mindmap", "quiz", "extended_reading", "code_practice",
]

PROFILE_DEFAULTS = {
    "major": "计算机相关专业",
    "course": "数据结构",
    "learning_goal": "补齐知识短板，通过期末考试",
    "knowledge_base": "有部分基础，但不系统",
    "cognitive_style": "偏好图解和案例",
    "weaknesses": ["递归", "树结构"],
    "mistake_preference": "暂无明显易错点",
    "resource_preference": ["讲解文档", "思维导图", "练习题", "代码案例"],
}

PROFILE_TYPE_MAP = {
    "major": str,
    "course": str,
    "learning_goal": str,
    "knowledge_base": str,
    "cognitive_style": str,
    "weaknesses": list,
    "mistake_preference": str,
    "resource_preference": list,
}


def sanitize_profile(profile: dict) -> dict:
    """
    修复学生画像：补全缺失字段，修正字段类型和空值
    """
    # 1. 补全缺失字段
    for field, default in PROFILE_DEFAULTS.items():
        if field not in profile:
            profile[field] = default

    # 2. 确保 weaknesses 为列表，且元素为字符串
    if not isinstance(profile.get("weaknesses"), list):
        profile["weaknesses"] = [profile["weaknesses"]] if profile["weaknesses"] else []
    profile["weaknesses"] = [str(w).strip() for w in profile["weaknesses"] if str(w).strip()]
    if not profile["weaknesses"]:
        profile["weaknesses"] = ["待补充"]

    # 3. 确保 resource_preference 为列表，且元素为字符串
    if not isinstance(profile.get("resource_preference"), list):
        profile["resource_preference"] = [profile["resource_preference"]] if profile["resource_preference"] else []
    profile["resource_preference"] = [str(rp).strip() for rp in profile["resource_preference"] if str(rp).strip()]
    if not profile["resource_preference"]:
        profile["resource_preference"] = ["讲解文档", "练习题", "代码案例"]

    # 4. 其他字符串字段非空
    for field in ["major", "course", "learning_goal", "knowledge_base", "cognitive_style", "mistake_preference"]:
        if not isinstance(profile.get(field), str) or not profile[field].strip():
            profile[field] = PROFILE_DEFAULTS[field]

    return profile


def validate_profile(profile: dict) -> None:
    """
    校验学生画像：
    1. 包含全部必需字段
    2. 字段类型正确
    3. 内容非空（列表非空）
    """
    missing = [f for f in REQUIRED_PROFILE_FIELDS if f not in profile]
    if missing:
        raise ValueError(f"学生画像缺少字段：{missing}")

    for field, expected_type in PROFILE_TYPE_MAP.items():
        value = profile.get(field)
        if not isinstance(value, expected_type):
            raise TypeError(f"字段 {field} 应为 {expected_type.__name__}，实际为 {type(value).__name__}")

    if not profile["weaknesses"]:
        raise ValueError("weaknesses 列表不能为空")
    if not profile["resource_preference"]:
        raise ValueError("resource_preference 列表不能为空")

    for w in profile["weaknesses"]:
        if not isinstance(w, str) or not w.strip():
            raise ValueError("weaknesses 中每个弱点必须为非空字符串")

    for rp in profile["resource_preference"]:
        if not isinstance(rp, str) or not rp.strip():
            raise ValueError("resource_preference 中每个偏好必须为非空字符串")

    for field in ["major", "course", "learning_goal", "knowledge_base", "cognitive_style", "mistake_preference"]:
        if not isinstance(profile[field], str) or not profile[field].strip():
            raise ValueError(f"{field} 必须为非空字符串")


def validate_resources(resources: dict) -> None:
    """
    校验学习资源：
    1. 包含全部5类资源
    2. 每类资源非空且格式基本正确
    """
    missing = [f for f in REQUIRED_RESOURCE_TYPES if f not in resources]
    if missing:
        raise ValueError(f"学习资源缺少类型：{missing}")

    # course_explanation 应为非空字符串
    if not isinstance(resources.get("course_explanation"), str) or not resources["course_explanation"].strip():
        raise ValueError("course_explanation 必须为非空字符串")

    # mindmap 应为非空字符串（Mermaid语法）
    if not isinstance(resources.get("mindmap"), str) or not resources["mindmap"].strip():
        raise ValueError("mindmap 必须为非空字符串")

    # quiz 应为列表，且至少包含1个题目，每个题目有 question 和 answer
    quiz = resources.get("quiz")
    if not isinstance(quiz, list) or len(quiz) == 0:
        raise ValueError("quiz 必须为非空列表")
    for idx, q in enumerate(quiz):
        if not isinstance(q, dict):
            raise TypeError(f"quiz[{idx}] 应为字典")
        if "question" not in q or "answer" not in q:
            raise ValueError(f"quiz[{idx}] 缺少 question 或 answer 字段")
        if not q["question"] or not q["answer"]:
            raise ValueError(f"quiz[{idx}] 的 question 和 answer 不能为空")

    # extended_reading 应为非空字符串列表
    reading = resources.get("extended_reading")
    if not isinstance(reading, list) or len(reading) == 0:
        raise ValueError("extended_reading 必须为非空列表")
    for item in reading:
        if not isinstance(item, str) or not item.strip():
            raise ValueError("extended_reading 中的每个条目必须为非空字符串")

    # code_practice 应为非空字符串
    if not isinstance(resources.get("code_practice"), str) or not resources["code_practice"].strip():
        raise ValueError("code_practice 必须为非空字符串")


def validate_learning_path(learning_path: list) -> None:
    """
    校验学习路径：
    1. 非空列表
    2. 每个步骤包含 step, title, resource, reason 字段
    3. step 为正整数且不重复
    4. 字段值非空
    """
    if not isinstance(learning_path, list) or len(learning_path) == 0:
        raise ValueError("学习路径必须为非空列表")

    steps_seen = set()
    for idx, item in enumerate(learning_path):
        if not isinstance(item, dict):
            raise TypeError(f"路径第 {idx+1} 项应为字典")

        required_keys = {"step", "title", "resource", "reason"}
        missing_keys = required_keys - item.keys()
        if missing_keys:
            raise ValueError(f"路径第 {idx+1} 项缺少字段：{missing_keys}")

        step = item["step"]
        if not isinstance(step, int) or step <= 0:
            raise ValueError(f"step 必须为正整数，实际为 {step}")
        if step in steps_seen:
            raise ValueError(f"step 重复：{step}")
        steps_seen.add(step)

        for key in ("title", "resource", "reason"):
            if not isinstance(item[key], str) or not item[key].strip():
                raise ValueError(f"路径第 {idx+1} 项的 {key} 不能为空")