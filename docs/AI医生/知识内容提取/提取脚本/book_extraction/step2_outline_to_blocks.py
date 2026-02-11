#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Step2 v2: Use a curated outline (start_page only) to build section blocks
from Step1 page-level extraction.

Inputs:
  --pages   pages.jsonl (each line: {"page_no": int, "text": str})
  --outline outline.json (your curated structure with start_page per unit/chapter/section)

Outputs (in --outdir):
  1) outline_with_end.json  (same outline but with computed end_page for unit/chapter/section)
  2) section_blocks.jsonl   (one line per section block with merged text across pages)

Assumptions:
  - outline.book.page_type == "print_page" (your sample)
  - start_page are 1-indexed and refer to the same paging as Step1 extraction
  - sections have start_page; unit/chapter have start_page and contain children

Notes:
  - This script does NOT attempt to detect headings; it trusts outline start_page.
  - It also does basic sanity checks: overlaps, missing pages.
"""

import json
import os
from typing import Dict, List, Tuple


def load_pages_jsonl(path: str) -> Dict[int, str]:
    """Return {page_no: text}"""
    pages = {}
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            if not line.strip():
                continue
            obj = json.loads(line)
            page_no = int(obj["page_no"])
            pages[page_no] = obj.get("text", "")
    return pages


def load_outline(path: str) -> Dict:
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def get_max_page(pages_map: Dict[int, str]) -> int:
    return max(pages_map.keys()) if pages_map else 0


def compute_end_pages(outline: Dict, max_page: int) -> Dict:
    """
    Compute end_page for unit/chapter/section based on next sibling's start_page.
    Returns a deep-copied outline with end_page inserted.
    """
    book = outline.get("book", {})
    units = book.get("units", [])

    # Helper to set end_page for a list of nodes by looking at next start_page
    def set_end_for_nodes(nodes: List[Dict], parent_end: int) -> None:
        # nodes expected to have start_page
        for i, node in enumerate(nodes):
            start = int(node["start_page"])
            if i + 1 < len(nodes):
                next_start = int(nodes[i + 1]["start_page"])
                end = next_start - 1
            else:
                end = parent_end
            node["end_page"] = max(start, end)  # ensure non-negative span

    # First pass: set unit end_page by next unit start_page or max_page
    for i, u in enumerate(units):
        u_start = int(u["start_page"])
        if i + 1 < len(units):
            u_end = int(units[i + 1]["start_page"]) - 1
        else:
            u_end = max_page
        u["end_page"] = max(u_start, u_end)

        chapters = u.get("chapters", [])
        # Set chapter end_page within unit end
        set_end_for_nodes(chapters, u["end_page"])

        # For each chapter, set section end_page within chapter end
        for ch in chapters:
            sections = ch.get("sections", [])
            set_end_for_nodes(sections, ch["end_page"])

    return outline


def iter_sections(outline: Dict) -> List[Dict]:
    """Flatten outline to a list of section descriptors with unit/chapter metadata."""
    book = outline["book"]
    res = []
    for u in book.get("units", []):
        for ch in u.get("chapters", []):
            for sec in ch.get("sections", []):
                res.append(
                    {
                        "unit_id": u.get("unit_id"),
                        "unit_title_en": u.get("title_en", ""),
                        "unit_title_zh": u.get("title_zh", ""),
                        "chapter_id": ch.get("chapter_id"),
                        "chapter_title_en": ch.get("title_en", ""),
                        "chapter_title_zh": ch.get("title_zh", ""),
                        "section_title_en": sec.get("title_en", ""),
                        "section_title_zh": sec.get("title_zh", ""),
                        "start_page": int(sec["start_page"]),
                        "end_page": int(sec.get("end_page", sec["start_page"])),
                    }
                )
    # sort by start_page
    res.sort(key=lambda x: (x["start_page"], x["end_page"]))
    return res


def sanity_check_sections(sections: List[Dict], max_page: int) -> List[str]:
    """
    Return list of warnings (strings).
    Checks:
      - start_page <= end_page
      - start_page within [1, max_page]
      - overlaps between consecutive sections
    """
    warnings = []
    prev = None
    for s in sections:
        sp, ep = s["start_page"], s["end_page"]
        if sp < 1 or sp > max_page:
            warnings.append(f"Section start_page out of range: {s['section_title_en']} start={sp}")
        if ep < 1 or ep > max_page:
            warnings.append(f"Section end_page out of range: {s['section_title_en']} end={ep}")
        if sp > ep:
            warnings.append(f"Section has start_page > end_page: {s['section_title_en']} {sp}>{ep}")
        if prev:
            # Overlap check
            if sp <= prev["end_page"]:
                warnings.append(
                    f"Overlap: prev({prev['section_title_en']} {prev['start_page']}-{prev['end_page']}) "
                    f"and curr({s['section_title_en']} {sp}-{ep})"
                )
        prev = s
    return warnings


def build_section_block(pages_map: Dict[int, str], start_page: int, end_page: int) -> str:
    parts = []
    for p in range(start_page, end_page + 1):
        page_text = pages_map.get(p, "")  # missing page -> empty
        if page_text.strip():  # 只对非空页面添加页码标记
            parts.append(f"[页码: {p}]\n{page_text}")
        elif page_text:  # 空字符串但存在（保留空行）
            parts.append(f"[页码: {p}]\n")
    return "\n\n".join(parts).strip()


def main():
    # 写死的输入和输出路径
    pages_path = r"E:\pycharmProject\AIdoctor\docs\AI医生\知识内容提取\提取结果\贝茨\贝茨体格检查和病史采集指南_pages.jsonl"
    outline_path = r"E:\pycharmProject\AIdoctor\docs\AI医生\知识内容提取\提取结果\贝茨\outline_health_history_only.json"
    outdir = r"E:\pycharmProject\AIdoctor\docs\AI医生\知识内容提取\提取结果\贝茨"
    
    # 是否输出空块（默认False，跳过空块）
    emit_empty = False

    os.makedirs(outdir, exist_ok=True)

    pages_map = load_pages_jsonl(pages_path)
    max_page = get_max_page(pages_map)
    outline = load_outline(outline_path)

    # 1) compute end_page everywhere
    outline = compute_end_pages(outline, max_page=max_page)

    # 2) flatten sections + sanity checks
    sections = iter_sections(outline)
    warnings = sanity_check_sections(sections, max_page=max_page)

    # 3) write outline_with_end.json
    outline_out = os.path.join(outdir, "outline_with_end.json")
    with open(outline_out, "w", encoding="utf-8") as f:
        json.dump(outline, f, ensure_ascii=False, indent=2)

    # 4) write warnings (if any)
    warn_out = os.path.join(outdir, "step2_warnings.txt")
    with open(warn_out, "w", encoding="utf-8") as f:
        for w in warnings:
            f.write(w + "\n")

    # 5) write section_blocks.jsonl
    blocks_out = os.path.join(outdir, "section_blocks.jsonl")
    kept = 0
    skipped_empty = 0

    with open(blocks_out, "w", encoding="utf-8") as f:
        for s in sections:
            text = build_section_block(pages_map, s["start_page"], s["end_page"])
            if not text and not emit_empty:
                skipped_empty += 1
                continue

            rec = {
                "unit_id": s["unit_id"],
                "unit_title_en": s["unit_title_en"],
                "unit_title_zh": s["unit_title_zh"],
                "chapter_id": s["chapter_id"],
                "chapter_title_en": s["chapter_title_en"],
                "chapter_title_zh": s["chapter_title_zh"],
                "section_title_en": s["section_title_en"],
                "section_title_zh": s["section_title_zh"],
                "start_page": s["start_page"],
                "end_page": s["end_page"],
                "text": text,
            }
            f.write(json.dumps(rec, ensure_ascii=False) + "\n")
            kept += 1

    print("[OK] max_page:", max_page)
    print("[OK] outline_with_end:", outline_out)
    print("[OK] section_blocks:", blocks_out)
    print("[OK] warnings:", warn_out, f"(count={len(warnings)})")
    print("[OK] blocks kept:", kept, "| empty skipped:", skipped_empty)
    if warnings:
        print("Tip: open step2_warnings.txt and fix outline start_page overlaps/gaps if needed.")


if __name__ == "__main__":
    main()
