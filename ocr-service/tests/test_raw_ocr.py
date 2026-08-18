"""
RAW OCR 工程边界单测。

不安装、不调用真实 Tesseract 二进制；仅对 pytesseract.image_to_string 做受控替换。
夹具仅为内存合成图与工程文本，不含医学名词、患者身份或真实 PHI。
"""

from __future__ import annotations

import importlib
import inspect
import io
import sys

import pytest
from PIL import Image
from pytesseract.pytesseract import TesseractNotFoundError

from app.services.raw_ocr import (
    DEFAULT_OCR_LANGUAGE,
    RawOcrEngine,
    RawOcrEngineUnavailableError,
    RawOcrExecutionFailedError,
)


# 合成夹具仅用于工程验证，禁止医疗/身份语义。
_SYNTHETIC_OCR_TEXT = "HELLO OCR 123"

# raw 边界不得引用的临床/结构化提取符号。
_FORBIDDEN_CLINICAL_SYMBOLS = (
    "extract_structured_data",
    "_determine_status",
    "_identify_report_type",
    "_parse_indicator_value",
    "OcrService",
    "patient_info",
    "normal_range",
)

# raw 边界不得导入的跨包依赖。
_FORBIDDEN_IMPORT_PREFIXES = (
    "packages.python_runtime",
    "packages.model_runtime",
    "aidoctor_shared_contracts",
    "capabilities",
    "common.aidoctor_llm",
    "langgraph",
)


def _build_synthetic_rgb_image() -> Image.Image:
    """构造内存中的小幅 RGB 图，不落盘、不含文字语义。"""
    return Image.new("RGB", (32, 16), color=(255, 255, 255))


def test_preprocess_image_accepts_synthetic_pil_image() -> None:
    """预处理接受合成 PIL 图并返回 PIL.Image。"""
    engine = RawOcrEngine()
    processed_image = engine.preprocess_image(_build_synthetic_rgb_image())
    assert isinstance(processed_image, Image.Image)


def test_successful_engine_call_returns_raw_text(monkeypatch: pytest.MonkeyPatch) -> None:
    """引擎成功时返回原始文本。"""
    recorded_language: list[str] = []

    def fake_image_to_string(image: Image.Image, lang: str = "") -> str:
        recorded_language.append(lang)
        return _SYNTHETIC_OCR_TEXT

    monkeypatch.setattr("app.services.raw_ocr.pytesseract.image_to_string", fake_image_to_string)
    engine = RawOcrEngine()
    raw_text = engine.recognize_raw_text(_build_synthetic_rgb_image())
    assert raw_text == _SYNTHETIC_OCR_TEXT
    assert recorded_language == [DEFAULT_OCR_LANGUAGE]


def test_successful_empty_text_is_not_technical_failure(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    """成功识别但无字时返回空串，不得抛出技术异常。"""

    def fake_image_to_string(image: Image.Image, lang: str = "") -> str:
        return ""

    monkeypatch.setattr("app.services.raw_ocr.pytesseract.image_to_string", fake_image_to_string)
    engine = RawOcrEngine()
    raw_text = engine.recognize_raw_text(_build_synthetic_rgb_image())
    assert raw_text == ""


def test_tesseract_not_found_is_engine_unavailable(monkeypatch: pytest.MonkeyPatch) -> None:
    """Tesseract 未找到必须映射为引擎不可用，不得坍缩为空串。"""

    def fake_image_to_string(image: Image.Image, lang: str = "") -> str:
        raise TesseractNotFoundError()

    monkeypatch.setattr("app.services.raw_ocr.pytesseract.image_to_string", fake_image_to_string)
    engine = RawOcrEngine()
    with pytest.raises(RawOcrEngineUnavailableError):
        engine.recognize_raw_text(_build_synthetic_rgb_image())


def test_unexpected_engine_exception_is_execution_failure(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    """非“引擎不可用”的异常必须映射为执行失败。"""

    def fake_image_to_string(image: Image.Image, lang: str = "") -> str:
        raise RuntimeError("synthetic engine boom")

    monkeypatch.setattr("app.services.raw_ocr.pytesseract.image_to_string", fake_image_to_string)
    engine = RawOcrEngine()
    with pytest.raises(RawOcrExecutionFailedError):
        engine.recognize_raw_text(_build_synthetic_rgb_image())


def test_language_argument_is_deterministic(monkeypatch: pytest.MonkeyPatch) -> None:
    """语言参数默认 chi_sim+eng，显式传入时保持调用方给定值。"""
    recorded_language: list[str] = []

    def fake_image_to_string(image: Image.Image, lang: str = "") -> str:
        recorded_language.append(lang)
        return _SYNTHETIC_OCR_TEXT

    monkeypatch.setattr("app.services.raw_ocr.pytesseract.image_to_string", fake_image_to_string)
    engine = RawOcrEngine()
    engine.recognize_raw_text(_build_synthetic_rgb_image())
    engine.recognize_raw_text(_build_synthetic_rgb_image(), language="eng")
    assert recorded_language == [DEFAULT_OCR_LANGUAGE, "eng"]
    assert DEFAULT_OCR_LANGUAGE == "chi_sim+eng"


def test_raw_ocr_does_not_import_or_call_clinical_extraction() -> None:
    """raw_ocr 不得导入 OcrService，也不得包含临床提取符号。"""
    sys.modules.pop("app.services.ocr_service", None)
    raw_ocr_module = importlib.import_module("app.services.raw_ocr")
    importlib.reload(raw_ocr_module)
    assert "app.services.ocr_service" not in sys.modules

    module_source = inspect.getsource(raw_ocr_module)
    for forbidden_symbol in _FORBIDDEN_CLINICAL_SYMBOLS:
        assert forbidden_symbol not in module_source

    loaded_module_names = tuple(sys.modules)
    for forbidden_prefix in _FORBIDDEN_IMPORT_PREFIXES:
        assert not any(
            module_name == forbidden_prefix or module_name.startswith(forbidden_prefix + ".")
            for module_name in loaded_module_names
        )


def test_recognize_from_bytes_uses_engine_path(monkeypatch: pytest.MonkeyPatch) -> None:
    """bytes 入口只做解码后走同一识别路径，不依赖 FastAPI UploadFile。"""

    def fake_image_to_string(image: Image.Image, lang: str = "") -> str:
        return _SYNTHETIC_OCR_TEXT

    monkeypatch.setattr("app.services.raw_ocr.pytesseract.image_to_string", fake_image_to_string)
    image_buffer = io.BytesIO()
    _build_synthetic_rgb_image().save(image_buffer, format="PNG")
    engine = RawOcrEngine()
    raw_text = engine.recognize_from_bytes(image_buffer.getvalue())
    assert raw_text == _SYNTHETIC_OCR_TEXT
