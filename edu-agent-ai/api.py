from fastapi import FastAPI, Request
from pydantic import BaseModel
from typing import Optional
import traceback

from school_agent.graph import graph

app = FastAPI(title="Edu Agent AI", version="0.1.0")

class ChatRequest(BaseModel):
    user_input: str
    student_id: str = "student_001"
    session_id: Optional[str] = "api_session"

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/chat")
async def chat(request: Request):
    try:
        body = await request.json()
        print("Received body:", body)
        # 手动构建 ChatRequest 对象以兼容可能缺少的字段
        req = ChatRequest(
            user_input=body.get("user_input", ""),
            student_id=body.get("student_id", "student_001"),
            session_id=body.get("session_id", "api_session")
        )
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
    except Exception as e:
        print("Error in chat endpoint:", traceback.format_exc())
        return {"error": str(e)}, 500
