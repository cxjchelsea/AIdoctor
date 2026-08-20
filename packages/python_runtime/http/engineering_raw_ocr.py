"""非生产 engineering-only Raw OCR Runtime 显式组合工厂。

分类：NON_PRODUCTION_ENGINEERING_OPT_IN_COMPOSITION

本模块只绑定工程能力身份与 Runtime 抽象接线。
禁止导入 RawOcrEngine、RawOcrToolAdapter、ocr-service、cv2、PIL、pytesseract。
禁止模块级 FastAPI 应用实例；调用方必须显式调用工厂。
默认 create_app() 行为不受本模块影响。
"""

from __future__ import annotations

from fastapi import FastAPI

from packages.python_runtime.executor import DeterministicRuntimeExecutor
from packages.python_runtime.http.app import create_app
from packages.python_runtime.ports import ArtifactPort, ContextToolPort
from packages.python_runtime.tool_router import ToolRouter

# 工程 opt-in 能力身份；不得出现在默认 create_app() 授权面。
ENGINEERING_RAW_OCR_CAPABILITY_ID = "engineering.ocr.raw"
ENGINEERING_RAW_OCR_CAPABILITY_VERSION = "0.0.1"


def create_engineering_raw_ocr_app(
    raw_ocr_tool_port: ContextToolPort,
    artifact_port: ArtifactPort,
) -> FastAPI:
    """
    显式构造仅授权 engineering.ocr.raw 的工程 Runtime HTTP 应用。

    调用方负责注入已构造的 ContextToolPort 与 ArtifactPort。
    本工厂不加载 OCR 实现，也不创建模块级应用对象。
    """

    router = ToolRouter()
    router.register_context_tool(
        ENGINEERING_RAW_OCR_CAPABILITY_ID,
        ENGINEERING_RAW_OCR_CAPABILITY_VERSION,
        raw_ocr_tool_port,
        require_artifact=True,
        artifact_only=True,
    )
    executor = DeterministicRuntimeExecutor(
        tool_router=router,
        artifact_port=artifact_port,
    )
    return create_app(
        runtime_executor=executor,
        authorized_capability_ids=frozenset({ENGINEERING_RAW_OCR_CAPABILITY_ID}),
    )
