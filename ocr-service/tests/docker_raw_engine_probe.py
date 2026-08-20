"""
OCR Docker 镜像内 RawOcrEngine 技术验证探针。

独立脚本：不依赖 pytest。权威执行方式：

    python tests/docker_raw_engine_probe.py

成功退出 0；任一要求失败退出非 0。禁止 skip / mock / 覆盖 language。
夹具仅为内存合成工程文本，不含医学名词、患者身份或真实 PHI。
"""

from __future__ import annotations

import io
import shutil
import subprocess
import sys

from PIL import Image, ImageDraw, ImageFont

from app.services.raw_ocr import DEFAULT_OCR_LANGUAGE, RawOcrEngine

REQUIRED_TESSERACT_LANGUAGES = ("eng", "chi_sim")
SYNTHETIC_ENGINEERING_TEXT = "HELLO OCR 123"
REQUIRED_TOKENS = ("HELLO", "OCR")


def _fail(message: str) -> None:
    print(f"OCR_DOCKER_RAW_ENGINE_PROBE=FAIL reason={message}", flush=True)
    raise SystemExit(1)


def _list_tesseract_languages() -> set[str]:
    """读取 tesseract --list-langs，兼容 stdout/stderr 历史差异。"""
    completed_process = subprocess.run(
        ["tesseract", "--list-langs"],
        check=False,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        timeout=30,
    )
    if completed_process.returncode != 0:
        _fail(
            "tesseract --list-langs failed "
            f"rc={completed_process.returncode}"
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


def _require_real_tesseract() -> str:
    """镜像内必须存在真实 Tesseract 二进制与 eng/chi_sim traineddata。"""
    tesseract_binary = shutil.which("tesseract")
    if tesseract_binary is None:
        _fail("tesseract binary not found via shutil.which")

    available_languages = _list_tesseract_languages()
    missing_languages = [
        language_name
        for language_name in REQUIRED_TESSERACT_LANGUAGES
        if language_name not in available_languages
    ]
    if missing_languages:
        _fail("missing traineddata: " + ", ".join(missing_languages))

    print(f"DOCKER_RAW_OCR_TESSERACT_BINARY={tesseract_binary}", flush=True)
    print("DOCKER_RAW_OCR_LANG_ENG=YES", flush=True)
    print("DOCKER_RAW_OCR_LANG_CHI_SIM=YES", flush=True)
    return tesseract_binary


def _render_synthetic_hello_ocr_png_bytes() -> bytes:
    """
    用镜像已有 Pillow 在内存渲染高对比度工程图。
    不落盘、不下载字体、不含医学或患者内容。
    """
    canvas_width = 1600
    canvas_height = 400
    canvas = Image.new("RGB", (canvas_width, canvas_height), color=(255, 255, 255))
    drawer = ImageDraw.Draw(canvas)
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


def main() -> int:
    _require_real_tesseract()

    if DEFAULT_OCR_LANGUAGE != "chi_sim+eng":
        _fail(f"DEFAULT_OCR_LANGUAGE={DEFAULT_OCR_LANGUAGE!r}")

    print(
        f"DOCKER_RAW_OCR_DEFAULT_LANGUAGE={DEFAULT_OCR_LANGUAGE}",
        flush=True,
    )

    image_bytes = _render_synthetic_hello_ocr_png_bytes()
    engine = RawOcrEngine()
    # 不覆盖 language，执行当前默认 chi_sim+eng。
    raw_text = engine.recognize_from_bytes(image_bytes)
    normalized_text = " ".join(raw_text.upper().split())

    for token in REQUIRED_TOKENS:
        if token not in normalized_text.split():
            _fail(
                f"missing token {token!r} in normalized={normalized_text!r}"
            )
        print(f"DOCKER_RAW_OCR_TOKEN_{token}=YES", flush=True)

    print("OCR_DOCKER_RAW_ENGINE_PROBE=PASS", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
