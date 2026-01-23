#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
HPO中文翻译字典下载脚本
从官方和第三方资源下载HPO中文翻译字典

支持资源：
1. HPO International 中文翻译文件（GitHub）
2. CHPO（Chinese HPO）项目
3. 其他可用的中文翻译资源
"""

import os
import sys
import json
import csv
import requests
import pandas as pd
from pathlib import Path
from typing import Dict, List, Optional
from urllib.parse import urljoin

# 添加脚本目录到路径
script_dir = Path(__file__).parent
sys.path.insert(0, str(script_dir.parent.parent))


class HPODictDownloader:
    """HPO中文翻译字典下载器"""
    
    def __init__(self):
        """初始化下载器"""
        self.script_dir = Path(__file__).parent
        self.output_dir = self.script_dir.parent / "提取结果" / "hpo"
        self.dict_dir = self.output_dir / "翻译字典"
        self.dict_dir.mkdir(parents=True, exist_ok=True)
        
        # 资源URL配置
        self.resources = {
            'hpo_translations_github': {
                'name': 'HPO International 中文翻译（GitHub - obophenotype）',
                'base_url': 'https://raw.githubusercontent.com/obophenotype/hpo-translations/main/',
                'files': {
                    # 尝试多个可能的路径
                    'zh.babelon.tsv': 'zh/zh.babelon.tsv',
                    'zh_CN.babelon.tsv': 'zh_CN/zh_CN.babelon.tsv',
                },
                'description': 'HPO官方国际化项目的中文翻译文件，包含标签、定义和同义词的翻译'
            },
            'hpo_translations_drseb': {
                'name': 'HPO中文翻译（GitHub - drseb）',
                'base_url': 'https://raw.githubusercontent.com/drseb/HPO-translations/master/',
                'files': {
                    'chinese.json': 'chinese.json',  # 可能需要检查实际文件名
                },
                'description': 'drseb维护的HPO中文翻译项目'
            },
            # 注意：CHPO可能需要从论文或官网获取，这里先提供框架
        }
    
    def download_file(self, url: str, save_path: Path, timeout: int = 30) -> bool:
        """
        下载文件
        
        Args:
            url: 文件URL
            save_path: 保存路径
            timeout: 超时时间（秒）
            
        Returns:
            是否下载成功
        """
        try:
            print(f"正在下载: {url}")
            response = requests.get(url, timeout=timeout, stream=True)
            response.raise_for_status()
            
            # 保存文件
            save_path.parent.mkdir(parents=True, exist_ok=True)
            with open(save_path, 'wb') as f:
                for chunk in response.iter_content(chunk_size=8192):
                    f.write(chunk)
            
            print(f"✓ 下载成功: {save_path}")
            return True
            
        except requests.exceptions.RequestException as e:
            print(f"❌ 下载失败: {e}")
            return False
        except Exception as e:
            print(f"❌ 保存失败: {e}")
            return False
    
    def download_hpo_translations(self) -> Optional[Path]:
        """
        下载HPO International中文翻译文件
        尝试多个资源，直到成功下载
        
        Returns:
            下载的文件路径，如果失败返回None
        """
        # 按优先级尝试多个资源
        resource_keys = ['hpo_translations_github', 'hpo_translations_drseb']
        
        for resource_key in resource_keys:
            if resource_key not in self.resources:
                continue
                
            resource = self.resources[resource_key]
            base_url = resource['base_url']
            
            print(f"\n{'='*60}")
            print(f"尝试下载资源: {resource['name']}")
            print(f"说明: {resource['description']}")
            print(f"{'='*60}\n")
            
            downloaded_files = []
            
            # 尝试下载每个文件，直到成功
            for filename, filepath in resource['files'].items():
                url = urljoin(base_url, filepath)
                save_path = self.dict_dir / filename
                
                # 如果文件已存在，询问是否覆盖
                if save_path.exists():
                    print(f"⚠️  文件已存在: {save_path}")
                    response = input("是否重新下载？(y/n，默认n): ").strip().lower()
                    if response != 'y':
                        print(f"跳过下载，使用现有文件")
                        downloaded_files.append(save_path)
                        continue
                
                if self.download_file(url, save_path):
                    downloaded_files.append(save_path)
                    # 如果成功下载，返回这个文件
                    print(f"\n✅ 成功下载 {len(downloaded_files)} 个文件")
                    return downloaded_files[0]
                else:
                    print(f"⚠️  尝试下一个文件路径...")
            
            # 如果这个资源的所有文件都失败了，尝试下一个资源
            print(f"⚠️  资源 {resource['name']} 下载失败，尝试下一个资源...")
        
        # 所有资源都失败了
        print(f"\n❌ 所有资源下载失败")
        print(f"\n建议：")
        print(f"1. 检查网络连接")
        print(f"2. 手动访问以下页面下载：")
        print(f"   - https://obophenotype.github.io/hpo-translations/translations/zh/")
        print(f"   - https://github.com/obophenotype/hpo-translations")
        print(f"   - https://github.com/drseb/HPO-translations")
        print(f"3. 下载后放到目录: {self.dict_dir}")
        
        return None
    
    def parse_json_translation(self, json_path: Path) -> pd.DataFrame:
        """
        解析JSON格式的HPO翻译文件
        
        Args:
            json_path: JSON文件路径
            
        Returns:
            解析后的DataFrame
        """
        try:
            print(f"\n正在解析翻译文件: {json_path}")
            
            with open(json_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            
            print(f"✓ 读取成功")
            
            result_data = []
            
            # 处理不同的JSON结构
            if isinstance(data, dict):
                for hpo_id, entry in data.items():
                    if not hpo_id.startswith('HP:'):
                        continue
                    
                    # 尝试提取中文标签
                    chinese_label = None
                    chinese_def = None
                    english_label = None
                    
                    if isinstance(entry, dict):
                        # 结构1: {"label": {"en": "...", "zh": "..."}}
                        if 'label' in entry and isinstance(entry['label'], dict):
                            chinese_label = entry['label'].get('zh') or entry['label'].get('zh-CN')
                            english_label = entry['label'].get('en')
                        
                        # 结构2: {"zh": "...", "en": "..."}
                        elif 'zh' in entry or 'zh-CN' in entry:
                            chinese_label = entry.get('zh') or entry.get('zh-CN')
                            english_label = entry.get('en')
                        
                        # 提取定义
                        if 'definition' in entry and isinstance(entry['definition'], dict):
                            chinese_def = entry['definition'].get('zh') or entry['definition'].get('zh-CN')
                    
                    if chinese_label:
                        result_data.append({
                            'HPO编码': hpo_id,
                            '标准名称（英文）': english_label or '',
                            '标准名称（中文）': chinese_label,
                            '定义（中文）': chinese_def or '',
                        })
            
            result_df = pd.DataFrame(result_data)
            print(f"✓ 解析完成，共 {len(result_df)} 条有效翻译记录")
            
            return result_df
            
        except Exception as e:
            print(f"❌ 解析失败: {e}")
            import traceback
            traceback.print_exc()
            return pd.DataFrame()
    
    def parse_babelon_tsv(self, tsv_path: Path) -> pd.DataFrame:
        """
        解析HPO翻译的babelon.tsv文件
        
        babelon.tsv格式说明：
        - 列：HPO_ID, label_en, label_zh, definition_en, definition_zh, ...
        - 每行代表一个HPO术语的翻译
        
        Args:
            tsv_path: TSV文件路径
            
        Returns:
            解析后的DataFrame
        """
        try:
            print(f"\n正在解析翻译文件: {tsv_path}")
            
            # 读取TSV文件
            df = pd.read_csv(tsv_path, sep='\t', encoding='utf-8')
            print(f"✓ 读取成功，共 {len(df)} 条记录")
            
            # 显示列名
            print(f"列名: {list(df.columns)}")
            
            # 尝试识别关键列
            hpo_id_col = None
            label_zh_col = None
            definition_zh_col = None
            label_en_col = None
            
            for col in df.columns:
                col_lower = str(col).lower()
                if 'hpo' in col_lower and ('id' in col_lower or 'code' in col_lower):
                    hpo_id_col = col
                elif 'label' in col_lower and ('zh' in col_lower or 'chinese' in col_lower or '中文' in col):
                    label_zh_col = col
                elif 'definition' in col_lower and ('zh' in col_lower or 'chinese' in col_lower or '中文' in col):
                    definition_zh_col = col
                elif 'label' in col_lower and ('en' in col_lower or 'english' in col_lower):
                    label_en_col = col
            
            print(f"\n识别的列:")
            print(f"  HPO编码列: {hpo_id_col}")
            print(f"  中文标签列: {label_zh_col}")
            print(f"  中文定义列: {definition_zh_col}")
            print(f"  英文标签列: {label_en_col}")
            
            if not hpo_id_col:
                raise ValueError("无法识别HPO编码列")
            
            # 构建标准格式的DataFrame
            result_data = []
            for _, row in df.iterrows():
                hpo_id = str(row[hpo_id_col]).strip()
                
                # 标准化HPO编码格式
                if hpo_id and not hpo_id.startswith('HP:'):
                    if hpo_id.startswith('HP_'):
                        hpo_id = hpo_id.replace('_', ':', 1)
                    elif hpo_id.startswith('HP'):
                        hpo_id = 'HP:' + hpo_id[2:].lstrip(':')
                
                if not hpo_id or hpo_id == 'nan':
                    continue
                
                result_data.append({
                    'HPO编码': hpo_id,
                    '标准名称（英文）': str(row[label_en_col]).strip() if label_en_col and pd.notna(row.get(label_en_col)) else '',
                    '标准名称（中文）': str(row[label_zh_col]).strip() if label_zh_col and pd.notna(row.get(label_zh_col)) else '',
                    '定义（中文）': str(row[definition_zh_col]).strip() if definition_zh_col and pd.notna(row.get(definition_zh_col)) else '',
                })
            
            result_df = pd.DataFrame(result_data)
            
            # 过滤掉没有中文翻译的记录
            result_df = result_df[result_df['标准名称（中文）'].notna() & (result_df['标准名称（中文）'] != '')]
            
            print(f"\n✓ 解析完成，共 {len(result_df)} 条有效翻译记录")
            print(f"  有中文翻译的记录: {len(result_df)}")
            
            return result_df
            
        except Exception as e:
            print(f"❌ 解析失败: {e}")
            import traceback
            traceback.print_exc()
            return pd.DataFrame()
    
    def convert_to_mapping_csv(self, df: pd.DataFrame, output_path: Optional[Path] = None) -> Path:
        """
        将翻译DataFrame转换为映射CSV文件
        
        Args:
            df: 翻译DataFrame
            output_path: 输出文件路径（可选）
            
        Returns:
            输出文件路径
        """
        if output_path is None:
            output_path = self.output_dir / "hpo_chinese_mapping.csv"
        
        print(f"\n正在保存映射文件: {output_path}")
        
        # 只保留必要的列
        mapping_df = df[['HPO编码', '标准名称（中文）', '定义（中文）']].copy()
        
        # 保存为CSV
        mapping_df.to_csv(output_path, index=False, encoding='utf-8-sig')
        
        print(f"✓ 保存成功，共 {len(mapping_df)} 条映射记录")
        
        return output_path
    
    def check_existing_files(self) -> Optional[Path]:
        """
        检查是否已有翻译文件
        
        Returns:
            找到的文件路径，如果没有返回None
        """
        # 检查常见文件名
        possible_files = [
            'zh.babelon.tsv',
            'zh_CN.babelon.tsv',
            'chinese.json',
            'hpo_chinese.json',
            'zh.json',
        ]
        
        for filename in possible_files:
            file_path = self.dict_dir / filename
            if file_path.exists():
                print(f"✓ 找到现有文件: {file_path}")
                return file_path
        
        return None
    
    def download_and_convert(self) -> Optional[Path]:
        """
        下载并转换翻译字典
        
        Returns:
            生成的映射文件路径，如果失败返回None
        """
        print(f"\n{'='*60}")
        print(f"HPO中文翻译字典下载工具")
        print(f"{'='*60}\n")
        
        # 先检查是否已有文件
        existing_file = self.check_existing_files()
        if existing_file:
            response = input(f"\n发现现有翻译文件: {existing_file}\n是否使用现有文件？(y/n，默认y): ").strip().lower()
            if response != 'n':
                tsv_path = existing_file
            else:
                # 下载新文件
                tsv_path = self.download_hpo_translations()
        else:
            # 下载翻译文件
            tsv_path = self.download_hpo_translations()
        
        if not tsv_path or not tsv_path.exists():
            print("\n❌ 未找到翻译文件")
            print(f"\n💡 手动下载方案：")
            print(f"1. 访问以下页面查找中文翻译文件：")
            print(f"   - https://obophenotype.github.io/hpo-translations/translations/zh/")
            print(f"   - https://github.com/obophenotype/hpo-translations")
            print(f"   - https://github.com/drseb/HPO-translations")
            print(f"\n2. 下载翻译文件（支持格式：.tsv, .json, .csv）")
            print(f"\n3. 将文件放到以下目录：")
            print(f"   {self.dict_dir}")
            print(f"\n4. 重新运行此脚本")
            return None
        
        # 解析翻译文件（根据文件类型选择解析方法）
        if tsv_path.suffix.lower() == '.json':
            df = self.parse_json_translation(tsv_path)
        else:
            df = self.parse_babelon_tsv(tsv_path)
        
        if df.empty:
            print("\n❌ 解析失败或没有有效翻译")
            print(f"文件路径: {tsv_path}")
            print(f"请检查文件格式是否正确")
            return None
        
        # 转换为映射CSV
        mapping_path = self.convert_to_mapping_csv(df)
        
        print(f"\n{'='*60}")
        print(f"✅ 完成！")
        print(f"映射文件已保存到: {mapping_path}")
        print(f"\n下一步：")
        print(f"  运行 merge_hpo_chinese.py 来合并翻译到HPO数据")
        print(f"{'='*60}\n")
        
        return mapping_path


def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description='HPO中文翻译字典下载工具')
    parser.add_argument('--output-dir', type=str, help='输出目录（默认：提取结果/hpo/翻译字典）')
    
    args = parser.parse_args()
    
    downloader = HPODictDownloader()
    
    if args.output_dir:
        downloader.dict_dir = Path(args.output_dir)
        downloader.dict_dir.mkdir(parents=True, exist_ok=True)
    
    try:
        mapping_path = downloader.download_and_convert()
        
        if mapping_path:
            print(f"\n✅ 成功！映射文件: {mapping_path}")
            print(f"现在可以运行 merge_hpo_chinese.py 来合并翻译")
        else:
            print(f"\n❌ 失败！请检查网络连接或资源URL")
            sys.exit(1)
            
    except KeyboardInterrupt:
        print(f"\n\n用户中断")
        sys.exit(0)
    except Exception as e:
        print(f"\n❌ 发生错误: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == '__main__':
    main()

