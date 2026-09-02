"""代码评审：支撑 `POST /api/ai/code/analyze`（dev-wuyoucheng §1.3.4 / §2.4.5 / 附录 A）。

- 判分事实由 code-service 传入（编译/Checkstyle/PMD/运行结果），AI 只做参考反馈、**不改判**。
- 大模型故障时返回最小结构化响应（不伪造分析），绝不阻塞判分主链路（§2.4.5 容错精神）。
- wire 命名遵循《契约对齐决议》C4：对外一律 camelCase（模型可能回 snake_case，在此归一化）。
"""
import json
import re
from typing import Any, Dict


def _log(msg: str):
    """打印日志，兼容 Windows GBK 终端"""
    try:
        print(msg.encode('gbk', errors='replace').decode('gbk'))
    except Exception:
        print(msg.encode('ascii', errors='replace').decode('ascii'))


# 附录 A：固定 system prompt（吴友诚 spec，勿让模型自行发挥）
CODE_REVIEW_SYSTEM_PROMPT = """你是一位资深 Java 代码评审专家。请基于以下信息给出改进建议。
【输入】源码、编译结果、Checkstyle/PMD 静态检查结果、运行结果。
【要求】只返回严格 JSON（不要 markdown/代码块），结构如下：
{
  "suggestions": [{"severity":"info|warning|error","location":"文件:行号","title":"...","detail":"...","example":"..."}],
  "summary": "一句话总结",
  "overall_comment": "整体评价",
  "score_hint": 0-100
}
【范围】仅 JavaSE 基础，指出可读性/健壮性/规范问题，不引入 Spring 等框架。"""


def _status(flag: Any) -> str:
    """0/1 → 否/是，None / 缺省 → 未知"""
    if flag == 1:
        return "是"
    if flag == 0:
        return "否"
    return "未知"


def build_prompt(req: Dict[str, Any]) -> str:
    """把 code-service 传来的判分事实拼成用户 prompt（§1.3.4 字段为参照）"""
    language = req.get("language") or "java"
    source_code = req.get("sourceCode") or ""
    ctx = req.get("context") or {}

    parts = [
        f"【语言】{language}",
        f"【源码】\n{source_code}",
    ]
    if ctx:
        parts.append(
            "【判分事实】编译通过={}；Checkstyle 违规数={}；PMD 违规数={}；运行通过={}。"
            "程序实际输出={!r}；期望输出={!r}。".format(
                _status(ctx.get("compileOk")),
                ctx.get("checkstyleErrors", "未知"),
                ctx.get("pmdViolations", "未知"),
                _status(ctx.get("runPassed")),
                ctx.get("runStdout") or "",
                ctx.get("expectedOutput") or "",
            )
        )
    return "\n\n".join(parts)


def _escape_string_newlines(text: str) -> str:
    """LLM（尤其 deepseek）常把代码示例/建议里的换行、Tab 直接写进 JSON 字符串字面量
    （不转义），严格 json.loads 会报 'Expecting ',' delimiter'。用状态机只把字符串
    内部的裸 \\n/\\r/\\t 转义，结构空白与外层转义序列（\\n、\\" 等）原样保留。"""
    out = []
    in_str = False
    i, n = 0, len(text)
    while i < n:
        ch = text[i]
        if in_str:
            if ch == "\\":
                out.append(ch)
                if i + 1 < n:
                    out.append(text[i + 1])
                    i += 1
            elif ch == '"':
                in_str = False
                out.append(ch)
            elif ch == "\n":
                out.append("\\n")
            elif ch == "\r":
                out.append("\\r")
            elif ch == "\t":
                out.append("\\t")
            else:
                out.append(ch)
        else:
            if ch == '"':
                in_str = True
            out.append(ch)
        i += 1
    return "".join(out)


def _load_json(cand: str) -> Dict[str, Any]:
    """对单个候选串依次尝试：严格 json.loads → 字符串内裸换行转义 → json_repair 容错。
    deepseek 实测会产出非严格 JSON（裸换行、注释里未转义的双引号如 System.out.println("x") 等），
    逐级兜底，绝不因解析失败而降级为空反馈（除非真找不到 JSON）。"""
    try:
        obj = json.loads(cand)
        return obj if isinstance(obj, dict) else None
    except json.JSONDecodeError:
        pass
    try:
        obj = json.loads(_escape_string_newlines(cand))
        return obj if isinstance(obj, dict) else None
    except json.JSONDecodeError:
        pass
    try:
        from json_repair import loads as repair_loads
        obj = repair_loads(cand)
        return obj if isinstance(obj, dict) else None
    except Exception:
        return None


def _extract_json(text: str) -> Dict[str, Any]:
    """从 LLM 返回提取 JSON：整串 → ```json``` 代码块 → 花括号区间，每层都走 _load_json 容错"""
    if not text:
        raise ValueError("LLM 返回为空")

    candidates = [text, _escape_string_newlines(text)]
    for cand in candidates:
        data = _load_json(cand)
        if data is not None:
            return data
    for cand in candidates:
        m = re.search(r"```(?:json)?\s*([\s\S]*?)```", cand)
        if m:
            data = _load_json(m.group(1).strip())
            if data is not None:
                return data
        m = re.search(r"\{[\s\S]*\}", cand)
        if m:
            data = _load_json(m.group(0))
            if data is not None:
                return data
    raise ValueError("LLM 返回中未找到 JSON")


def _clamp_score(v: Any) -> int:
    try:
        return max(0, min(100, int(float(v))))
    except (TypeError, ValueError):
        return 0


def _normalize(data: Any) -> Dict[str, Any]:
    """归一化为对外 camelCase（兼容模型按附录 A 返回 snake_case）"""
    if not isinstance(data, dict):
        raise ValueError("LLM 返回非 JSON 对象")

    suggestions = []
    for s in data.get("suggestions") or []:
        if not isinstance(s, dict):
            continue
        suggestions.append({
            "severity": str(s.get("severity") or "info"),
            "location": str(s.get("location") or ""),
            "title": str(s.get("title") or ""),
            "detail": str(s.get("detail") or ""),
            "example": str(s.get("example") or ""),
        })

    return {
        "suggestions": suggestions,
        "summary": str(data.get("summary") or ""),
        "overallComment": str(data.get("overallComment") or data.get("overall_comment") or ""),
        "scoreHint": _clamp_score(data.get("scoreHint") if data.get("scoreHint") is not None else data.get("score_hint")),
    }


def fallback_response() -> Dict[str, Any]:
    """LLM 故障的最小响应：不伪造分析，让 Java 侧拿到合法 data 并继续判分"""
    return {
        "suggestions": [],
        "summary": "AI 暂不可用，本次未生成改进建议（不影响判分）。",
        "overallComment": "",
        "scoreHint": 0,
    }


def analyze_code(req: Dict[str, Any]) -> Dict[str, Any]:
    """调用 LLM 生成代码评审建议；任何异常都降级为最小响应，绝不让 AI 抖动拖垮判分"""
    from school_agent.services.llm_client import call_llm

    prompt = build_prompt(req)
    _log(f"[代码评审AI] ===== 收到请求 =====")
    _log(f"[代码评审AI] language={req.get('language') or 'java'}, sourceCode 长度={len(req.get('sourceCode') or '')}")
    _log(f"[代码评审AI] 🚀 正在调用 LLM...")
    text = call_llm(prompt, system_prompt=CODE_REVIEW_SYSTEM_PROMPT)
    _log(f"[代码评审AI] 📥 LLM 返回(前300字): {text[:300] if text else '空'}...")
    try:
        data = _normalize(_extract_json(text))
    except Exception as e:
        _log(f"[代码评审AI] ⚠️ LLM 输出不可解析({e})，返回最小响应")
        return fallback_response()
    _log(f"[代码评审AI] ✅ 生成 {len(data['suggestions'])} 条建议, scoreHint={data['scoreHint']}")
    _log(f"[代码评审AI] ===== 请求结束 =====")
    return data
