import json
from typing import Any, Dict

from school_agent.schemas.profile_schema import DimensionLevel
from school_agent.services.llm_client import call_llm
from school_agent.services.log_store import append_learning_log
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import get_main_topic
from school_agent.utils.time_utils import now_iso

EVALUATION_SYSTEM_PROMPT = """你是学习评估专家。根据学生与AI辅导老师的对话，评估学生在六个维度的表现。

## 六维评分标准

### 知识掌握度 (knowledge_mastery)
- 入门(0-40)：概念混淆，无法独立应用
- 熟练(41-70)：能理解概念，基本能应用
- 精通(71-100)：理解深刻，能灵活运用和迁移

### 学习目标清晰度 (learning_goal_clarity)
- 入门(0-40)：随意提问，无明确方向
- 熟练(41-70)：有短期目标（考试/作业）
- 精通(71-100)：有系统学习规划

### 认知风格适配 (cognitive_adaptation)
- 入门(0-40)：偏好未显现
- 熟练(41-70)：能识别学习偏好
- 精通(71-100)：有明确有效学习策略

### 错误规避力 (mistake_avoidance)
- 入门(0-40)：频繁重复同类错误
- 熟练(41-70)：能识别部分错误，会主动验证
- 精通(71-100)：主动规避错误，有检查习惯

### 学习自主性 (learning_autonomy)
- 入门(0-40)：被动等待答案
- 熟练(41-70)：主动提问和追问
- 精通(71-100)：自驱学习，深入探索

### 综合能力 (overall_level)
- 入门(0-40)：基础补齐型
- 熟练(41-70)：稳定提升型
- 精通(71-100)：进阶拓展型

## 输出格式

严格返回JSON：
{
  "understanding_score": 70,
  "dimension_assessments": {
    "knowledge_mastery": {
      "observed_level": "level_1",
      "score": 35,
      "note": "具体说明本轮观察到的情况（引用对话内容）"
    },
    "learning_goal_clarity": {
      "observed_level": "level_1",
      "score": 30,
      "note": "具体说明"
    },
    "cognitive_adaptation": {
      "observed_level": "level_1",
      "score": 30,
      "note": "具体说明"
    },
    "mistake_avoidance": {
      "observed_level": "level_1",
      "score": 30,
      "note": "具体说明"
    },
    "learning_autonomy": {
      "observed_level": "level_1",
      "score": 30,
      "note": "具体说明"
    },
    "overall_level": {
      "observed_level": "level_1",
      "score": 30,
      "note": "综合判断"
    }
  },
  "weak_points": ["需要加强的具体知识点"],
  "suggestion": "针对性的学习建议"
}

## 规则
- 基于本轮对话中实际观察到的内容评分
- 不要因为"没有足够信息"就给30分——如果对话中学生展示了理解，就应该给相应的分数
- 如果学生只是打招呼/闲聊，所有维度保持默认（level_1, 30分），并在note中说明"本轮无学习内容"
- note要引用对话中的具体内容，不能是泛泛的评价
- score范围0-100"""


def _parse_json(text: str) -> Dict[str, Any]:
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


def _rule_based_evaluation(state: dict) -> Dict[str, Any]:
    """基于规则的兜底评估（LLM不可用时）。"""
    profile = state.get("profile", {})
    topic = get_main_topic(profile)
    user_input = state.get("user_input", "")

    # 更精细的信号检测
    weak_signals = ["不会", "不懂", "错", "看不懂", "没理解", "还是不会", "太难", "搞不定"]
    strong_signals = ["我理解了", "我懂了", "明白了", "原来是这样", "我知道了", "懂了谢谢"]
    deep_signals = ["总结", "对比", "分析", "优化", "原理", "源码", "为什么", "区别"]
    question_signals = ["怎么", "如何", "什么是", "能不能", "可以", "请"]

    has_weak = any(w in user_input for w in weak_signals)
    has_strong = any(w in user_input for w in strong_signals)
    has_deep = any(w in user_input for w in deep_signals)
    has_question = any(w in user_input for w in question_signals)

    # 综合判断
    if has_deep and not has_weak:
        base_score = 75
        base_level = "level_3" if "源码" in user_input or "优化" in user_input else "level_2"
    elif has_strong:
        base_score = 68
        base_level = "level_2"
    elif has_weak:
        base_score = 40
        base_level = "level_1"
    elif has_question:
        base_score = 55
        base_level = "level_1"
    else:
        base_score = 50
        base_level = "level_1"

    return {
        "understanding_score": base_score,
        "dimension_assessments": {
            "knowledge_mastery": {
                "observed_level": base_level,
                "score": base_score,
                "note": f"基于关键词规则评估，主题：{topic}",
            },
            "learning_goal_clarity": {
                "observed_level": "level_2" if has_deep else "level_1",
                "score": 45 if has_deep else 30,
                "note": "基于提问深度推断学习目标清晰度",
            },
            "cognitive_adaptation": {
                "observed_level": "level_1",
                "score": 35,
                "note": "规则模式暂无法精确判断认知风格",
            },
            "mistake_avoidance": {
                "observed_level": "level_2" if has_strong else "level_1",
                "score": base_score - 10 if has_weak else base_score,
                "note": "基于错误信号评估",
            },
            "learning_autonomy": {
                "observed_level": "level_2" if has_deep else "level_1",
                "score": base_score + 5 if has_deep else base_score - 5,
                "note": "基于自主探索信号评估",
            },
            "overall_level": {
                "observed_level": base_level,
                "score": base_score,
                "note": "综合规则评估",
            },
        },
        "weak_points": [topic] if has_weak else [],
        "suggestion": (
            f"建议继续深入学习{topic}相关知识点" if not has_weak
            else f"建议从{topic}的基础概念开始，逐步建立理解"
        ),
    }


def evaluate_learning(state: dict) -> dict:
    """学习效果评估。通过LLM分析对话中学生在各维度的表现。"""
    user_input = state.get("user_input", "")
    final_answer = state.get("final_answer", "")
    profile = state.get("profile", {})

    report = None
    try:
        # 传入完整 AI 回复（只做截断保护，不再限制500字符）
        ai_summary = final_answer if len(final_answer) <= 2000 else final_answer[:2000] + "\n...(已截断)"

        prompt = (
            f"学生提问：\n{user_input}\n\n"
            f"AI辅导回复：\n{ai_summary}\n\n"
            f"请评估本轮对话中学生在各维度的表现，给出具体的评分和依据。"
        )
        response = call_llm(prompt, system=EVALUATION_SYSTEM_PROMPT)
        report = _parse_json(response)
    except Exception:
        report = _rule_based_evaluation(state)

    report["evaluated_at"] = now_iso()

    dim_assessments = report.get("dimension_assessments", {})

    return {
        "evaluation_report": report,
        "agent_outputs": merge_agent_output(
            state,
            "evaluation_agent",
            {
                "status": "success",
                "score": report.get("understanding_score"),
                "dimension_count": len(dim_assessments),
            },
        ),
    }


def log_interaction(state: dict) -> dict:
    """学习日志节点。"""
    student_id = state.get("student_id", "student_001")
    log_path = append_learning_log(
        student_id,
        {
            "session_id": state.get("session_id"),
            "user_input": state.get("user_input"),
            "intent": state.get("intent"),
            "evaluation_report": state.get("evaluation_report"),
            "profile_changes": state.get("profile_changes"),
        },
    )

    return {
        "log_path": log_path,
        "agent_outputs": merge_agent_output(
            state,
            "log_store",
            {"status": "success", "log_path": log_path},
        ),
    }
