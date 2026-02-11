#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
HPO数据中文翻译合并脚本
将中文翻译合并到HPO数据中

支持方式：
1. 从外部翻译文件加载（CSV格式：HPO编码,中文名称）
2. 手动维护翻译字典
3. 支持逐步翻译（先翻译部分，后续补充）
"""

import os
import sys
import pandas as pd
import json
import requests
from pathlib import Path
from typing import Dict, Optional, Set, List, Tuple
from concurrent.futures import ThreadPoolExecutor, as_completed
from threading import Lock

# 添加脚本目录到路径
script_dir = Path(__file__).parent
sys.path.insert(0, str(script_dir.parent.parent))


class HPChineseMerger:
    """HPO 中文翻译合并器"""
    
    def __init__(self):
        """初始化合并器"""
        self.script_dir = Path(__file__).parent
        # 脚本在: docs/AI医生/知识内容提取/提取脚本/merge_hpo_chinese.py
        # 提取结果在: docs/AI医生/知识内容提取/提取结果/hpo/
        self.output_dir = self.script_dir.parent / "提取结果" / "hpo"
        self.source_dir = self.script_dir.parent / "源数据"
        
        # 默认文件路径
        self.hpo_data_path = self.output_dir / "hpo_data.csv"
        self.chinese_mapping_path = self.output_dir / "hpo_chinese_mapping.csv"
        self.translation_template_path = self.output_dir / "hpo_translation_template.csv"
        self.translation_cache_path = self.output_dir / "translation_cache.json"
        self.ai_config_path = self.script_dir / "ai_translation_config.json"
        
        # 内置常用术语翻译字典（可以逐步扩充）
        self.builtin_translations = {
            # 示例：添加一些常用术语的翻译
            "All": "全部",
            "Abnormality of body height": "身高异常",
            "Multicystic kidney dysplasia": "多囊肾发育不良",
            "Mode of inheritance": "遗传方式",
            "Autosomal dominant inheritance": "常染色体显性遗传",
            "Autosomal recessive inheritance": "常染色体隐性遗传",
            # 可以继续添加更多标准翻译
        }
        
        # 翻译缓存（避免重复翻译）
        self.translation_cache = self._load_translation_cache()
        self.cache_lock = Lock()  # 用于线程安全的缓存访问
        
        # AI翻译配置（可选，从配置文件加载）
        self.use_ai_translation = False
        self.ai_translation_api_key = None
        self.ai_translation_api_url = None
        self.ai_translation_api_type = "openai"  # openai, custom, ollama
        self.ai_translation_timeout = 30.0
        self.ai_translation_model = None
        self.ai_max_workers = 5  # 并发线程数（可根据服务器性能调整）
        
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
            return
        
        try:
            with open(self.ai_config_path, 'r', encoding='utf-8') as f:
                config = json.load(f)
            
            # 只有enabled为true时才加载配置
            if config.get("enabled", False):
                self.use_ai_translation = True
                self.ai_translation_api_url = config.get("api_url")
                self.ai_translation_api_type = config.get("api_type", "openai")
                self.ai_translation_api_key = config.get("api_key")
                self.ai_translation_model = config.get("model")
                self.ai_translation_timeout = float(config.get("timeout", 30.0))
                self.ai_max_workers = int(config.get("max_workers", 5))
                
                # 验证必需配置
                if not self.ai_translation_api_url:
                    print(f"⚠️  配置文件中的 api_url 未设置，AI翻译功能将被禁用")
                    self.use_ai_translation = False
        except Exception as e:
            print(f"⚠️  加载AI翻译配置文件失败: {e}")
            print(f"  配置文件路径: {self.ai_config_path}")
    
    def _translate_with_dict(self, text: str, translation_mapping: Dict[str, Dict[str, str]], 
                           hpo_data_df: Optional[pd.DataFrame] = None) -> Optional[str]:
        """
        使用字典查找翻译（基于已有的标准名称翻译）
        
        Args:
            text: 要翻译的文本
            translation_mapping: 翻译映射字典 {hpo_code: {'标准名称（中文）': ...}}
            hpo_data_df: HPO数据DataFrame（用于查找英文名称对应的中文翻译）
            
        Returns:
            翻译结果，如果找不到返回None
        """
        if not text or not text.strip():
            return None
        
        text = text.strip()
        
        # 1. 先检查缓存
        if text in self.translation_cache:
            return self.translation_cache[text]
        
        # 2. 检查内置字典
        if text in self.builtin_translations:
            result = self.builtin_translations[text]
            self.translation_cache[text] = result
            return result
        
        # 3. 在HPO数据中查找（如果提供了DataFrame）
        if hpo_data_df is not None:
            # 精确匹配标准名称（英文）
            matching_rows = hpo_data_df[hpo_data_df['标准名称（英文）'].str.strip() == text]
            if not matching_rows.empty:
                hpo_code = matching_rows.iloc[0]['HPO编码'].strip()
                # 标准化HPO编码
                if not hpo_code.startswith('HP:'):
                    if hpo_code.startswith('HP_'):
                        hpo_code = hpo_code.replace('_', ':', 1)
                    elif hpo_code.startswith('HP'):
                        hpo_code = 'HP:' + hpo_code[2:].lstrip(':')
                
                # 查找该编码的中文翻译
                if hpo_code in translation_mapping:
                    trans_dict = translation_mapping[hpo_code]
                    if '标准名称（中文）' in trans_dict:
                        result = trans_dict['标准名称（中文）']
                        self.translation_cache[text] = result
                        return result
            
            # 模糊匹配（包含关系）
            for idx, row in hpo_data_df.iterrows():
                eng_name = str(row.get('标准名称（英文）', '')).strip()
                if eng_name and (text.lower() in eng_name.lower() or eng_name.lower() in text.lower()):
                    hpo_code = str(row.get('HPO编码', '')).strip()
                    if not hpo_code.startswith('HP:'):
                        if hpo_code.startswith('HP_'):
                            hpo_code = hpo_code.replace('_', ':', 1)
                        elif hpo_code.startswith('HP'):
                            hpo_code = 'HP:' + hpo_code[2:].lstrip(':')
                    
                    if hpo_code in translation_mapping:
                        trans_dict = translation_mapping[hpo_code]
                        if '标准名称（中文）' in trans_dict:
                            result = trans_dict['标准名称（中文）']
                            self.translation_cache[text] = result
                            return result
        
        return None
    
    def _translate_with_ai(self, text: str) -> Optional[str]:
        """
        使用AI翻译（调用服务器部署的模型）
        
        支持多种API格式：
        1. OpenAI兼容格式
        2. Ollama格式
        3. 自定义格式
        
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
            print(f"⚠️  AI翻译API地址未配置")
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

英文术语：{text}

中文翻译："""
        return prompt
    
    def _call_openai_api(self, prompt: str) -> Optional[str]:
        """
        调用OpenAI兼容格式的API
        
        Args:
            prompt: 提示词
            
        Returns:
            翻译结果
        """
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
                # OpenAI格式：result["choices"][0]["message"]["content"]
                if "choices" in result and len(result["choices"]) > 0:
                    return result["choices"][0]["message"]["content"]
                # 其他可能的格式
                elif "response" in result:
                    return result["response"]
                elif "content" in result:
                    return result["content"]
                else:
                    print(f"⚠️  无法解析API响应: {result}")
                    return None
            else:
                print(f"⚠️  API调用失败: HTTP {response.status_code}, {response.text}")
                return None
                
        except requests.exceptions.Timeout:
            print(f"⚠️  API调用超时（{self.ai_translation_timeout}秒）")
            return None
        except Exception as e:
            print(f"⚠️  API调用异常: {e}")
            return None
    
    def _call_ollama_api(self, prompt: str) -> Optional[str]:
        """
        调用Ollama格式的API
        
        Args:
            prompt: 提示词
            
        Returns:
            翻译结果
        """
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
                # Ollama格式：result["response"]
                if "response" in result:
                    return result["response"]
                else:
                    print(f"⚠️  无法解析API响应: {result}")
                    return None
            else:
                print(f"⚠️  API调用失败: HTTP {response.status_code}, {response.text}")
                return None
                
        except requests.exceptions.Timeout:
            print(f"⚠️  API调用超时（{self.ai_translation_timeout}秒）")
            return None
        except Exception as e:
            print(f"⚠️  API调用异常: {e}")
            return None
    
    def _call_custom_api(self, prompt: str, original_text: str) -> Optional[str]:
        """
        调用自定义格式的API
        
        Args:
            prompt: 提示词
            original_text: 原始文本
            
        Returns:
            翻译结果
        """
        headers = {
            "Content-Type": "application/json"
        }
        
        if self.ai_translation_api_key:
            headers["Authorization"] = f"Bearer {self.ai_translation_api_key}"
        
        # 自定义格式：根据您的API调整
        payload = {
            "text": original_text,
            "prompt": prompt,
            "target_lang": "zh-CN"
        }
        
        # 如果有模型名称，添加到payload
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
                # 尝试多种可能的响应格式
                if "translation" in result:
                    return result["translation"]
                elif "response" in result:
                    return result["response"]
                elif "content" in result:
                    return result["content"]
                elif "text" in result:
                    return result["text"]
                else:
                    # 如果响应是字符串，直接返回
                    if isinstance(result, str):
                        return result
                    print(f"⚠️  无法解析API响应，请检查响应格式: {result}")
                    return None
            else:
                print(f"⚠️  API调用失败: HTTP {response.status_code}, {response.text}")
                return None
                
        except requests.exceptions.Timeout:
            print(f"⚠️  API调用超时（{self.ai_translation_timeout}秒）")
            return None
        except Exception as e:
            print(f"⚠️  API调用异常: {e}")
            return None
    
    def _batch_translate_with_ai(self, df: pd.DataFrame, tasks: List[Tuple[int, str, str, str]]) -> Tuple[int, int]:
        """
        批量并发翻译（使用线程池）
        
        Args:
            df: HPO数据DataFrame
            tasks: 翻译任务列表 [(idx, field_type, text, original_text), ...]
            
        Returns:
            (matched_synonym_count, matched_comment_count) 匹配计数
        """
        def translate_task(task: Tuple[int, str, str, str]) -> Tuple[int, str, str, Optional[str]]:
            """单个翻译任务"""
            idx, field_type, text, original_text = task
            try:
                if field_type in ['exact_synonyms', 'related_synonyms']:
                    result = self._translate_synonyms(text, {}, None, use_ai=True)
                else:  # comment
                    result = self._translate_text(text, {}, None, use_ai=True)
                return (idx, field_type, original_text, result)
            except Exception as e:
                print(f"⚠️  翻译失败 (idx={idx}, {field_type}): {e}")
                return (idx, field_type, original_text, None)
        
        # 使用线程池并发翻译
        completed = 0
        synonym_count = 0
        comment_count = 0
        
        with ThreadPoolExecutor(max_workers=self.ai_max_workers) as executor:
            futures = {executor.submit(translate_task, task): task for task in tasks}
            
            for future in as_completed(futures):
                completed += 1
                try:
                    idx, field_type, original_text, result = future.result()
                    
                    if result:
                        if field_type == 'exact_synonyms':
                            df.at[idx, '精确同义词（中文）'] = result
                            synonym_count += 1
                        elif field_type == 'related_synonyms':
                            df.at[idx, '相关同义词（中文）'] = result
                            synonym_count += 1
                        elif field_type == 'comment':
                            df.at[idx, '注释（中文）'] = result
                            comment_count += 1
                except Exception as e:
                    print(f"⚠️  处理翻译结果失败: {e}")
                
                # 显示进度
                if completed % 10 == 0 or completed == len(tasks):
                    print(f"  AI翻译进度: {completed}/{len(tasks)} ({completed/len(tasks)*100:.1f}%)")
        
        return (synonym_count, comment_count)
    
    def _translate_text(self, text: str, translation_mapping: Dict[str, Dict[str, str]], 
                       hpo_data_df: Optional[pd.DataFrame] = None, use_ai: bool = False) -> Optional[str]:
        """
        翻译文本（优先使用字典，可选AI翻译）
        
        Args:
            text: 要翻译的文本
            translation_mapping: 翻译映射字典
            hpo_data_df: HPO数据DataFrame（用于查找翻译）
            use_ai: 是否使用AI翻译（如果字典找不到）
            
        Returns:
            翻译结果
        """
        # 先尝试字典翻译
        result = self._translate_with_dict(text, translation_mapping, hpo_data_df)
        if result:
            return result
        
        # 如果字典找不到且允许使用AI，尝试AI翻译
        if use_ai:
            result = self._translate_with_ai(text)
            if result:
                return result
        
        return None
    
    def _translate_synonyms(self, synonyms_str: str, translation_mapping: Dict[str, Dict[str, str]], 
                           hpo_data_df: Optional[pd.DataFrame] = None, use_ai: bool = False) -> str:
        """
        翻译同义词（多个用分号分隔）
        
        Args:
            synonyms_str: 同义词字符串（多个用分号分隔）
            translation_mapping: 翻译映射字典
            hpo_data_df: HPO数据DataFrame（用于查找翻译）
            use_ai: 是否使用AI翻译
            
        Returns:
            翻译后的同义词字符串
        """
        if not synonyms_str or not synonyms_str.strip():
            return ''
        
        synonyms = [s.strip() for s in synonyms_str.split(';') if s.strip()]
        translated = []
        
        for synonym in synonyms:
            translation = self._translate_text(synonym, translation_mapping, hpo_data_df, use_ai)
            if translation:
                translated.append(translation)
            else:
                # 如果找不到翻译，保留原文（或可以标记为待翻译）
                translated.append(synonym)
        
        return '; '.join(translated)
        
    def load_chinese_mapping(self, mapping_path: str = None) -> Dict[str, Dict[str, str]]:
        """
        加载中文翻译映射（支持多个字段）
        
        Args:
            mapping_path: 映射文件路径（CSV格式：HPO编码,标准名称（中文）,定义（中文）等）
            
        Returns:
            映射字典 {hpo_code: {'标准名称（中文）': ..., '定义（中文）': ..., ...}}
        """
        if mapping_path is None:
            mapping_path = self.chinese_mapping_path
        else:
            mapping_path = Path(mapping_path)
            if not mapping_path.is_absolute():
                mapping_path = self.output_dir / mapping_path
        
        mapping_path = Path(mapping_path)
        if not mapping_path.exists():
            print(f"⚠️  翻译映射文件不存在: {mapping_path}")
            print(f"   将使用内置翻译字典和空映射")
            return {}
        
        print(f"正在加载中文映射: {mapping_path}")
        
        mapping = {}
        try:
            df = pd.read_csv(mapping_path, encoding='utf-8-sig')
            print(f"✓ 读取映射文件，共 {len(df)} 条记录")
            
            # 自动识别列名
            code_col = None
            chinese_name_col = None
            chinese_def_col = None
            
            # 遍历所有列，识别代码列和中文列
            for col in df.columns:
                col_lower = str(col).lower()
                # 识别代码列
                if any(kw in col_lower for kw in ['code', '编码', 'hpo', 'id']):
                    code_col = col
                # 识别"标准名称（中文）"列
                elif ('中文' in col or 'chinese' in col_lower) and \
                     ('名称' in col or 'name' in col_lower or 'label' in col_lower) and \
                     '定义' not in col and 'definition' not in col_lower and \
                     '英文' not in col and 'english' not in col_lower:
                    chinese_name_col = col
                # 识别"定义（中文）"列
                elif ('中文' in col or 'chinese' in col_lower) and \
                     ('定义' in col or 'definition' in col_lower) and \
                     '英文' not in col and 'english' not in col_lower:
                    chinese_def_col = col
            
            if code_col is None:
                raise ValueError(f"无法识别代码列。请确保文件包含'HPO编码'列")
            
            if chinese_name_col is None:
                raise ValueError(f"无法识别'标准名称（中文）'列。请确保文件包含该列")
            
            print(f"  代码列: {code_col}")
            print(f"  标准名称（中文）列: {chinese_name_col}")
            if chinese_def_col:
                print(f"  定义（中文）列: {chinese_def_col}")
            
            # 构建映射字典
            for _, row in df.iterrows():
                code = str(row[code_col]).strip()
                
                if not code or code == 'nan':
                    continue
                
                # 标准化HPO编码格式
                if not code.startswith('HP:'):
                    if code.startswith('HP_'):
                        code = code.replace('_', ':', 1)
                    elif code.startswith('HP'):
                        code = 'HP:' + code[2:].lstrip(':')
                
                # 初始化该编码的翻译字典
                if code not in mapping:
                    mapping[code] = {}
                
                # 加载标准名称（中文）
                if chinese_name_col:
                    chinese_name = str(row[chinese_name_col]).strip()
                    if chinese_name and chinese_name != 'nan':
                        mapping[code]['标准名称（中文）'] = chinese_name
                
                # 加载定义（中文）
                if chinese_def_col:
                    chinese_def = str(row[chinese_def_col]).strip()
                    if chinese_def and chinese_def != 'nan':
                        mapping[code]['定义（中文）'] = chinese_def
            
            # 统计加载的翻译数量
            name_count = sum(1 for v in mapping.values() if '标准名称（中文）' in v)
            def_count = sum(1 for v in mapping.values() if '定义（中文）' in v)
            print(f"✓ 成功加载 {len(mapping)} 条HPO编码的翻译映射")
            print(f"  - 标准名称（中文）: {name_count} 条")
            if chinese_def_col:
                print(f"  - 定义（中文）: {def_count} 条")
            
        except Exception as e:
            print(f"❌ 加载映射文件失败: {e}")
            import traceback
            traceback.print_exc()
            return {}
        
        return mapping
    
    def merge_chinese_translations(self, 
                                   hpo_data_path: str = None,
                                   mapping_path: str = None,
                                   output_path: str = None,
                                   use_builtin: bool = True) -> pd.DataFrame:
        """
        合并中文翻译到HPO数据
        
        Args:
            hpo_data_path: HPO数据文件路径
            mapping_path: 中文翻译映射文件路径
            output_path: 输出文件路径（可选）
            use_builtin: 是否使用内置翻译字典
            
        Returns:
            合并后的DataFrame
        """
        # 加载HPO数据
        if hpo_data_path is None:
            hpo_data_path = self.hpo_data_path
        else:
            hpo_data_path = Path(hpo_data_path)
            if not hpo_data_path.is_absolute():
                hpo_data_path = self.output_dir / hpo_data_path
        
        hpo_data_path = Path(hpo_data_path)
        if not hpo_data_path.exists():
            raise FileNotFoundError(f"HPO数据文件不存在: {hpo_data_path}")
        
        print(f"\n正在加载HPO数据: {hpo_data_path}")
        df = pd.read_csv(hpo_data_path, encoding='utf-8-sig')
        print(f"✓ 读取HPO数据，共 {len(df)} 条记录")
        
        # 加载中文翻译映射
        translation_mapping = {}
        if mapping_path or self.chinese_mapping_path.exists():
            translation_mapping = self.load_chinese_mapping(mapping_path)
        
        # 合并内置翻译
        if use_builtin:
            print(f"\n使用内置翻译字典（{len(self.builtin_translations)} 条）")
            # 通过标准名称匹配
            for idx, row in df.iterrows():
                english_name = str(row.get('标准名称（英文）', '')).strip()
                if english_name and english_name in self.builtin_translations:
                    hpo_code = str(row.get('HPO编码', '')).strip()
                    if hpo_code:
                        # 标准化HPO编码格式
                        if not hpo_code.startswith('HP:'):
                            if hpo_code.startswith('HP_'):
                                hpo_code = hpo_code.replace('_', ':', 1)
                            elif hpo_code.startswith('HP'):
                                hpo_code = 'HP:' + hpo_code[2:].lstrip(':')
                        
                        if hpo_code not in translation_mapping:
                            translation_mapping[hpo_code] = {}
                        if '标准名称（中文）' not in translation_mapping[hpo_code]:
                            translation_mapping[hpo_code]['标准名称（中文）'] = self.builtin_translations[english_name]
        
        # 添加中文翻译列
        print(f"\n正在合并中文翻译...")
        
        # 初始化中文列
        if '标准名称（中文）' not in df.columns:
            df['标准名称（中文）'] = ''
        if '定义（中文）' not in df.columns:
            df['定义（中文）'] = ''
        if '精确同义词（中文）' not in df.columns:
            df['精确同义词（中文）'] = ''
        if '相关同义词（中文）' not in df.columns:
            df['相关同义词（中文）'] = ''
        if '注释（中文）' not in df.columns:
            df['注释（中文）'] = ''
        
        matched_name_count = 0
        matched_def_count = 0
        matched_synonym_count = 0
        matched_comment_count = 0
        unmatched_codes = []
        
        print(f"正在翻译 {len(df)} 条记录...")
        
        # 第一步：先处理映射文件中的翻译（标准名称和定义）
        for idx, row in df.iterrows():
            hpo_code = str(row.get('HPO编码', '')).strip()
            
            # 标准化HPO编码
            if not hpo_code.startswith('HP:'):
                if hpo_code.startswith('HP_'):
                    hpo_code = hpo_code.replace('_', ':', 1)
                elif hpo_code.startswith('HP'):
                    hpo_code = 'HP:' + hpo_code[2:].lstrip(':')
            
            # 查找翻译
            if hpo_code in translation_mapping:
                trans_dict = translation_mapping[hpo_code]
                
                # 合并标准名称（中文）
                if '标准名称（中文）' in trans_dict:
                    df.at[idx, '标准名称（中文）'] = trans_dict['标准名称（中文）']
                    matched_name_count += 1
                
                # 合并定义（中文）
                if '定义（中文）' in trans_dict:
                    df.at[idx, '定义（中文）'] = trans_dict['定义（中文）']
                    matched_def_count += 1
                
                # 如果至少有一个翻译，就不算未匹配
                if '标准名称（中文）' not in trans_dict:
                    unmatched_codes.append(hpo_code)
            else:
                unmatched_codes.append(hpo_code)
        
        # 第二步：收集需要AI翻译的内容（并发处理）
        if self.use_ai_translation:
            print(f"\n正在收集需要AI翻译的内容...")
            
            # 优化：预先构建英文名称到中文翻译的快速查找字典（避免重复遍历DataFrame）
            print(f"  正在构建快速查找字典...")
            eng_to_chinese_dict = {}
            # 从DataFrame构建：英文名称 -> 中文翻译
            for idx, row in df.iterrows():
                hpo_code = str(row.get('HPO编码', '')).strip()
                # 标准化HPO编码
                if not hpo_code.startswith('HP:'):
                    if hpo_code.startswith('HP_'):
                        hpo_code = hpo_code.replace('_', ':', 1)
                    elif hpo_code.startswith('HP'):
                        hpo_code = 'HP:' + hpo_code[2:].lstrip(':')
                
                if hpo_code in translation_mapping:
                    trans_dict = translation_mapping[hpo_code]
                    if '标准名称（中文）' in trans_dict:
                        eng_name = str(row.get('标准名称（英文）', '')).strip()
                        if eng_name:
                            eng_to_chinese_dict[eng_name.lower()] = trans_dict['标准名称（中文）']
            
            # 添加内置字典
            for eng, chn in self.builtin_translations.items():
                eng_to_chinese_dict[eng.lower()] = chn
            
            print(f"  快速查找字典已构建，共 {len(eng_to_chinese_dict)} 条")
            
            translation_tasks = []  # [(idx, field_type, text, original_text), ...]
            processed = 0
            
            for idx, row in df.iterrows():
                processed += 1
                if processed % 1000 == 0:
                    print(f"  收集进度: {processed}/{len(df)} ({processed/len(df)*100:.1f}%)")
                
                # 收集同义词翻译任务（快速检查，不遍历DataFrame）
                exact_synonyms = str(row.get('精确同义词（多个用分号分隔）', '')).strip()
                if exact_synonyms:
                    # 快速检查：同义词是否在字典中
                    synonyms_list = [s.strip() for s in exact_synonyms.split(';') if s.strip()]
                    needs_ai = False
                    for syn in synonyms_list:
                        syn_lower = syn.lower()
                        if syn_lower not in eng_to_chinese_dict and f"ai_{syn}" not in self.translation_cache:
                            needs_ai = True
                            break
                    if needs_ai:
                        translation_tasks.append((idx, 'exact_synonyms', exact_synonyms, exact_synonyms))
                
                related_synonyms = str(row.get('相关同义词（多个用分号分隔）', '')).strip()
                if related_synonyms:
                    synonyms_list = [s.strip() for s in related_synonyms.split(';') if s.strip()]
                    needs_ai = False
                    for syn in synonyms_list:
                        syn_lower = syn.lower()
                        if syn_lower not in eng_to_chinese_dict and f"ai_{syn}" not in self.translation_cache:
                            needs_ai = True
                            break
                    if needs_ai:
                        translation_tasks.append((idx, 'related_synonyms', related_synonyms, related_synonyms))
                
                # 收集注释翻译任务（注释通常较长，直接使用AI）
                comment = str(row.get('注释', '')).strip()
                if comment and len(comment) > 10:  # 只翻译较长的注释
                    # 检查缓存
                    cache_key = f"ai_{comment}"
                    if cache_key not in self.translation_cache:
                        translation_tasks.append((idx, 'comment', comment, comment))
            
            # 并发翻译
            if translation_tasks:
                print(f"  发现 {len(translation_tasks)} 项需要AI翻译，使用 {self.ai_max_workers} 个并发线程...")
                ai_synonym_count, ai_comment_count = self._batch_translate_with_ai(df, translation_tasks)
                matched_synonym_count += ai_synonym_count
                matched_comment_count += ai_comment_count
        else:
            # 不使用AI，只使用字典翻译
            for idx, row in df.iterrows():
                exact_synonyms = str(row.get('精确同义词（多个用分号分隔）', '')).strip()
                if exact_synonyms:
                    translated_synonyms = self._translate_synonyms(exact_synonyms, translation_mapping, 
                                                                  hpo_data_df=df, use_ai=False)
                    if translated_synonyms and translated_synonyms != exact_synonyms:
                        df.at[idx, '精确同义词（中文）'] = translated_synonyms
                        matched_synonym_count += 1
                
                related_synonyms = str(row.get('相关同义词（多个用分号分隔）', '')).strip()
                if related_synonyms:
                    translated_related = self._translate_synonyms(related_synonyms, translation_mapping,
                                                                  hpo_data_df=df, use_ai=False)
                    if translated_related and translated_related != related_synonyms:
                        df.at[idx, '相关同义词（中文）'] = translated_related
                        matched_synonym_count += 1
                
                comment = str(row.get('注释', '')).strip()
                if comment:
                    translated_comment = self._translate_text(comment, translation_mapping, 
                                                             hpo_data_df=df, use_ai=False)
                    if translated_comment:
                        df.at[idx, '注释（中文）'] = translated_comment
                        matched_comment_count += 1
        
        # 保存翻译缓存
        self._save_translation_cache()
        
        print(f"✓ 成功匹配标准名称（中文）: {matched_name_count} 条（{matched_name_count/len(df)*100:.2f}%）")
        print(f"✓ 成功匹配定义（中文）: {matched_def_count} 条（{matched_def_count/len(df)*100:.2f}%）")
        print(f"✓ 成功翻译同义词: {matched_synonym_count} 条")
        print(f"✓ 成功翻译注释: {matched_comment_count} 条")
        print(f"⚠️  未匹配 {len(unmatched_codes)} 条记录")
        
        # 保存结果
        if output_path is None:
            output_path = self.output_dir / "hpo_data_with_chinese.csv"
        else:
            output_path = Path(output_path)
            if not output_path.is_absolute():
                output_path = self.output_dir / output_path
        
        print(f"\n正在保存结果到: {output_path}")
        df.to_csv(output_path, index=False, encoding='utf-8-sig')
        print(f"✓ 保存成功！")
        
        # 如果未匹配的记录较多，生成翻译模板
        if len(unmatched_codes) > 0 and len(unmatched_codes) <= 1000:
            self.generate_translation_template(df, unmatched_codes[:1000])
        
        return df
    
    def generate_translation_template(self, df: pd.DataFrame, unmatched_codes: list):
        """
        生成翻译模板文件，方便用户手动添加翻译
        
        Args:
            df: HPO数据DataFrame
            unmatched_codes: 未匹配的HPO编码列表
        """
        template_data = []
        
        for code in unmatched_codes:
            row = df[df['HPO编码'].str.strip() == code]
            if not row.empty:
                row = row.iloc[0]
                template_data.append({
                    'HPO编码': code,
                    '标准名称（英文）': row.get('标准名称（英文）', ''),
                    '定义（英文）': row.get('定义', '')[:200] if pd.notna(row.get('定义', '')) else '',  # 截取前200字符
                    '标准名称（中文）': '',  # 待填写
                    '定义（中文）': '',  # 待填写
                })
        
        if template_data:
            template_df = pd.DataFrame(template_data)
            template_path = self.translation_template_path
            
            print(f"\n正在生成翻译模板: {template_path}")
            template_df.to_csv(template_path, index=False, encoding='utf-8-sig')
            print(f"✓ 翻译模板已生成，共 {len(template_data)} 条待翻译记录")
            print(f"  请编辑此文件，填写'标准名称（中文）'和'定义（中文）'列")
            print(f"  填写完成后，将此文件重命名为 'hpo_chinese_mapping.csv' 并重新运行此脚本")
    
    def create_empty_mapping_template(self, count: int = 100):
        """
        创建空的翻译映射模板文件
        
        Args:
            count: 要包含的记录数量（默认100条，按HPO编码排序）
        """
        hpo_data_path = self.hpo_data_path
        if not hpo_data_path.exists():
            print(f"❌ HPO数据文件不存在: {hpo_data_path}")
            return
        
        print(f"正在读取HPO数据...")
        df = pd.read_csv(hpo_data_path, encoding='utf-8-sig')
        
        # 取前count条记录
        template_df = df.head(count)[['HPO编码', '标准名称（英文）', '定义']].copy()
        template_df['标准名称（中文）'] = ''
        template_df['定义（中文）'] = ''
        
        # 重命名列
        template_df.columns = ['HPO编码', '标准名称（英文）', '定义（英文）', '标准名称（中文）', '定义（中文）']
        
        template_path = self.translation_template_path
        print(f"\n正在创建翻译模板: {template_path}")
        template_df.to_csv(template_path, index=False, encoding='utf-8-sig')
        print(f"✓ 翻译模板已创建，共 {len(template_df)} 条记录")
        print(f"  请编辑此文件，填写'标准名称（中文）'和'定义（中文）'列")
        print(f"  填写完成后，将此文件重命名为 'hpo_chinese_mapping.csv' 并重新运行此脚本")


def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description='HPO数据中文翻译合并工具')
    parser.add_argument('--hpo-data', type=str, help='HPO数据文件路径（默认：hpo_data.csv）')
    parser.add_argument('--mapping', type=str, help='中文翻译映射文件路径（默认：hpo_chinese_mapping.csv）')
    parser.add_argument('--output', type=str, help='输出文件路径（默认：hpo_data_with_chinese.csv）')
    parser.add_argument('--no-builtin', action='store_true', help='不使用内置翻译字典')
    parser.add_argument('--use-ai', action='store_true', help='使用AI翻译补充同义词等字段（需要配置API）')
    parser.add_argument('--ai-api-url', type=str, help='AI翻译API地址（必需）')
    parser.add_argument('--ai-api-key', type=str, help='AI翻译API密钥（可选）')
    parser.add_argument('--ai-api-type', type=str, choices=['openai', 'ollama', 'custom'], 
                       default='openai', help='API类型：openai（OpenAI兼容）、ollama（Ollama）、custom（自定义）')
    parser.add_argument('--ai-model', type=str, help='模型名称（可选，根据API类型使用）')
    parser.add_argument('--ai-timeout', type=float, default=30.0, help='API超时时间（秒，默认30）')
    parser.add_argument('--create-template', type=int, metavar='N', 
                       help='创建空的翻译模板文件，包含前N条记录')
    
    args = parser.parse_args()
    
    merger = HPChineseMerger()
    
    # 配置AI翻译（命令行参数优先于配置文件）
    if args.use_ai:
        if not args.ai_api_url:
            print("❌ 错误：使用 --use-ai 时必须提供 --ai-api-url")
            sys.exit(1)
        
        merger.use_ai_translation = True
        merger.ai_translation_api_url = args.ai_api_url
        merger.ai_translation_api_type = args.ai_api_type
        merger.ai_translation_timeout = args.ai_timeout
        
        if args.ai_api_key:
            merger.ai_translation_api_key = args.ai_api_key
        
        if args.ai_model:
            merger.ai_translation_model = args.ai_model
        
        print(f"✓ 已启用AI翻译功能（命令行配置）")
        print(f"  API地址: {merger.ai_translation_api_url}")
        print(f"  API类型: {merger.ai_translation_api_type}")
        if merger.ai_translation_model:
            print(f"  模型: {merger.ai_translation_model}")
        print(f"  超时: {merger.ai_translation_timeout}秒")
    elif merger.use_ai_translation:
        # 如果从配置文件加载了配置，显示提示
        print(f"✓ 已启用AI翻译功能（从配置文件加载）")
        print(f"  API地址: {merger.ai_translation_api_url}")
        print(f"  API类型: {merger.ai_translation_api_type}")
        if merger.ai_translation_model:
            print(f"  模型: {merger.ai_translation_model}")
        print(f"  超时: {merger.ai_translation_timeout}秒")
    
    # 如果只是创建模板
    if args.create_template:
        merger.create_empty_mapping_template(args.create_template)
        return
    
    # 合并翻译
    try:
        df = merger.merge_chinese_translations(
            hpo_data_path=args.hpo_data,
            mapping_path=args.mapping,
            output_path=args.output,
            use_builtin=not args.no_builtin
        )
        
        print(f"\n✅ 翻译合并完成！")
        print(f"   总记录数: {len(df)}")
        print(f"   已翻译: {df['标准名称（中文）'].notna().sum()}")
        print(f"   未翻译: {df['标准名称（中文）'].isna().sum() + (df['标准名称（中文）'] == '').sum()}")
        
    except Exception as e:
        print(f"\n❌ 处理失败: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == '__main__':
    main()


