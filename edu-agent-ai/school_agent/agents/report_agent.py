"""效果评估报告智能体 — 根据学生画像生成个性化评估报告。

分析学生的学习数据、画像六维、对话历史，生成结构化评估报告。
"""
from school_agent.services.flow_logger import log
from school_agent.services.llm_client import call_llm
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import to_text
from school_agent.utils.time_utils import now_iso
import json

REPORT_SYSTEM = """你是一个学习评估专家。根据学生的画像数据生成结构化的学习评估报告。

## 画像六维说明
- knowledge_mastery (知识掌握度): level_1了解概念 / level_2熟练应用 / level_3深入精通
- learning_goal_clarity (学习目标清晰度): level_1方向模糊 / level_2目标明确 / level_3系统规划
- cognitive_adaptation (认知风格适配): level_1有待观察 / level_2初显偏好 / level_3策略自驱
- mistake_avoidance (错误规避力): level_1易重复错 / level_2能自查纠 / level_3主动预防
- learning_autonomy (学习自主性): level_1被动等待 / level_2主动提问 / level_3自主深耕
- overall_level (综合能力): level_1基础补齐 / level_2稳步提升 / level_3拔尖拓展

## 输出格式
返回JSON，不要其他文字：
{
    "summary": "总体评价（100-200字，概括学生学习状态和进步情况）",
    "score": 85,
    "dimensions": [
        {"name": "知识掌握度", "score": 75, "level": "level_2", "comment": "具体评价"},
        {"name": "学习目标", "score": 70, "level": "level_2", "comment": "具体评价"},
        {"name": "认知风格", "score": 65, "level": "level_2", "comment": "具体评价"},
        {"name": "错误规避", "score": 60, "level": "level_1", "comment": "具体评价"},
        {"name": "学习自主性", "score": 80, "level": "level_2", "comment": "具体评价"},
        {"name": "综合能力", "score": 72, "level": "level_2", "comment": "具体评价"}
    ],
    "strengths": ["优势1", "优势2"],
    "weaknesses": ["不足1", "不足2"],
    "suggestions": ["建议1", "建议2", "建议3"],
    "trend": "up" | "stable" | "down"
}
"""


def report_agent(state: dict) -> dict:
    log("AGENT", "report_agent.report_agent(state: dict) -> dict")
    """生成学习效果评估报告"""
    profile = state.get("profile", {})
    student_id = state.get("student_id", "student_001")

    # 构建画像摘要
    dims = []
    dim_labels = {
        "knowledge_mastery": "知识掌握度",
        "learning_goal_clarity": "学习目标清晰度",
        "cognitive_adaptation": "认知风格适配",
        "mistake_avoidance": "错误规避力",
        "learning_autonomy": "学习自主性",
        "overall_level": "综合能力",
    }
    for key, label in dim_labels.items():
        dim = profile.get(key, {})
        if isinstance(dim, dict):
            dims.append(f"- {label}: {dim.get('level', 'level_1')} ({dim.get('score', 30)}分)")

    topic = profile.get("topic", "未设置")
    course = profile.get("course", "未设置")
    weaknesses = to_text(profile.get("weaknesses", []))
    goal = profile.get("learning_goal", "")
    pace = profile.get("pace", "")

    # 加载学习记录
    study_logs = state.get("study_logs", [])
    log_summary = ""
    if study_logs:
        intents = [l.get("intent", "?") for l in study_logs if l.get("intent")]
        log_summary = f"近期学习活动：{', '.join(set(intents))}" if intents else ""

    prompt = f"""学生画像摘要：
主题：{topic}
课程：{course}
学习目标：{goal or '未设置'}
节奏：{pace or '未设置'}
薄弱点：{weaknesses or '无记录'}

六维画像：
{chr(10).join(dims)}

请根据以上画像生成学习评估报告JSON。"""

    try:
        resp = call_llm(prompt, system=REPORT_SYSTEM)
        raw = resp.strip()
        # 兼容 ```json / ``` 包裹或纯 JSON
        start = raw.find("{")
        end = raw.rfind("}")
        if start != -1 and end > start:
            raw = raw[start:end+1]
        result = json.loads(raw)
    except Exception as e:
        log("REPORT", f"LLM parse failed: {e}")
        result = {
            "summary": "AI评估生成中，请稍后重试",
            "score": 0,
            "dimensions": [],
            "strengths": [],
            "weaknesses": [],
            "suggestions": [],
            "trend": "stable",
        }

    return {
        "evaluation_report": result,
        "agent_outputs": merge_agent_output(state, "report_agent", {"status": "success"}),
    }
