#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
ICPC-2e-v7.0 XML文件解析和中文翻译脚本
解析XML文件并翻译所有需要翻译的内容

支持翻译的内容：
- preferred: 主要术语（Title）
- shortTitle: 短标题
- inclusion: 包含内容
- exclusion: 排除内容
- criteria: 标准
- consider: 考虑
- note: 注释
"""

import os
import sys
import json
import xml.etree.ElementTree as ET
import pandas as pd
import requests
from pathlib import Path
from typing import Dict, Optional, List, Set
from concurrent.futures import ThreadPoolExecutor, as_completed
from threading import Lock
import re

# 添加脚本目录到路径
script_dir = Path(__file__).parent
sys.path.insert(0, str(script_dir.parent.parent))


class ICPCXMLTranslator:
    """ICPC XML文件解析和翻译器"""
    
    def __init__(self):
        """初始化翻译器"""
        self.script_dir = Path(__file__).parent
        self.source_dir = self.script_dir.parent / "源数据" / "ICPC-2e-v7.0"
        self.output_dir = self.script_dir.parent / "提取结果" / "icpc"
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
        # 默认文件路径
        self.input_file = self.source_dir / "ICPC-2e-v7.0.xml"
        self.output_json = self.output_dir / "icpc_xml_chinese.json"
        self.output_csv = self.output_dir / "icpc_xml_chinese.csv"
        self.translation_cache_path = self.output_dir / "translation_cache.json"
        self.ai_config_path = self.script_dir / "ai_translation_config.json"
        
        # 需要翻译的Rubric类型
        self.translatable_rubrics = {
            'preferred',      # 主要术语
            'shortTitle',     # 短标题
            'inclusion',      # 包含内容
            'exclusion',      # 排除内容
            'criteria',      # 标准
            'consider',      # 考虑
            'note'           # 注释
        }
        
        # 翻译缓存
        self.translation_cache = self._load_translation_cache()
        self.cache_lock = Lock()
        
        # AI翻译配置
        self.use_ai_translation = False
        self.ai_translation_api_url = None
        self.ai_translation_api_type = "ollama"
        self.ai_translation_api_key = None
        self.ai_translation_timeout = 30.0
        self.ai_translation_model = None
        self.ai_max_workers = 5
        
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
            return
        
        try:
            with open(self.ai_config_path, 'r', encoding='utf-8') as f:
                config = json.load(f)
            
            if config.get("enabled", False):
                self.use_ai_translation = True
                self.ai_translation_api_url = config.get("api_url")
                self.ai_translation_api_type = config.get("api_type", "ollama")
                self.ai_translation_api_key = config.get("api_key")
                self.ai_translation_model = config.get("model")
                self.ai_translation_timeout = float(config.get("timeout", 30.0))
                self.ai_max_workers = int(config.get("max_workers", 5))
                
                if not self.ai_translation_api_url:
                    print(f"⚠️  配置文件中的 api_url 未设置，AI翻译功能将被禁用")
                    self.use_ai_translation = False
                else:
                    print(f"✓ 已从配置文件加载AI翻译设置")
                    print(f"  API地址: {self.ai_translation_api_url}")
                    print(f"  API类型: {self.ai_translation_api_type}")
                    print(f"  模型: {self.ai_translation_model}")
                    print(f"  超时: {self.ai_translation_timeout}秒")
                    print(f"  并发数: {self.ai_max_workers}")
        except Exception as e:
            print(f"⚠️  加载AI翻译配置文件失败: {e}")
    
    def _build_translation_prompt(self, text: str, context: str = "") -> str:
        """
        构建翻译提示词
        
        Args:
            text: 要翻译的文本
            context: 上下文信息（可选）
        """
        prompt = f"""请将以下医学术语翻译成中文。要求：
1. 使用标准医学术语
2. 保持术语的准确性和专业性
3. 只返回中文翻译，不要添加任何解释或说明
4. 如果是缩写（如NOS、NEC等），请保留原样或使用标准中文缩写
5. 如果是代码引用（如<Reference>-34</Reference>），请保留原样
"""
        if context:
            prompt += f"\n上下文：{context}\n"
        
        prompt += f"\n英文术语：{text}\n\n中文翻译："
        return prompt
    
    def _call_ollama_api(self, prompt: str) -> Optional[str]:
        """调用Ollama格式的API"""
        payload = {
            "model": self.ai_translation_model or "qwen2.5:14b",
            "prompt": prompt,
            "stream": False,
            "options": {
                "temperature": 0.3,
                "num_predict": 500
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
                    print(f"⚠️  无法解析API响应: {result}")
                    return None
            else:
                print(f"⚠️  API调用失败: HTTP {response.status_code}")
                return None
                
        except requests.exceptions.Timeout:
            print(f"⚠️  API调用超时（{self.ai_translation_timeout}秒）")
            return None
        except Exception as e:
            print(f"⚠️  API调用异常: {e}")
            return None
    
    def _translate_with_ai(self, text: str, context: str = "") -> Optional[str]:
        """
        使用AI翻译
        
        Args:
            text: 要翻译的文本
            context: 上下文信息
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
            prompt = self._build_translation_prompt(text, context)
            
            if self.ai_translation_api_type == "ollama":
                result = self._call_ollama_api(prompt)
            else:
                return None
            
            if result:
                result = result.strip().strip('"').strip("'")
                # 保留Reference标签
                result = re.sub(r'<Reference>([^<]+)</Reference>', r'<Reference>\1</Reference>', result)
                with self.cache_lock:
                    self.translation_cache[cache_key] = result
                return result
            else:
                return None
                
        except Exception as e:
            print(f"⚠️  AI翻译失败: {e}")
            return None
    
    def _parse_xml(self, xml_file: Path) -> List[Dict]:
        """
        解析XML文件
        
        Args:
            xml_file: XML文件路径
            
        Returns:
            解析后的数据列表
        """
        print(f"正在解析XML文件: {xml_file}")
        
        tree = ET.parse(xml_file)
        root = tree.getroot()
        
        # 构建代码到类信息的映射
        classes = {}
        chapters = {}
        components = {}
        
        # 第一遍：收集所有Class信息
        for class_elem in root.findall('.//Class'):
            code = class_elem.get('code')
            kind = class_elem.get('kind')
            
            if not code:
                continue
            
            class_info = {
                'code': code,
                'kind': kind,
                'super_class': None,
                'sub_classes': [],
                'rubrics': {}
            }
            
            # 获取SuperClass
            super_class = class_elem.find('SuperClass')
            if super_class is not None:
                class_info['super_class'] = super_class.get('code')
            
            # 获取SubClass
            for sub_class in class_elem.findall('SubClass'):
                class_info['sub_classes'].append(sub_class.get('code'))
            
            # 获取Rubrics
            for rubric in class_elem.findall('Rubric'):
                rubric_kind = rubric.get('kind')
                label_elem = rubric.find('Label')
                if label_elem is not None:
                    # 检查语言属性（支持xml:lang和lang属性）
                    lang = label_elem.get('{http://www.w3.org/XML/1998/namespace}lang') or label_elem.get('lang', 'en')
                    if lang == 'en':
                        label_text = label_elem.text or ""
                        if label_text.strip():
                            if rubric_kind not in class_info['rubrics']:
                                class_info['rubrics'][rubric_kind] = []
                            class_info['rubrics'][rubric_kind].append(label_text)
            
            classes[code] = class_info
            
            # 分类存储
            if kind == 'chapter':
                chapters[code] = class_info
            elif kind == 'component':
                components[code] = class_info
        
        # 第二遍：构建完整的数据结构
        data_list = []
        
        for code, class_info in classes.items():
            # 跳过chapter和component类型，只处理具体的分类项
            if class_info['kind'] in ['chapter', 'component']:
                continue
            
            item = {
                'code': code,
                'kind': class_info['kind'],
                'super_class': class_info['super_class'],
                'preferred': '',
                'preferred_中文': '',
                'shortTitle': '',
                'shortTitle_中文': '',
                'inclusion': '',
                'inclusion_中文': '',
                'exclusion': '',
                'exclusion_中文': '',
                'criteria': '',
                'criteria_中文': '',
                'consider': '',
                'consider_中文': '',
                'note': '',
                'note_中文': '',
                'icd10': ''
            }
            
            # 提取各种Rubric
            for rubric_kind, rubric_texts in class_info['rubrics'].items():
                if rubric_kind == 'preferred' and rubric_texts:
                    item['preferred'] = rubric_texts[0]
                elif rubric_kind == 'shortTitle' and rubric_texts:
                    item['shortTitle'] = rubric_texts[0]
                elif rubric_kind == 'inclusion' and rubric_texts:
                    item['inclusion'] = '; '.join(rubric_texts)
                elif rubric_kind == 'exclusion' and rubric_texts:
                    item['exclusion'] = '; '.join(rubric_texts)
                elif rubric_kind == 'criteria' and rubric_texts:
                    item['criteria'] = '; '.join(rubric_texts)
                elif rubric_kind == 'consider' and rubric_texts:
                    item['consider'] = '; '.join(rubric_texts)
                elif rubric_kind == 'note' and rubric_texts:
                    item['note'] = '; '.join(rubric_texts)
                elif rubric_kind == 'icd10' and rubric_texts:
                    item['icd10'] = '; '.join(rubric_texts)
            
            data_list.append(item)
        
        print(f"✓ 解析完成，共 {len(data_list)} 条记录")
        return data_list
    
    def _translate_batch(self, items: List[Dict]) -> Dict[str, str]:
        """
        批量翻译文本
        
        Args:
            items: 数据项列表
            
        Returns:
            翻译结果字典
        """
        translations = {}
        texts_to_translate = []
        
        # 收集所有需要翻译的文本
        for item in items:
            for field in ['preferred', 'shortTitle', 'inclusion', 'exclusion', 'criteria', 'consider', 'note']:
                text = item.get(field, '').strip()
                if text:
                    cache_key = f"ai_{text}"
                    if cache_key in self.translation_cache:
                        translations[text] = self.translation_cache[cache_key]
                    elif text not in texts_to_translate:
                        texts_to_translate.append(text)
        
        if not texts_to_translate:
            return translations
        
        print(f"  需要翻译: {len(texts_to_translate)} 条")
        print(f"  已缓存: {len(translations)} 条")
        
        # 并发翻译
        def translate_one(text):
            result = self._translate_with_ai(text)
            return text, result
        
        completed = 0
        with ThreadPoolExecutor(max_workers=self.ai_max_workers) as executor:
            future_to_text = {executor.submit(translate_one, text): text for text in texts_to_translate}
            
            for future in as_completed(future_to_text):
                text, result = future.result()
                if result:
                    translations[text] = result
                    completed += 1
                    if completed % 10 == 0:
                        print(f"  进度: {completed}/{len(texts_to_translate)}")
                else:
                    translations[text] = ""
        
        return translations
    
    def translate_xml(self, 
                      input_file: Optional[str] = None,
                      output_json: Optional[str] = None,
                      output_csv: Optional[str] = None) -> List[Dict]:
        """
        解析并翻译XML文件
        
        Args:
            input_file: 输入XML文件路径
            output_json: 输出JSON文件路径
            output_csv: 输出CSV文件路径
            
        Returns:
            翻译后的数据列表
        """
        # 设置文件路径
        if input_file:
            input_path = Path(input_file)
        else:
            input_path = self.input_file
        
        if output_json:
            output_json_path = Path(output_json)
        else:
            output_json_path = self.output_json
        
        if output_csv:
            output_csv_path = Path(output_csv)
        else:
            output_csv_path = self.output_csv
        
        if not input_path.exists():
            raise FileNotFoundError(f"输入文件不存在: {input_path}")
        
        # 解析XML
        data_list = self._parse_xml(input_path)
        
        if not self.use_ai_translation:
            print(f"\n⚠️  AI翻译功能未启用，只解析不翻译")
            # 保存未翻译的数据
            self._save_results(data_list, output_json_path, output_csv_path)
            return data_list
        
        # 批量翻译
        print(f"\n开始翻译...")
        translations = self._translate_batch(data_list)
        
        # 应用翻译结果
        print(f"\n正在应用翻译结果...")
        translated_count = 0
        
        for item in data_list:
            for field in ['preferred', 'shortTitle', 'inclusion', 'exclusion', 'criteria', 'consider', 'note']:
                text = item.get(field, '').strip()
                if text and text in translations:
                    chinese_field = f"{field}_中文"
                    item[chinese_field] = translations[text]
                    if translations[text]:
                        translated_count += 1
        
        print(f"✓ 翻译完成，共翻译 {translated_count} 个字段")
        
        # 保存翻译缓存
        print(f"\n正在保存翻译缓存...")
        self._save_translation_cache()
        print(f"✓ 缓存已保存")
        
        # 保存结果
        self._save_results(data_list, output_json_path, output_csv_path)
        
        # 显示统计信息
        self._print_statistics(data_list)
        
        return data_list
    
    def _save_results(self, data_list: List[Dict], json_path: Path, csv_path: Path):
        """保存结果到JSON和CSV文件"""
        print(f"\n正在保存结果...")
        
        # 保存JSON
        json_path.parent.mkdir(parents=True, exist_ok=True)
        with open(json_path, 'w', encoding='utf-8') as f:
            json.dump(data_list, f, ensure_ascii=False, indent=2)
        print(f"✓ JSON已保存: {json_path}")
        
        # 保存CSV
        if data_list:
            df = pd.DataFrame(data_list)
            csv_path.parent.mkdir(parents=True, exist_ok=True)
            df.to_csv(csv_path, index=False, encoding='utf-8-sig')
            print(f"✓ CSV已保存: {csv_path}")
    
    def _print_statistics(self, data_list: List[Dict]):
        """打印统计信息"""
        print(f"\n" + "="*80)
        print("翻译统计")
        print("="*80)
        print(f"总记录数: {len(data_list)}")
        
        # 统计各字段的翻译情况
        fields = ['preferred', 'shortTitle', 'inclusion', 'exclusion', 'criteria', 'consider', 'note']
        for field in fields:
            total = sum(1 for item in data_list if item.get(field, '').strip())
            translated = sum(1 for item in data_list if item.get(f"{field}_中文", '').strip())
            if total > 0:
                print(f"{field}: {translated}/{total} ({translated/total*100:.1f}%)")
        
        # 显示示例
        print(f"\n示例数据（前3条）:")
        for item in data_list[:3]:
            code = item['code']
            preferred = item.get('preferred', '')
            preferred_cn = item.get('preferred_中文', '')
            print(f"  {code}: {preferred} -> {preferred_cn}")


def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description='解析并翻译ICPC-2e-v7.0 XML文件')
    parser.add_argument('--input', type=str, help='输入XML文件路径')
    parser.add_argument('--output-json', type=str, help='输出JSON文件路径')
    parser.add_argument('--output-csv', type=str, help='输出CSV文件路径')
    
    args = parser.parse_args()
    
    try:
        translator = ICPCXMLTranslator()
        translator.translate_xml(
            input_file=args.input,
            output_json=args.output_json,
            output_csv=args.output_csv
        )
    except Exception as e:
        print(f"\n✗ 错误: {str(e)}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()

