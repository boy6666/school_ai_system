# school_agent/agents/quiz_agent.py
import json
from pathlib import Path
from typing import List, Dict, Any
from school_agent.config import DATA_DIR
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import get_main_topic, to_text

QUESTION_BANK_DIR = DATA_DIR / "question_bank"

class Question:
    def __init__(self, data: dict):
        self.id = data.get("id", "")
        self.topic = data.get("topic", "")
        self.type = data.get("type", "choice")
        self.difficulty = data.get("difficulty", "中等")
        self.question = data.get("question", "")
        self.options = data.get("options", [])
        self.answer = data.get("answer", "")
        self.analysis = data.get("analysis", "")
        self.tags = data.get("tags", [])
        self.weakness_target = data.get("weakness_target", [])
        self.cognitive_style = data.get("cognitive_style", [])
        self.suitable_for = data.get("suitable_for", [])

def load_questions() -> List[Question]:
    """从 data/question_bank/ 目录加载所有 JSON 文件中的题目，若无则返回空列表"""
    questions = []
    if not QUESTION_BANK_DIR.exists():
        return questions
    for json_file in QUESTION_BANK_DIR.glob("*.json"):
        try:
            with open(json_file, "r", encoding="utf-8") as f:
                data = json.load(f)
                if isinstance(data, list):
                    for item in data:
                        questions.append(Question(item))
                elif isinstance(data, dict):
                    questions.append(Question(data))
        except Exception as e:
            print(f"加载题库文件失败 {json_file}: {e}")
    return questions

def match_questions(profile: Dict[str, Any], topic: str, count: int = 5) -> List[Question]:
    """根据学生画像从题库中匹配最合适的题目"""
    all_q = load_questions()
    if not all_q:
        return []

    knowledge_base = profile.get("knowledge_base", "基础一般")
    weaknesses = profile.get("weaknesses", [])
    cognitive_style = profile.get("cognitive_style", "")

    scored = []
    for q in all_q:
        score = 0
        if topic.lower() in q.topic.lower():
            score += 10
        if knowledge_base == "零基础" and q.difficulty == "基础":
            score += 5
        elif knowledge_base == "有一定编程基础" and q.difficulty in ["基础", "中等"]:
            score += 3
        elif knowledge_base == "熟练" and q.difficulty == "提高":
            score += 5
        for w in weaknesses:
            if w in q.weakness_target:
                score += 8
        if cognitive_style in q.cognitive_style:
            score += 4
        for tag in q.tags:
            if tag in topic or any(w in tag for w in weaknesses):
                score += 2
        if score > 0:
            scored.append((score, q))
    scored.sort(key=lambda x: x[0], reverse=True)
    return [q for _, q in scored[:count]]

def quiz_agent(state: dict) -> dict:
    profile = state.get("profile", {})
    topic = get_main_topic(profile)
    questions = match_questions(profile, topic, count=5)

    if not questions:
        final_answer = "当前题库中没有找到与您的问题匹配的题目。请尝试添加题目到 `data/question_bank/` 目录。"
    else:
        lines = [f"## 个性化练习题：{topic}", ""]
        for idx, q in enumerate(questions, start=1):
            lines.append(f"### {idx}. {q.type}｜{q.difficulty}")
            lines.append(f"**题目：** {q.question}")
            if q.options:
                lines.append("")
                for opt in q.options:
                    lines.append(f"- {opt}")
            lines.append("")
            lines.append(f"**参考答案：** {q.answer}")
            lines.append(f"**解析：** {q.analysis}")
            lines.append("")
        final_answer = "\n".join(lines)

    return {
        "final_answer": final_answer,
        "agent_outputs": merge_agent_output(state, "quiz_agent", {"status": "success" if questions else "no_questions"}),
    }