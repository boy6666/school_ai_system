"""代码评审服务测试：prompt 构造 / JSON 提取 / camelCase 归一 / LLM 故障降级"""

import json
import sys
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from school_agent.services.code_review import analyze_code, build_prompt, fallback_response  # noqa: E402


class TestBuildPrompt:
    def test_context_facts_formatted(self):
        req = {
            "language": "java",
            "sourceCode": "public class Main {}",
            "context": {
                "compileOk": 1,
                "checkstyleErrors": 2,
                "pmdViolations": 1,
                "runPassed": 0,
                "runStdout": "hi\n",
                "expectedOutput": "hi\n",
            },
        }
        prompt = build_prompt(req)
        assert "public class Main {}" in prompt
        assert "编译通过=是" in prompt
        assert "Checkstyle 违规数=2" in prompt
        assert "PMD 违规数=1" in prompt
        assert "运行通过=否" in prompt
        assert "'hi\\n'" in prompt

    def test_missing_context_ok(self):
        prompt = build_prompt({"language": "java", "sourceCode": "x"})
        assert "【源码】" in prompt
        assert "【判分事实】" not in prompt


class TestAnalyzeCode:
    def test_llm_ok_normalizes_snake_to_camel(self):
        llm_out = json.dumps({
            "suggestions": [
                {"severity": "warning", "location": "Main.java:1",
                 "title": "建议空指针判断", "detail": "……", "example": "if (a != null)"}
            ],
            "summary": "代码可运行，建议增强健壮性。",
            "overall_comment": "整体良好",
            "score_hint": 88,
        })
        with patch("school_agent.services.llm_client.call_llm", return_value=llm_out):
            data = analyze_code({"language": "java", "sourceCode": "public class Main {}"})

        assert len(data["suggestions"]) == 1
        s = data["suggestions"][0]
        assert s["severity"] == "warning"
        assert s["title"] == "建议空指针判断"
        assert data["summary"] == "代码可运行，建议增强健壮性。"
        assert data["overallComment"] == "整体良好"      # snake → camel
        assert data["scoreHint"] == 88                   # snake → camel

    def test_llm_json_in_code_block(self):
        llm_out = "```json\n" + json.dumps({
            "suggestions": [], "summary": "s", "overall_comment": "o", "score_hint": 90,
        }) + "\n```"
        with patch("school_agent.services.llm_client.call_llm", return_value=llm_out):
            data = analyze_code({"language": "java", "sourceCode": "x"})
        assert data["scoreHint"] == 90
        assert data["summary"] == "s"

    def test_llm_failure_falls_back_minimal(self):
        with patch("school_agent.services.llm_client.call_llm",
                   return_value="LLM 调用失败: connection refused"):
            data = analyze_code({"language": "java", "sourceCode": "x"})

        assert data == fallback_response()
        assert data["suggestions"] == []
        assert "暂不可用" in data["summary"]
        assert data["scoreHint"] == 0

    def test_score_hint_clamped(self):
        llm_out = json.dumps({"suggestions": [], "summary": "s", "score_hint": 150})
        with patch("school_agent.services.llm_client.call_llm", return_value=llm_out):
            data = analyze_code({"language": "java", "sourceCode": "x"})
        assert data["scoreHint"] == 100

    def test_llm_raw_newline_inside_string(self):
        # 偶发场景：deepseek 把 example 代码里的换行直接写进 JSON 字符串（不转义），
        # 严格 json.loads 报 'Expecting ',' delimiter'；须容错转义后再解析（REAL 联调踩到）
        # 字符串值里放真·换行字符（非 \\n 转义）——严格 json.loads 必然拒绝
        llm_out = ('{"suggestions": [{"severity":"error","location":"Calculator.java:4",'
                   '"title":"除以零","detail":"d","example":"if (b != 0) {' + '\n'
                   + '  System.out.println(a/b);' + '\n' + '}"}],'
                   '"summary":"s","overall_comment":"o","score_hint":70}')
        with patch("school_agent.services.llm_client.call_llm", return_value=llm_out):
            data = analyze_code({"language": "java", "sourceCode": "x"})
        assert data["scoreHint"] == 70
        assert data["suggestions"][0]["example"].startswith("if (b != 0) {")
        assert "System.out.println(a/b);" in data["suggestions"][0]["example"]

    def test_llm_unescaped_quote_inside_string(self):
        # example 值内含字面未转义双引号（System.out.println("提示")）——严格 json.loads 必然拒绝，
        # 裸换行转义也救不了，须 json_repair 兜底（REAL 联调实测 raw2 踩中）
        llm_out = ('{"suggestions": [{"severity":"warning","location":"Calculator.java:5",'
                   '"title":"输出提示","detail":"d","example":"System.out.println("提示");"}],'
                   '"summary":"s","overall_comment":"o","score_hint":60}')
        with patch("school_agent.services.llm_client.call_llm", return_value=llm_out):
            data = analyze_code({"language": "java", "sourceCode": "x"})
        assert data["scoreHint"] == 60
        assert data["suggestions"][0]["example"] == 'System.out.println("提示");'
