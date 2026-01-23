#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
HPO数据转换为CSV格式
将JSON格式的HPO数据转换为易读的CSV表格，方便医学专业人员查看
"""

import json
import csv
from pathlib import Path
from typing import Dict, List


def convert_hpo_json_to_csv(json_path: str, csv_path: str, limit: int = None):
    """
    将HPO JSON数据转换为CSV格式
    
    Args:
        json_path: JSON文件路径
        csv_path: 输出CSV文件路径
        limit: 限制导出的记录数（None表示全部导出）
    """
    print(f"正在读取JSON文件: {json_path}")
    
    with open(json_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    terms = data.get('terms', {})
    print(f"找到 {len(terms)} 个HPO术语")
    
    # 准备CSV数据
    csv_rows = []
    
    # 表头
    headers = [
        'HPO编码',
        '标准名称（英文）',
        '定义',
        '注释',
        '精确同义词（多个用分号分隔）',
        '相关同义词（多个用分号分隔）',
        '父类HPO编码（多个用分号分隔）',
        '子类HPO编码（多个用分号分隔）',
        '是否为叶子节点',
        '替代ID（多个用分号分隔）',
        '是否已废弃',
        '被替换为',
        '创建日期',
        '外部引用（多个用分号分隔）'
    ]
    
    # 按HPO ID排序
    sorted_terms = sorted(terms.items(), key=lambda x: x[0])
    
    # 如果指定了限制，只导出前N条
    if limit:
        sorted_terms = sorted_terms[:limit]
        print(f"将导出前 {limit} 条记录...")
    
    print("正在转换数据...")
    for term_id, term_data in sorted_terms:
        # 格式化外部引用
        xrefs_str = ""
        if term_data.get('xrefs'):
            xref_parts = []
            for xref in term_data['xrefs']:
                if isinstance(xref, dict):
                    xref_parts.append(f"{xref.get('type', '')}:{xref.get('id', '')}")
                else:
                    xref_parts.append(str(xref))
            xrefs_str = "; ".join(xref_parts)
        
        row = [
            term_data.get('id', ''),
            term_data.get('label', ''),
            term_data.get('definition', '') or '',
            term_data.get('comment', '') or '',
            '; '.join(term_data.get('exact_synonyms', [])),
            '; '.join(term_data.get('related_synonyms', [])),
            '; '.join(term_data.get('parent_ids', [])),
            '; '.join(term_data.get('child_ids', [])),
            '是' if term_data.get('is_leaf', False) else '否',
            '; '.join(term_data.get('alternative_ids', [])),
            '是' if term_data.get('deprecated', False) else '否',
            term_data.get('replaced_by', '') or '',
            term_data.get('creation_date', '') or '',
            xrefs_str
        ]
        csv_rows.append(row)
    
    # 写入CSV文件
    print(f"正在保存CSV文件: {csv_path}")
    with open(csv_path, 'w', newline='', encoding='utf-8-sig') as f:  # utf-8-sig支持Excel直接打开
        writer = csv.writer(f)
        writer.writerow(headers)
        writer.writerows(csv_rows)
    
    print(f"✓ 成功导出 {len(csv_rows)} 条记录到 {csv_path}")
    print(f"  文件可以使用Excel、WPS等表格软件直接打开查看")


def generate_sample_report(json_path: str, html_path: str, sample_count: int = 100):
    """
    生成HTML格式的示例报告
    
    Args:
        json_path: JSON文件路径
        html_path: 输出HTML文件路径
        sample_count: 示例术语数量
    """
    print(f"正在生成HTML报告...")
    
    with open(json_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    metadata = data.get('metadata', {})
    terms = data.get('terms', {})
    
    # 选择一些有代表性的示例
    sample_terms = []
    sorted_terms = sorted(terms.items(), key=lambda x: x[0])
    
    # 选择前N个作为示例
    for term_id, term_data in sorted_terms[:sample_count]:
        sample_terms.append(term_data)
    
    # 生成HTML
    html_content = f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>HPO数据提取结果报告</title>
    <style>
        body {{
            font-family: "Microsoft YaHei", Arial, sans-serif;
            margin: 20px;
            background-color: #f5f5f5;
        }}
        .container {{
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }}
        h1 {{
            color: #2c3e50;
            border-bottom: 3px solid #3498db;
            padding-bottom: 10px;
        }}
        h2 {{
            color: #34495e;
            margin-top: 30px;
        }}
        .metadata {{
            background: #ecf0f1;
            padding: 15px;
            border-radius: 5px;
            margin: 20px 0;
        }}
        .metadata-item {{
            margin: 8px 0;
        }}
        .term-card {{
            border: 1px solid #ddd;
            margin: 15px 0;
            padding: 15px;
            border-radius: 5px;
            background: #fafafa;
        }}
        .term-id {{
            font-weight: bold;
            color: #e74c3c;
            font-size: 1.1em;
        }}
        .term-label {{
            font-size: 1.2em;
            color: #2c3e50;
            margin: 10px 0;
        }}
        .term-definition {{
            color: #555;
            margin: 10px 0;
            line-height: 1.6;
        }}
        .synonyms {{
            margin: 10px 0;
        }}
        .synonyms strong {{
            color: #27ae60;
        }}
        .relationships {{
            margin: 10px 0;
        }}
        .relationships strong {{
            color: #8e44ad;
        }}
        .tag {{
            display: inline-block;
            background: #3498db;
            color: white;
            padding: 2px 8px;
            border-radius: 3px;
            font-size: 0.9em;
            margin: 2px;
        }}
        .note {{
            background: #fff3cd;
            border-left: 4px solid #ffc107;
            padding: 10px;
            margin: 20px 0;
        }}
    </style>
</head>
<body>
    <div class="container">
        <h1>HPO（人类表型本体）数据提取结果报告</h1>
        
        <div class="metadata">
            <h2>数据概览</h2>
            <div class="metadata-item"><strong>HPO版本:</strong> {metadata.get('version', '未知')}</div>
            <div class="metadata-item"><strong>数据源:</strong> {metadata.get('source', '未知')}</div>
            <div class="metadata-item"><strong>提取日期:</strong> {metadata.get('extraction_date', '未知')}</div>
            <div class="metadata-item"><strong>术语总数:</strong> {metadata.get('total_terms', 0):,} 个</div>
            <div class="metadata-item"><strong>同义词总数:</strong> {metadata.get('total_synonyms', 0):,} 个</div>
        </div>
        
        <div class="note">
            <strong>说明:</strong> 本报告展示了前 {sample_count} 个HPO术语的详细信息。
            完整数据请查看CSV文件或JSON文件。
        </div>
        
        <h2>术语示例</h2>
"""
    
    for term in sample_terms:
        exact_synonyms = term.get('exact_synonyms', [])
        related_synonyms = term.get('related_synonyms', [])
        parent_ids = term.get('parent_ids', [])
        child_ids = term.get('child_ids', [])
        xrefs = term.get('xrefs', [])
        
        # 构建外部引用字符串
        xrefs_str = ""
        if xrefs:
            xref_parts = []
            for x in xrefs[:3]:
                xref_type = x.get('type', '')
                xref_id = x.get('id', '')
                xref_parts.append(f"{xref_type}:{xref_id}")
            xrefs_str = ", ".join(xref_parts)
        
        html_content += f"""
        <div class="term-card">
            <div class="term-id">HPO编码: {term.get('id', '')}</div>
            <div class="term-label">{term.get('label', '')}</div>
            {f'<div class="term-definition"><strong>定义:</strong> {term.get("definition", "")}</div>' if term.get('definition') else ''}
            {f'<div class="term-definition"><strong>注释:</strong> {term.get("comment", "")}</div>' if term.get('comment') else ''}
            
            {f'<div class="synonyms"><strong>精确同义词:</strong> {", ".join(exact_synonyms)}</div>' if exact_synonyms else ''}
            {f'<div class="synonyms"><strong>相关同义词:</strong> {", ".join(related_synonyms)}</div>' if related_synonyms else ''}
            
            <div class="relationships">
                {f'<strong>父类:</strong> {", ".join(parent_ids[:5])}{" ..." if len(parent_ids) > 5 else ""} ' if parent_ids else ''}
                {f'<strong>子类:</strong> {", ".join(child_ids[:5])}{" ..." if len(child_ids) > 5 else ""} ' if child_ids else ''}
                {f'<span class="tag">{"叶子节点" if term.get("is_leaf") else "非叶子节点"}</span>' if term.get('is_leaf') is not None else ''}
                {f'<span class="tag">已废弃</span>' if term.get('deprecated') else ''}
            </div>
            
            {f'<div><strong>外部引用:</strong> {xrefs_str}</div>' if xrefs_str else ''}
        </div>
"""
    
    html_content += """
    </div>
</body>
</html>
"""
    
    with open(html_path, 'w', encoding='utf-8') as f:
        f.write(html_content)
    
    print(f"✓ HTML报告已保存到: {html_path}")


def main():
    """主函数"""
    script_dir = Path(__file__).parent
    json_file = script_dir / 'hpo_data.json'
    
    if not json_file.exists():
        print(f"错误: 找不到文件 {json_file}")
        print("请先运行 extract_hpo_data.py 生成JSON文件")
        return
    
    # 生成完整CSV文件（所有术语）
    csv_file = script_dir / 'hpo_data.csv'
    print("=" * 60)
    print("生成完整CSV文件...")
    print("=" * 60)
    convert_hpo_json_to_csv(str(json_file), str(csv_file))
    
    # 生成示例CSV文件（前1000条，便于快速查看）
    sample_csv_file = script_dir / 'hpo_data_示例_前1000条.csv'
    print("\n" + "=" * 60)
    print("生成示例CSV文件（前1000条）...")
    print("=" * 60)
    convert_hpo_json_to_csv(str(json_file), str(sample_csv_file), limit=1000)
    
    # 生成HTML报告
    html_file = script_dir / 'hpo_data_报告.html'
    print("\n" + "=" * 60)
    print("生成HTML报告...")
    print("=" * 60)
    generate_sample_report(str(json_file), str(html_file), sample_count=100)
    
    print("\n" + "=" * 60)
    print("转换完成！")
    print("=" * 60)
    print(f"✓ 完整CSV文件: {csv_file}")
    print(f"✓ 示例CSV文件: {sample_csv_file}")
    print(f"✓ HTML报告: {html_file}")
    print("\n提示: CSV文件可以使用Excel、WPS等表格软件直接打开")


if __name__ == '__main__':
    main()

