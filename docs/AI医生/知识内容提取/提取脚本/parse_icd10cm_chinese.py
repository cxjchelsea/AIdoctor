#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
解析台湾健保署 ICD-10-CM 中文对照表
"""
import os
import sys
import pandas as pd
from pathlib import Path
from typing import Dict, List, Optional

# 添加脚本目录到路径
script_dir = Path(__file__).parent
sys.path.insert(0, str(script_dir.parent.parent))

class ICD10CMChineseParser:
    """ICD-10-CM 中文对照表解析器"""
    
    def __init__(self, ods_path: str = None):
        """
        初始化解析器
        
        Args:
            ods_path: ODS文件路径
        """
        if ods_path is None:
            # 默认在源数据目录查找
            # __file__ 在: docs/AI医生/知识内容提取/提取脚本/parse_icd10cm_chinese.py
            # 源数据在: docs/AI医生/知识内容提取/源数据
            source_dir = Path(__file__).parent.parent / "源数据"
            # 查找ODS文件
            ods_files = list(source_dir.glob("*中文版ICD-10-CM*.ods"))
            if not ods_files:
                # 也尝试直接在源数据目录查找（不带路径）
                source_dir_alt = Path(__file__).parent.parent.parent / "知识内容提取" / "源数据"
                ods_files = list(source_dir_alt.glob("*中文版ICD-10-CM*.ods"))
                if not ods_files:
                    raise FileNotFoundError(f"在 {source_dir} 或 {source_dir_alt} 中找不到ICD-10-CM中文对照表ODS文件")
                source_dir = source_dir_alt
            ods_path = ods_files[0]
        
        self.ods_path = Path(ods_path)
        if not self.ods_path.exists():
            raise FileNotFoundError(f"文件不存在: {self.ods_path}")
        
        print(f"正在解析文件: {self.ods_path}")
        self.data: Optional[pd.DataFrame] = None
        self.mapping: Dict[str, Dict] = {}  # code -> {chinese_name, english_name, ...}
    
    def list_sheets(self) -> List[str]:
        """列出ODS文件中的所有工作表"""
        try:
            import openpyxl
            from zipfile import ZipFile
            import xml.etree.ElementTree as ET
            
            # ODS文件实际上是ZIP压缩的XML文件
            with ZipFile(self.ods_path, 'r') as ods_zip:
                content_xml = ods_zip.read('content.xml')
                
            # 解析XML查找工作表
            root = ET.fromstring(content_xml)
            ns = {'table': 'urn:oasis:names:tc:opendocument:xmlns:table:1.0'}
            
            sheets = []
            for table in root.findall('.//table:table', ns):
                sheet_name = table.get('table:name', 'Unknown')
                sheets.append(sheet_name)
            
            return sheets
        except Exception as e:
            # 如果XML解析失败，尝试用Excel读取引擎
            try:
                # 尝试读取所有工作表
                xl_file = pd.ExcelFile(self.ods_path, engine='odf')
                return xl_file.sheet_names
            except:
                print(f"警告: 无法列出工作表: {str(e)}")
                return ['Sheet1']  # 默认工作表名
    
    def parse(self, sheet_name: str = None) -> pd.DataFrame:
        """
        解析ODS文件
        
        Args:
            sheet_name: 工作表名称，如果不指定则自动查找包含数据的第一个工作表
            
        Returns:
            解析后的DataFrame
        """
        try:
            # 读取ODS文件（使用pandas，需要安装odfpy或ezodf）
            print("正在读取ODS文件...")
            
            # 可能需要安装: pip install odfpy
            # 或者: pip install ezodf pyexcel-ods3
            
            # 先列出所有工作表
            print("正在列出所有工作表...")
            try:
                xl_file = pd.ExcelFile(self.ods_path, engine='odf')
                sheet_names = xl_file.sheet_names
                print(f"✓ 找到 {len(sheet_names)} 个工作表:")
                for i, name in enumerate(sheet_names, 1):
                    print(f"  {i}. {name}")
            except Exception as e:
                print(f"⚠ 无法列出工作表: {str(e)}")
                sheet_names = [None]
            
            # 如果没有指定工作表，尝试自动查找包含数据的
            if sheet_name is None and sheet_names:
                # 尝试读取第一个工作表看看是不是更新日志
                try:
                    test_df = pd.read_excel(self.ods_path, engine='odf', sheet_name=sheet_names[0])
                    # 检查是否只包含日期和摘要列（更新日志特征）
                    if len(test_df.columns) <= 2 and any('日期' in str(col) or '摘要' in str(col) for col in test_df.columns):
                        print(f"\n⚠ 第一个工作表 '{sheet_names[0]}' 看起来是更新日志，尝试读取其他工作表...")
                        # 优先查找"ICD-10-CM"工作表（完整对照表）
                        if 'ICD-10-CM' in sheet_names:
                            sheet_name = 'ICD-10-CM'
                            print(f"✓ 找到ICD-10-CM对照表工作表: {sheet_name}")
                        else:
                            # 尝试读取其他工作表
                            for test_sheet in sheet_names[1:]:
                                try:
                                    test_df = pd.read_excel(self.ods_path, engine='odf', sheet_name=test_sheet)
                                    # 检查是否包含代码相关的列，且数据量较大（不是更新明细）
                                    col_names = [str(col).lower() for col in test_df.columns]
                                    if (any(keyword in ' '.join(col_names) for keyword in ['code', '代码', '编码', 'icd', '中文', 'chinese', '名称', 'name']) 
                                        and len(test_df) > 1000):  # 完整对照表应该有很多行
                                        sheet_name = test_sheet
                                        print(f"✓ 找到数据工作表: {test_sheet} ({len(test_df)} 行)")
                                        break
                                except:
                                    continue
                            
                            # 如果还是没找到，使用第一个非日志工作表
                            if sheet_name is None and len(sheet_names) > 1:
                                sheet_name = sheet_names[1]
                                print(f"✓ 使用工作表: {sheet_name}")
                    else:
                        sheet_name = sheet_names[0]
                except:
                    sheet_name = sheet_names[0] if sheet_names else None
            
            # 读取指定的工作表
            print(f"\n正在读取工作表: {sheet_name if sheet_name else '默认'}")
            self.data = pd.read_excel(
                self.ods_path,
                engine='odf',
                sheet_name=sheet_name
            )
            
            print(f"✓ 成功读取文件，共 {len(self.data)} 行")
            print(f"列名: {list(self.data.columns)}")
            
            # 显示前几行
            print("\n前5行数据:")
            print(self.data.head())
            
            return self.data
            
        except Exception as e:
            print(f"✗ 读取失败: {str(e)}")
            print("\n提示：如果遇到错误，可能需要安装依赖库：")
            print("  pip install odfpy")
            print("  或者")
            print("  pip install ezodf pyexcel-ods3")
            raise
    
    def analyze_structure(self):
        """分析文件结构"""
        if self.data is None:
            self.parse()
        
        print("\n" + "="*80)
        print("文件结构分析")
        print("="*80)
        print(f"\n总行数: {len(self.data)}")
        print(f"列数: {len(self.data.columns)}")
        print(f"\n列名详情:")
        for i, col in enumerate(self.data.columns):
            print(f"  {i+1}. {col}")
        
        print(f"\n数据类型:")
        print(self.data.dtypes)
        
        print(f"\n缺失值统计:")
        print(self.data.isnull().sum())
        
        # 检查是否有空行
        empty_rows = self.data.isnull().all(axis=1).sum()
        print(f"\n空行数: {empty_rows}")
    
    def build_mapping(self, code_col: str = None, chinese_col: str = None, english_col: str = None) -> Dict[str, Dict]:
        """
        构建代码到中文名称的映射
        
        Args:
            code_col: 代码列名（如果不指定，自动查找）
            chinese_col: 中文名称列名（如果不指定，自动查找）
            english_col: 英文名称列名（如果不指定，自动查找）
            
        Returns:
            映射字典 {code: {chinese_name, english_name, ...}}
        """
        if self.data is None:
            self.parse()
        
        print("\n" + "="*80)
        print("构建映射字典")
        print("="*80)
        
        # 自动查找列名
        if code_col is None:
            # 查找可能包含"代码"、"code"、"编码"的列
            for col in self.data.columns:
                col_lower = str(col).lower()
                if any(keyword in col_lower for keyword in ['code', '代码', '编码', 'icd']):
                    code_col = col
                    break
        
        if chinese_col is None:
            # 查找可能包含"中文"、"chinese"、"名称"的列
            for col in self.data.columns:
                col_lower = str(col).lower()
                if any(keyword in col_lower for keyword in ['中文', 'chinese', '名称']):
                    # 排除英文列
                    if 'english' not in col_lower and '英文' not in col_lower:
                        chinese_col = col
                        break
        
        if english_col is None:
            # 查找可能包含"英文"、"english"的列
            for col in self.data.columns:
                col_lower = str(col).lower()
                if any(keyword in col_lower for keyword in ['english', '英文']):
                    english_col = col
                    break
        
        print(f"\n使用的列:")
        print(f"  代码列: {code_col}")
        print(f"  中文名称列: {chinese_col}")
        print(f"  英文名称列: {english_col}")
        
        if code_col is None:
            raise ValueError("无法找到代码列，请手动指定code_col参数")
        
        # 构建映射
        self.mapping = {}
        
        for idx, row in self.data.iterrows():
            code = str(row[code_col]).strip() if pd.notna(row[code_col]) else None
            
            if code and code.lower() not in ['nan', 'none', '']:
                chinese_name = str(row[chinese_col]).strip() if chinese_col and pd.notna(row.get(chinese_col)) else ""
                english_name = str(row[english_col]).strip() if english_col and pd.notna(row.get(english_col)) else ""
                
                self.mapping[code] = {
                    'chinese_name': chinese_name,
                    'english_name': english_name,
                    'row_index': idx
                }
        
        print(f"\n✓ 构建完成，共 {len(self.mapping)} 个代码映射")
        
        # 显示示例
        print(f"\n示例映射（前5个）:")
        for i, (code, info) in enumerate(list(self.mapping.items())[:5]):
            print(f"  {code}: {info['chinese_name']}")
        
        return self.mapping
    
    def save_mapping_json(self, output_path: str):
        """保存映射为JSON文件"""
        import json
        
        output_path = Path(output_path)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(self.mapping, f, ensure_ascii=False, indent=2)
        
        print(f"\n✓ 映射已保存到: {output_path}")
    
    def save_mapping_csv(self, output_path: str):
        """保存映射为CSV文件"""
        output_path = Path(output_path)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        
        mapping_list = [
            {
                'code': code,
                'chinese_name': info['chinese_name'],
                'english_name': info.get('english_name', '')
            }
            for code, info in self.mapping.items()
        ]
        
        df = pd.DataFrame(mapping_list)
        df.to_csv(output_path, index=False, encoding='utf-8-sig')
        
        print(f"\n✓ 映射已保存到: {output_path}")

def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description='解析台湾健保署 ICD-10-CM 中文对照表')
    parser.add_argument('--ods-file', type=str, help='ODS文件路径')
    parser.add_argument('--sheet', type=str, help='工作表名称（如果不指定则自动查找）')
    parser.add_argument('--analyze', action='store_true', help='分析文件结构')
    parser.add_argument('--build-mapping', action='store_true', help='构建映射字典')
    parser.add_argument('--save-json', type=str, help='保存为JSON文件')
    parser.add_argument('--save-csv', type=str, help='保存为CSV文件')
    
    args = parser.parse_args()
    
    try:
        parser_obj = ICD10CMChineseParser(ods_path=args.ods_file)
        
        # 分析结构
        if args.analyze or not args.build_mapping:
            parser_obj.parse(sheet_name=args.sheet)
            parser_obj.analyze_structure()
        
        # 构建映射
        if args.build_mapping or args.save_json or args.save_csv:
            if parser_obj.data is None:
                parser_obj.parse(sheet_name=args.sheet)
            parser_obj.build_mapping()
        
        # 保存文件
        if args.save_json:
            parser_obj.save_mapping_json(args.save_json)
        
        if args.save_csv:
            parser_obj.save_mapping_csv(args.save_csv)
        
    except Exception as e:
        print(f"\n✗ 错误: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main()

