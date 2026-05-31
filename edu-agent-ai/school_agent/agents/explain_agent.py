from school_agent.services.llm_client import call_llm
from school_agent.utils.code_fixer import fix_code
from school_agent.utils.json_utils import merge_agent_output
from school_agent.utils.text_utils import compact_text, get_main_topic, to_text

EXPLAIN_SYSTEM_PROMPT = """你是一个知识渊博、善于表达的大学编程讲解老师，名字叫"小航"。

## ⚠️ 代码规则（最高优先级！违反即为错误回答！）
你必须输出标准 Java 代码，这样的代码必须能直接编译运行。
以下行为绝对禁止，违反一次回答即为无效：
- 禁止把 class 写成"类"，禁止把 interface 写成"接口"
- 禁止把 public/private/void/static/extends/implements/abstract/new/return 翻译成中文
- 禁止把 String 写成"字符串"，禁止把 int 写成"整数"
- 禁止 this/super 写成中文
- 一句话总结：代码块里每个英文关键字都必须保持英文，注释可以用中文

正确：public class Bird extends Animal implements Flyable { private String name; }
错误：公共类 鸟 扩展 动物 实现 可飞行 { 私有字符串 名称; }

## 排版格式（严格遵守）
- 每个段落之间必须有空行分隔，不许把多段挤在一起
- 每个要点用 - 符号开头，独占一行
- 代码块前后各空一行
- 用加粗突出关键词

## 回复长度
- 每次 200-400 字，代码不计入字数
- 每段不超过 4 行
- 每次最多 1 个代码示例，不超过 20 行
- 结尾追问一句"这部分清楚吗？"

## 讲解风格
- 先给生活类比 → 再讲概念 → 给代码 → 追问
- 像朋友聊天，不要写教科书
- 结合学生薄弱点，自然地调整讲解"""


def explain_agent(state: dict) -> dict:
    """个性化讲解智能体——知识点的自然讲解。

    当学生请求讲解、解释某个知识点时触发，
    结合学生画像和课程知识库进行结构化但自然的讲解。
    """
    profile = state.get("profile", {})
    user_input = state.get("user_input", "")
    topic = get_main_topic(profile)
    context = compact_text(state.get("retrieved_context", ""), max_chars=2000)

    # 构建背景信息
    weaknesses = profile.get("weaknesses", [])
    mistake_patterns = profile.get("mistake_patterns", [])
    cognitive_style = profile.get("cognitive_style", "")
    learning_goal = profile.get("learning_goal", "")
    grade = profile.get("grade", "")

    profile_parts = []
    if grade:
        profile_parts.append(f"年级：{grade}")
    if learning_goal:
        profile_parts.append(f"学习目标：{learning_goal}")
    if topic and topic != "当前知识点":
        profile_parts.append(f"当前主题：{topic}")
    if weaknesses:
        profile_parts.append(f"薄弱点：{', '.join(weaknesses[:5])}")
    if mistake_patterns:
        profile_parts.append(f"易错模式：{', '.join(mistake_patterns[:3])}")
    if cognitive_style:
        profile_parts.append(f"学习偏好：{cognitive_style}")

    profile_summary = "\n".join(profile_parts) if profile_parts else "暂无学生信息"

    prompt = f"""## 学生背景
{profile_summary}

## 知识库参考资料
{context if context else "暂无相关知识库内容，请基于你自己的知识讲解。"}

## 学生请求
{user_input}

请讲解。记住最关键的规则：
⚠️ 代码必须纯英文 Java 语法！class 不能写成"类"，String 不能写成"字符串"
⚠️ 每段话之间加空行，不要挤在一起
- 总字数 200-400 字，1 个代码示例，结尾追问"""

    try:
        raw = call_llm(prompt, system=EXPLAIN_SYSTEM_PROMPT)
        llm_answer = fix_code(raw)  # 强制修复中文代码
    except Exception:
        llm_answer = (
            f"好的，我来给你讲讲 {topic}。\n\n"
            f"{topic} 是计算机领域一个很重要的概念。"
            f"简单来说，它解决的是……嗯，让我用一个生活中的例子来帮你理解。"
            f"你可以先告诉我，你目前对 {topic} 了解多少？这样我可以更有针对性地讲解。"
        )

    # 轻量包装：只在末尾添加知识库引用提示，不破坏对话感
    if context and len(context) > 50:
        llm_answer += (
            "\n\n---\n"
            "💡 以上讲解参考了课程知识库的内容。如果你想深入某个细节，随时问我！"
        )

    return {
        "final_answer": llm_answer,
        "agent_outputs": merge_agent_output(
            state,
            "explain_agent",
            {"status": "success", "topic": topic},
        ),
    }
