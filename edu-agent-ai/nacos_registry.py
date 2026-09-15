"""Nacos 接入：服务注册（服务发现）+ 配置中心拉取。

契约（与 Java / 网关对齐，改名需三处同步）：
    服务名   edu-agent-ai     ← teacher 的 @FeignClient(name = "edu-agent-ai")
                              ← 网关 application.yml: uri: lb://edu-agent-ai
    命名空间 edu-agent-local   分组 edu-agent（与其它 Java 服务处于同一内网）

为什么用标准库手写而不用 nacos-sdk-python：
    - 3.x 会引入 60+ 传递依赖（alibabacloud KMS SDK / grpcio / a2a-sdk…），为一个注册动作不值得；
    - 0.1.x 已停更，且同样不含心跳线程，省下的代码有限。
    本模块与 deploy/push-nacos-config.py 走同一套 Nacos v1 HTTP 接口，风格一致、可离线调试。

失败策略：全部只告警、不抛异常。服务发现是增强而非前置——本机不开 Nacos 时 AI 必须仍能独立启动。
"""
import base64
import json
import os
import socket
import threading
import urllib.error
import urllib.parse
import urllib.request

CONFIG_DATA_ID = "edu-agent-ai.yaml"


def _log(msg: str):
    """打印日志，兼容 Windows GBK 终端（与 api.py 同风格）"""
    try:
        print(msg.encode("gbk", errors="replace").decode("gbk"))
    except Exception:
        print(msg.encode("ascii", errors="replace").decode("ascii"))


class NacosSettings:
    """Nacos 连接参数。运行期读取环境变量（而非模块导入期），
    以便配置中心下发的值能在注册前先生效。"""

    def __init__(self):
        self.addr = os.getenv("NACOS_ADDR", "127.0.0.1:8848")
        self.namespace = os.getenv("NACOS_NAMESPACE", "edu-agent-local")
        self.group = os.getenv("NACOS_GROUP", "edu-agent")
        self.username = os.getenv("NACOS_USERNAME", "")
        self.password = os.getenv("NACOS_PASSWORD", "")
        self.service_name = os.getenv("AI_SERVICE_NAME", "edu-agent-ai")
        self.service_port = int(os.getenv("AI_SERVICE_PORT", "8001"))
        self.register_ip = os.getenv("NACOS_REGISTER_IP", "").strip()
        self.heartbeat_interval = float(os.getenv("NACOS_HEARTBEAT_INTERVAL", "5"))
        self.timeout = float(os.getenv("NACOS_HTTP_TIMEOUT", "5"))
        self.enabled = os.getenv("NACOS_ENABLE", "1").strip().lower() not in ("0", "false", "no")

    def headers(self) -> dict:
        h = {"Content-Type": "application/x-www-form-urlencoded"}
        if self.username:
            token = base64.b64encode(f"{self.username}:{self.password}".encode()).decode()
            h["Authorization"] = f"Basic {token}"
        return h

    def host(self) -> str:
        return self.addr.split(":")[0]

    def detect_ip(self) -> str:
        """注册用的 IP：容器内必须广播容器网卡地址（不能是 127.0.0.1，否则 Nacos 无法回连）。
        用 UDP socket 的出口地址探测——connect 不发包，只让内核选出到 Nacos 所在的网卡。"""
        if self.register_ip:
            return self.register_ip
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
                s.connect((self.host(), 53))
                return s.getsockname()[0]
        except Exception:
            return "127.0.0.1"


def _request(settings: NacosSettings, path: str, params: dict, method: str) -> str:
    """调用 Nacos v1 HTTP 接口，返回响应体。失败抛异常，由调用方决定是否致命。"""
    url = f"http://{settings.addr}/nacos{path}"
    body = urllib.parse.urlencode(params).encode()
    req = urllib.request.Request(url, data=body, method=method, headers=settings.headers())
    with urllib.request.urlopen(req, timeout=settings.timeout) as resp:
        return resp.read().decode("utf-8", "replace")


class NacosRegistrar:
    """把本进程注册为 Nacos 实例，并在后台维持心跳。

    Nacos 对 ephemeral 实例的判定是「15s 无心跳即摘除」，故必须持续发心跳；
    心跳失败即视为掉线，下一轮重新注册（网络抖动后自愈）。
    """

    def __init__(self, settings: NacosSettings):
        self.settings = settings
        self.ip = settings.detect_ip()
        self._stop = threading.Event()
        self._thread = None
        self._registered = False

    # ── 单次操作 ────────────────────────────────────────────────
    def register(self) -> bool:
        params = {
            "serviceName": self.settings.service_name,
            "ip": self.ip,
            "port": self.settings.service_port,
            "groupName": self.settings.group,
            "namespaceId": self.settings.namespace,
            "weight": "1",
            "enabled": "true",
            "healthy": "true",
            "ephemeral": "true",
            "metadata": json.dumps({"lang": "python", "module": "ai"}),
        }
        return _request(self.settings, "/v1/ns/instance", params, "POST").strip().lower() == "ok"

    def heartbeat(self) -> bool:
        beat = {
            "serviceName": self.settings.service_name,
            "ip": self.ip,
            "port": self.settings.service_port,
            "weight": 1,
            "ephemeral": True,
        }
        params = {
            "serviceName": self.settings.service_name,
            "groupName": self.settings.group,
            "namespaceId": self.settings.namespace,
            "beat": json.dumps(beat),
        }
        return '"code":200' in _request(self.settings, "/v1/ns/instance/beat", params, "PUT").replace(" ", "")

    def deregister(self) -> bool:
        params = {
            "serviceName": self.settings.service_name,
            "ip": self.ip,
            "port": self.settings.service_port,
            "groupName": self.settings.group,
            "namespaceId": self.settings.namespace,
            "ephemeral": "true",
        }
        return _request(self.settings, "/v1/ns/instance", params, "DELETE").strip().lower() == "ok"

    # ── 生命周期 ────────────────────────────────────────────────
    def _loop(self):
        backoff = 1.0
        while not self._stop.is_set():
            if not self._registered:
                try:
                    self._registered = self.register()
                    if self._registered:
                        _log(f"[Nacos] 已注册 {self.settings.service_name} → "
                             f"{self.ip}:{self.settings.service_port} "
                             f"(ns={self.settings.namespace}, group={self.settings.group})")
                        backoff = 1.0
                except Exception as e:  # noqa: BLE001
                    self._registered = False
                    _log(f"[Nacos] 注册失败（{self.settings.addr} 不可达？），{backoff:.0f}s 后重试: {e}")
                    self._stop.wait(backoff)
                    backoff = min(backoff * 2, 30.0)
                    continue
            else:
                try:
                    if not self.heartbeat():
                        self._registered = False
                        _log("[Nacos] 心跳被拒，转入重新注册")
                except Exception as e:  # noqa: BLE001
                    self._registered = False
                    _log(f"[Nacos] 心跳失败，转入重新注册: {e}")
            self._stop.wait(self.settings.heartbeat_interval)

    def start(self):
        if not self.settings.enabled:
            _log("[Nacos] NACOS_ENABLE=0，跳过服务注册")
            return
        self._thread = threading.Thread(target=self._loop, name="nacos-heartbeat", daemon=True)
        self._thread.start()

    def stop(self):
        self._stop.set()
        if self._thread is not None:
            self._thread.join(timeout=self.settings.timeout + 1)
        if self._registered:
            try:
                self.deregister()
                _log("[Nacos] 已注销实例")
            except Exception as e:  # noqa: BLE001
                _log(f"[Nacos] 注销失败（实例将随心跳超时自动摘除）: {e}")
        self._registered = False


def apply_config_to_env(data_id: str = CONFIG_DATA_ID) -> bool:
    """从 Nacos 配置中心拉取 YAML 并写入 os.environ，返回是否成功应用。

    文件形如 ``env: {KEY: value}``，键名与环境变量一一对应，避免第二套命名。
    Nacos 的值会覆盖已有环境变量（配置中心优先，与 Java 服务 spring.config.import 的语义一致）。
    必须在 school_agent.config 被导入之前调用——config.py 是导入期读环境变量的。
    """
    settings = NacosSettings()
    if not settings.enabled:
        return False
    params = {
        "dataId": data_id,
        "group": settings.group,
        "tenant": settings.namespace,
    }
    try:
        content = _request(settings, "/v1/cs/configs?" + urllib.parse.urlencode(params), {}, "GET")
    except urllib.error.HTTPError as e:
        if e.code == 404:
            _log(f"[Nacos] 配置中心无 {data_id}（未推送？），沿用环境变量")
        else:
            _log(f"[Nacos] 拉取配置失败 HTTP {e.code}，沿用环境变量")
        return False
    except Exception as e:  # noqa: BLE001
        _log(f"[Nacos] 拉取配置失败（{settings.addr} 不可达？），沿用环境变量: {e}")
        return False

    if not content.strip():
        return False
    try:
        import yaml
    except ImportError:
        _log("[Nacos] 未安装 PyYAML，跳过配置中心下发（建议 pip install pyyaml）")
        return False
    try:
        doc = yaml.safe_load(content) or {}
    except Exception as e:  # noqa: BLE001
        _log(f"[Nacos] {data_id} 解析失败，沿用环境变量: {e}")
        return False

    env_map = doc.get("env") or {}
    if not isinstance(env_map, dict):
        _log(f"[Nacos] {data_id} 的 env 段不是映射，忽略")
        return False
    for key, value in env_map.items():
        os.environ[str(key)] = "" if value is None else str(value)
    if env_map:
        _log(f"[Nacos] 已应用配置中心 {data_id}: {', '.join(sorted(str(k) for k in env_map))}")
    return True


def start() -> NacosRegistrar:
    """供 FastAPI lifespan 调用：启动后台注册/心跳线程。"""
    registrar = NacosRegistrar(NacosSettings())
    registrar.start()
    return registrar
