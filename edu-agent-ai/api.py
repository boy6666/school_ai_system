from typing import List, Optional

try:
    from fastapi import FastAPI
    from pydantic import BaseModel
except Exception:  # pragma: no cover
    FastAPI = None
    BaseModel = object

from school_agent.agents.profile_agent import get_profile_snapshot
from school_agent.graph import graph
from school_agent.services.profile_store import load_student_profile, save_student_profile
from school_agent.utils.time_utils import now_iso


if FastAPI is None:
    app = None
else:
    app = FastAPI(title="Edu Agent AI", version="0.2.0")


    class ChatRequest(BaseModel):
        user_input: str
        student_id: str = "student_001"
        session_id: Optional[str] = "api_session"


    @app.get("/health")
    def health():
        return {"status": "ok"}


    @app.get("/profile/{student_id}")
    def get_profile(student_id: str):
        """获取学生画像快照（含三维层次信息）。"""
        profile = load_student_profile(student_id)
        snapshot = get_profile_snapshot(profile) if profile else {}
        return {
            "student_id": student_id,
            "profile": snapshot,
            "exists": bool(profile),
        }


    @app.post("/chat")
    def chat(req: ChatRequest):
        """智能辅导对话。

        画像在后台自动初始化/更新，无需用户额外操作。
        返回中包含 profile_changes 展示本轮对话造成的画像变化。
        """
        result = graph.invoke(
            {
                "student_id": req.student_id,
                "session_id": req.session_id,
                "user_input": req.user_input,
            },
            config={"configurable": {"thread_id": f"{req.student_id}_{req.session_id}"}},
        )

        profile_data = result.get("profile", {})
        profile_snapshot = get_profile_snapshot(profile_data) if profile_data else {}

        return {
            "intent": result.get("intent"),
            "final_answer": result.get("final_answer"),
            "profile": profile_snapshot,
            "profile_changes": result.get("profile_changes", {}),
            "resources": result.get("resources"),
            "learning_path": result.get("learning_path"),
            "safety_report": result.get("safety_report"),
            "evaluation_report": result.get("evaluation_report"),
            "resource_dir": result.get("resource_dir"),
        }
