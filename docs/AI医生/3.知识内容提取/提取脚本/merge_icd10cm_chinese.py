#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
将ICD-10-CM中文对照表合并到疾病目录中
"""
import os
import sys
import pandas as pd
import json
from pathlib import Path
from typing import Dict, Optional

# 添加脚本目录到路径
script_dir = Path(__file__).parent
sys.path.insert(0, str(script_dir.parent.parent))

class ICD10CMChineseMerger:
    """ICD-10-CM 中文对照合并器"""
    
    def __init__(self):
        """初始化合并器"""
        self.script_dir = Path(__file__).parent
        # 脚本在: docs/AI医生/知识内容提取/提取脚本/merge_icd10cm_chinese.py
        # 提取结果在: docs/AI医生/知识内容提取/提取结果/
        self.output_dir = self.script_dir.parent / "提取结果"
        self.source_dir = self.script_dir.parent / "源数据"
        
        # 默认文件路径
        self.english_catalog_path = self.output_dir / "icd10cm_disease_catalog.csv"
        self.chinese_mapping_path = self.output_dir / "icd10cm_chinese_mapping.csv"
        
        # ICD-10-CM 章节和节的常见翻译（标准术语）
        self.chapter_translations = {
            "Certain infectious and parasitic diseases (A00-B99)": "某些传染病和寄生虫病",
            "Neoplasms (C00-D49)": "肿瘤",
            "Diseases of the blood and blood-forming organs and certain disorders involving the immune mechanism (D50-D89)": "血液及造血器官疾病和某些涉及免疫机制的疾患",
            # 可以继续添加更多标准翻译
        }
        
    def load_chinese_mapping(self, mapping_path: str = None) -> Dict[str, Dict]:
        """
        加载中文对照映射
        
        Args:
            mapping_path: 映射文件路径（CSV或JSON）
            
        Returns:
            映射字典 {code: {chinese_name, english_name, ...}}
        """
        if mapping_path is None:
            mapping_path = self.chinese_mapping_path
        else:
            # 处理路径
            mapping_path = Path(mapping_path)
            if not mapping_path.is_absolute():
                # 如果路径中已经包含"提取结果"，直接使用
                if "提取结果" in str(mapping_path):
                    # 从项目根目录解析
                    mapping_path = self.script_dir.parent.parent.parent / mapping_path
                else:
                    # 否则从输出目录解析
                    mapping_path = self.output_dir / mapping_path
        
        mapping_path = Path(mapping_path)
        if not mapping_path.exists():
            raise FileNotFoundError(f"中文映射文件不存在: {mapping_path}\n请检查文件路径是否正确")
        
        print(f"正在加载中文映射: {mapping_path}")
        
        mapping = {}
        
        if mapping_path.suffix.lower() == '.json':
            # 加载JSON格式
            with open(mapping_path, 'r', encoding='utf-8') as f:
                mapping = json.load(f)
        else:
            # 加载CSV格式
            df = pd.read_csv(mapping_path, encoding='utf-8-sig')
            print(f"✓ 读取映射文件，共 {len(df)} 条记录")
            
            # 确定列名（自动识别）
            code_col = None
            chinese_col = None
            english_col = None
            
            for col in df.columns:
                col_lower = str(col).lower()
                if any(kw in col_lower for kw in ['code', '代码', '编码', 'icd']):
                    code_col = col
                elif any(kw in col_lower for kw in ['chinese', '中文', '名称']):
                    if 'english' not in col_lower and '英文' not in col_lower:
                        chinese_col = col
                elif any(kw in col_lower for kw in ['english', '英文']):
                    english_col = col
            
            print(f"  代码列: {code_col}")
            print(f"  中文名称列: {chinese_col}")
            print(f"  英文名称列: {english_col}")
            
            if code_col is None:
                raise ValueError(f"无法找到代码列，请检查映射文件列名")
            
            # 构建映射字典
            for _, row in df.iterrows():
                code = str(row[code_col]).strip() if pd.notna(row[code_col]) else None
                if code and code.lower() not in ['nan', 'none', '']:
                    chinese_name = str(row[chinese_col]).strip() if chinese_col and pd.notna(row.get(chinese_col)) else ""
                    english_name = str(row[english_col]).strip() if english_col and pd.notna(row.get(english_col)) else ""
                    
                    mapping[code] = {
                        'chinese_name': chinese_name,
                        'english_name': english_name
                    }
        
        print(f"✓ 加载完成，共 {len(mapping)} 个代码映射")
        return mapping
    
    def merge_chinese_to_catalog(self, 
                                  catalog_path: str = None,
                                  mapping_path: str = None,
                                  output_path: str = None) -> pd.DataFrame:
        """
        将中文对照合并到疾病目录
        
        Args:
            catalog_path: 英文疾病目录CSV文件路径
            mapping_path: 中文映射文件路径
            output_path: 输出文件路径
            
        Returns:
            合并后的DataFrame
        """
        # 设置默认路径
        if catalog_path is None:
            catalog_path = self.english_catalog_path
        else:
            # 处理路径
            catalog_path = Path(catalog_path)
            if not catalog_path.is_absolute():
                # 如果路径中已经包含"提取结果"，直接使用
                if "提取结果" in str(catalog_path):
                    # 从项目根目录解析
                    catalog_path = self.script_dir.parent.parent.parent / catalog_path
                else:
                    # 否则从输出目录解析
                    catalog_path = self.output_dir / catalog_path
        
        if output_path is None:
            output_path = self.output_dir / "icd10cm_disease_catalog_zh_en.csv"
        else:
            # 处理路径
            output_path = Path(output_path)
            if not output_path.is_absolute():
                # 如果路径中已经包含"提取结果"，直接使用
                if "提取结果" in str(output_path):
                    # 从项目根目录解析
                    output_path = self.script_dir.parent.parent.parent / output_path
                else:
                    # 否则从输出目录解析
                    output_path = self.output_dir / output_path
        
        catalog_path = Path(catalog_path)
        output_path = Path(output_path)
        
        if not catalog_path.exists():
            raise FileNotFoundError(f"英文疾病目录文件不存在: {catalog_path}\n请检查文件路径是否正确")
        
        print(f"\n正在读取英文疾病目录: {catalog_path}")
        df_catalog = pd.read_csv(catalog_path, encoding='utf-8-sig')
        print(f"✓ 读取完成，共 {len(df_catalog)} 条记录")
        
        # 加载中文映射
        mapping = self.load_chinese_mapping(mapping_path)
        
        # 合并中文名称
        print(f"\n正在合并中文名称...")
        
        # 确定代码列名
        code_col = 'ICD-10-CM编码' if 'ICD-10-CM编码' in df_catalog.columns else 'icd10cm_code'
        if code_col not in df_catalog.columns:
            # 尝试查找包含code的列
            for col in df_catalog.columns:
                if 'code' in str(col).lower() or '编码' in str(col):
                    code_col = col
                    break
        
        if code_col not in df_catalog.columns:
            raise ValueError(f"无法找到代码列，可用列: {list(df_catalog.columns)}")
        
        print(f"  使用代码列: {code_col}")
        
        # 添加中文字段
        df_catalog['中文名称'] = ''
        df_catalog['中文描述'] = ''
        df_catalog['章节描述（中文）'] = ''
        df_catalog['节描述（中文）'] = ''
        df_catalog['完整路径（中文）'] = ''
        
        # 构建章节和节描述的翻译映射（从数据中提取并翻译）
        chapter_translation = {}  # 英文 -> 中文
        section_translation = {}  # 英文 -> 中文
        
        matched_count = 0
        for idx, row in df_catalog.iterrows():
            code = str(row[code_col]).strip() if pd.notna(row[code_col]) else ""
            
            if code and code in mapping:
                chinese_info = mapping[code]
                df_catalog.at[idx, '中文名称'] = chinese_info.get('chinese_name', '')
                
                # 如果标准疾病名和描述不同，也添加中文描述
                if '标准疾病名' in df_catalog.columns:
                    standard_name = str(row['标准疾病名']) if pd.notna(row.get('标准疾病名')) else ""
                    if standard_name and chinese_info.get('chinese_name'):
                        df_catalog.at[idx, '中文描述'] = chinese_info.get('chinese_name', '')
                
                matched_count += 1
            
            # 提取章节和节描述用于翻译
            if '章节描述' in df_catalog.columns:
                chapter_desc_en = str(row['章节描述']) if pd.notna(row.get('章节描述')) else ""
                if chapter_desc_en and chapter_desc_en not in chapter_translation:
                    chapter_translation[chapter_desc_en] = ""  # 待翻译
            
            if '节描述' in df_catalog.columns:
                section_desc_en = str(row['节描述']) if pd.notna(row.get('节描述')) else ""
                if section_desc_en and section_desc_en not in section_translation:
                    section_translation[section_desc_en] = ""  # 待翻译
        
        # 翻译章节和节描述
        print(f"\n正在翻译章节和节描述...")
        print(f"  需要翻译的章节描述: {len(chapter_translation)} 条")
        print(f"  需要翻译的节描述: {len(section_translation)} 条")
        
        # 使用标准翻译字典或保持英文
        # 注意：ODS文件中通常没有章节和节的描述，这些是结构性元数据
        # 我们可以使用标准ICD-10-CM术语或保持英文
        
        # 为每条记录添加中文章节和节描述
        for idx, row in df_catalog.iterrows():
            # 翻译章节描述
            if '章节描述' in df_catalog.columns:
                chapter_desc_en = str(row['章节描述']) if pd.notna(row.get('章节描述')) else ""
                if chapter_desc_en:
                    # 首先检查标准翻译字典
                    if chapter_desc_en in self.chapter_translations:
                        df_catalog.at[idx, '章节描述（中文）'] = self.chapter_translations[chapter_desc_en]
                    # 然后检查提取的翻译字典
                    elif chapter_desc_en in chapter_translation and chapter_translation[chapter_desc_en]:
                        df_catalog.at[idx, '章节描述（中文）'] = chapter_translation[chapter_desc_en]
                    else:
                        # 暂时保持英文，后续可以手动翻译或使用AI翻译
                        df_catalog.at[idx, '章节描述（中文）'] = chapter_desc_en
            
            # 翻译节描述
            if '节描述' in df_catalog.columns:
                section_desc_en = str(row['节描述']) if pd.notna(row.get('节描述')) else ""
                if section_desc_en:
                    # 检查提取的翻译字典
                    if section_desc_en in section_translation and section_translation[section_desc_en]:
                        df_catalog.at[idx, '节描述（中文）'] = section_translation[section_desc_en]
                    else:
                        # 暂时保持英文，后续可以手动翻译或使用AI翻译
                        df_catalog.at[idx, '节描述（中文）'] = section_desc_en
            
            # 构建完整路径（中文）
            if '完整路径' in df_catalog.columns:
                full_path_en = str(row['完整路径']) if pd.notna(row.get('完整路径')) else ""
                chinese_name = df_catalog.at[idx, '中文名称']
                
                if full_path_en:
                    # 替换路径中的英文疾病名为中文（如果有）
                    path_parts = full_path_en.split(' > ')
                    if len(path_parts) > 0:
                        # 最后一个部分通常是疾病名，替换为中文
                        if chinese_name:
                            path_parts[-1] = chinese_name
                        # 中间部分如果有章节和节描述，也可以替换
                        # 例如："第1章 > Certain infectious..." -> "第1章 > 某些传染病..."
                        for i, part in enumerate(path_parts):
                            if i > 0:  # 跳过"第X章"部分
                                # 尝试用章节/节的中文描述替换
                                chapter_desc_cn = df_catalog.at[idx, '章节描述（中文）'] if '章节描述（中文）' in df_catalog.columns else ""
                                section_desc_cn = df_catalog.at[idx, '节描述（中文）'] if '节描述（中文）' in df_catalog.columns else ""
                                
                                if part == df_catalog.at[idx, '章节描述'] and chapter_desc_cn and chapter_desc_cn != part:
                                    path_parts[i] = chapter_desc_cn
                                elif part == df_catalog.at[idx, '节描述'] and section_desc_cn and section_desc_cn != part:
                                    path_parts[i] = section_desc_cn
                        
                        df_catalog.at[idx, '完整路径（中文）'] = ' > '.join(path_parts)
                    else:
                        df_catalog.at[idx, '完整路径（中文）'] = full_path_en
        
        print(f"✓ 合并完成，成功匹配 {matched_count} 条记录 ({matched_count/len(df_catalog)*100:.1f}%)")
        
        # 保存结果
        print(f"\n正在保存结果到: {output_path}")
        output_path.parent.mkdir(parents=True, exist_ok=True)
        df_catalog.to_csv(output_path, index=False, encoding='utf-8-sig')
        print(f"✓ 保存完成")
        
        # 生成统计信息
        print(f"\n" + "="*80)
        print("合并统计")
        print("="*80)
        print(f"总记录数: {len(df_catalog)}")
        print(f"匹配中文名称: {matched_count}")
        print(f"未匹配记录: {len(df_catalog) - matched_count}")
        print(f"匹配率: {matched_count/len(df_catalog)*100:.2f}%")
        
        # 显示示例
        print(f"\n示例数据（前3条有中文名称的记录）:")
        with_chinese = df_catalog[df_catalog['中文名称'] != ''].head(3)
        for idx, row in with_chinese.iterrows():
            code = row[code_col]
            english_name = row.get('标准疾病名', row.get('描述', ''))
            chinese_name = row['中文名称']
            print(f"  {code}: {english_name} -> {chinese_name}")
        
        return df_catalog

def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description='将ICD-10-CM中文对照表合并到疾病目录中')
    parser.add_argument('--catalog', type=str, help='英文疾病目录CSV文件路径')
    parser.add_argument('--mapping', type=str, help='中文映射文件路径（CSV或JSON）')
    parser.add_argument('--output', type=str, help='输出文件路径')
    
    args = parser.parse_args()
    
    try:
        merger = ICD10CMChineseMerger()
        merger.merge_chinese_to_catalog(
            catalog_path=args.catalog,
            mapping_path=args.mapping,
            output_path=args.output
        )
    except Exception as e:
        print(f"\n✗ 错误: {str(e)}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()

