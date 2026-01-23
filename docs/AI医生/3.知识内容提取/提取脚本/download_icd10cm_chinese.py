#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
下载台湾健保署 ICD-10-CM 中文对照表
"""
import os
import sys
import requests
from pathlib import Path
from typing import Optional

# 添加脚本目录到路径
script_dir = Path(__file__).parent
sys.path.insert(0, str(script_dir.parent.parent))

class ICD10CMChineseDownloader:
    """ICD-10-CM 中文对照表下载器"""
    
    def __init__(self, output_dir: str = None):
        """
        初始化下载器
        
        Args:
            output_dir: 输出目录，默认在源数据目录下
        """
        if output_dir is None:
            # 默认输出到源数据目录
            self.output_dir = Path(__file__).parent.parent.parent / "源数据" / "icd10cm-chinese"
        else:
            self.output_dir = Path(output_dir)
        
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
        # 台湾健保署 ICD-10-CM 中文对照表下载页面
        self.base_url = "https://www.nhi.gov.tw"
        self.download_page = "https://www.nhi.gov.tw/ch/cp-6071-469da-3051-1.html"
        
    def download_file(self, url: str, filename: str) -> bool:
        """
        下载文件
        
        Args:
            url: 下载链接
            filename: 保存的文件名
            
        Returns:
            是否下载成功
        """
        output_path = self.output_dir / filename
        
        try:
            print(f"正在下载: {filename}")
            print(f"URL: {url}")
            
            headers = {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
            }
            
            response = requests.get(url, headers=headers, stream=True, timeout=30)
            response.raise_for_status()
            
            # 保存文件
            total_size = int(response.headers.get('content-length', 0))
            downloaded = 0
            
            with open(output_path, 'wb') as f:
                for chunk in response.iter_content(chunk_size=8192):
                    if chunk:
                        f.write(chunk)
                        downloaded += len(chunk)
                        if total_size > 0:
                            percent = (downloaded / total_size) * 100
                            print(f"\r  进度: {percent:.1f}% ({downloaded}/{total_size} bytes)", end='')
            
            print(f"\n  ✓ 下载完成: {output_path}")
            print(f"  文件大小: {output_path.stat().st_size / 1024 / 1024:.2f} MB")
            return True
            
        except requests.exceptions.RequestException as e:
            print(f"  ✗ 下载失败: {str(e)}")
            return False
        except Exception as e:
            print(f"  ✗ 错误: {str(e)}")
            return False
    
    def download_from_url(self, url: str, filename: str = None) -> bool:
        """
        从URL下载文件（需要手动提供下载链接）
        
        Args:
            url: 文件的直接下载链接
            filename: 保存的文件名，如果不提供则从URL提取
            
        Returns:
            是否下载成功
        """
        if filename is None:
            filename = url.split('/')[-1]
            # 如果文件名包含查询参数，需要清理
            if '?' in filename:
                filename = filename.split('?')[0]
        
        return self.download_file(url, filename)
    
    def print_download_instructions(self):
        """打印下载指引"""
        print("=" * 80)
        print("ICD-10-CM 中文对照表下载指引")
        print("=" * 80)
        print()
        print("由于台湾健保署网站需要手动访问，请按照以下步骤下载：")
        print()
        print("1. 访问台湾健保署 ICD-10-CM 中文版下载页面：")
        print(f"   {self.download_page}")
        print()
        print("2. 在页面上找到以下文件（2023年正式版）：")
        print("   - 2023年中文版 ICD-10-CM/PCS（正式版）")
        print("     格式：XLS 或 ODS（推荐下载 ODS 格式，约 3 MB）")
        print()
        print("3. 下载文件后，将文件放到以下目录：")
        print(f"   {self.output_dir}")
        print()
        print("4. 或者，如果你有直接下载链接，可以运行：")
        print("   python download_icd10cm_chinese.py --url <下载链接> --filename <文件名>")
        print()
        print("5. 文件命名建议：")
        print("   - icd10cm-chinese-2023.ods 或 icd10cm-chinese-2023.xls")
        print()
        print("=" * 80)
        print()

def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description='下载台湾健保署 ICD-10-CM 中文对照表')
    parser.add_argument('--url', type=str, help='直接下载链接')
    parser.add_argument('--filename', type=str, help='保存的文件名')
    parser.add_argument('--output-dir', type=str, help='输出目录')
    
    args = parser.parse_args()
    
    downloader = ICD10CMChineseDownloader(output_dir=args.output_dir)
    
    if args.url:
        # 如果有URL，直接下载
        filename = args.filename or None
        success = downloader.download_from_url(args.url, filename)
        if success:
            print("\n✓ 下载成功！")
        else:
            print("\n✗ 下载失败，请检查URL是否正确")
            sys.exit(1)
    else:
        # 否则打印下载指引
        downloader.print_download_instructions()

if __name__ == "__main__":
    main()

