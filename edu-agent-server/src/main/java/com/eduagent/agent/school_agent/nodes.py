import json
from pathlib import Path
from typing import Dict, Any

from langchain_openai import ChatOpenAI

from school_agent.config import (
    MODEL_NAME,
    BASE_URL,
    API_KEY,
    TEMPERATURE,
    MAX_TOKENS,
)
from school_agent.kb import (
    search_knowledge_base,
    get_knowledge_catalog,
    get_top_knowledge_doc,
)
from school_agent.guards import (
    validate_profile,
    validate_resources,
    validate_learning_path,
)


model = ChatOpenAI(
    model=MODEL_NAME,
    base_url=BASE_URL,
    api_key=API_KEY,
    temperature=TEMPERATURE,
    max_tokens=MAX_TOKENS,
)


PROFILE_SCHEMA_TEXT = """
{
  "major": "学生专业",
  "course": "课程，尽量从知识库目录中选择",
  "topic": "本次具体学习知识点，尽量从知识库目录中选择",
  "learning_goal": "学习目标",
  "knowledge_base": "已有知识基础",
  "cognitive_style": "认知风格",
  "weaknesses": ["薄弱点1", "薄弱点2"],
  "mistake_preference": "常见易错倾向",
  "resource_preference": ["讲解文档", "思维导图", "练习题", "代码案例"]
}
"""


def _safe_json_loads(text: str) -> Dict[str, Any]:
    """
    尽量从模型输出中解析 JSON。

    Qwen 小模型可能输出：
    ```json
    {...}
    ```
    或者在 JSON 前后加解释文字。
    """
    if not text:
        return {}

    cleaned = text.strip()
    cleaned = cleaned.replace("```json", "").replace("```", "").strip()

    try:
        return json.loads(cleaned)
    except Exception:
        pass

    start = cleaned.find("{")
    end = cleaned.rfind("}")

    if start != -1 and end != -1 and end > start:
        try:
            return json.loads(cleaned[start:end + 1])
        except Exception:
            return {}

    return {}


def _to_text(value: Any) -> str:
    """
    把 list / str / None 安全转成文本。
    """
    if value is None:
        return ""

    if isinstance(value, list):
        return "、".join(str(x) for x in value)

    return str(value)


def _check_profile(profile: Dict[str, Any]) -> list[str]:
    """
    检查画像字段是否完整。

    这不是业务兜底，只是 schema 校验。
    """
    required_fields = [
        "major",
        "course",
        "topic",
        "learning_goal",
        "knowledge_base",
        "cognitive_style",
        "weaknesses",
        "mistake_preference",
        "resource_preference",
    ]

    errors = []

    for field in required_fields:
        if field not in profile:
            errors.append(f"缺少字段：{field}")
        elif profile[field] in [None, "", [], {}]:
            errors.append(f"字段为空：{field}")

    if "weaknesses" in profile and not isinstance(profile["weaknesses"], list):
        errors.append("weaknesses 必须是数组")

    if "resource_preference" in profile and not isinstance(profile["resource_preference"], list):
        errors.append("resource_preference 必须是数组")

    return errors


def _ai_extract_profile(user_input: str) -> Dict[str, Any]:
    """
    第一次让 AI 根据用户输入 + 知识库目录抽取画像。

    这里不写死 if-else。
    AI 根据知识库目录判断 course/topic。
    """
    catalog = get_knowledge_catalog(query=user_input, limit=25)

    prompt = f"""
你是学生学习画像抽取智能体。

当前系统知识库目录：
{catalog}

任务：
根据学生输入，从知识库目录中选择最相关的 course 和 topic，并构建学生画像。

要求：
1. 只输出 JSON，不要解释。
2. course 尽量从知识库目录的 course 中选择。
3. topic 尽量从知识库目录的 topic/title/tags 中选择。
4. 如果用户说“复习”，说明已有一定基础，不要默认零基础。
5. 如果用户说“大神”，可以理解为基础较强，但仍要根据 topic 规划复习。
6. 如果用户输入“函数”，在 Java 语境下可以对应“方法”。
7. 不要输出“未知课程”“当前知识点”，除非知识库目录完全没有相关内容。
8. weaknesses 必须是数组。
9. resource_preference 必须是数组。

输出 JSON 格式：
{PROFILE_SCHEMA_TEXT}

学生输入：
{user_input}
"""

    resp = model.invoke(prompt)
    return _safe_json_loads(resp.content)


def _ai_repair_profile(
    user_input: str,
    profile: Dict[str, Any],
    errors: list[str],
) -> Dict[str, Any]:
    """
    如果第一次画像不合格，让 AI 根据错误信息修复。
    """
    catalog = get_knowledge_catalog(query=user_input, limit=25)

    prompt = f"""
你是学生学习画像修复智能体。

知识库目录：
{catalog}

用户原始输入：
{user_input}

当前画像：
{json.dumps(profile, ensure_ascii=False)}

错误：
{json.dumps(errors, ensure_ascii=False)}

请修复画像。

要求：
1. 只输出修复后的 JSON，不要解释。
2. 必须补齐所有字段。
3. course 尽量从知识库目录中选择。
4. topic 必须是用户当前最想学习的具体知识点。
5. weaknesses 必须是数组。
6. resource_preference 必须是数组。
7. 不要输出“未知课程”“当前知识点”。

目标 JSON schema：
{PROFILE_SCHEMA_TEXT}
"""

    resp = model.invoke(prompt)
    return _safe_json_loads(resp.content)


def _make_need_more_info_profile(user_input: str, errors: list[str]) -> Dict[str, Any]:
    """
    当 AI 两次都无法生成合格画像时，不写死知识点，
    而是标记需要用户补充信息。
    """
    return {
        "major": "信息不足",
        "course": "信息不足",
        "topic": "信息不足",
        "learning_goal": "需要用户补充学习目标",
        "knowledge_base": "需要用户补充已有基础",
        "cognitive_style": "需要用户补充学习偏好",
        "weaknesses": ["需要用户补充薄弱点"],
        "mistake_preference": "需要用户补充常见错误",
        "resource_preference": ["需要用户补充资源偏好"],
        "need_more_info": True,
        "raw_input": user_input,
        "errors": errors,
    }


def _get_main_topic(profile: Dict[str, Any]) -> str:
    """
    优先使用 AI 抽取出来的 topic。
    """
    topic = profile.get("topic")

    if isinstance(topic, str) and topic.strip() and topic != "信息不足":
        return topic.strip()

    weaknesses = profile.get("weaknesses")

    if isinstance(weaknesses, list) and weaknesses:
        first = str(weaknesses[0])
        if first and "需要用户补充" not in first:
            return first

    if isinstance(weaknesses, str) and weaknesses.strip():
        return weaknesses.strip()

    return str(profile.get("course", "当前知识点"))
def _is_missing_value(value: Any) -> bool:
    """
    判断一个字段是不是无效值。
    """
    if value is None:
        return True

    if value == "":
        return True

    if isinstance(value, str):
        return value.strip() in {
            "未知课程",
            "未知专业",
            "当前知识点",
            "信息不足",
            "需要用户补充学习目标",
            "需要用户补充已有基础",
            "需要用户补充学习偏好",
            "需要用户补充常见错误",
        }

    if isinstance(value, list):
        if not value:
            return True
        joined = " ".join(str(x) for x in value)
        return "需要用户补充" in joined or "当前知识点" in joined

    return False


def _repair_profile_with_knowledge(
    profile: Dict[str, Any],
    top_doc: Dict[str, Any],
    user_input: str,
) -> Dict[str, Any]:
    """
    用知识库检索到的最相关知识块修正画像。

    这不是写死规则。
    逻辑是：
    如果 AI 没判断出 course/topic，
    就使用知识库命中的 course/topic/title。
    """
    if not top_doc:
        return profile

    repaired = dict(profile)

    course = top_doc.get("course") or ""
    topic = top_doc.get("topic") or top_doc.get("title") or ""

    if _is_missing_value(repaired.get("course")) and course:
        repaired["course"] = course

    if _is_missing_value(repaired.get("topic")) and topic:
        repaired["topic"] = topic

    if _is_missing_value(repaired.get("major")):
        repaired["major"] = "计算机相关专业"

    if _is_missing_value(repaired.get("learning_goal")):
        if "复习" in user_input:
            repaired["learning_goal"] = f"复习并巩固{topic}的核心概念、易错点和代码应用"
        else:
            repaired["learning_goal"] = f"学习并掌握{topic}的核心概念、易错点和代码应用"

    if _is_missing_value(repaired.get("knowledge_base")):
        if "大神" in user_input or "复习" in user_input:
            repaired["knowledge_base"] = "已有一定基础，当前目标是复习巩固"
        else:
            repaired["knowledge_base"] = "基础情况未知，需要从核心概念开始学习"

    if _is_missing_value(repaired.get("cognitive_style")):
        repaired["cognitive_style"] = "偏好结构化讲解和代码案例"

    if _is_missing_value(repaired.get("weaknesses")):
        repaired["weaknesses"] = [topic]

    if _is_missing_value(repaired.get("mistake_preference")):
        repaired["mistake_preference"] = f"容易混淆{topic}的概念、使用场景和常见细节"

    if _is_missing_value(repaired.get("resource_preference")):
        repaired["resource_preference"] = ["讲解文档", "思维导图", "练习题", "代码案例"]

    repaired.pop("need_more_info", None)
    repaired.pop("errors", None)

    if isinstance(repaired.get("weaknesses"), str):
        repaired["weaknesses"] = [repaired["weaknesses"]]

    if isinstance(repaired.get("resource_preference"), str):
        repaired["resource_preference"] = [repaired["resource_preference"]]

    return repaired

def classify_student(profile: Dict[str, Any]) -> str:
    knowledge_base = _to_text(profile.get("knowledge_base"))
    weaknesses = profile.get("weaknesses", [])
    mistake_preference = _to_text(profile.get("mistake_preference"))
    learning_goal = _to_text(profile.get("learning_goal"))

    text = f"{knowledge_base} {_to_text(weaknesses)} {mistake_preference} {learning_goal}"

    score = 0

    if any(word in text for word in ["没基础", "零基础", "基础薄弱", "听不懂", "完全不会", "不系统"]):
        score += 1
    elif any(word in text for word in ["有部分基础", "一般", "学过一点", "不熟练"]):
        score += 2
    elif any(word in text for word in ["基础扎实", "掌握较好", "比较熟练", "熟练"]):
        score += 3
    else:
        score += 2

    if weaknesses:
        score += 1
    else:
        score += 3

    if any(word in text for word in ["混淆", "易错", "不会用", "不知道", "薄弱"]):
        score += 1
    else:
        score += 2

    if score <= 4:
        return "基础补齐型"
    if score <= 7:
        return "稳定提升型"
    return "进阶拓展型"


def build_profile(state: dict) -> dict:
    """
    节点 1：构建学生画像。

    当前策略：
    1. AI 根据用户输入 + 知识库目录抽取画像
    2. 程序检查字段完整性
    3. 不合格则让 AI 修复
    4. 仍失败则标记 need_more_info
    """
    user_input = state.get("user_input", "")

    try:
        profile = _ai_extract_profile(user_input)
    except Exception:
        profile = {}

    errors = _check_profile(profile)

    if errors:
        try:
            repaired = _ai_repair_profile(user_input, profile, errors)
            repaired_errors = _check_profile(repaired)

            if not repaired_errors:
                profile = repaired
                errors = []
            else:
                profile = _make_need_more_info_profile(user_input, repaired_errors)
        except Exception:
            profile = _make_need_more_info_profile(user_input, errors)

    if isinstance(profile.get("weaknesses"), str):
        profile["weaknesses"] = [profile["weaknesses"]]

    if isinstance(profile.get("resource_preference"), str):
        profile["resource_preference"] = [profile["resource_preference"]]

    if not profile.get("need_more_info"):
        validate_profile(profile)
        profile["overall_type"] = classify_student(profile)

    return {"profile": profile}


def retrieve_knowledge(state: dict) -> dict:
    """
    节点 2：根据用户输入、课程、知识点检索知识库。

    额外能力：
    如果 build_profile 没有抽出 course/topic，
    但知识库检索命中了具体知识块，
    就用知识库 metadata 修正 profile。
    """
    user_input = state.get("user_input", "")
    profile = state.get("profile", {})

    course = _to_text(profile.get("course"))
    topic = _to_text(profile.get("topic"))
    weaknesses = _to_text(profile.get("weaknesses"))

    query = f"{course} {topic} {weaknesses} {user_input}"

    context = search_knowledge_base(query)
    top_doc = get_top_knowledge_doc(query)

    repaired_profile = _repair_profile_with_knowledge(
        profile=profile,
        top_doc=top_doc,
        user_input=user_input,
    )

    return {
        "retrieved_context": context,
        "profile": repaired_profile,
    }


def generate_resources(state: dict) -> dict:
    """
    节点 3：生成 5 类个性化学习资源。

    当前版本使用：
    画像 + 知识库内容 + 模板生成

    这样比一次性让小模型生成完整 JSON 稳定。
    """
    profile = state.get("profile", {})
    context = state.get("retrieved_context", "")

    course = _to_text(profile.get("course", "未知课程"))
    main_topic = _get_main_topic(profile)
    weaknesses = _to_text(profile.get("weaknesses"))
    cognitive_style = _to_text(profile.get("cognitive_style"))
    resource_preference = _to_text(profile.get("resource_preference"))
    knowledge_base = _to_text(profile.get("knowledge_base"))

    resources = {
        "course_explanation": (
            f"【{course}个性化讲解文档】\n\n"
            f"一、学习主题\n"
            f"本次重点学习：{main_topic}\n\n"
            f"二、学生情况\n"
            f"- 已有基础：{knowledge_base}\n"
            f"- 薄弱点：{weaknesses}\n"
            f"- 认知风格：{cognitive_style}\n"
            f"- 资源偏好：{resource_preference}\n\n"
            f"三、学习建议\n"
            f"1. 先梳理 {main_topic} 的核心定义和使用场景。\n"
            f"2. 再结合知识库中的示例理解常见错误。\n"
            f"3. 最后通过练习题和代码案例进行巩固。\n\n"
            f"四、知识库参考\n"
            f"{context[:700]}"
        ),

        "mindmap": (
            "graph TD;\n"
            f"A[{course}学习] --> B[{main_topic}核心概念];\n"
            f"A --> C[{main_topic}常见用法];\n"
            f"A --> D[{main_topic}易错点];\n"
            f"A --> E[练习与巩固];\n"
            f"B --> B1[定义];\n"
            f"B --> B2[关键特征];\n"
            f"C --> C1[基础示例];\n"
            f"C --> C2[实际应用];\n"
            f"D --> D1[概念混淆];\n"
            f"D --> D2[边界条件];\n"
            f"E --> E1[选择题];\n"
            f"E --> E2[简答题];\n"
            f"E --> E3[代码练习];"
        ),

        "quiz": [
            {
                "type": "choice",
                "question": f"学习 {main_topic} 时，最应该先掌握什么？",
                "options": [
                    "基本定义、使用场景和常见错误",
                    "直接背诵所有代码",
                    "跳过概念直接做难题",
                    "只看答案不练习",
                ],
                "answer": "基本定义、使用场景和常见错误",
            },
            {
                "type": "short_answer",
                "question": f"请用自己的话解释 {main_topic} 的核心作用。",
                "answer": (
                    f"{main_topic} 的核心作用需要结合 {course} 的知识体系理解，"
                    f"重点是掌握定义、典型场景、常见错误和基本实践方法。"
                ),
            },
            {
                "type": "practice",
                "question": f"请结合 {course} 写一个与 {main_topic} 相关的小例子。",
                "answer": "答案应包含核心概念、简单示例和解释说明。",
            },
        ],

        "extended_reading": [
            f"建议阅读：{course} 中 {main_topic} 的基础概念资料。",
            f"建议阅读：{main_topic} 的常见错误和排查方法。",
            f"建议阅读：{main_topic} 的案例讲解或代码实操资料。",
        ],

        "code_practice": (
            f"# {course} - {main_topic} 代码实操案例\n\n"
            f"## 练习目标\n"
            f"围绕 {main_topic} 写一个最小可运行示例，并解释每一行的作用。\n\n"
            f"## 实操要求\n"
            f"1. 写出一个和 {main_topic} 相关的基础示例。\n"
            f"2. 标注容易出错的地方。\n"
            f"3. 修改一个参数或条件，观察输出变化。\n\n"
            f"## 参考说明\n"
            f"请结合知识库中的内容完成代码练习。\n"
        ),
    }

    validate_resources(resources)

    return {"resources": resources}


def plan_learning_path(state: dict) -> dict:
    """
    节点 4：根据学生画像和资源生成学习路径。
    """
    profile = state.get("profile", {})

    course = _to_text(profile.get("course", "未知课程"))
    main_topic = _get_main_topic(profile)
    cognitive_style = _to_text(profile.get("cognitive_style"))
    learning_goal = _to_text(profile.get("learning_goal"))

    path = [
        {
            "step": 1,
            "title": f"复习 {main_topic} 的核心概念",
            "resource": "course_explanation",
            "reason": f"学习目标是：{learning_goal}。先明确核心概念，避免后续练习时概念混淆。",
        },
        {
            "step": 2,
            "title": f"用思维导图梳理 {course} 中的 {main_topic}",
            "resource": "mindmap",
            "reason": f"学生认知风格是：{cognitive_style}，适合用结构化方式建立整体认识。",
        },
        {
            "step": 3,
            "title": "完成基础练习题",
            "resource": "quiz",
            "reason": "通过选择题、简答题和练习题检查是否真正理解。",
        },
        {
            "step": 4,
            "title": "完成代码实操案例",
            "resource": "code_practice",
            "reason": "通过动手实践巩固抽象概念，暴露真实易错点。",
        },
        {
            "step": 5,
            "title": "阅读拓展材料",
            "resource": "extended_reading",
            "reason": "通过拓展阅读加深理解，形成长期记忆。",
        },
    ]

    validate_learning_path(path)

    return {"learning_path": path}


def save_outputs(state: dict) -> dict:
    """
    节点 5：保存学生画像、资源、学习路径，并生成最终回答。
    """
    student_id = state.get("student_id", "student_001")

    profile = state.get("profile", {})
    resources = state.get("resources", {})
    learning_path = state.get("learning_path", [])

    base_dir = Path("data/resources") / student_id
    base_dir.mkdir(parents=True, exist_ok=True)

    (base_dir / "profile.json").write_text(
        json.dumps(profile, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    (base_dir / "resources.json").write_text(
        json.dumps(resources, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    (base_dir / "learning_path.json").write_text(
        json.dumps(learning_path, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    if "course_explanation" in resources:
        (base_dir / "course_explanation.md").write_text(
            resources["course_explanation"],
            encoding="utf-8",
        )

    if "mindmap" in resources:
        (base_dir / "mindmap.mmd").write_text(
            resources["mindmap"],
            encoding="utf-8",
        )

    if "quiz" in resources:
        (base_dir / "quiz.json").write_text(
            json.dumps(resources["quiz"], ensure_ascii=False, indent=2),
            encoding="utf-8",
        )

    if "extended_reading" in resources:
        reading_text = "\n".join(
            f"- {item}" for item in resources["extended_reading"]
        )
        (base_dir / "extended_reading.md").write_text(
            reading_text,
            encoding="utf-8",
        )

    if "code_practice" in resources:
        (base_dir / "code_practice.md").write_text(
            resources["code_practice"],
            encoding="utf-8",
        )

    final_answer = f"""
已生成个性化学习方案。

一、学生画像
- 学生类型：{profile.get("overall_type", "稳定提升型")}
- 专业：{profile.get("major", "未知")}
- 课程：{profile.get("course", "未知")}
- 知识点：{profile.get("topic", "未知")}
- 学习目标：{profile.get("learning_goal", "未知")}
- 知识基础：{profile.get("knowledge_base", "未知")}
- 认知风格：{profile.get("cognitive_style", "未知")}
- 薄弱点：{", ".join(profile.get("weaknesses", [])) if isinstance(profile.get("weaknesses", []), list) else profile.get("weaknesses", "未知")}
- 易错倾向：{profile.get("mistake_preference", "未知")}
- 资源偏好：{profile.get("resource_preference", "未知")}

二、学习资源
- 课程讲解：{"已生成" if resources.get("course_explanation") else "未生成"}
- 思维导图：{"已生成" if resources.get("mindmap") else "未生成"}
- 测验题：{"已生成" if resources.get("quiz") else "未生成"}
- 拓展阅读：{"已生成" if resources.get("extended_reading") else "未生成"}
- 编程练习：{"已生成" if resources.get("code_practice") else "未生成"}

三、学习路径
{chr(10).join([f"{index + 1}. {item}" for index, item in enumerate(learning_path)]) if learning_path else "暂无学习路径"}

四、个性化建议
- 如果学生类型为“基础补齐型”，建议先补前置知识，降低学习难度。
- 如果学生类型为“稳定提升型”，建议围绕薄弱点进行查漏补缺和变式训练。
- 如果学生类型为“进阶拓展型”，建议增加项目任务、综合题和跨知识点应用。

文件已保存到：
{base_dir}
"""

    return {
        "final_answer": final_answer,
        "resource_dir": str(base_dir),
    }