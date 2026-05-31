from school_agent.services.llm_client import call_llm
from school_agent.utils.code_fixer import fix_code
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import compact_text, get_main_topic, to_text

TUTOR_SYSTEM_PROMPT = """你是一个友善、耐心的大学编程辅导老师，名字叫"小航"。

## ⚠️ 代码规则（最高优先级！违反即为错误回答！）
你必须输出标准 Java 代码，代码必须能直接编译运行。
以下行为绝对禁止：
- 禁止把 class 写成"类"，禁止把 interface 写成"接口"
- 禁止把 public/private/void/static/extends/implements/abstract/new/return 翻译成中文
- 禁止把 String 写成"字符串"，禁止把 int 写成"整数"
- 正确：public class Bird extends Animal implements Flyable { private String name; }
- 错误：公共类 鸟 扩展 动物 实现 可飞行 { 私有字符串 名称; }

## 排版格式（严格遵守）
- 每个段落之间必须有空行分隔
- 每个要点用 - 开头，独占一行
- 代码块前后各空一行
- 用加粗突出关键词

## 回复长度
- 每次 200-400 字，代码不计字数
- 每段不超过 4 行，每次最多 1 个代码示例（≤20行）
- 结尾追问一句

## 你的风格
- 像朋友聊天，先安抚再讲解
- 结合学生画像中的薄弱点，有针对性地展开
- 不说"根据你的画像"这类机械的话"""


def tutor_agent(state: dict) -> dict:
    """智能辅导智能体——答疑、纠错、学习引导。

    当学生提问中包含"不会""不懂""错在哪""为什么错"等信号时触发，
    进行自然的一对一辅导对话。
    """
    profile = state.get("profile", {})
    user_input = state.get("user_input", "")
    topic = get_main_topic(profile)
    context = compact_text(state.get("retrieved_context", ""), max_chars=1500)

    # 构建画像摘要（自然地融入 prompt，不要让 AI 机械地复述）
    weaknesses = profile.get("weaknesses", [])
    mistake_patterns = profile.get("mistake_patterns", [])
    cognitive_style = profile.get("cognitive_style", "")
    learning_goal = profile.get("learning_goal", "")
    grade = profile.get("grade", "")
    overall_type = profile.get("overall_type", "")

    profile_context_parts = []
    if grade:
        profile_context_parts.append(f"学生年级：{grade}")
    if learning_goal:
        profile_context_parts.append(f"学习目标：{learning_goal}")
    if topic and topic != "当前知识点":
        profile_context_parts.append(f"当前学习主题：{topic}")
    if weaknesses:
        profile_context_parts.append(f"已知薄弱点：{', '.join(weaknesses[:5])}")
    if mistake_patterns:
        profile_context_parts.append(f"常见错误模式：{', '.join(mistake_patterns[:3])}")
    if cognitive_style:
        profile_context_parts.append(f"学习偏好：{cognitive_style}")
    if overall_type:
        profile_context_parts.append(f"学生类型：{overall_type}")

    profile_summary = "\n".join(profile_context_parts) if profile_context_parts else "暂无学生画像信息"

    prompt = f"""## 学生背景信息
{profile_summary}

## 知识库参考
{context if context else "暂无相关知识库内容"}

## 学生的问题
{user_input}

请回答。最关键规则：
⚠️ 代码必须纯英文 Java 语法！class 不能写成"类"
⚠️ 每段话之间加空行分隔
- 200-400字，1个代码示例，结尾追问"""

    try:
        raw = call_llm(prompt, system=TUTOR_SYSTEM_PROMPT)
        llm_answer = fix_code(raw)  # 强制修复中文代码
    except Exception:
        llm_answer = (
            f"我理解你在 {topic} 上遇到了困难。"
            f"让我们一起来分析一下这个问题。"
            f"你可以先说说你已经理解的部分，我再帮你补充不清楚的地方。"
        )

    return {
        "final_answer": llm_answer,
        "agent_outputs": merge_agent_output(
            state,
            "tutor_agent",
            {"status": "success", "topic": topic},
        ),
    }
