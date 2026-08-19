"""
遗留 OcrService 兼容性测试。

证明内部委托 RawOcrEngine 后，历史可观察行为保持不变。
不宣称临床参考范围、状态分类或患者解析的医学有效性。
"""

from __future__ import annotations

import asyncio
import io

import pytest
from PIL import Image

from app.services.ocr_service import OcrService
from app.services.raw_ocr import (
    RawOcrEngineUnavailableError,
    RawOcrExecutionFailedError,
)


# 工程合成文本：用于证明提取路径仍被调用，不含真实身份。
_SYNTHETIC_OCR_TEXT = "HELLO OCR 123"


class _InMemoryImageFile:
    """最小异步可读对象，模拟遗留 recognize() 所需的文件接口。"""

    def __init__(self, image_bytes: bytes) -> None:
        self._image_bytes = image_bytes

    async def read(self) -> bytes:
        return self._image_bytes


def _build_synthetic_png_bytes() -> bytes:
    """生成内存 PNG，不落盘。"""
    image = Image.new("RGB", (32, 16), color=(255, 255, 255))
    buffer = io.BytesIO()
    image.save(buffer, format="PNG")
    return buffer.getvalue()


def test_legacy_success_still_runs_structured_extraction(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    """Raw 成功返回文本时，遗留 recognize 仍走 extract_structured_data。"""
    ocr_service = OcrService()
    extraction_calls: list[str] = []
    original_extract = ocr_service.extract_structured_data

    async def tracked_extract(text: str):
        extraction_calls.append(text)
        return await original_extract(text)

    monkeypatch.setattr(ocr_service, "extract_structured_data", tracked_extract)
    monkeypatch.setattr(
        ocr_service._raw_ocr_engine,
        "recognize_raw_text",
        lambda image, language="chi_sim+eng": _SYNTHETIC_OCR_TEXT,
    )

    result = asyncio.run(ocr_service.recognize(_InMemoryImageFile(_build_synthetic_png_bytes())))
    assert result["raw_text"] == _SYNTHETIC_OCR_TEXT
    assert extraction_calls == [_SYNTHETIC_OCR_TEXT]
    assert "structured_data" in result
    assert result["structured_data"]["examination_type"] == "未知"


def test_legacy_engine_unavailable_still_returns_empty(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    """Raw 引擎不可用时，遗留 _ocr_recognize 吞异常并返回空结果，不向外抛新技术异常。"""
    ocr_service = OcrService()

    def fail_unavailable(image, language="chi_sim+eng"):
        raise RawOcrEngineUnavailableError("synthetic tesseract missing")

    monkeypatch.setattr(ocr_service._raw_ocr_engine, "recognize_raw_text", fail_unavailable)
    swallowed_text = ocr_service._ocr_recognize(Image.new("RGB", (8, 8), color=(255, 255, 255)))
    assert swallowed_text == ""

    result = asyncio.run(ocr_service.recognize(_InMemoryImageFile(_build_synthetic_png_bytes())))
    assert result == {"raw_text": "", "structured_data": {}}


def test_legacy_execution_failure_still_returns_empty(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    """Raw 执行失败同样被遗留路径折叠为空串。"""
    ocr_service = OcrService()

    def fail_execution(image, language="chi_sim+eng"):
        raise RawOcrExecutionFailedError("synthetic execution boom")

    monkeypatch.setattr(ocr_service._raw_ocr_engine, "recognize_raw_text", fail_execution)
    result = asyncio.run(ocr_service.recognize(_InMemoryImageFile(_build_synthetic_png_bytes())))
    assert result == {"raw_text": "", "structured_data": {}}


def test_legacy_clinical_extraction_surface_remains_callable() -> None:
    """临床提取函数仍可调用，可观察字段形状保持兼容。不宣称医学正确性。"""
    ocr_service = OcrService()
    structured_data = asyncio.run(ocr_service.extract_structured_data(_SYNTHETIC_OCR_TEXT))
    assert set(structured_data.keys()) == {
        "indicators",
        "examination_type",
        "examination_date",
        "patient_info",
    }
    assert structured_data["indicators"] == []
    assert structured_data["examination_type"] == "未知"
    assert structured_data["examination_date"] is None
    assert structured_data["patient_info"] is None
    assert ocr_service._determine_status(5.0, "4-10") == "normal"
    assert ocr_service._identify_report_type(_SYNTHETIC_OCR_TEXT) == "未知"
    assert ocr_service._parse_indicator_value(_SYNTHETIC_OCR_TEXT, "NONEXISTENT_INDICATOR") is None
