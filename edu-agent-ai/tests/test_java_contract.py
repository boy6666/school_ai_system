"""Java ↔ Python 契约测试：把 Feign 两侧的字段映射固化成断言。

背景（改造前 Python 侧与 Java 侧完全对不上，且失败是静默的）：
    Java  send: {message, context}                  → recv: Result<AiChatResult(answer, intent, references)>
    Java  send: {mode, chapter, topic, type, ...}   → recv: Result<Map>，读 data.items / data.analysis
    Python(旧): {user_input, student_id, profile}   → {intent, final_answer, ...}（裸 JSON，无信封）
    Python(旧): {resourceType, prompt}              → {content, resourceType, chapter}

本文件钉住适配层（api.py 的 ChatRequestFeign / ResourceGenRequestFeign / _result_* / _build_resource_prompt）
的两个方向：入参字段能被吃进去、出参结构与 Java 的读取键名一致。
以后任何一侧改名，这里应当直接变红。
"""

import importlib.util
import json
import sys
import types
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

AI_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(AI_DIR))

CHAT_URL = "/api/edu-agent-ai/chat"
RESOURCE_URL = "/api/edu-agent-ai/resource/generate"

_AI_MODULE = None


def _load_ai_module():
    """加载真实的 api.py（不打桩 school_agent：测的就是线上那份代码）。

    只关掉 Nacos——否则模块导入期会去连 127.0.0.1:8848（nacos_registry.apply_config_to_env）。
    用独立模块名加载，避免与 test_nacos_and_routes.py 里打桩加载的 api 互相污染缓存。
    """
    spec = importlib.util.spec_from_file_location("api_java_contract", AI_DIR / "api.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


@pytest.fixture
def ai(monkeypatch):
    global _AI_MODULE
    monkeypatch.setenv("NACOS_ENABLE", "0")
    if _AI_MODULE is None:
        _AI_MODULE = _load_ai_module()
    return _AI_MODULE


@pytest.fixture
def client(ai):
    # 不用 with TestClient(...)：那样会跑 lifespan，触发 Nacos 注册线程
    return TestClient(ai.app)


@pytest.fixture
def fake_graph(ai, monkeypatch):
    """替换 graph.invoke，记录收到的 state/config，返回一份固定的图结果。"""

    class FakeGraph:
        def __init__(self):
            self.last_state = None
            self.last_config = None

        def invoke(self, state, config=None):
            self.last_state = state
            self.last_config = config
            return {
                "intent": "TUTOR",
                "final_answer": "答案是 A",
                "profile": {"name": "张三"},
                "resources": {"mindmap": {"nodes": []}},
                "learning_path": {"goal": "掌握集合"},
                "safety_report": None,
                "evaluation_report": None,
                "resource_dir": "/tmp/resources",
                "profile_complete": True,
            }

    graph = FakeGraph()
    monkeypatch.setattr(ai, "graph", graph)
    return graph


@pytest.fixture
def fake_llm(monkeypatch):
    """替换 llm_client.call_llm，返回预定文本并记录入参。

    端点内是函数级 import（`from school_agent.services.llm_client import call_llm`），
    运行时才解析 sys.modules，所以这里替换模块即可生效。
    """
    state = {"reply": "", "calls": []}
    module = types.ModuleType("school_agent.services.llm_client")

    def call_llm(prompt, system_prompt="", **kwargs):
        state["calls"].append({"prompt": prompt, "system_prompt": system_prompt})
        return state["reply"]

    module.call_llm = call_llm
    module.call_llm_json = lambda *a, **k: {}
    monkeypatch.setitem(sys.modules, "school_agent.services.llm_client", module)
    return state


# ══════════════════════════════════════════════════════════════════
# 信封与工具函数
# ══════════════════════════════════════════════════════════════════

class TestResultEnvelope:
    """Result 信封必须与 edu-agent-common 的 Result<T>{code:int, message:String, data:T} 同形。"""

    def test_success_envelope_matches_java_result(self, ai):
        assert ai._result_ok({"x": 1}) == {"code": 0, "message": "success", "data": {"x": 1}}

    def test_fail_envelope_matches_java_result(self, ai):
        assert ai._result_fail(500, "炸了") == {"code": 500, "message": "炸了", "data": None}

    def test_error_codes_match_java_error_code_enum(self, ai):
        # edu-agent-common ErrorCode：SUCCESS(0) / BAD_REQUEST(400) / SYSTEM_ERROR(500)
        assert ai._result_ok(None)["code"] == 0
        assert ai._result_fail(400, "参数错误")["code"] == 400
        assert ai._result_fail(500, "系统异常")["code"] == 500

    def test_llm_failure_marker_detection(self, ai):
        # call_llm 不抛异常，失败时返回该前缀文本（llm_client.py:44）
        assert ai._is_llm_failure("LLM 调用失败: Connection error")
        assert not ai._is_llm_failure("都挺好")
        assert not ai._is_llm_failure(None)


class TestExtractJson:
    """LLM 输出的 JSON 提取：对象和数组都要支持（quiz 返回数组，路径规划返回对象）。"""

    def test_plain_object(self, ai):
        assert ai._extract_json('{"a": 1}') == {"a": 1}

    def test_plain_array(self, ai):
        assert ai._extract_json('[{"a": 1}]') == [{"a": 1}]

    def test_fenced_json(self, ai):
        assert ai._extract_json('好的\n```json\n[{"a": 1}]\n```\n以上') == [{"a": 1}]

    def test_braces_inside_prose(self, ai):
        assert ai._extract_json('结果如下 {"a": 1} 完毕') == {"a": 1}

    def test_brackets_inside_prose(self, ai):
        assert ai._extract_json('题目如下 [{"a": 1}] 完毕') == [{"a": 1}]

    def test_garbage_returns_none(self, ai):
        assert ai._extract_json("我不知道该怎么出题") is None

    def test_empty_returns_none(self, ai):
        assert ai._extract_json("") is None
        assert ai._extract_json(None) is None


# ══════════════════════════════════════════════════════════════════
# /chat
# ══════════════════════════════════════════════════════════════════

class TestChatFeignShape:
    """Java AiChatRequest{message, context} → Result<AiChatResult{answer, intent, references}>"""

    def test_message_and_context_are_accepted(self, ai, client, fake_graph):
        resp = client.post(CHAT_URL, json={
            "message": "什么是多态",
            "context": {"studentId": "s_1", "classId": "c_9"},
        })
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == 0 and body["message"] == "success"
        # AiChatResult 正好三个字段，多一个少一个都算漂了
        assert set(body["data"]) == {"answer", "intent", "references"}
        assert body["data"]["answer"] == "答案是 A"
        assert body["data"]["intent"] == "TUTOR"

    def test_message_reaches_graph_as_user_input(self, ai, client, fake_graph):
        client.post(CHAT_URL, json={"message": "什么是多态", "context": {"studentId": "s_1"}})
        assert fake_graph.last_state["user_input"] == "什么是多态"
        # 旧实现 message 被 Pydantic 静默丢掉 → user_input=""，图收到空问题
        assert fake_graph.last_state["user_input"] != ""

    def test_context_student_id_and_extra_keys_pass_through(self, ai, client, fake_graph):
        client.post(CHAT_URL, json={
            "message": "问题",
            "context": {"studentId": "s_1", "classId": "c_9", "courseName": "JavaSE"},
        })
        assert fake_graph.last_state["student_id"] == "s_1"
        profile = fake_graph.last_state["profile"]
        assert profile["classId"] == "c_9" and profile["courseName"] == "JavaSE"
        # 记忆线程按 student 隔离
        assert fake_graph.last_config["configurable"]["thread_id"].startswith("s_1_")

    def test_nested_profile_in_context_is_honoured(self, ai, client, fake_graph):
        client.post(CHAT_URL, json={
            "message": "问题",
            "context": {"studentId": "s_2", "profile": {"level": "basic"}, "classId": "c_1"},
        })
        profile = fake_graph.last_state["profile"]
        assert profile["level"] == "basic" and profile["classId"] == "c_1"

    def test_references_carries_graph_extras_not_null(self, ai, client, fake_graph):
        body = client.post(CHAT_URL, json={"message": "问题", "context": {}}).json()
        refs = body["data"]["references"]
        assert refs is not None
        assert refs["learning_path"] == {"goal": "掌握集合"}
        assert refs["resource_dir"] == "/tmp/resources"

    def test_blank_message_returns_bad_request_envelope(self, ai, client, fake_graph):
        # 对应 Java DTO 的 @NotBlank；HTTP 仍为 200（与 GlobalExceptionHandler 一致）
        resp = client.post(CHAT_URL, json={"message": "   ", "context": {}})
        assert resp.status_code == 200
        assert resp.json() == {"code": 400, "message": "参数错误: message 不能为空", "data": None}

    def test_legacy_direct_shape_unchanged(self, ai, client, fake_graph):
        resp = client.post(CHAT_URL, json={
            "user_input": "旧形态提问", "student_id": "s_3", "session_id": "sess_1",
        })
        assert resp.status_code == 200
        body = resp.json()
        assert "code" not in body  # 直连形态仍是裸 JSON
        assert body["final_answer"] == "答案是 A"
        assert body["profile_complete"] is True
        assert fake_graph.last_state["user_input"] == "旧形态提问"
        assert fake_graph.last_config["configurable"]["thread_id"] == "s_3_sess_1"


class TestChatAnswerUnwrapping:
    """explain 等意图的 final_answer 是 JSON 串，必须拆成正文再给 Java，否则前端渲染成一坨 JSON。"""

    EXPLAIN_JSON = json.dumps({
        "type": "explain", "content": "多态是同一接口的不同实现。",
        "weaknesses_focus": ["接口"], "suggestion": "多练接口题",
    }, ensure_ascii=False)

    def test_json_final_answer_becomes_plain_text(self, ai, client, fake_graph, monkeypatch):
        monkeypatch.setattr(fake_graph, "invoke",
                            lambda state, config=None: {"intent": "explain", "final_answer": self.EXPLAIN_JSON})
        body = client.post(CHAT_URL, json={"message": "什么是多态", "context": {}}).json()
        assert body["data"]["answer"] == "多态是同一接口的不同实现。"
        assert body["data"]["intent"] == "explain"

    def test_leftover_fields_go_to_references(self, ai, client, fake_graph, monkeypatch):
        monkeypatch.setattr(fake_graph, "invoke",
                            lambda state, config=None: {"intent": "explain", "final_answer": self.EXPLAIN_JSON})
        refs = client.post(CHAT_URL, json={"message": "q", "context": {}}).json()["data"]["references"]
        assert refs["weaknesses_focus"] == ["接口"]
        assert refs["suggestion"] == "多练接口题"
        assert "content" not in refs  # 正文已提到 answer，不重复

    def test_plain_text_answer_untouched(self, ai, client, fake_graph):
        body = client.post(CHAT_URL, json={"message": "q", "context": {}}).json()
        assert body["data"]["answer"] == "答案是 A"

    def test_legacy_shape_keeps_raw_final_answer(self, ai, client, fake_graph, monkeypatch):
        # 直连形态不做任何加工，保持历史行为
        monkeypatch.setattr(fake_graph, "invoke",
                            lambda state, config=None: {"intent": "explain", "final_answer": self.EXPLAIN_JSON})
        body = client.post(CHAT_URL, json={"user_input": "q"}).json()
        assert body["final_answer"] == self.EXPLAIN_JSON


# ══════════════════════════════════════════════════════════════════
# /resource/generate
# ══════════════════════════════════════════════════════════════════

QUIZ_REPLY = json.dumps([
    {"question": "以下哪个是接口关键字？", "options": ["class", "interface", "enum", "struct"],
     "answer": "interface", "explanation": "interface 用于声明接口"},
    {"question": "String 是否可变？", "options": ["可变", "不可变"], "answer": "不可变",
     "explanation": "String 是 immutable 的"},
], ensure_ascii=False)


class TestResourceQuizFeignShape:
    """Java QuestionServiceImpl 读 data.items（List<Map>），逐项取 type/chapter/topic/content/options/answer/explanation/difficulty。"""

    def test_items_are_returned_and_mapped(self, ai, client, fake_llm):
        fake_llm["reply"] = QUIZ_REPLY
        body = client.post(RESOURCE_URL, json={
            "mode": "quiz", "chapter": "第3章 集合", "topic": "HashMap",
            "type": "choice", "difficulty": "easy", "count": 2,
        }).json()

        assert body["code"] == 0
        items = body["data"]["items"]
        assert isinstance(items, list) and len(items) == 2
        first = items[0]
        # Java 映射时读取的全部键
        for key in ("type", "chapter", "topic", "content", "options", "answer",
                    "explanation", "difficulty"):
            assert key in first, f"缺字段 {key}"
        assert first["content"] == "以下哪个是接口关键字？"  # Java 读的是 content，不是 question
        assert first["question"] == first["content"]
        assert first["options"] == ["class", "interface", "enum", "struct"]
        assert first["answer"] == "interface"
        assert first["type"] == "choice"
        # 请求级字段回填，供 Java 在模型漏填时兜底
        assert first["chapter"] == "第3章 集合"
        assert first["topic"] == "HashMap"
        assert first["difficulty"] == "easy"

    def test_prompt_is_built_from_structured_fields(self, ai, client, fake_llm):
        fake_llm["reply"] = QUIZ_REPLY
        client.post(RESOURCE_URL, json={
            "mode": "quiz", "chapter": "第3章 集合", "topic": "HashMap",
            "type": "choice", "difficulty": "easy", "count": 2,
        })
        call = fake_llm["calls"][0]
        # 旧实现只把 req.prompt 喂给 LLM，而 Java 从不发 prompt → 空串发给模型
        assert call["prompt"] != ""
        for token in ("第3章 集合", "HashMap", "choice", "easy", "2"):
            assert token in call["prompt"]
        assert call["prompt"].strip().endswith("。") or "JSON 数组" in call["prompt"]
        # 角色取自 mode：出题专家，而不是默认的 mindmap
        assert "出题" in call["system_prompt"]

    def test_code_fenced_reply_is_parsed(self, ai, client, fake_llm):
        fake_llm["reply"] = "好的，题目如下：\n```json\n" + QUIZ_REPLY + "\n```"
        body = client.post(RESOURCE_URL, json={"mode": "quiz", "chapter": "c"}).json()
        assert body["code"] == 0 and len(body["data"]["items"]) == 2

    def test_object_wrapped_items_are_unwrapped(self, ai, client, fake_llm):
        fake_llm["reply"] = json.dumps({"items": json.loads(QUIZ_REPLY)}, ensure_ascii=False)
        body = client.post(RESOURCE_URL, json={"mode": "quiz", "chapter": "c"}).json()
        assert body["code"] == 0 and len(body["data"]["items"]) == 2

    def test_unparsable_reply_fails_loudly(self, ai, client, fake_llm):
        # 关键回归：宁可报错，也不要静默返回空题单（旧实现 Java 侧恒为 emptyList）
        fake_llm["reply"] = "这些题我就不出了"
        resp = client.post(RESOURCE_URL, json={"mode": "quiz", "chapter": "c"})
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == 500
        assert body["data"] is None
        assert "JSON" in body["message"]

    def test_llm_failure_is_not_reported_as_success(self, ai, client, fake_llm):
        fake_llm["reply"] = "LLM 调用失败: Connection error."
        body = client.post(RESOURCE_URL, json={"mode": "quiz", "chapter": "c"}).json()
        assert body["code"] == 500
        assert body["message"].startswith("LLM 调用失败")
        assert body["data"] is None


class TestResourceEvaluationFeignShape:
    """Java AiTutorServiceImpl.explainGrade 读 data.analysis（String），extra 带 studentId/assignmentId。"""

    def test_analysis_is_returned(self, ai, client, fake_llm):
        fake_llm["reply"] = "该生集合部分掌握较好，IO 部分偏弱。"
        body = client.post(RESOURCE_URL, json={
            "mode": "evaluation",
            "extra": {"studentId": "s_1", "assignmentId": "a_7"},
        }).json()
        assert body["code"] == 0
        assert body["data"]["analysis"] == "该生集合部分掌握较好，IO 部分偏弱。"

    def test_analysis_extracted_from_json_reply(self, ai, client, fake_llm):
        fake_llm["reply"] = json.dumps({"analysis": "掌握度 70%", "score": 70}, ensure_ascii=False)
        body = client.post(RESOURCE_URL, json={"mode": "evaluation"}).json()
        assert body["data"]["analysis"] == "掌握度 70%"

    def test_extra_is_forwarded_to_llm(self, ai, client, fake_llm):
        fake_llm["reply"] = "分析"
        client.post(RESOURCE_URL, json={
            "mode": "evaluation", "extra": {"studentId": "s_1", "assignmentId": "a_7"},
        })
        prompt = fake_llm["calls"][0]["prompt"]
        assert "s_1" in prompt and "a_7" in prompt
        assert "评价" in fake_llm["calls"][0]["system_prompt"]

    def test_extra_prompt_overrides_built_prompt(self, ai, client, fake_llm):
        fake_llm["reply"] = "分析"
        client.post(RESOURCE_URL, json={
            "mode": "evaluation", "chapter": "第3章", "extra": {"prompt": "只看 IO 部分"},
        })
        assert fake_llm["calls"][0]["prompt"] == "只看 IO 部分"


class TestResourceGenericAndLegacy:
    def test_resource_mode_returns_content_with_role_from_type(self, ai, client, fake_llm):
        fake_llm["reply"] = "- 集合\n- IO"
        body = client.post(RESOURCE_URL, json={
            "mode": "resource", "type": "mindmap", "chapter": "第3章", "topic": "集合",
        }).json()
        assert body["code"] == 0
        assert body["data"]["content"] == "- 集合\n- IO"
        assert body["data"]["resourceType"] == "mindmap"   # type 命中角色表
        assert "思维导图" in fake_llm["calls"][0]["system_prompt"]

    def test_unknown_mode_falls_back_to_generic_role(self, ai, client, fake_llm):
        fake_llm["reply"] = "内容"
        body = client.post(RESOURCE_URL, json={
            "mode": "resource", "type": "whatever", "chapter": "c",
        }).json()
        assert body["code"] == 0
        assert fake_llm["calls"][0]["system_prompt"].startswith("你是一位教育内容生成专家")

    def test_legacy_direct_shape_unchanged(self, ai, client, fake_llm):
        fake_llm["reply"] = "# 拓展阅读"
        resp = client.post(RESOURCE_URL, json={
            "chapter": "第1章", "topic": "变量", "resourceType": "reading", "prompt": "写拓展阅读",
        })
        assert resp.status_code == 200
        body = resp.json()
        assert "code" not in body  # 直连形态仍是裸 JSON
        assert body == {"content": "# 拓展阅读", "resourceType": "reading", "chapter": "第1章"}
        assert fake_llm["calls"][0]["prompt"] == "写拓展阅读"  # 直连的 prompt 原样透传
        assert "拓展阅读" in fake_llm["calls"][0]["system_prompt"]

    def test_legacy_path_keeps_error_text_visible(self, ai, client, fake_llm):
        # 直连形态的历史行为：把错误文本当 content 回给调用方（前端直接展示）
        fake_llm["reply"] = "LLM 调用失败: Connection error."
        body = client.post(RESOURCE_URL, json={"resourceType": "summary", "prompt": "总结"}).json()
        assert body["content"].startswith("LLM 调用失败")

    def test_empty_body_falls_back_to_legacy_shape(self, ai, client, fake_llm):
        fake_llm["reply"] = "x"
        body = client.post(RESOURCE_URL, json={}).json()
        assert "code" not in body and body["resourceType"] == "mindmap"
