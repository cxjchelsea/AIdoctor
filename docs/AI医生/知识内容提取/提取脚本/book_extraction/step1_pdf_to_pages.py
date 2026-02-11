#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Step 1: PDF -> page_no + text
- Preserve line breaks as much as possible
- Output JSONL (default) or JSON

Dependencies:
  Prefer: PyMuPDF (pip install pymupdf)
  Fallback: pdfplumber (pip install pdfplumber)

Usage examples:
  python step1_pdf_to_pages.py --pdf "贝茨体格检查和病史采集指南-21-26.pdf" --out pages.jsonl
  python step1_pdf_to_pages.py --pdf input.pdf --out pages.json --format json
  python step1_pdf_to_pages.py --pdf input.pdf --out pages.jsonl --min-chars 30 --strip-trailing
"""

import argparse
import json
import os
import re
import sys
import warnings
from typing import Dict, List, Optional, Tuple

# 抑制PyMuPDF的FontBBox警告（不影响文本提取功能）
warnings.filterwarnings("ignore", message=".*FontBBox.*")


def normalize_text(
    text: str,
    strip_trailing: bool = False,
    collapse_blank_lines: bool = False,
) -> str:
    """
    Keep newlines, but optionally clean some noise.
    """
    if text is None:
        return ""

    # Normalize line endings
    text = text.replace("\r\n", "\n").replace("\r", "\n")

    # Remove excessive spaces at line ends (optional)
    if strip_trailing:
        text = "\n".join([ln.rstrip() for ln in text.split("\n")])

    # Collapse too many blank lines (optional)
    if collapse_blank_lines:
        text = re.sub(r"\n{3,}", "\n\n", text)

    return text


def extract_with_pymupdf(pdf_path: str) -> List[Dict]:
    import fitz  # PyMuPDF
    import contextlib
    import io

    # 抑制PyMuPDF的FontBBox警告（这些警告不影响文本提取）
    # 通过重定向stderr来抑制警告输出
    stderr_buffer = io.StringIO()
    with contextlib.redirect_stderr(stderr_buffer):
        doc = fitz.open(pdf_path)
        pages: List[Dict] = []
        for i in range(doc.page_count):
            page = doc.load_page(i)
            # "text" keeps line breaks in reading order reasonably well.
            txt = page.get_text("text")
            pages.append(
                {
                    "page_no": i + 1,  # 1-indexed
                    "text": txt or "",
                }
            )
        doc.close()
    return pages


def extract_with_pdfplumber(pdf_path: str) -> List[Dict]:
    import pdfplumber

    pages: List[Dict] = []
    with pdfplumber.open(pdf_path) as pdf:
        for i, page in enumerate(pdf.pages):
            # This keeps line breaks but can be noisier than PyMuPDF depending on PDF.
            txt = page.extract_text() or ""
            pages.append(
                {
                    "page_no": i + 1,
                    "text": txt,
                }
            )
    return pages


def extract_pages(pdf_path: str) -> Tuple[str, List[Dict]]:
    """
    Returns (engine_name, pages)
    """
    # Prefer PyMuPDF
    try:
        pages = extract_with_pymupdf(pdf_path)
        return "pymupdf", pages
    except Exception as e1:
        # Fallback to pdfplumber
        try:
            pages = extract_with_pdfplumber(pdf_path)
            return "pdfplumber", pages
        except Exception as e2:
            raise RuntimeError(
                "Failed to extract PDF text using both PyMuPDF and pdfplumber.\n"
                f"PyMuPDF error: {e1}\n"
                f"pdfplumber error: {e2}"
            )


def main():
    # 默认路径配置（写死）
    DEFAULT_PDF_PATH = r"E:\pycharmProject\AIdoctor\docs\AI医生\知识内容提取\源数据\贝茨体格检查和病史采集指南.pdf"
    DEFAULT_OUTPUT_DIR = r"E:\pycharmProject\AIdoctor\docs\AI医生\知识内容提取\提取结果"
    
    ap = argparse.ArgumentParser(description="Step 1: PDF -> per-page text (JSONL/JSON)")
    ap.add_argument("--pdf", default=DEFAULT_PDF_PATH, help="Path to input PDF (default: 贝茨体格检查和病史采集指南.pdf)")
    ap.add_argument("--out", default=None, help="Path to output file (.jsonl or .json). Default: auto-generated in output directory")
    ap.add_argument(
        "--format",
        choices=["jsonl", "json"],
        default=None,
        help="Output format. Default inferred from --out extension.",
    )
    ap.add_argument(
        "--min-chars",
        type=int,
        default=0,
        help="Filter out pages with text length < min_chars (after normalization). Default 0 keeps all.",
    )
    ap.add_argument(
        "--strip-trailing",
        action="store_true",
        help="Strip trailing spaces at each line end.",
    )
    ap.add_argument(
        "--collapse-blank-lines",
        action="store_true",
        help="Collapse 3+ blank lines into 2.",
    )
    ap.add_argument(
        "--include-meta",
        action="store_true",
        help="Include pdf_path, engine, extracted_at in each record (useful for provenance).",
    )

    args = ap.parse_args()

    pdf_path = args.pdf
    
    # 如果没有指定输出路径，自动生成
    if args.out is None:
        # 确保输出目录存在
        os.makedirs(DEFAULT_OUTPUT_DIR, exist_ok=True)
        # 根据PDF文件名生成输出文件名
        pdf_basename = os.path.splitext(os.path.basename(pdf_path))[0]
        out_path = os.path.join(DEFAULT_OUTPUT_DIR, f"{pdf_basename}_pages.jsonl")
    else:
        out_path = args.out

    if not os.path.exists(pdf_path):
        print(f"[ERROR] PDF not found: {pdf_path}", file=sys.stderr)
        sys.exit(1)

    fmt = args.format
    if fmt is None:
        ext = os.path.splitext(out_path.lower())[1]
        fmt = "json" if ext == ".json" else "jsonl"

    engine, pages = extract_pages(pdf_path)

    # Normalize + optional filtering
    normalized_pages: List[Dict] = []
    for p in pages:
        txt = normalize_text(
            p.get("text", ""),
            strip_trailing=args.strip_trailing,
            collapse_blank_lines=args.collapse_blank_lines,
        )
        if args.min_chars and len(txt.strip()) < args.min_chars:
            continue

        rec = {"page_no": p["page_no"], "text": txt}
        if args.include_meta:
            rec["pdf_path"] = os.path.abspath(pdf_path)
            rec["engine"] = engine
        normalized_pages.append(rec)

    # Write output
    os.makedirs(os.path.dirname(os.path.abspath(out_path)), exist_ok=True)

    if fmt == "jsonl":
        with open(out_path, "w", encoding="utf-8") as f:
            for rec in normalized_pages:
                f.write(json.dumps(rec, ensure_ascii=False) + "\n")
    else:
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(normalized_pages, f, ensure_ascii=False, indent=2)

    print(f"[OK] Extracted pages: {len(normalized_pages)}")
    print(f"[OK] Engine: {engine}")
    print(f"[OK] Output: {out_path}")


if __name__ == "__main__":
    main()
