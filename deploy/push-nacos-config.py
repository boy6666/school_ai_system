#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
把 deploy/nacos-config/*.yaml 推送到 Nacos 配置中心。

职责：
  1. 创建本地单环境 namespace（已存在则忽略）：edu-agent-local
     —— 用 customNamespaceId 固定 ID，使各服务 yml 里的
        spring.cloud.nacos.*.namespace=edu-agent-local 能直接命中。
  2. 把每个 data-id 推到该 namespace。
     —— group 规则：edu-agent-resource.yaml -> resource-group，其余 -> edu-agent

前置：Nacos 已启动（默认 8848），且【关闭鉴权】NACOS_AUTH_ENABLE=false（standalone 默认即关闭）。
      若开启了鉴权，请先在脚本里补 accessToken 逻辑，或临时关闭。

用法：
  python3 deploy/push-nacos-config.py                 # 用默认 127.0.0.1:8848
  NACOS_ADDR=192.168.1.10:8848 python3 deploy/push-nacos-config.py
  NACOS_USERNAME=nacos NACOS_PASSWORD=nacos python3 deploy/push-nacos-config.py
"""
import os
import sys
import base64
import urllib.parse
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
CONFIG_DIR = os.path.join(HERE, "nacos-config")

NACOS_ADDR = os.environ.get("NACOS_ADDR", "127.0.0.1:8848")
NACOS_USERNAME = os.environ.get("NACOS_USERNAME", "nacos")
NACOS_PASSWORD = os.environ.get("NACOS_PASSWORD", "nacos")

NAMESPACES = ["edu-agent-local"]
# 普通服务组 & 资源服务组
GROUP_DEFAULT = "edu-agent"
GROUP_RESOURCE = "resource-group"

# data-id 文件名 -> group
def group_of(data_id: str) -> str:
    return GROUP_RESOURCE if data_id == "edu-agent-resource.yaml" else GROUP_DEFAULT


def _basic_auth_header() -> dict:
    token = base64.b64encode(f"{NACOS_USERNAME}:{NACOS_PASSWORD}".encode()).decode()
    return {"Authorization": f"Basic {token}"}


def _post(path: str, params: dict) -> tuple[int, str]:
    url = f"http://{NACOS_ADDR}/nacos{path}"
    data = urllib.parse.urlencode(params).encode()
    req = urllib.request.Request(url, data=data, method="POST",
                                 headers={**_basic_auth_header(),
                                          "Content-Type": "application/x-www-form-urlencoded"})
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            return r.status, r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")
    except Exception as e:  # noqa: BLE001
        return -1, str(e)


def ensure_namespace(ns_id: str) -> None:
    # customNamespaceId 让 namespace ID 固定为 ns_id，便于 yml 直接引用
    st, body = _post("/v1/console/ns/namespace", {
        "customNamespaceId": ns_id,
        "namespaceName": ns_id,
        "namespaceDesc": "EduAgent " + ns_id,
    })
    # 已存在时 Nacos 返回 "namespace already exist!" 之类，视为成功
    if st == 200 and ("true" in body.lower() or "exist" in body.lower()):
        print(f"  namespace {ns_id}: OK ({body.strip()})")
    else:
        print(f"  namespace {ns_id}: status={st} {body.strip()}  (若已存在可忽略)")


def publish(data_id: str, group: str, namespace_id: str, content: str) -> None:
    st, body = _post("/v1/cs/configs", {
        "dataId": data_id,
        "group": group,
        "tenant": namespace_id,
        "content": content,
        "type": "yaml",
    })
    if st == 200 and "true" in body.lower():
        print(f"  [OK] {data_id} -> ns={namespace_id} group={group}")
    else:
        print(f"  [FAIL] {data_id} -> ns={namespace_id} group={group} status={st} {body.strip()}")


def main() -> int:
    if not os.path.isdir(CONFIG_DIR):
        print(f"未找到配置目录: {CONFIG_DIR}", file=sys.stderr)
        return 1

    files = sorted(f for f in os.listdir(CONFIG_DIR) if f.endswith(".yaml"))
    if not files:
        print("nacos-config 目录下没有 .yaml 文件", file=sys.stderr)
        return 1

    print(f"==> Nacos 地址: {NACOS_ADDR}")
    print("==> 创建 namespace ...")
    for ns in NAMESPACES:
        ensure_namespace(ns)

    print("==> 推送 data-id ...")
    for ns in NAMESPACES:
        for fname in files:
            gid = group_of(fname)
            with open(os.path.join(CONFIG_DIR, fname), encoding="utf-8") as fh:
                publish(fname, gid, ns, fh.read())

    print("==> 完成。服务启动后会从对应 namespace 拉取配置。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
