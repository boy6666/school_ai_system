from typing import Optional

try:
    from fastapi import FastAPI
    from pydantic import BaseModel
except Exception:  # pragma: no cover
    FastAPI = None
    BaseModel = object

from school_agent.graph import graph


if FastAPI is None:
    app = None
else:
    app = FastAPI(title="Edu Agent AI", version="0.1.0")


    class ChatRequest(BaseModel):
        user_input: str = ""
        student_id: str = "student_001"
        session_id: Optional[str] = "api_session"
        profile: Optional[dict] = None


    @app.get("/health")
    def health():
        return {"status": "ok"}


    @app.post("/chat")
    def chat(req: ChatRequest):
        initial_state = {
                "student_id": req.student_id,
                "session_id": req.session_id,
                "user_input": req.user_input,
            }
        if req.profile:
            initial_state["profile"] = req.profile
        
        result = graph.invoke(initial_state,
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

class ResourceGenRequest(BaseModel):
    chapter: str = ""
    topic: str = ""
    resourceType: str = "mindmap"
    level: str = "basic"
    prompt: str = ""

@app.post("/resource/generate")
def resource_generate(req: ResourceGenRequest):
    from school_agent.services.llm_client import call_llm
    
    result = call_llm(req.prompt)
    return {
        "content": result,
        "resourceType": req.resourceType,
        "chapter": req.chapter,
    }

