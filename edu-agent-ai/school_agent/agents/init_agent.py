"""新用户初始化智能体 — 完整引导流程"""
from school_agent.services.flow_logger import log
from school_agent.services.llm_client import call_llm
from school_agent.utils.json_utils import merge_agent_output
import json

def init_agent(state: dict) -> dict:
    """Run full init: profile → path → resources"""
    log("INIT", "新用户初始化流程启动")
    student_id = state.get("student_id", "unknown")
    user_input = state.get("user_input", "")
    
    # Step 1: Build profile from user's first message
    profile_prompt = f"""新学生自我介绍："{user_input}"
请分析并输出JSON画像：
{{"course":"课程名","topic":"学习主题","knowledge_base":"基础水平","weaknesses":["薄弱点"],"learning_goal":"学习目标","pace":"slow/medium/fast"}}
只返回JSON，不要markdown。"""
    
    profile_result = call_llm(profile_prompt)
    profile = {}
    try:
        profile = json.loads(profile_result.strip().strip("```json").strip("```"))
    except:
        profile = {"course":"","topic":"","knowledge_base":"","weaknesses":[],"learning_goal":"","pace":""}
    
    log("INIT", f"画像构建: {profile.get('topic','')} / {profile.get('knowledge_base','')}")
    
    # Step 2: Generate learning path
    path_prompt = f"""基于学生画像：{json.dumps(profile, ensure_ascii=False)}
规划学习路径。返回JSON：
{{"goal":"学习目标","today":["任务1","任务2","任务3"],"week":["任务1","任务2"],"exam":["复习任务"],"practice":["实践任务"],"suggestions":"学习建议","exam_advice":"考试建议"}}
只返回JSON。"""
    
    path_result = call_llm(path_prompt)
    path = {}
    try:
        path = json.loads(path_result.strip().strip("```json").strip("```"))
    except:
        path = {"goal":"","today":[],"week":[],"exam":[],"practice":[],"suggestions":"","exam_advice":""}
    
    log("INIT", f"路径规划: {path.get('goal','')[:30]}")
    
    # Step 3: Generate welcome message
    welcome = f"""👋 你好！我是你的专属辅导老师**小航**。

📊 **画像分析完成**
- 课程：{profile.get('course','')}
- 基础：{profile.get('knowledge_base','')}
- 薄弱点：{', '.join(profile.get('weaknesses',[]))}
- 节奏：{profile.get('pace','')}

🗺️ **学习路径已规划**
- 今日任务：{', '.join(path.get('today',[]))}
- 本周计划：{', '.join(path.get('week',[]))}

现在你可以：
- 在**学习路径**页面查看完整计划
- 点击**资源生成**获取配套学习资料
- 随时在这里问我任何学习相关问题

让我们开始学习之旅吧！🚀"""

    return {
        "profile": profile,
        "learning_path": path,
        "final_answer": welcome,
        "agent_outputs": merge_agent_output(state, "init_agent", {"status": "complete"}),
        "agent_steps": [
            {"agent": "画像分析", "status": "done", "detail": f"课程={profile.get('course','')}, 基础={profile.get('knowledge_base','')}"},
            {"agent": "路径规划", "status": "done", "detail": f"目标={path.get('goal','')[:20]}"},
            {"agent": "初始化完成", "status": "done", "detail": "已为你准备好个性化学习方案"},
        ],
    }
