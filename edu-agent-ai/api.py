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
        import time as _time
        t0 = _time.time()
        _log(f"\n{'='*60}")
        _log(f"[资源生成AI] ===== 收到请求 =====")
        _log(f"[资源生成AI] chapter={req.chapter}, topic={req.topic}, resourceType={req.resourceType}, level={req.level}")
        _log(f"[资源生成AI] prompt(前200字): {req.prompt[:200] if req.prompt else '空'}...")

        from school_agent.services.llm_client import call_llm

        # 根据资源类型设置角色 system prompt
        role_prompts = {
            "mindmap": "你是一位思维导图设计大师。你的任务是将知识点转化为 JSON 树形结构数据。每个节点包含 id（唯一字符串）、topic（显示文本），children（子节点数组，可选）。只输出纯 JSON，不要 Markdown 标记、代码块或任何额外文字。",
            "quiz": "你是一位资深出题专家。你的任务是根据知识点设计高质量的练习题。必须返回 JSON 数组，每个元素包含 question（题目）、options（选项数组，选择题需要）、answer（正确答案）、explanation（解析）。",
            "reading": "你是一位教育内容创作专家。你的任务是为知识点撰写深入浅出的拓展阅读材料。使用 Markdown 格式，包含概念解释、应用场景和延伸阅读方向。",
            "code": "你是一位高级Java编程导师。你的任务是为知识点编写教学级代码案例。代码必须完整、可运行、包含详细中文注释。使用 Markdown 代码块标注。",
            "review": "你是一位学习回顾分析师。根据学生的学习数据（画像、学习时长、完成任务数、资源使用情况），生成结构化的学习回顾报告。包含进度总结、薄弱点分析、改进建议。返回 JSON 格式。",
            "summary": "你是一位学习总结专家。根据学生的学习画像、路径进度、学习时长和任务完成情况，生成全面的学习总结报告。包含综合评分、优点分析、不足分析、重点方向和学习建议。返回 JSON 格式。",
            "evaluation": "你是一位学习评估专家。根据学生的学习数据生成客观的学习评价，包含各维度的知识掌握度评分和综合评估。返回 JSON 格式。",
            "suggestion": "你是一位学习建议导师。根据学生画像和薄弱点，生成具体可执行的个性化学习建议。返回 JSON 数组格式。",
        }
        system_prompt = role_prompts.get(req.resourceType,
            "你是一位教育内容生成专家。根据要求生成高质量的学习内容。")

        _log(f"[资源生成AI] 角色={req.resourceType}, 耗时={_time.time()-t0:.2f}s")

        # 传入 system_prompt + 用户 prompt
        _log(f"[资源生成AI] 🚀 正在调用 LLM...")
        t1 = _time.time()
        result = call_llm(req.prompt, system_prompt=system_prompt)
        t2 = _time.time()
        _log(f"[资源生成AI] 📥 LLM 返回, 耗时={t2-t1:.2f}s, 长度={len(result) if result else 0} 字")
        _log(f"[资源生成AI] 📥 LLM 返回(前200字): {result[:200] if result else '空'}...")
        _log(f"[资源生成AI] ===== 请求结束, 总耗时={t2-t0:.2f}s =====\n{'='*60}")

        return {
            "content": result,
            "resourceType": req.resourceType,
            "chapter": req.chapter,
        }
