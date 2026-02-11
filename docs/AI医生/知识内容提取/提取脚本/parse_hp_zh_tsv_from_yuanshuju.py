#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
解析源数据目录下的HPO中文翻译TSV文件
支持 hp-zh.babelon.tsv、hp-zh.synonyms.tsv 格式

用法：
  python parse_hp_zh_tsv_from_yuanshuju.py
  python parse_hp_zh_tsv_from_yuanshuju.py --source-dir "path/to/源数据"
"""

import sys
import pandas as pd
from pathlib import Path
from typing import Optional
from collections import defaultdict

script_dir = Path(__file__).parent
# 提取脚本 的父级 = 知识内容提取；源数据、提取结果 均在 知识内容提取 下
base_dir = script_dir.parent
source_dir = base_dir / "源数据"
output_dir = base_dir / "提取结果" / "hpo"


def parse_hp_zh_babelon(babelon_path: Path) -> pd.DataFrame:
    """
    解析 hp-zh.babelon.tsv 格式：
    source_language, translation_language, subject_id, predicate_id, source_value, translation_value, translation_status
    - subject_id: HP:0000001
    - predicate_id: rdfs:label -> 标签；IAO:0000115 -> 定义
    - translation_value: 中文翻译
    """
    print(f"正在解析: {babelon_path}")
    df = pd.read_csv(babelon_path, sep='\t', encoding='utf-8')
    print(f"  共 {len(df)} 行")

    # 按 subject_id 聚合：label 和 definition 可能有多行
    rows = defaultdict(lambda: {'HPO编码': '', '标准名称（英文）': '', '标准名称（中文）': '', '定义（中文）': ''})

    for _, row in df.iterrows():
        sid = str(row.get('subject_id', '')).strip()
        if not sid or sid == 'nan' or not sid.startswith('HP:'):
            continue

        # 标准化 HP:0000001 格式
        if sid.startswith('HP_'):
            sid = 'HP:' + sid[3:]
        rows[sid]['HPO编码'] = sid

        pred = str(row.get('predicate_id', '')).strip()
        src = str(row.get('source_value', '')).strip() if pd.notna(row.get('source_value')) else ''
        trans = str(row.get('translation_value', '')).strip() if pd.notna(row.get('translation_value')) else ''

        if not trans:
            continue

        if 'rdfs:label' in pred or pred == 'rdfs:label':
            rows[sid]['标准名称（英文）'] = src
            rows[sid]['标准名称（中文）'] = trans
        elif 'IAO_0000115' in pred or 'IAO:0000115' in pred or 'definition' in pred.lower():
            rows[sid]['定义（中文）'] = trans

    result = [v for v in rows.values() if v['标准名称（中文）']]
    out = pd.DataFrame(result)
    if not out.empty:
        out = out[['HPO编码', '标准名称（英文）', '标准名称（中文）', '定义（中文）']]
    print(f"  解析出 {len(out)} 条有效翻译（含中文标签）")
    return out


def main():
    import argparse
    ap = argparse.ArgumentParser(description='解析源数据目录下的HPO中文翻译TSV')
    ap.add_argument('--source-dir', type=str, default=None, help='源数据目录，默认: 提取方案/源数据')
    ap.add_argument('--output', type=str, default=None, help='输出hpo_chinese_mapping.csv路径')
    args = ap.parse_args()

    if args.source_dir:
        src = Path(args.source_dir)
        src = src.resolve() if src.is_absolute() else (base_dir / src).resolve()
    else:
        src = source_dir.resolve()

    if args.output:
        out_path = Path(args.output)
        if not out_path.is_absolute():
            out_path = (output_dir / out_path).resolve()
        else:
            out_path = out_path.resolve()
    else:
        out_path = (output_dir / "hpo_chinese_mapping.csv").resolve()

    babelon = src / "hp-zh.babelon.tsv"
    if not babelon.exists():
        print(f"未找到: {babelon}")
        print(f"请将 hp-zh.babelon.tsv 放在: {src}")
        sys.exit(1)

    df = parse_hp_zh_babelon(babelon)
    if df.empty:
        print("没有解析出有效翻译，请检查TSV格式。")
        sys.exit(1)

    out_path.parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(out_path, index=False, encoding='utf-8-sig')
    print(f"\n已保存: {out_path}")
    print(f"下一步: python merge_hpo_chinese.py --mapping \"{out_path}\"")


if __name__ == '__main__':
    main()

