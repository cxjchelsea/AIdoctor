"""非生产 engineering-only Raw OCR Runtime 兼容委托。

分类：NON_PRODUCTION_ENGINEERING_OPT_IN_COMPOSITION

本模块不再自行构造 ToolRouter / DeterministicRuntimeExecutor / create_app。
完整 opt-in 委托给规范 controlled_composition。
禁止导入 RawOcrEngine、RawOcrToolAdapter、ocr-service、cv2、PIL、pytesseract。
禁止模块级 FastAPI 应用实例；调用方必须显式调用工厂。
默认 create_app() 行为不受本模块影响。
"""

from __future__ import annotations

from fastapi import FastAPI

from packages.python_runtime.ports import ArtifactPort, ContextToolPort

from .controlled_composition import (
    CONTROLLED_RAW_OCR_CAPABILITY_ID as ENGINEERING_RAW_OCR_CAPABILITY_ID,
)
from .controlled_composition import (
    CONTROLLED_RAW_OCR_CAPABILITY_VERSION as ENGINEERING_RAW_OCR_CAPABILITY_VERSION,
)
from .controlled_composition import (
    ControlledRawOcrComposition,
    create_controlled_raw_ocr_app,
)

__all__ = [
    "ENGINEERING_RAW_OCR_CAPABILITY_ID",
    "ENGINEERING_RAW_OCR_CAPABILITY_VERSION",
    "create_engineering_raw_ocr_app",
]


def create_engineering_raw_ocr_app(
    raw_ocr_tool_port: ContextToolPort,
    artifact_port: ArtifactPort,
) -> FastAPI:
    """
    兼容既有工程调用方：完整注册且授权 Raw OCR。

    实际组合发生在 create_controlled_raw_ocr_app(...)。
    """

    return create_controlled_raw_ocr_app(
        ControlledRawOcrComposition(
            register_raw_ocr=True,
            authorize_raw_ocr=True,
        ),
        raw_ocr_tool_port=raw_ocr_tool_port,
        artifact_port=artifact_port,
    )
