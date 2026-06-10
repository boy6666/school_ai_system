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
    difficulty: str = "medium"  # easy | medium | hard
    profile: Optional[dict] = None  # 学生画像
    chapter_id: Optional[str] = None
    course_id: Optional[str] = None
    student_id: str = "student_001"


@app.post("/resource/generate")
def resource_generate(req: ResourceGenRequest):
    from school_agent.services.llm_client import call_llm

    # 构建基于画像和难度的个性化 prompt
    profile = req.profile or {}
    weaknesses = profile.get("weaknesses", "")
    knowledge_base = profile.get("knowledge_base", "未知")
    pace = profile.get("pace", "normal")
    course = profile.get("course", req.chapter)

    # 难度映射
    difficulty_map = {
        "easy": "请使用更简单的语言，减少专业术语，多用生活化比喻，适合零基础初学者。",
        "medium": "请使用标准教学语言，包含必要的术语解释和示例。",
        "hard": "请使用进阶语言，包含深入的技术细节、底层原理和高级应用场景。",
    }
    difficulty_instruction = difficulty_map.get(req.difficulty, difficulty_map["medium"])

    # 画像分析信息
    profile_context = ""
    if weaknesses:
        profile_context += f"学生薄弱点：{weaknesses}。请在这些方面多加解释和练习。"
    if knowledge_base and knowledge_base != "未知":
        profile_context += f"学生已有基础：{knowledge_base}。"
    if pace:
        pace_map = {"fast": "学习节奏快，内容可以密集一些。", "slow": "学习节奏慢，请分解步骤、循序渐进。", "normal": ""}
        profile_context += pace_map.get(pace, "")

    # 构建完整 prompt
    if req.prompt:
        full_prompt = f"{req.prompt}\n{difficulty_instruction}\n{profile_context}"
    else:
        full_prompt = _build_resource_prompt(
            chapter=req.chapter,
            topic=req.topic or req.chapter,
            resource_type=req.resourceType,
            difficulty=req.difficulty,
            difficulty_instruction=difficulty_instruction,
            profile_context=profile_context,
            course=course,
        )

    result = call_llm(full_prompt)
    return {
        "content": result,
        "resourceType": req.resourceType,
        "chapter": req.chapter,
        "difficulty": req.difficulty,
        "chapter_id": req.chapter_id,
        "course_id": req.course_id,
    }


def _build_resource_prompt(
    chapter: str,
    topic: str,
    resource_type: str,
    difficulty: str,
    difficulty_instruction: str,
    profile_context: str,
    course: str,
) -> str:
    """根据资源类型构建个性化 prompt"""

    base = f"课程：{course}。章节：{chapter}。知识点：{topic}。\n{difficulty_instruction}\n{profile_context}\n"

    prompts = {
        "mindmap": (
            base +
            f"请为「{topic}」生成一份Mermaid思维导图代码。要求："
            f"(1)使用graph TD布局 (2)5-8个主节点，各挂2-3子节点 (3)节点使用中文 (4)体现知识逻辑层次。"
            f"只返回以graph TD开头的Mermaid代码，不要额外说明。"
        ),
        "quiz": (
            base +
            f"请为「{topic}」生成5道练习题(含答案)。要求："
            f"(1)题型包含选择题和简答题 (2)由易到难排列 (3)每题包含解析。"
            f"返回JSON数组，每题包含字段：type(choice/short)/question(题目)/options(选项数组,简答题为空)/answer(正确答案索引或文字)/explanation(解析)。"
            f"只返回JSON数组，不要额外说明。"
        ),
        "reading": (
            base +
            f"请为学习「{topic}」的学生生成一份拓展阅读材料(约500字)。要求："
            f"(1)推荐2-3个与{topic}相关的进阶概念 (2)列出实际项目应用场景 (3)推荐进一步学习资源方向。"
            f"使用HTML格式输出，段落用<p>标签，标题用<h4>标签。"
        ),
        "code": (
            base +
            f"请为「{topic}」生成一个完整的Java代码案例。要求："
            f"(1)代码可编译运行 (2)包含详细中文注释说明核心逻辑 (3)代码量约30-80行 (4)包含边界条件处理。"
            f"只返回Java代码，用```java...```包裹，不要额外说明。"
        ),
    }

    return prompts.get(resource_type, prompts["mindmap"])

