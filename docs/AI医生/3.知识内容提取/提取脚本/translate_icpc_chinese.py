#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
ICPC-2e数据中文翻译脚本
使用AI模型翻译ICPC-2e数据为中文

支持：
1. 从CSV文件读取ICPC-2e数据
2. 使用AI模型翻译Title和ShortTitle字段
3. 保存翻译结果到CSV文件
4. 支持翻译缓存，避免重复翻译
"""

import os
import sys
import pandas as pd
import json
import requests
from pathlib import Path
from typing import Dict, Optional
from concurrent.futures import ThreadPoolExecutor, as_completed
from threading import Lock

# 添加脚本目录到路径
script_dir = Path(__file__).parent
sys.path.insert(0, str(script_dir.parent.parent))


class ICPCChineseTranslator:
    """ICPC-2e 中文翻译器"""
    
    def __init__(self):
        """初始化翻译器"""
        self.script_dir = Path(__file__).parent
        # 脚本在: docs/AI医生/3.知识内容提取/提取脚本/
        # 源数据在: docs/AI医生/3.知识内容提取/源数据/ICPC-2e-v7.0/
        # 提取结果在: docs/AI医生/3.知识内容提取/提取结果/icpc/
        self.source_dir = self.script_dir.parent / "源数据" / "ICPC-2e-v7.0"
        self.output_dir = self.script_dir.parent / "提取结果" / "icpc"
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
        # 默认文件路径
        self.icpc_data_path = self.source_dir / "ICPC-2e-v7.0-Title-csv.txt"
        self.translation_cache_path = self.output_dir / "translation_cache.json"
        self.ai_config_path = self.script_dir / "ai_translation_config.json"
        
        # 翻译缓存（避免重复翻译）
        self.translation_cache = self._load_translation_cache()
        self.cache_lock = Lock()  # 用于线程安全的缓存访问
        
        # AI翻译配置（从配置文件加载）
        self.use_ai_translation = False
        self.ai_translation_api_key = None
        self.ai_translation_api_url = None
        self.ai_translation_api_type = "ollama"
        self.ai_translation_timeout = 30.0
        self.ai_translation_model = None
        self.ai_max_workers = 5  # 并发线程数
        
        # 从配置文件加载AI翻译配置
        self._load_ai_config()
    
    def _load_translation_cache(self) -> Dict[str, str]:
        """加载翻译缓存"""
        if self.translation_cache_path.exists():
            try:
                with open(self.translation_cache_path, 'r', encoding='utf-8') as f:
                    return json.load(f)
            except:
                return {}
        return {}
    
    def _save_translation_cache(self):
        """保存翻译缓存"""
        try:
            with open(self.translation_cache_path, 'w', encoding='utf-8') as f:
                json.dump(self.translation_cache, f, ensure_ascii=False, indent=2)
        except Exception as e:
            print(f"⚠️  保存翻译缓存失败: {e}")
    
    def _load_ai_config(self):
        """从配置文件加载AI翻译配置"""
        if not self.ai_config_path.exists():
            print(f"⚠️  AI翻译配置文件不存在: {self.ai_config_path}")
            print(f"   请创建配置文件或使用命令行参数配置")
            return
        
        try:
            with open(self.ai_config_path, 'r', encoding='utf-8') as f:
                config = json.load(f)
            
            # 只有enabled为true时才加载配置
            if config.get("enabled", False):
                self.use_ai_translation = True
                self.ai_translation_api_url = config.get("api_url")
                self.ai_translation_api_type = config.get("api_type", "ollama")
                self.ai_translation_api_key = config.get("api_key")
                self.ai_translation_model = config.get("model")
                self.ai_translation_timeout = float(config.get("timeout", 30.0))
                self.ai_max_workers = int(config.get("max_workers", 5))
                
                # 验证必需配置
                if not self.ai_translation_api_url:
                    print(f"⚠️  配置文件中的 api_url 未设置，AI翻译功能将被禁用")
                    self.use_ai_translation = False
                else:
                    print(f"✓ 已从配置文件加载AI翻译设置")
                    print(f"  API地址: {self.ai_translation_api_url}")
                    print(f"  API类型: {self.ai_translation_api_type}")
                    print(f"  模型: {self.ai_translation_model}")
                    print(f"  超时: {self.ai_translation_timeout}秒")
                    print(f"  并发线程数: {self.ai_max_workers}")
        except Exception as e:
            print(f"⚠️  加载AI翻译配置文件失败: {e}")
            print(f"  配置文件路径: {self.ai_config_path}")
    
    def _translate_with_ai(self, text: str) -> Optional[str]:
        """
        使用AI翻译（调用服务器部署的模型）
        
        Args:
            text: 要翻译的文本
            
        Returns:
            翻译结果，如果未配置或失败返回None
        """
        if not self.use_ai_translation:
            return None
        
        if not text or not text.strip():
            return None
        
        if not self.ai_translation_api_url:
            return None
        
        text = text.strip()
        
        # 检查缓存
        cache_key = f"ai_{text}"
        if cache_key in self.translation_cache:
            return self.translation_cache[cache_key]
        
        try:
            # 构建翻译提示词
            prompt = self._build_translation_prompt(text)
            
            # 根据API类型调用不同的接口
            if self.ai_translation_api_type == "openai":
                result = self._call_openai_api(prompt)
            elif self.ai_translation_api_type == "ollama":
                result = self._call_ollama_api(prompt)
            elif self.ai_translation_api_type == "custom":
                result = self._call_custom_api(prompt, text)
            else:
                print(f"⚠️  不支持的API类型: {self.ai_translation_api_type}")
                return None
            
            if result:
                # 清理翻译结果（移除可能的引号、多余空格等）
                result = result.strip().strip('"').strip("'")
                # 线程安全地更新缓存
                with self.cache_lock:
                    self.translation_cache[cache_key] = result
                return result
            else:
                return None
                
        except Exception as e:
            print(f"⚠️  AI翻译失败: {e}")
            return None
    
    def _build_translation_prompt(self, text: str) -> str:
        """
        构建翻译提示词
        
        Args:
            text: 要翻译的英文文本
            
        Returns:
            完整的提示词
        """
        prompt = f"""请将以下医学术语翻译成中文。要求：
1. 使用标准医学术语
2. 保持术语的准确性和专业性
3. 只返回中文翻译，不要添加任何解释或说明
4. 如果是多个术语用斜杠分隔，请保持相同的分隔方式

英文术语：{text}

中文翻译："""
        return prompt
    
    def _call_openai_api(self, prompt: str) -> Optional[str]:
        """调用OpenAI兼容格式的API"""
        headers = {
            "Content-Type": "application/json"
        }
        
        if self.ai_translation_api_key:
            headers["Authorization"] = f"Bearer {self.ai_translation_api_key}"
        
        payload = {
            "model": self.ai_translation_model or "gpt-3.5-turbo",
            "messages": [
                {
                    "role": "user",
                    "content": prompt
                }
            ],
            "temperature": 0.3,
            "max_tokens": 200
        }
        
        try:
            response = requests.post(
                self.ai_translation_api_url,
                headers=headers,
                json=payload,
                timeout=self.ai_translation_timeout
            )
            
            if response.status_code == 200:
                result = response.json()
                if "choices" in result and len(result["choices"]) > 0:
                    return result["choices"][0]["message"]["content"]
                elif "response" in result:
                    return result["response"]
                elif "content" in result:
                    return result["content"]
                else:
                    return None
            else:
                return None
                
        except requests.exceptions.Timeout:
            return None
        except Exception as e:
            return None
    
    def _call_ollama_api(self, prompt: str) -> Optional[str]:
        """调用Ollama格式的API"""
        payload = {
            "model": self.ai_translation_model or "llama2",
            "prompt": prompt,
            "stream": False,
            "options": {
                "temperature": 0.3,
                "num_predict": 200
            }
        }
        
        try:
            response = requests.post(
                self.ai_translation_api_url,
                json=payload,
                timeout=self.ai_translation_timeout
            )
            
            if response.status_code == 200:
                result = response.json()
                if "response" in result:
                    return result["response"]
                else:
                    return None
            else:
                return None
                
        except requests.exceptions.Timeout:
            return None
        except Exception as e:
            return None
    
    def _call_custom_api(self, prompt: str, original_text: str) -> Optional[str]:
        """调用自定义格式的API"""
        headers = {
            "Content-Type": "application/json"
        }
        
        if self.ai_translation_api_key:
            headers["Authorization"] = f"Bearer {self.ai_translation_api_key}"
        
        payload = {
            "text": original_text,
            "prompt": prompt,
            "target_lang": "zh-CN"
        }
        
        if self.ai_translation_model:
            payload["model"] = self.ai_translation_model
        
        try:
            response = requests.post(
                self.ai_translation_api_url,
                headers=headers,
                json=payload,
                timeout=self.ai_translation_timeout
            )
            
            if response.status_code == 200:
                result = response.json()
                if "translation" in result:
                    return result["translation"]
                elif "response" in result:
                    return result["response"]
                elif "content" in result:
                    return result["content"]
                elif "text" in result:
                    return result["text"]
                elif isinstance(result, str):
                    return result
                else:
                    return None
            else:
                return None
                
        except requests.exceptions.Timeout:
            return None
        except Exception as e:
            return None
    
    def load_icpc_data(self, data_path: str = None) -> pd.DataFrame:
        """
        加载ICPC-2e数据
        
        Args:
            data_path: 数据文件路径（默认：ICPC-2e-v7.0-Title-csv.txt）
            
        Returns:
            DataFrame
        """
        if data_path is None:
            data_path = self.icpc_data_path
        else:
            data_path = Path(data_path)
            if not data_path.is_absolute():
                data_path = self.source_dir / data_path
        
        data_path = Path(data_path)
        if not data_path.exists():
            raise FileNotFoundError(f"ICPC数据文件不存在: {data_path}")
        
        print(f"\n正在加载ICPC数据: {data_path}")
        
        # ICPC CSV文件使用分号分隔，且有注释行（以--开头）
        try:
            # 读取文件，跳过注释行
            lines = []
            with open(data_path, 'r', encoding='utf-8') as f:
                for line in f:
                    line = line.strip()
                    if line and not line.startswith('--') and not line.startswith('---'):
                        lines.append(line)
            
            # 解析CSV数据（分号分隔，双引号包围）
            data = []
            for line in lines:
                # 简单的CSV解析（处理分号和引号）
                parts = []
                current = ""
                in_quotes = False
                
                for char in line:
                    if char == '"':
                        in_quotes = not in_quotes
                    elif char == ';' and not in_quotes:
                        parts.append(current.strip())
                        current = ""
                    else:
                        current += char
                
                if current:
                    parts.append(current.strip())
                
                if len(parts) >= 3:
                    data.append({
                        'Code': parts[0].strip('"'),
                        'Title': parts[1].strip('"'),
                        'ShortTitle': parts[2].strip('"')
                    })
            
            df = pd.DataFrame(data)
            print(f"✓ 读取ICPC数据，共 {len(df)} 条记录")
            return df
            
        except Exception as e:
            print(f"❌ 加载数据失败: {e}")
            import traceback
            traceback.print_exc()
            raise
    
    def translate_dataframe(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        翻译DataFrame中的Title和ShortTitle字段
        
        Args:
            df: 原始DataFrame
            
        Returns:
            翻译后的DataFrame（添加Title_CN和ShortTitle_CN列）
        """
        if not self.use_ai_translation:
            print("❌ 错误：AI翻译未启用，请检查配置文件")
            return df
        
        # 添加中文列
        df['Title_CN'] = ''
        df['ShortTitle_CN'] = ''
        
        print(f"\n正在翻译 {len(df)} 条记录...")
        print(f"使用 {self.ai_max_workers} 个并发线程")
        
        # 准备翻译任务
        tasks = []
        for idx, row in df.iterrows():
            title = str(row.get('Title', '')).strip()
            short_title = str(row.get('ShortTitle', '')).strip()
            
            if title:
                tasks.append((idx, 'Title', title))
            if short_title:
                tasks.append((idx, 'ShortTitle', short_title))
        
        print(f"  共 {len(tasks)} 个翻译任务")
        
        # 并发翻译
        completed = 0
        success_count = 0
        
        def translate_task(task):
            """单个翻译任务"""
            idx, field_type, text = task
            try:
                result = self._translate_with_ai(text)
                return (idx, field_type, result)
            except Exception as e:
                print(f"⚠️  翻译失败 (idx={idx}, {field_type}): {e}")
                return (idx, field_type, None)
        
        with ThreadPoolExecutor(max_workers=self.ai_max_workers) as executor:
            futures = {executor.submit(translate_task, task): task for task in tasks}
            
            for future in as_completed(futures):
                completed += 1
                try:
                    idx, field_type, result = future.result()
                    
                    if result:
                        if field_type == 'Title':
                            df.at[idx, 'Title_CN'] = result
                        elif field_type == 'ShortTitle':
                            df.at[idx, 'ShortTitle_CN'] = result
                        success_count += 1
                except Exception as e:
                    print(f"⚠️  处理翻译结果失败: {e}")
                
                # 显示进度
                if completed % 10 == 0 or completed == len(tasks):
                    print(f"  翻译进度: {completed}/{len(tasks)} ({completed/len(tasks)*100:.1f}%) - 成功: {success_count}")
        
        # 保存翻译缓存
        self._save_translation_cache()
        
        print(f"\n✓ 翻译完成！")
        print(f"  总任务数: {len(tasks)}")
        print(f"  成功翻译: {success_count}")
        print(f"  失败: {len(tasks) - success_count}")
        
        # 统计翻译覆盖率
        title_translated = (df['Title_CN'] != '').sum()
        short_title_translated = (df['ShortTitle_CN'] != '').sum()
        print(f"\n翻译覆盖率：")
        print(f"  Title: {title_translated}/{len(df)} ({title_translated/len(df)*100:.2f}%)")
        print(f"  ShortTitle: {short_title_translated}/{len(df)} ({short_title_translated/len(df)*100:.2f}%)")
        
        return df
    
    def save_result(self, df: pd.DataFrame, output_path: str = None):
        """
        保存翻译结果
        
        Args:
            df: 翻译后的DataFrame
            output_path: 输出文件路径（默认：icpc_data_with_chinese.csv）
        """
        if output_path is None:
            output_path = self.output_dir / "icpc_data_with_chinese.csv"
        else:
            output_path = Path(output_path)
            if not output_path.is_absolute():
                output_path = self.output_dir / output_path
        
        print(f"\n正在保存结果到: {output_path}")
        df.to_csv(output_path, index=False, encoding='utf-8-sig')
        print(f"✓ 保存成功！")


def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description='ICPC-2e数据中文翻译工具')
    parser.add_argument('--input', type=str, help='ICPC数据文件路径（默认：ICPC-2e-v7.0-Title-csv.txt）')
    parser.add_argument('--output', type=str, help='输出文件路径（默认：icpc_data_with_chinese.csv）')
    parser.add_argument('--use-ai', action='store_true', help='使用AI翻译（需要配置API）')
    parser.add_argument('--ai-api-url', type=str, help='AI翻译API地址（必需）')
    parser.add_argument('--ai-api-key', type=str, help='AI翻译API密钥（可选）')
    parser.add_argument('--ai-api-type', type=str, choices=['openai', 'ollama', 'custom'], 
                       default='ollama', help='API类型：openai（OpenAI兼容）、ollama（Ollama）、custom（自定义）')
    parser.add_argument('--ai-model', type=str, help='模型名称（可选，根据API类型使用）')
    parser.add_argument('--ai-timeout', type=float, default=30.0, help='API超时时间（秒，默认30）')
    parser.add_argument('--ai-workers', type=int, default=5, help='并发线程数（默认5）')
    
    args = parser.parse_args()
    
    translator = ICPCChineseTranslator()
    
    # 配置AI翻译（命令行参数优先于配置文件）
    if args.use_ai or args.ai_api_url:
        if not args.ai_api_url:
            if not translator.use_ai_translation:
                print("❌ 错误：使用 --use-ai 时必须提供 --ai-api-url 或配置文件中设置")
                sys.exit(1)
        else:
            translator.use_ai_translation = True
            translator.ai_translation_api_url = args.ai_api_url
            translator.ai_translation_api_type = args.ai_api_type
            translator.ai_translation_timeout = args.ai_timeout
            translator.ai_max_workers = args.ai_workers
            
            if args.ai_api_key:
                translator.ai_translation_api_key = args.ai_api_key
            
            if args.ai_model:
                translator.ai_translation_model = args.ai_model
            
            print(f"✓ 已启用AI翻译功能（命令行配置）")
            print(f"  API地址: {translator.ai_translation_api_url}")
            print(f"  API类型: {translator.ai_translation_api_type}")
            if translator.ai_translation_model:
                print(f"  模型: {translator.ai_translation_model}")
            print(f"  超时: {translator.ai_translation_timeout}秒")
            print(f"  并发线程数: {translator.ai_max_workers}")
    
    if not translator.use_ai_translation:
        print("❌ 错误：AI翻译未启用")
        print("   请使用 --use-ai 和 --ai-api-url 参数，或配置 ai_translation_config.json 文件")
        sys.exit(1)
    
    # 加载数据
    try:
        df = translator.load_icpc_data(args.input)
        
        # 翻译数据
        df = translator.translate_dataframe(df)
        
        # 保存结果
        translator.save_result(df, args.output)
        
        print(f"\n✅ 翻译完成！")
        print(f"   总记录数: {len(df)}")
        print(f"   Title已翻译: {(df['Title_CN'] != '').sum()}")
        print(f"   ShortTitle已翻译: {(df['ShortTitle_CN'] != '').sum()}")
        
    except Exception as e:
        print(f"\n❌ 处理失败: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == '__main__':
    main()

