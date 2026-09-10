"""Canonical Runtime controlled Raw OCR composition.

分类：NON_PRODUCTION_ENGINEERING_CONTROLLED_COMPOSITION

本模块只做 server-side typed 组合：注册与授权彼此独立，
最终始终调用既有 create_app()。不实现第二套 HTTP Runtime。
禁止导入 OCR 实现、图像库、遗留服务、provider 或临床能力。
禁止模块级 FastAPI 应用、网络绑定、环境变量启用开关、
文件系统打开或 artifact payload 读取。
"""

from __future__ import annotations

from dataclasses import dataclass

from fastapi import FastAPI

from packages.python_runtime.executor import DeterministicRuntimeExecutor
from packages.python_runtime.http.app import create_app
from packages.python_runtime.ports import ArtifactPort, ContextToolPort
from packages.python_runtime.tool_router import ToolRouter

CONTROLLED_RAW_OCR_CAPABILITY_ID = "engineering.ocr.raw"
CONTROLLED_RAW_OCR_CAPABILITY_VERSION = "0.0.1"


class ControlledCompositionError(Exception):
    """非法 server-side 组合；不得回退默认 executor 或静默关闭注册。"""


@dataclass(frozen=True)
class ControlledRawOcrComposition:
    """显式、不可变的注册/授权决策。不是字符串 profile，也不是请求策略。"""

    register_raw_ocr: bool = False
    authorize_raw_ocr: bool = False


def create_controlled_raw_ocr_app(
    composition: ControlledRawOcrComposition,
    raw_ocr_tool_port: ContextToolPort | None = None,
    artifact_port: ArtifactPort | None = None,
) -> FastAPI:
    """按 typed 组合构造规范 Runtime；HTTP 门面必须来自 create_app()。"""

    if composition.register_raw_ocr and (
        raw_ocr_tool_port is None or artifact_port is None
    ):
        raise ControlledCompositionError(
            "register_raw_ocr requires an injected ContextToolPort and ArtifactPort"
        )

    router = ToolRouter()
    if composition.register_raw_ocr:
        assert raw_ocr_tool_port is not None
        router.register_context_tool(
            CONTROLLED_RAW_OCR_CAPABILITY_ID,
            CONTROLLED_RAW_OCR_CAPABILITY_VERSION,
            raw_ocr_tool_port,
            require_artifact=True,
            artifact_only=True,
        )

    authorized_capability_ids = (
        frozenset({CONTROLLED_RAW_OCR_CAPABILITY_ID})
        if composition.authorize_raw_ocr
        else frozenset()
    )
    executor = DeterministicRuntimeExecutor(
        tool_router=router,
        artifact_port=artifact_port,
    )
    return create_app(
        runtime_executor=executor,
        authorized_capability_ids=authorized_capability_ids,
    )
