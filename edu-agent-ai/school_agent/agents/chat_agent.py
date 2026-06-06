"""通用对话智能体 — 新手引导 + 日常聊天"""
from school_agent.services.flow_logger import log
from school_agent.services.llm_client import call_llm
from school_agent.utils.json_utils import merge_agent_output
import json

# 第一段：纯系统介绍，不提问题
SYSTEM_INTRO = """👋 你好！我是你的专属学习伙伴小航。

这个平台会帮你：
- 分析学习情况，定制专属学习计划
- 规划每日学习任务
- 生成配套的学习资料

接下来我会问你几个简单的问题，帮你建立学习画像。准备好了吗？"""

# 画像采集 prompt（中文）
PROFILE_SYSTEM = """你是学习画像采集助手。每次只问一个问题。

已收集的画像：{profile}
还缺少：{missing}

返回JSON（只返回JSON，不要其他内容）：
{{"reply":"一个问题（50字以内）","complete":false,"extracted":{{"course":"","topic":"","knowledge_base":"","pace":""}}}}

规则：
1. 每次只问一个问题，按顺序：topic -> course -> knowledge_base -> pace
2. 当学生回答含糊时，追问确认
3. 绝不回答编程知识问题
4. 语气友好鼓励"""




def chat_agent(state: dict) -> dict:
    user_input = state.get("user_input", "")
    profile = state.get("profile", {})
    is_onboarding = state.get("is_onboarding", False)

    log("CHAT-DEBUG", f"onboarding={is_onboarding}, init={profile.get('initialized')}, topic={profile.get('topic')}")

    # Phase 1: 系统介绍（仅第一次，无问题）
    if is_onboarding and (not profile or not profile.get("initialized")):
        return {
            "final_answer": SYSTEM_INTRO,
            "is_onboarding": True,
            "profile": {"initialized": True, "course": "", "topic": "", "knowledge_base": "", "weaknesses": [], "pace": ""},
            "agent_outputs": merge_agent_output(state, "chat_agent", {"stage": "intro"}),
        }

    # Phase 2: 画像采集对话
    profile_for_llm = {k: v for k, v in profile.items() if k != "initialized"}
    profile_json = json.dumps(profile_for_llm, ensure_ascii=False)
    
    required = ["topic", "course", "knowledge_base", "pace"]
    missing = [f for f in required if not profile.get(f)]
    
    # Phase 2: LLM-driven profile collection
    profile_for_llm = {k: v for k, v in profile.items() if k != "initialized"}
    profile_json = json.dumps(profile_for_llm, ensure_ascii=False)

    required = ["topic", "course", "knowledge_base", "pace"]
    missing = [f for f in required if not profile.get(f)]
    missing_str = ', '.join(missing) if missing else 'none'
    missing_str = "\u3001".join(missing) if missing else "\u65e0"

    prompt = PROFILE_SYSTEM.replace("{profile}", profile_json).replace("{missing}", missing_str)
    prompt += "\n学生说：" + user_input + "\n返回JSON："

    resp = call_llm(prompt)
    
    try:
        result = json.loads(resp.strip().strip("```json").strip("```"))
    except:
        result = {"reply": "明白了！还差" + missing_str + "的信息，我们继续？", "complete": False, "extracted": {}}

    # 合并画像
    for key, value in result.get("extracted", {}).items():
        if value and (not profile.get(key)):
            profile[key] = value

    # 判断画像是否完整
    all_filled = all(profile.get(f) for f in required)
    is_complete = result.get("complete", False) or all_filled

    if is_complete:
        return {
            "final_answer": result.get("reply", "画像已完善！") + "\n\n画像采集完成，正在为你生成学习路径...",
            "profile": profile,
            "is_onboarding": False,
            "profile_complete": True,
            "silent_tasks": ["update_profile", "init_path"],
            "agent_outputs": merge_agent_output(state, "chat_agent", {"stage": "complete"}),
        }

    return {
        "final_answer": result.get("reply", "继续说，我在听~"),
        "profile": profile,
        "is_onboarding": True,
        "agent_outputs": merge_agent_output(state, "chat_agent", {"stage": "collecting"}),
    }
