"""AI 微服务化契约测试：Nacos 接入 + 对外路径前缀 + 网关路由对齐。

不依赖真实 Nacos / LLM：网络调用一律用假的 ``_request`` 拦截，
api 模块以桩模块导入（否则会拉起 school_agent.graph 并在导入期真连 Nacos）。
"""
import importlib
import json
import os
import sys
import threading
import time
import types
import urllib.error
import urllib.parse
import contextlib
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import nacos_registry  # noqa: E402

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
GATEWAY_YML = REPO_ROOT / "edu-agent-gateway" / "src" / "main" / "resources" / "application.yml"
FEIGN_CLIENT = (
    REPO_ROOT / "edu-agent-teacher" / "src" / "main" / "java" / "com" / "eduagent"
    / "teacher" / "feign" / "AiServiceClient.java"
)
AI_CONFIG_YAML = REPO_ROOT / "deploy" / "nacos-config" / "edu-agent-ai.yaml"


class _Recorder:
    """替换 nacos_registry._request：记录调用参数，按脚本返回/抛错。"""

    def __init__(self, reply="ok", error=None):
        self.calls = []
        self.reply = reply
        self.error = error

    def __call__(self, settings, path, params, method):
        self.calls.append({"path": path, "params": params, "method": method, "addr": settings.addr})
        if self.error is not None:
            raise self.error
        return self.reply

    @property
    def last(self):
        return self.calls[-1]


_MISSING = object()


@contextlib.contextmanager
def _stub_modules(stubs):
    """只临时替换指定模块名。

    不能用 ``mock.patch.dict(sys.modules, ...)``：它在退出时会把块内新导入的子模块
    （如 fastapi.routing）一并移除，之后重新导入会得到不同的 APIRoute 类对象，
    使测试里的 isinstance 判定失败。
    """
    saved = {name: sys.modules.get(name, _MISSING) for name in stubs}
    sys.modules.update(stubs)
    try:
        yield
    finally:
        for name, old in saved.items():
            if old is _MISSING:
                sys.modules.pop(name, None)
            else:
                sys.modules[name] = old


def _settings(**env):
    base = {
        "NACOS_ADDR": "nacos:8848",
        "NACOS_NAMESPACE": "edu-agent-local",
        "NACOS_GROUP": "edu-agent",
        "AI_SERVICE_NAME": "edu-agent-ai",
        "AI_SERVICE_PORT": "8001",
        "NACOS_REGISTER_IP": "10.1.2.3",
    }
    base.update(env)
    with mock.patch.dict(os.environ, base, clear=False):
        for key in ("NACOS_ENABLE", "NACOS_HEARTBEAT_INTERVAL", "NACOS_USERNAME", "NACOS_PASSWORD"):
            if key not in env:
                os.environ.pop(key, None)
        return nacos_registry.NacosSettings()


def _import_api_with_stubs():
    """以桩模块导入 api，避免拉起 langgraph/LLM 与导入期真实 Nacos 调用。"""
    pkg = types.ModuleType("school_agent")
    graph_mod = types.ModuleType("school_agent.graph")
    graph_mod.graph = object()
    registry_stub = types.ModuleType("nacos_registry")
    registry_stub.apply_config_to_env = lambda *a, **k: False
    registry_stub.start = lambda: types.SimpleNamespace(stop=lambda: None)
    patch = {
        "school_agent": pkg,
        "school_agent.graph": graph_mod,
        "nacos_registry": registry_stub,
    }
    with _stub_modules(patch):
        return importlib.import_module("api")


class TestNacosSettings:
    """注册参数默认值与开关"""

    def test_defaults_match_platform_convention(self):
        s = _settings()
        assert s.service_name == "edu-agent-ai"
        assert s.namespace == "edu-agent-local"
        assert s.group == "edu-agent"
        assert s.service_port == 8001
        assert s.enabled is True

    def test_register_ip_override_wins(self):
        s = _settings(NACOS_REGISTER_IP="172.20.0.7")
        assert s.detect_ip() == "172.20.0.7"

    def test_detect_ip_never_returns_loopback_by_default(self):
        # 容器内注册 127.0.0.1 会让网关回连到自己，必须探测真实网卡
        s = _settings(NACOS_REGISTER_IP="")
        s.addr = "127.0.0.1:1"  # 不可达时走兜底分支，仅验证不抛异常
        assert isinstance(s.detect_ip(), str)

    def test_enabled_toggle(self):
        assert _settings(NACOS_ENABLE="0").enabled is False
        assert _settings(NACOS_ENABLE="false").enabled is False
        assert _settings(NACOS_ENABLE="1").enabled is True

    def test_basic_auth_header(self):
        assert "Authorization" not in _settings().headers()
        assert "Authorization" in _settings(NACOS_USERNAME="nacos", NACOS_PASSWORD="pw").headers()


class TestNacosRegistrar:
    """注册 / 心跳 / 注销 三件事的请求形状"""

    def test_register_params(self):
        s = _settings()
        rec = _Recorder(reply="ok")
        with mock.patch.object(nacos_registry, "_request", rec):
            assert nacos_registry.NacosRegistrar(s).register() is True
        assert rec.last["method"] == "POST"
        assert rec.last["path"] == "/v1/ns/instance"
        p = rec.last["params"]
        assert p["serviceName"] == "edu-agent-ai"
        assert p["namespaceId"] == "edu-agent-local"
        assert p["groupName"] == "edu-agent"
        assert p["port"] == 8001
        assert p["ip"] == "10.1.2.3"
        assert p["ephemeral"] == "true"
        assert json.loads(p["metadata"])["lang"] == "python"

    def test_register_treats_non_ok_body_as_failure(self):
        rec = _Recorder(reply="something else")
        with mock.patch.object(nacos_registry, "_request", rec):
            assert nacos_registry.NacosRegistrar(_settings()).register() is False

    def test_heartbeat_params_use_sdk_beat_shape(self):
        rec = _Recorder(reply='{"code":200,"clientBeatInterval":5000}')
        with mock.patch.object(nacos_registry, "_request", rec):
            assert nacos_registry.NacosRegistrar(_settings()).heartbeat() is True
        assert rec.last["method"] == "PUT"
        assert rec.last["path"] == "/v1/ns/instance/beat"
        beat = json.loads(rec.last["params"]["beat"])
        assert beat["serviceName"] == "edu-agent-ai"
        assert beat["ip"] == "10.1.2.3"
        assert beat["port"] == 8001
        assert beat["ephemeral"] is True
        assert rec.last["params"]["groupName"] == "edu-agent"
        assert rec.last["params"]["namespaceId"] == "edu-agent-local"

    def test_heartbeat_rejects_non_200_code(self):
        rec = _Recorder(reply='{"code":404}')
        with mock.patch.object(nacos_registry, "_request", rec):
            assert nacos_registry.NacosRegistrar(_settings()).heartbeat() is False

    def test_deregister_params(self):
        rec = _Recorder(reply="ok")
        with mock.patch.object(nacos_registry, "_request", rec):
            assert nacos_registry.NacosRegistrar(_settings()).deregister() is True
        assert rec.last["method"] == "DELETE"
        assert rec.last["path"] == "/v1/ns/instance"
        assert rec.last["params"]["serviceName"] == "edu-agent-ai"

    def test_start_skipped_when_disabled(self):
        reg = nacos_registry.NacosRegistrar(_settings(NACOS_ENABLE="0"))
        reg.start()
        assert reg._thread is None

    def test_stop_deregisters_registered_instance(self):
        reg = nacos_registry.NacosRegistrar(_settings())
        reg._registered = True
        called = []
        reg.deregister = lambda: called.append(True) or True
        reg.stop()
        assert called == [True]
        assert reg._registered is False

    def test_unreachable_nacos_is_not_fatal(self):
        # 本机不开 Nacos 时 AI 必须仍能启动：注册失败只告警、指数退避重试
        rec = _Recorder(error=urllib.error.URLError("connection refused"))
        reg = nacos_registry.NacosRegistrar(_settings())
        with mock.patch.object(nacos_registry, "_request", rec):
            reg.start()
            deadline = time.time() + 6
            while len(rec.calls) < 2 and time.time() < deadline:
                time.sleep(0.05)
            reg.stop()
        assert len(rec.calls) >= 2, "注册失败后应重试"

    def test_reregister_after_heartbeat_failure(self):
        reg = nacos_registry.NacosRegistrar(_settings(NACOS_HEARTBEAT_INTERVAL="0.02"))
        lock = threading.Lock()
        seen = []

        def fake_register():
            with lock:
                seen.append(True)
            return True

        reg.register = fake_register
        reg.heartbeat = lambda: False
        reg.start()
        deadline = time.time() + 6
        while len(seen) < 2 and time.time() < deadline:
            time.sleep(0.02)
        reg.stop()
        assert len(seen) >= 2, "心跳被拒后应重新注册"


class TestConfigCenter:
    """配置中心下发（env 段 1:1 映射到环境变量）"""

    def test_env_applied_and_overrides_existing(self):
        content = "env:\n  LLM_MODEL: qwen-max\n  LOG_LEVEL: DEBUG\n"
        rec = _Recorder(reply=content)
        with mock.patch.object(nacos_registry, "_request", rec):
            with mock.patch.dict(os.environ, {"LLM_MODEL": "gpt-4o-mini"}):
                assert nacos_registry.apply_config_to_env() is True
                assert os.environ["LLM_MODEL"] == "qwen-max"
                assert os.environ["LOG_LEVEL"] == "DEBUG"
        assert rec.last["method"] == "GET"
        assert rec.last["path"].startswith("/v1/cs/configs?")
        qs = urllib.parse.parse_qs(rec.last["path"].split("?", 1)[1])
        assert qs["dataId"] == ["edu-agent-ai.yaml"]
        assert qs["group"] == ["edu-agent"]
        assert qs["tenant"] == ["edu-agent-local"]

    def test_missing_config_keeps_env(self):
        err = urllib.error.HTTPError("u", 404, "not found", {}, None)
        with mock.patch.object(nacos_registry, "_request", _Recorder(error=err)):
            with mock.patch.dict(os.environ, {"LLM_MODEL": "keep-me"}):
                assert nacos_registry.apply_config_to_env() is False
                assert os.environ["LLM_MODEL"] == "keep-me"

    def test_unreachable_config_center_is_not_fatal(self):
        err = urllib.error.URLError("connection refused")
        with mock.patch.object(nacos_registry, "_request", _Recorder(error=err)):
            assert nacos_registry.apply_config_to_env() is False

    def test_broken_yaml_is_not_fatal(self):
        with mock.patch.object(nacos_registry, "_request", _Recorder(reply="env: [oops")):
            assert nacos_registry.apply_config_to_env() is False

    def test_empty_body_is_ignored(self):
        with mock.patch.object(nacos_registry, "_request", _Recorder(reply="  ")):
            assert nacos_registry.apply_config_to_env() is False


class TestApiRoutes:
    """对外路径前缀：与网关断言、Java Feign path 三处一致"""

    def test_prefix_matches_feign_client_path(self):
        assert _import_api_with_stubs().API_PREFIX == "/api/edu-agent-ai"

    def test_routes_are_all_under_prefix(self):
        mod = _import_api_with_stubs()
        public = {
            "/api/edu-agent-ai/health",
            "/api/edu-agent-ai/chat",
            "/api/edu-agent-ai/path/generate",
            "/api/edu-agent-ai/resource/generate",
        }
        paths = {route.path for route in mod.app.routes}
        missing = public - paths
        assert not missing, f"缺少对外端点: {missing}"
        # 不允许再有挂在根路径上的业务端点（老版本 /chat 直挂根，网关需 StripPrefix）
        for bare in ("/chat", "/path/generate", "/resource/generate"):
            assert bare not in paths, f"{bare} 不应挂在根路径"

    def test_root_health_alias_kept_for_container_probe(self):
        mod = _import_api_with_stubs()
        from fastapi.routing import APIRoute

        route = next(r for r in mod.app.routes if isinstance(r, APIRoute) and r.path == "/health")
        assert route.endpoint() == {"status": "ok"}


class TestGatewayAlignment:
    """网关路由 = Python 实际暴露端点，且禁用通配（契约 C5：/code/analyze 过网关必须 404）"""

    def _ai_route(self):
        import yaml

        doc = yaml.safe_load(GATEWAY_YML.read_text(encoding="utf-8"))
        routes = doc["spring"]["cloud"]["gateway"]["routes"]
        return next(r for r in routes if r["id"] == "edu-agent-ai"), doc

    def test_route_uses_service_discovery(self):
        route, _ = self._ai_route()
        assert route["uri"] == "lb://edu-agent-ai"

    def test_predicates_enumerated_without_wildcard(self):
        route, _ = self._ai_route()
        joined = ",".join(route["predicates"])
        assert "*" not in joined, "禁止通配断言：会把内网专用端点暴露给普通用户"
        assert "/api/edu-agent-ai/**" not in joined

    def test_gateway_covers_every_public_ai_endpoint(self):
        route, _ = self._ai_route()
        declared = {
            p.strip()
            for pred in route["predicates"]
            if pred.startswith("Path=")
            for p in pred[len("Path="):].split(",")
        }
        mod = _import_api_with_stubs()
        public = {
            r.path for r in mod.app.routes
            if r.path.startswith(mod.API_PREFIX) and r.path != f"{mod.API_PREFIX}/code/analyze"
        }
        assert public <= declared, f"网关未覆盖: {public - declared}"

    def test_internal_only_endpoint_not_routed(self):
        route, _ = self._ai_route()
        joined = ",".join(route["predicates"])
        assert "/api/edu-agent-ai/code/analyze" not in joined

    def test_health_whitelisted_for_ops_probe(self):
        _, doc = self._ai_route()
        assert "/api/edu-agent-ai/health" in doc["gateway"]["auth"]["whitelist"].split(",")


class TestConfigFileKeys:
    """下发配置的键名必须真的被 Python 读取（否则是死配置）"""

    def test_env_keys_are_read_by_python_code(self):
        import yaml

        doc = yaml.safe_load(AI_CONFIG_YAML.read_text(encoding="utf-8"))
        keys = set(doc["env"])
        assert keys, "edu-agent-ai.yaml 缺少 env 段"
        read_envs = {"OPENAI_API_KEY", "OPENAI_BASE_URL", "LLM_MODEL",
                     "LLM_TEMPERATURE", "LLM_MAX_TOKENS", "LOG_LEVEL"}
        assert keys <= read_envs, f"存在无人读取的键: {keys - read_envs}"

    def test_no_legacy_mock_flag(self):
        text = AI_CONFIG_YAML.read_text(encoding="utf-8")
        assert "USE_MOCK_LLM:" not in text, "Python 侧无 mock 分支，该键会误导联调"


class TestFeignContract:
    """Java 侧 @FeignClient 是路径/服务名的权威来源"""

    def test_feign_client_declaration(self):
        text = FEIGN_CLIENT.read_text(encoding="utf-8")
        assert 'name = "edu-agent-ai"' in text
        assert 'path = "/api/edu-agent-ai"' in text
