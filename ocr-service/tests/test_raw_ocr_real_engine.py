"""
真实 Tesseract 二进制技术验证。

本文件不 mock pytesseract.image_to_string，也不 mock 二进制发现或 subprocess。
夹具仅为内存合成工程文本，不含医学名词、患者身份或真实 PHI。
默认识别路径必须使用现有 DEFAULT_OCR_LANGUAGE（chi_sim+eng），不得覆盖 language。
"""

from __future__ import annotations

import io
import os
import shutil
import subprocess

import pytest
from PIL import Image, ImageDraw, ImageFont

from app.services.raw_ocr import DEFAULT_OCR_LANGUAGE, RawOcrEngine

# 仅权威 CI 置 1；本地缺二进制时允许 skip，CI 缺二进制必须失败。
REQUIRE_REAL_TESSERACT = os.environ.get("AIDOCTOR_REQUIRE_REAL_TESSERACT") == "1"
REQUIRED_TESSERACT_LANGUAGES = ("eng", "chi_sim")
SYNTHETIC_ENGINEERING_TEXT = "HELLO OCR 123"


def _list_tesseract_languages() -> set[str]:
    """读取 tesseract --list-langs，兼容 stdout/stderr 历史差异。"""
    completed_process = subprocess.run(
        ["tesseract", "--list-langs"],
        check=False,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    combined_output = "\n".join(
        output_part
        for output_part in (completed_process.stdout, completed_process.stderr)
        if output_part
    )
    discovered_languages: set[str] = set()
    for raw_line in combined_output.splitlines():
        language_name = raw_line.strip()
        if not language_name or language_name.lower().startswith("list of"):
            continue
        discovered_languages.add(language_name)
    return discovered_languages


def _require_real_tesseract_or_skip() -> None:
    """
    强制环境：缺二进制或缺 traineddata 必须失败。
    非强制本地环境：允许 skip，且不得据此宣称引擎已验证。
    """
    tesseract_binary = shutil.which("tesseract")
    if tesseract_binary is None:
        if REQUIRE_REAL_TESSERACT:
            pytest.fail(
                "AIDOCTOR_REQUIRE_REAL_TESSERACT=1 但未找到 tesseract 二进制"
            )
        pytest.skip("本地未安装 tesseract 二进制；非强制环境允许跳过真实引擎测试")

    available_languages = _list_tesseract_languages()
    missing_languages = [
        language_name
        for language_name in REQUIRED_TESSERACT_LANGUAGES
        if language_name not in available_languages
    ]
    if missing_languages:
        missing_text = ", ".join(missing_languages)
        if REQUIRE_REAL_TESSERACT:
            pytest.fail(
                f"AIDOCTOR_REQUIRE_REAL_TESSERACT=1 但缺少 traineddata: {missing_text}"
            )
        pytest.skip(
            f"本地缺少 Tesseract traineddata: {missing_text}；非强制环境允许跳过"
        )


def _render_synthetic_hello_ocr_png_bytes() -> bytes:
    """
    用仓库已有 Pillow 在内存渲染高对比度工程图。
    不落盘、不下载字体、不含医学或患者内容。
    """
    canvas_width = 1600
    canvas_height = 400
    canvas = Image.new("RGB", (canvas_width, canvas_height), color=(255, 255, 255))
    drawer = ImageDraw.Draw(canvas)
    # Pillow 10.1.0 支持 load_default(size=)，使用内置可缩放默认字体。
    text_font = ImageFont.load_default(size=96)
    text_bbox = drawer.textbbox((0, 0), SYNTHETIC_ENGINEERING_TEXT, font=text_font)
    text_width = text_bbox[2] - text_bbox[0]
    text_height = text_bbox[3] - text_bbox[1]
    text_left = max((canvas_width - text_width) // 2, 40)
    text_top = max((canvas_height - text_height) // 2, 40)
    drawer.text(
        (text_left, text_top),
        SYNTHETIC_ENGINEERING_TEXT,
        fill=(0, 0, 0),
        font=text_font,
    )
    png_buffer = io.BytesIO()
    canvas.save(png_buffer, format="PNG")
    return png_buffer.getvalue()


def test_real_tesseract_default_chi_sim_eng_recognizes_synthetic_tokens() -> None:
    """默认 chi_sim+eng 路径必须实际调用 Tesseract 二进制并识别稳定工程 token。"""
    _require_real_tesseract_or_skip()

    # 生产常量不得被本批修改；主证明必须走该默认值。
    assert DEFAULT_OCR_LANGUAGE == "chi_sim+eng"

    image_bytes = _render_synthetic_hello_ocr_png_bytes()
    engine = RawOcrEngine()
    # 不覆盖 language，执行当前默认 chi_sim+eng。
    raw_text = engine.recognize_from_bytes(image_bytes)

    normalized_text = " ".join(raw_text.upper().split())
    assert "HELLO" in normalized_text
    assert "OCR" in normalized_text
