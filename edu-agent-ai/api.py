from typing import List, Optional

try:
    from fastapi import FastAPI
    from pydantic import BaseModel
except Exception:  # pragma: no cover
    FastAPI = None
    BaseModel = object

from school_agent.agents.profile_agent import build_profile, update_profile
from school_agent.graph import graph
from school_agent.services.profile_store import load_student_profile, save_student_profile
from school_agent.utils.time_utils import now_iso


if FastAPI is None:
    app = None
else:
    app = FastAPI(title="Edu Agent AI", version="0.1.0")


    class ChatRequest(BaseModel):
        user_input: str
        student_id: str = "student_001"
        session_id: Optional[str] = "api_session"


    class ProfileBuildRequest(BaseModel):
        student_id: str = "student_001"
        learning_goal: str = ""
        knowledge_base: List[str] = []
        current_mastery: str = ""
        cognitive_style: str = ""
        mistake_patterns: List[str] = []
        learning_behavior: str = ""
        daily_hours: float = 0


    @app.get("/health")
    def health():
        return {"status": "ok"}


    @app.get("/profile/{student_id}")
    def get_profile(student_id: str):
        profile = load_student_profile(student_id)
        return {
            "student_id": student_id,
            "profile": profile,
            "exists": bool(profile),
        }


    @app.post("/profile/build")
    def build_profile_api(req: ProfileBuildRequest):
        parts = []
        if req.learning_goal:
            parts.append(f"我的学习目标是{req.learning_goal}")
        if req.knowledge_base:
            parts.append(f"我目前掌握的Java知识点：{'、'.join(req.knowledge_base)}")
        if req.current_mastery:
            parts.append(f"我对各知识点的掌握情况：{req.current_mastery}")
        if req.cognitive_style:
            parts.append(f"我偏好的学习方式是{req.cognitive_style}")
        if req.mistake_patterns:
            parts.append(f"我常遇到的错误类型：{'、'.join(req.mistake_patterns)}")
        if req.learning_behavior:
            parts.append(f"我的学习习惯：{req.learning_behavior}，每天学习{req.daily_hours}小时")
        user_input = "。".join(parts) + "。"
        state = {
            "student_id": req.student_id,
            "session_id": "profile_build",
            "user_input": user_input,
        }
        result = build_profile(state)
        profile = result.get("profile", {})
        save_student_profile(req.student_id, profile)
        return {
            "student_id": req.student_id,
            "profile": profile,
        }


    @app.post("/chat")
    def chat(req: ChatRequest):
        result = graph.invoke(
            {
                "student_id": req.student_id,
                "session_id": req.session_id,
                "user_input": req.user_input,
            },
            config={"configurable": {"thread_id": f"{req.student_id}_{req.session_id}"}},
        )
        return {
            "intent": result.get("intent"),
            "final_answer": result.get("final_answer"),
            "profile": result.get("profile"),
            "resources": result.get("resources"),
            "learning_path": result.get("learning_path"),
            "safety_report": result.get("safety_report"),
            "evaluation_report": result.get("evaluation_report"),
            "resource_dir": result.get("resource_dir"),
        }
