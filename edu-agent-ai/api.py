import os
from typing import Optional

# 加载 .env 文件
try:
    from dotenv import load_dotenv
    env_path = os.path.join(os.path.dirname(__file__), '.env')
    if os.path.exists(env_path):
        load_dotenv(env_path)
except Exception:
    pass

try:
    from fastapi import FastAPI
    from pydantic import BaseModel
except Exception:
    FastAPI = None
    BaseModel = object

from school_agent.graph import graph


def _log(msg: str):
    """打印日志，兼容 Windows GBK 终端"""
    try:
        print(msg.encode('gbk', errors='replace').decode('gbk'))
    except Exception:
        print(msg.encode('ascii', errors='replace').decode('ascii'))


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
        _log(f"\n{'='*60}")
        _log(f"[API] 收到请求")
        _log(f"[API] user_input: {req.user_input[:100]}")
        _log(f"[API] student_id: {req.student_id}")
        _log(f"[API] session_id: {req.session_id}")
        _log(f"[API] profile keys: {list(req.profile.keys()) if req.profile else 'none'}")
        if req.profile:
            _log(f"[API] profile._onboarding_phase: {req.profile.get('_onboarding_phase', 'not set')}")

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

        _log(f"[API] 返回结果")
        _log(f"[API] intent: {result.get('intent')}")
        _log(f"[API] final_answer: {str(result.get('final_answer', ''))[:150]}")
        _log(f"[API] profile._onboarding_phase: {result.get('profile', {}).get('_onboarding_phase', 'not set')}")
        _log(f"{'='*60}\n")

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

    class PathGenRequest(BaseModel):
        student_id: str = ""
        prompt: str = ""
        profile: Optional[dict] = None

    @app.post("/path/generate")
    def path_generate(req: PathGenRequest):
        import json
        import re

        _log(f"\n{'='*60}")
        _log(f"[路径规划AI] ===== 收到请求 =====")
        _log(f"[路径规划AI] student_id={req.student_id}")
        _log(f"[路径规划AI] profile keys: {list(req.profile.keys()) if req.profile else 'none'}")

        from school_agent.services.llm_client import call_llm

        system_prompt = """你是一个AI学习路径规划专家。根据学生画像生成个性化的学习路径规划。
你必须只返回一个严格的JSON对象，不要包含任何markdown标记、代码块或额外说明。
直接以{开头，以}结尾。"""

        full_prompt = system_prompt + "\n\n" + req.prompt

        _log(f"[路径规划AI] 🚀 正在调用 LLM...")
        text = call_llm(full_prompt)
        _log(f"[路径规划AI] 📥 LLM 原始返回(前300字): {text[:300] if text else '空'}...")

        # 尝试从返回中提取 JSON
        result = {}
        try:
            # 先尝试直接解析
            result = json.loads(text)
            _log(f"[路径规划AI] ✅ 直接解析 JSON 成功")
        except json.JSONDecodeError:
            _log(f"[路径规划AI] ⚠️ 直接解析失败，尝试提取 JSON 块...")
            # 尝试提取 ```json ... ``` 块
            json_match = re.search(r'```(?:json)?\s*([\s\S]*?)```', text)
            if json_match:
                try:
                    result = json.loads(json_match.group(1).strip())
                    _log(f"[路径规划AI] ✅ 从代码块提取 JSON 成功")
                except json.JSONDecodeError:
                    _log(f"[路径规划AI] ❌ 代码块 JSON 解析失败")
            else:
                # 尝试提取 {} 之间的内容
                brace_match = re.search(r'\{[\s\S]*\}', text)
                if brace_match:
                    try:
                        result = json.loads(brace_match.group(0))
                        _log(f"[路径规划AI] ✅ 从花括号提取 JSON 成功")
                    except json.JSONDecodeError:
                        _log(f"[路径规划AI] ❌ 花括号 JSON 解析失败")

        _log(f"[路径规划AI] 📥 最终返回 keys: {list(result.keys()) if result else '空'}")
        if result:
            _log(f"[路径规划AI] goal={result.get('goal')}, stages数={len(result.get('stages', []))}")
        else:
            _log(f"[路径规划AI] ⚠️ 返回空结果，使用默认数据")
            result = {
                "goal": "掌握课程核心知识并完成实践项目",
                "targetMastery": "≥85%",
                "estimatedCompletion": "2026-07-06",
                "totalHours": 24,
                "masteryRate": 50,
                "learningRate": 30,
                "unmasteredRate": 20,
                "suggestions": "建议从基础概念开始，逐步深入到实践项目",
                "applicationAdvice": "完成每章课后练习，并尝试独立完成小项目",
                "examAdvice": "每章结束后进行自测，重点复习错题",
                "recommendTime": "每天 19:00-21:00",
                "stages": [
                    {"name": "今日计划", "tasks": [{"title": "课程基础概念学习", "duration": 45, "status": 0, "progress": 0}, {"title": "核心知识点梳理", "duration": 30, "status": 0, "progress": 0}]},
                    {"name": "本周路径", "tasks": [{"title": "语法与基础练习", "duration": 60, "status": 0, "progress": 0}, {"title": "面向对象编程实践", "duration": 45, "status": 0, "progress": 0}]},
                    {"name": "考试冲刺", "tasks": [{"title": "模拟测试一", "duration": 60, "status": 0, "progress": 0}, {"title": "错题分析与针对性复习", "duration": 45, "status": 0, "progress": 0}]},
                    {"name": "实践提升", "tasks": [{"title": "综合项目实战", "duration": 90, "status": 0, "progress": 0}, {"title": "代码审查与优化", "duration": 60, "status": 0, "progress": 0}]}
                ]
            }
        _log(f"[路径规划AI] ===== 请求结束 =====\n{'='*60}")
        return result

    @app.post("/resource/generate")
    def resource_generate(req: ResourceGenRequest):
        _log(f"\n{'='*60}")
        _log(f"[资源生成AI] ===== 收到请求 =====")
        _log(f"[资源生成AI] chapter={req.chapter}, topic={req.topic}, resourceType={req.resourceType}, level={req.level}")
        _log(f"[资源生成AI] prompt(前300字): {req.prompt[:300] if req.prompt else '空'}...")

        from school_agent.services.llm_client import call_llm
        _log(f"[资源生成AI] 🚀 正在调用 LLM...")
        result = call_llm(req.prompt)
        _log(f"[资源生成AI] 📥 LLM 返回长度: {len(result) if result else 0} 字")
        _log(f"[资源生成AI] 📥 LLM 返回(前200字): {result[:200] if result else '空'}...")
        _log(f"[资源生成AI] ===== 请求结束 =====\n{'='*60}")

        return {
            "content": result,
            "resourceType": req.resourceType,
            "chapter": req.chapter,
        }
