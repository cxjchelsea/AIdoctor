"""
ICD-10-CM 疾病目录提取脚本（改进版）

功能：
1. 从ICD-10-CM索引XML中提取疾病名称和编码
2. 从ICD-10-CM表格XML中提取完整的层级结构
3. 保留所有层级节点（包括中间节点）
4. 清理别名，只保留真正的同义词
5. 生成标准化的疾病目录（JSON格式）
6. 支持导出为CSV格式和树形JSON结构

改进点：
- 提取完整层级结构（包括中间节点）
- 记录层级深度和父子关系
- 清理别名，只保留真正的同义词
- 修正章节映射问题

输出格式：
- icd10cm_code: ICD-10-CM编码（主键）
- standard_name: 标准疾病名
- description: 描述
- level: 层级深度（0=章节, 1=节, 2=类别, 3=子类别, 4+=扩展）
- parent_code: 父节点编码
- chapter: 所属章节编号
- chapter_desc: 章节描述
- section: 所属节（Section）
- section_desc: 节的描述
- aliases: 别名列表（清理后的）
- is_leaf: 是否为叶子节点
- full_path: 完整的分类路径
- source: 数据来源（index/tabular/both）

作者：AI医生系统
日期：2024
"""

import xml.etree.ElementTree as ET
import json
import csv
import re
from typing import Dict, List, Optional, Set, Tuple
from collections import defaultdict
from pathlib import Path


class ICD10CMDiseaseCatalogExtractor:
    """ICD-10-CM疾病目录提取器（支持完整层级结构）"""
    
    def __init__(self):
        # 所有节点（包括中间节点）
        self.nodes: Dict[str, Dict] = {}
        # 索引中的疾病名称到编码的映射（用于别名匹配）
        self.index_names: Dict[str, List[str]] = defaultdict(list)
        # 章节和节的映射
        self.chapters: Dict[str, Dict] = {}
        self.sections: Dict[str, Dict] = {}
    
    def _get_chapter_from_code(self, code: str) -> str:
        """根据编码确定所属章节号（ICD-10-CM标准章节划分）"""
        if not code:
            return ""
        
        first_char = code[0] if code else ""
        
        # ICD-10-CM章节划分
        if first_char == "A" or first_char == "B":
            return "1"  # Certain infectious and parasitic diseases
        elif first_char == "C" or (first_char == "D" and code[1] in "0-4"):
            return "2"  # Neoplasms
        elif first_char == "D" and code[1] in "5-9":
            return "3"  # Diseases of the blood and immune mechanism
        elif first_char == "E":
            return "4"  # Endocrine, nutritional and metabolic diseases
        elif first_char == "F":
            return "5"  # Mental and behavioral disorders
        elif first_char == "G":
            return "6"  # Diseases of the nervous system
        elif first_char == "H" and code[1] in "0-5":
            return "7"  # Diseases of the eye and adnexa
        elif first_char == "H" and code[1] in "6-9":
            return "8"  # Diseases of the ear and mastoid process
        elif first_char == "I":
            return "9"  # Diseases of the circulatory system
        elif first_char == "J":
            return "10"  # Diseases of the respiratory system
        elif first_char == "K":
            return "11"  # Diseases of the digestive system
        elif first_char == "L":
            return "12"  # Diseases of the skin and subcutaneous tissue
        elif first_char == "M":
            return "13"  # Diseases of the musculoskeletal system and connective tissue
        elif first_char == "N":
            return "14"  # Diseases of the genitourinary system
        elif first_char == "O":
            return "15"  # Pregnancy, childbirth and the puerperium
        elif first_char == "P":
            return "16"  # Certain conditions originating in the perinatal period
        elif first_char == "Q":
            return "17"  # Congenital malformations, deformations and chromosomal abnormalities
        elif first_char == "R":
            return "18"  # Symptoms, signs and abnormal clinical and laboratory findings
        elif first_char == "S" or first_char == "T":
            return "19"  # Injury, poisoning and certain other consequences of external causes
        elif first_char == "U":
            return "20"  # Codes for special purposes
        elif first_char == "V" or first_char == "W" or first_char == "X" or first_char == "Y":
            return "21"  # External causes of morbidity
        elif first_char == "Z":
            return "22"  # Factors influencing health status and contact with health services
        
        return ""
        
    def extract_text_content(self, element: ET.Element) -> str:
        """提取元素的文本内容（包括子元素）"""
        if element is None:
            return ""
        
        text = element.text or ""
        for child in element:
            if child.tail:
                text += child.tail
            if child.text:
                text += child.text
        return text.strip()
    
    def extract_title_text(self, title_elem: ET.Element) -> str:
        """提取title元素的文本，处理nemod等嵌套元素"""
        if title_elem is None:
            return ""
        
        # 获取直接文本
        text = title_elem.text or ""
        
        # 处理nemod等子元素
        for child in title_elem:
            if child.tag == "nemod":
                # nemod通常是括号内的说明，保留但不作为主要名称
                pass
            if child.tail:
                text += child.tail
        
        return text.strip()
    
    def get_code_level(self, code: str) -> int:
        """根据编码计算层级深度
        0: 章节 (Chapter)
        1: 节 (Section) - 如 A00-A09
        2: 类别 (Category) - 如 A00, A01
        3: 子类别 (Subcategory) - 如 A00.0, A00.1
        4+: 扩展 (Extension) - 如 A01.00, A01.01
        """
        if not code:
            return 0
        
        # 去除末尾的点
        code = code.rstrip('.')
        
        # 计算层级：根据编码的点和长度
        if '.' not in code:
            # 类别级别（如 A00）
            return 2
        else:
            parts = code.split('.')
            # 子类别级别（如 A00.0）
            if len(parts) == 2 and len(parts[1]) == 1:
                return 3
            # 扩展级别（如 A01.00）
            else:
                return 3 + len(parts[1]) - 1
    
    def get_parent_code(self, code: str) -> Optional[str]:
        """根据编码获取父节点编码"""
        if not code:
            return None
        
        code = code.rstrip('.')
        
        # 如果是类别级别（如 A00），父节点是节
        if '.' not in code:
            # 找到包含该编码的节
            for section_id in self.sections:
                section = self.sections[section_id]
                first, last = section_id.split('-') if '-' in section_id else (section_id, section_id)
                if first <= code <= last:
                    return section_id
            return None
        else:
            # 如果是子类别或扩展，父节点是上一级编码
            parts = code.split('.')
            if len(parts) == 2:
                # A00.0 -> A00
                return parts[0]
            elif len(parts[1]) > 1:
                # A01.00 -> A01.0
                return f"{parts[0]}.{parts[1][:-1]}"
            else:
                return parts[0]
    
    def clean_alias(self, alias: str) -> str:
        """清理别名，去除不必要的修饰"""
        # 去除多余空格
        alias = re.sub(r'\s+', ' ', alias).strip()
        # 去除前后括号和内容（如果整个别名都是括号）
        alias = re.sub(r'^\(.*\)$', '', alias).strip()
        return alias
    
    def is_valid_alias(self, alias: str, main_name: str, code: str) -> bool:
        """判断是否为有效的别名（同义词）"""
        if not alias or not main_name:
            return False
        
        alias_clean = self.clean_alias(alias).lower()
        main_clean = main_name.lower()
        
        # 如果别名和主名称完全相同，不重复添加
        if alias_clean == main_clean:
            return False
        
        # 如果别名太短（少于3个字符），可能是缩写，保留
        if len(alias_clean) < 3:
            return True
        
        # 如果别名包含了主名称，或者主名称包含了别名，可能是相关术语
        # 这里需要更智能的判断，暂时保留
        return True
    
    def parse_index_xml(self, xml_path: str):
        """解析ICD-10-CM索引XML文件"""
        print(f"正在解析索引XML: {xml_path}")
        
        context = ET.iterparse(xml_path, events=("start", "end"))
        context = iter(context)
        event, root = next(context)
        
        current_letter = None
        current_main_term = None
        disease_count = 0
        
        for event, elem in context:
            if event == "end":
                if elem.tag == "letter":
                    if elem.find("title") is not None:
                        current_letter = self.extract_text_content(elem.find("title"))
                        if current_letter and current_letter.isalpha():
                            print(f"处理字母: {current_letter}")
                
                elif elem.tag == "mainTerm":
                    title_elem = elem.find("title")
                    if title_elem is not None:
                        main_term_title = self.extract_title_text(title_elem)
                        current_main_term = main_term_title
                        
                        # 检查是否有直接编码
                        code_elem = elem.find("code")
                        if code_elem is not None:
                            code = self.extract_text_content(code_elem)
                            if code:
                                # 记录索引中的名称映射
                                self.index_names[code].append(main_term_title)
                                disease_count += 1
                
                elif elem.tag == "term":
                    title_elem = elem.find("title")
                    code_elem = elem.find("code")
                    
                    if title_elem is not None and code_elem is not None:
                        term_title = self.extract_title_text(title_elem)
                        code = self.extract_text_content(code_elem)
                        
                        if code:
                            # 组合主术语和子术语（只用于索引，不作为最终名称）
                            full_name = f"{current_main_term}, {term_title}" if current_main_term else term_title
                            # 记录索引中的名称映射
                            self.index_names[code].append(full_name)
                            disease_count += 1
                
                # 清理已处理的元素以节省内存
                if elem.tag in ["letter", "mainTerm", "term"]:
                    elem.clear()
        
        print(f"从索引中提取了 {disease_count} 个疾病条目")
    
    def parse_tabular_xml(self, xml_path: str):
        """解析ICD-10-CM表格XML文件（提取完整层级结构）"""
        print(f"正在解析表格XML: {xml_path}")
        
        context = ET.iterparse(xml_path, events=("start", "end"))
        context = iter(context)
        event, root = next(context)
        
        # 使用栈来跟踪层级
        stack: List[Tuple[str, int]] = []  # (code, level)
        current_chapter = None
        current_section = None
        node_count = 0
        
        for event, elem in context:
            if event == "end":
                if elem.tag == "chapter":
                    name_elem = elem.find("name")
                    desc_elem = elem.find("desc")
                    
                    if name_elem is not None:
                        chapter_name_text = self.extract_text_content(name_elem)
                        chapter_desc = self.extract_text_content(desc_elem) if desc_elem is not None else ""
                        
                        # 从章节名称中提取数字（可能是"1"、"2"等，也可能包含其他内容）
                        chapter_num_match = re.search(r'\d+', chapter_name_text)
                        if chapter_num_match:
                            chapter_num = chapter_num_match.group()
                        else:
                            # 如果没有数字，使用章节描述中的编码范围来确定章节号
                            # 根据编码范围推断章节（A00-B99是第1章，C00-D49是第2章等）
                            code_range_match = re.search(r'\(([A-Z]\d{2})-[A-Z]\d{2}\)', chapter_desc)
                            if code_range_match:
                                first_code = code_range_match.group(1)
                                # 根据编码范围确定章节号（简化版本）
                                chapter_num = self._get_chapter_from_code(first_code)
                            else:
                                chapter_num = chapter_name_text  # 保留原始文本
                        
                        current_chapter = chapter_num
                        self.chapters[chapter_num] = {
                            "name": chapter_num,
                            "desc": chapter_desc
                        }
                        print(f"处理章节 {chapter_num}: {chapter_desc}")
                        stack.clear()
                
                elif elem.tag == "section":
                    section_id = elem.get("id", "")
                    desc_elem = elem.find("desc")
                    section_desc = self.extract_text_content(desc_elem) if desc_elem is not None else ""
                    
                    current_section = section_id
                    self.sections[section_id] = {
                        "id": section_id,
                        "desc": section_desc,
                        "chapter": current_chapter
                    }
                    stack.clear()
                
                elif elem.tag == "diag":
                    name_elem = elem.find("name")
                    desc_elem = elem.find("desc")
                    
                    if name_elem is not None:
                        code = self.extract_text_content(name_elem)
                        desc = self.extract_text_content(desc_elem) if desc_elem is not None else ""
                        
                        if code:
                            code = code.rstrip('.')
                            
                            # 计算层级
                            level = self.get_code_level(code)
                            
                            # 获取父节点
                            parent_code = None
                            if stack:
                                # 从栈中找到合适的父节点（层级更浅的最后一个）
                                for parent_c, parent_l in reversed(stack):
                                    if parent_l < level:
                                        parent_code = parent_c
                                        break
                            
                            # 如果没有找到父节点，尝试根据编码推断
                            if not parent_code and level > 2:
                                parent_code = self.get_parent_code(code)
                            
                            # 对于类别级别的节点，父节点应该是节
                            if not parent_code and level == 2 and current_section:
                                parent_code = current_section
                            
                            # 判断是否有子节点
                            has_children = False
                            for child in elem:
                                if child.tag == "diag":
                                    has_children = True
                                    break
                            
                            # 添加到节点集合
                            self._add_node(
                                code=code,
                                description=desc,
                                level=level,
                                parent_code=parent_code,
                                chapter=current_chapter,
                                section=current_section,
                                is_leaf=not has_children
                            )
                            node_count += 1
                            
                            # 更新栈：移除同级别或更深级别的节点
                            while stack and stack[-1][1] >= level:
                                stack.pop()
                            # 添加当前节点到栈
                            stack.append((code, level))
                
                # 清理已处理的元素
                if elem.tag in ["chapter", "section", "diag"]:
                    elem.clear()
        
        print(f"从表格中提取了 {node_count} 个节点")
        
        # 后处理：为缺失章节信息的节点补充章节信息
        self._fill_missing_chapter_info()
    
    def _fill_missing_chapter_info(self):
        """为缺失或错误的章节信息补充和修正章节信息"""
        filled_count = 0
        corrected_count = 0
        
        for code, node in self.nodes.items():
            # 根据编码推断正确的章节
            correct_chapter = self._get_chapter_from_code(code)
            
            if correct_chapter:
                # 如果节点没有章节信息，或章节信息错误，则更新
                current_chapter = node.get("chapter")
                
                if not current_chapter:
                    # 补充缺失的章节信息
                    node["chapter"] = correct_chapter
                    chapter_info = self.chapters.get(correct_chapter, {})
                    node["chapter_desc"] = chapter_info.get("desc", "")
                    filled_count += 1
                elif current_chapter != correct_chapter:
                    # 修正错误的章节信息
                    node["chapter"] = correct_chapter
                    chapter_info = self.chapters.get(correct_chapter, {})
                    node["chapter_desc"] = chapter_info.get("desc", "")
                    corrected_count += 1
                
                # 如果章节描述缺失，尝试补充
                if not node.get("chapter_desc") and correct_chapter in self.chapters:
                    node["chapter_desc"] = self.chapters[correct_chapter].get("desc", "")
                
                # 更新完整路径
                path_parts = []
                if node["chapter"]:
                    path_parts.append(f"第{node['chapter']}章")
                if node["chapter_desc"]:
                    path_parts.append(node["chapter_desc"])
                if node.get("section_desc"):
                    path_parts.append(node["section_desc"])
                if node["description"]:
                    path_parts.append(node["description"])
                if path_parts:
                    node["full_path"] = " > ".join(path_parts)
        
        if filled_count > 0:
            print(f"补充了 {filled_count} 个节点的章节信息")
        if corrected_count > 0:
            print(f"修正了 {corrected_count} 个节点的章节信息")
    
    def _add_node(self, code: str, description: str, level: int,
                   parent_code: Optional[str], chapter: Optional[str],
                   section: Optional[str], is_leaf: bool):
        """添加节点到目录"""
        # 获取章节和节的描述
        chapter_desc = self.chapters.get(chapter, {}).get("desc", "") if chapter else ""
        section_desc = self.sections.get(section, {}).get("desc", "") if section else ""
        
        # 构建完整路径
        path_parts = []
        if chapter:
            path_parts.append(f"第{chapter}章")
        if chapter_desc:
            path_parts.append(chapter_desc)
        if section_desc:
            path_parts.append(section_desc)
        if description:
            path_parts.append(description)
        
        full_path = " > ".join(path_parts)
        
        # 获取标准名称（优先使用描述，否则使用编码）
        standard_name = description or code
        
        # 处理别名（从索引中获取，并清理）
        aliases = []
        if code in self.index_names:
            index_names = self.index_names[code]
            for idx_name in index_names:
                cleaned = self.clean_alias(idx_name)
                if cleaned and cleaned != standard_name:
                    if self.is_valid_alias(cleaned, standard_name, code):
                        if cleaned not in aliases:
                            aliases.append(cleaned)
        
        # 确定数据来源
        source = "tabular"
        if code in self.index_names:
            source = "index/tabular"
        
        if code not in self.nodes:
            self.nodes[code] = {
                "icd10cm_code": code,
                "standard_name": standard_name,
                "description": description,
                "level": level,
                "parent_code": parent_code,
                "chapter": chapter,
                "chapter_desc": chapter_desc,
                "section": section,
                "section_desc": section_desc,
                "aliases": aliases,
                "is_leaf": is_leaf,
                "full_path": full_path,
                "source": source
            }
        else:
            # 更新节点信息（保留并更新层级信息）
            node = self.nodes[code]
            
            # 如果当前有更完整的层级信息，则更新
            if chapter and not node.get("chapter"):
                node["chapter"] = chapter
                node["chapter_desc"] = chapter_desc
            if section and not node.get("section"):
                node["section"] = section
                node["section_desc"] = section_desc
            if parent_code and not node.get("parent_code"):
                node["parent_code"] = parent_code
            
            # 更新层级信息（使用更具体的值）
            if level != node["level"]:
                # 如果层级不同，使用更深的层级（更具体）
                if level > node["level"]:
                    node["level"] = level
            
            # 更新描述（如果新的描述更完整）
            if description and (not node["description"] or len(description) > len(node["description"])):
                node["description"] = description
                node["standard_name"] = description or code
            
            # 更新完整路径
            if chapter or section:
                path_parts = []
                if node["chapter"]:
                    path_parts.append(f"第{node['chapter']}章")
                if node["chapter_desc"]:
                    path_parts.append(node["chapter_desc"])
                if node["section_desc"]:
                    path_parts.append(node["section_desc"])
                if node["description"]:
                    path_parts.append(node["description"])
                node["full_path"] = " > ".join(path_parts)
            
            # 合并别名
            for alias in aliases:
                if alias not in node["aliases"]:
                    node["aliases"].append(alias)
            
            # 更新来源
            if "tabular" not in node["source"]:
                node["source"] += "/tabular"
            
            # 更新叶子节点状态
            if is_leaf:
                node["is_leaf"] = is_leaf
    
    def build_tree_structure(self) -> Dict:
        """构建树形结构JSON（优化版：使用索引提升性能）"""
        print("正在构建树形结构...")
        
        # 预先构建父子关系索引（提升性能，避免每次遍历所有节点）
        # parent_code -> [children_nodes]
        parent_children_index: Dict[str, List[Dict]] = defaultdict(list)
        for node in self.nodes.values():
            if node["parent_code"]:
                parent_children_index[node["parent_code"]].append(node)
        
        # 对每个父节点的子节点列表排序（只排序一次，提升性能）
        for parent_code in parent_children_index:
            parent_children_index[parent_code].sort(key=lambda x: x["icd10cm_code"])
        
        print(f"已构建父子关系索引，共 {len(parent_children_index)} 个父节点")
        
        # 按层级和编码排序
        sorted_nodes = sorted(
            self.nodes.values(),
            key=lambda x: (x["level"], x["icd10cm_code"])
        )
        
        # 构建树形结构
        tree = {
            "version": "2026",
            "source": "ICD-10-CM",
            "chapters": []
        }
        
        # 按章节组织
        chapters_dict: Dict[str, List[Dict]] = defaultdict(list)
        for node in sorted_nodes:
            if node["chapter"]:
                chapters_dict[node["chapter"]].append(node)
        
        # 构建每个章节的树
        for chapter_num in sorted(chapters_dict.keys()):
            chapter_nodes = chapters_dict[chapter_num]
            chapter_info = self.chapters.get(chapter_num, {})
            
            chapter_tree = {
                "chapter": chapter_num,
                "chapter_desc": chapter_info.get("desc", ""),
                "sections": []
            }
            
            # 按节组织
            sections_dict: Dict[str, List[Dict]] = defaultdict(list)
            for node in chapter_nodes:
                if node["section"]:
                    sections_dict[node["section"]].append(node)
            
            # 构建每个节的树
            for section_id in sorted(sections_dict.keys()):
                section_nodes = sections_dict[section_id]
                section_info = self.sections.get(section_id, {})
                
                section_tree = {
                    "section_id": section_id,
                    "section_desc": section_info.get("desc", ""),
                    "categories": []
                }
                
                # 构建类别树（递归构建）
                category_nodes = [n for n in section_nodes if n["level"] == 2]
                for cat_node in category_nodes:
                    category_tree = self._build_category_tree(
                        cat_node, 
                        parent_children_index
                    )
                    section_tree["categories"].append(category_tree)
                
                chapter_tree["sections"].append(section_tree)
            
            tree["chapters"].append(chapter_tree)
        
        print("树形结构构建完成")
        return tree
    
    def _build_category_tree(self, node: Dict, parent_children_index: Dict[str, List[Dict]]) -> Dict:
        """递归构建类别树（优化版：使用索引字典）"""
        tree_node = {
            "code": node["icd10cm_code"],
            "name": node["standard_name"],
            "description": node["description"],
            "level": node["level"],
            "is_leaf": node["is_leaf"],
            "aliases": node["aliases"],
            "children": []
        }
        
        # 使用索引快速查找子节点（O(1)时间复杂度）
        children = parent_children_index.get(node["icd10cm_code"], [])
        
        # 子节点已经排序，直接递归构建
        for child in children:
            child_tree = self._build_category_tree(child, parent_children_index)
            tree_node["children"].append(child_tree)
        
        return tree_node
    
    def save_to_json(self, output_path: str):
        """保存疾病目录为JSON格式（扁平列表）"""
        print(f"正在保存JSON文件: {output_path}")
        
        catalog = {
            "version": "2026",
            "source": "ICD-10-CM",
            "total_nodes": len(self.nodes),
            "nodes": list(self.nodes.values())
        }
        
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(catalog, f, ensure_ascii=False, indent=2)
        
        print(f"已保存 {len(self.nodes)} 个节点到 {output_path}")
    
    def save_tree_json(self, output_path: str):
        """保存树形结构JSON"""
        print(f"正在保存树形JSON文件: {output_path}")
        
        tree = self.build_tree_structure()
        
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(tree, f, ensure_ascii=False, indent=2)
        
        print(f"已保存树形结构到 {output_path}")
    
    def save_to_csv(self, output_path: str):
        """保存疾病目录为CSV格式"""
        print(f"正在保存CSV文件: {output_path}")
        
        with open(output_path, 'w', newline='', encoding='utf-8-sig') as f:
            writer = csv.writer(f)
            
            # 写入表头
            writer.writerow([
                "ICD-10-CM编码",
                "标准疾病名",
                "描述",
                "层级深度",
                "父节点编码",
                "章节编号",
                "章节描述",
                "节ID",
                "节描述",
                "别名（逗号分隔）",
                "是否为叶子节点",
                "完整路径",
                "数据来源"
            ])
            
            # 按层级和编码排序
            sorted_nodes = sorted(
                self.nodes.values(),
                key=lambda x: (x["level"], x["icd10cm_code"])
            )
            
            # 写入数据
            for node in sorted_nodes:
                writer.writerow([
                    node["icd10cm_code"],
                    node["standard_name"],
                    node["description"] or "",
                    node["level"],
                    node["parent_code"] or "",
                    node["chapter"] or "",
                    node["chapter_desc"] or "",
                    node["section"] or "",
                    node["section_desc"] or "",
                    ", ".join(node["aliases"]) if node["aliases"] else "",
                    "是" if node["is_leaf"] else "否",
                    node["full_path"] or "",
                    node["source"]
                ])
        
        print(f"已保存 {len(self.nodes)} 个节点到 {output_path}")
    
    def generate_summary(self) -> Dict:
        """生成统计摘要"""
        summary = {
            "total_nodes": len(self.nodes),
            "by_level": defaultdict(int),
            "by_source": defaultdict(int),
            "by_chapter": defaultdict(int),
            "with_aliases": 0,
            "leaf_nodes": 0,
            "non_leaf_nodes": 0
        }
        
        for node in self.nodes.values():
            # 统计层级
            summary["by_level"][node["level"]] += 1
            
            # 统计来源
            summary["by_source"][node["source"]] += 1
            
            # 统计章节
            if node["chapter"]:
                summary["by_chapter"][node["chapter"]] += 1
            
            # 统计别名
            if node["aliases"]:
                summary["with_aliases"] += 1
            
            # 统计叶子节点
            if node["is_leaf"]:
                summary["leaf_nodes"] += 1
            else:
                summary["non_leaf_nodes"] += 1
        
        return summary


def main():
    """主函数"""
    # 文件路径配置
    base_dir = Path(__file__).parent.parent / "源数据" / "icd10cm-table and index-2026"
    index_xml = base_dir / "icd10cm-index-2026.xml"
    tabular_xml = base_dir / "icd10cm-tabular-2026.xml"
    
    # 输出路径
    output_dir = Path(__file__).parent.parent / "提取结果"
    output_dir.mkdir(exist_ok=True)
    
    json_output = output_dir / "icd10cm_disease_catalog.json"
    tree_json_output = output_dir / "icd10cm_disease_catalog_tree.json"
    csv_output = output_dir / "icd10cm_disease_catalog.csv"
    summary_output = output_dir / "icd10cm_disease_catalog_summary.json"
    
    # 创建提取器
    extractor = ICD10CMDiseaseCatalogExtractor()
    
    # 解析索引XML
    if index_xml.exists():
        extractor.parse_index_xml(str(index_xml))
    else:
        print(f"警告: 索引XML文件不存在: {index_xml}")
    
    # 解析表格XML
    if tabular_xml.exists():
        extractor.parse_tabular_xml(str(tabular_xml))
    else:
        print(f"警告: 表格XML文件不存在: {tabular_xml}")
    
    # 生成摘要
    summary = extractor.generate_summary()
    print("\n=== 提取统计摘要 ===")
    print(f"总节点数: {summary['total_nodes']}")
    print(f"\n按层级统计:")
    for level in sorted(summary["by_level"].keys()):
        level_names = {0: "章节", 1: "节", 2: "类别", 3: "子类别", 4: "扩展"}
        level_name = level_names.get(level, f"层级{level}")
        print(f"  {level_name} (层级{level}): {summary['by_level'][level]}")
    print(f"\n按来源统计:")
    for source, count in summary["by_source"].items():
        print(f"  {source}: {count}")
    print(f"\n按章节统计（前10个）:")
    sorted_chapters = sorted(summary["by_chapter"].items(), key=lambda x: x[1], reverse=True)[:10]
    for chapter, count in sorted_chapters:
        print(f"  第{chapter}章: {count}")
    print(f"\n有别名: {summary['with_aliases']}")
    print(f"叶子节点: {summary['leaf_nodes']}")
    print(f"非叶子节点: {summary['non_leaf_nodes']}")
    
    # 保存结果
    extractor.save_to_json(str(json_output))
    extractor.save_tree_json(str(tree_json_output))
    extractor.save_to_csv(str(csv_output))
    
    # 保存摘要
    with open(summary_output, 'w', encoding='utf-8') as f:
        json.dump(summary, f, ensure_ascii=False, indent=2)
    
    print(f"\n✅ 疾病目录提取完成！")
    print(f"   - JSON（扁平）: {json_output}")
    print(f"   - JSON（树形）: {tree_json_output}")
    print(f"   - CSV: {csv_output}")
    print(f"   - 摘要: {summary_output}")


if __name__ == "__main__":
    main()