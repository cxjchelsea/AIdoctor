#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
HPO数据提取脚本
从OWL文件中提取HPO术语数据，并转换为JSON格式
"""

import json
import re
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Set
from xml.etree import ElementTree as ET
from collections import defaultdict

# 命名空间定义
NAMESPACES = {
    'owl': 'http://www.w3.org/2002/07/owl#',
    'rdf': 'http://www.w3.org/1999/02/22-rdf-syntax-ns#',
    'rdfs': 'http://www.w3.org/2000/01/rdf-schema#',
    'obo': 'http://purl.obolibrary.org/obo/',
    'oboInOwl': 'http://www.geneontology.org/formats/oboInOwl#'
}


def uri_to_hpo_id(uri: str) -> Optional[str]:
    """
    将URI转换为HPO ID
    例如: http://purl.obolibrary.org/obo/HP_0000107 -> HP:0000107
    """
    if not uri:
        return None
    
    # 提取URI中最后一个/后的部分
    if '/' in uri:
        last_part = uri.split('/')[-1]
        # 将下划线替换为冒号
        if last_part.startswith('HP_'):
            return last_part.replace('_', ':', 1)
    return None


def extract_text(element: ET.Element, tag: str, default: str = None) -> Optional[str]:
    """提取元素的文本内容"""
    if element is None:
        return default
    
    # 查找指定标签的元素
    found = element.find(f'.//{tag}', NAMESPACES)
    if found is not None and found.text:
        return found.text.strip()
    return default


def extract_text_list(element: ET.Element, tag: str) -> List[str]:
    """提取所有匹配标签的文本内容列表"""
    if element is None:
        return []
    
    results = []
    for found in element.findall(f'.//{tag}', NAMESPACES):
        if found.text and found.text.strip():
            results.append(found.text.strip())
    return results


def extract_resource_attributes(element: ET.Element, tag: str) -> List[str]:
    """提取rdf:resource属性的值列表"""
    if element is None:
        return []
    
    results = []
    for found in element.findall(f'.//{tag}', NAMESPACES):
        resource = found.get(f'{{{NAMESPACES["rdf"]}}}resource')
        if resource:
            hpo_id = uri_to_hpo_id(resource)
            if hpo_id:
                results.append(hpo_id)
    return results


def extract_xrefs(element: ET.Element) -> List[Dict[str, str]]:
    """提取外部引用列表"""
    if element is None:
        return []
    
    xrefs = []
    for xref_elem in element.findall('.//oboInOwl:hasDbXref', NAMESPACES):
        if xref_elem.text:
            xref_text = xref_elem.text.strip()
            # 解析格式: TYPE:ID
            if ':' in xref_text:
                parts = xref_text.split(':', 1)
                xrefs.append({
                    'type': parts[0],
                    'id': parts[1]
                })
    return xrefs


def extract_definition(element: ET.Element) -> Optional[str]:
    """提取定义，包括owl:Axiom中的定义"""
    if element is None:
        return None
    
    # 先查找直接定义
    definition = extract_text(element, 'obo:IAO_0000115')
    if definition:
        return definition
    
    # 查找owl:Axiom中的定义
    for axiom in element.findall('.//owl:Axiom', NAMESPACES):
        source = axiom.find('owl:annotatedSource', NAMESPACES)
        property_elem = axiom.find('owl:annotatedProperty', NAMESPACES)
        target = axiom.find('owl:annotatedTarget', NAMESPACES)
        
        if source is not None and property_elem is not None and target is not None:
            # 检查是否是定义属性
            prop_resource = property_elem.get(f'{{{NAMESPACES["rdf"]}}}resource', '')
            if 'IAO_0000115' in prop_resource and target.text:
                return target.text.strip()
    
    return None


def extract_synonyms_from_axioms(axioms: List[ET.Element], synonym_type: str) -> List[str]:
    """从owl:Axiom中提取同义词"""
    synonyms = []
    for axiom in axioms:
        property_elem = axiom.find('owl:annotatedProperty', NAMESPACES)
        target = axiom.find('owl:annotatedTarget', NAMESPACES)
        
        if property_elem is not None and target is not None:
            prop_resource = property_elem.get(f'{{{NAMESPACES["rdf"]}}}resource', '')
            if synonym_type in prop_resource and target.text:
                synonyms.append(target.text.strip())
    return synonyms


def extract_parent_ids(element: ET.Element) -> List[str]:
    """提取父类ID列表，处理Restriction情况"""
    if element is None:
        return []
    
    parent_ids = []
    
    # 查找直接的rdfs:subClassOf
    for sub_class in element.findall('.//rdfs:subClassOf', NAMESPACES):
        resource = sub_class.get(f'{{{NAMESPACES["rdf"]}}}resource')
        if resource:
            hpo_id = uri_to_hpo_id(resource)
            if hpo_id:
                parent_ids.append(hpo_id)
        else:
            # 处理Restriction情况
            restriction = sub_class.find('owl:Restriction', NAMESPACES)
            if restriction is not None:
                # 查找someValuesFrom或allValuesFrom
                for prop in ['owl:someValuesFrom', 'owl:allValuesFrom']:
                    values_from = restriction.find(prop, NAMESPACES)
                    if values_from is not None:
                        resource = values_from.get(f'{{{NAMESPACES["rdf"]}}}resource')
                        if resource:
                            hpo_id = uri_to_hpo_id(resource)
                            if hpo_id:
                                parent_ids.append(hpo_id)
    
    return list(set(parent_ids))  # 去重


def extract_term_data(class_elem: ET.Element, axioms_by_source: Dict[str, List[ET.Element]]) -> Optional[Dict]:
    """提取单个术语的所有数据"""
    # 提取ID
    term_id = extract_text(class_elem, 'oboInOwl:id')
    if not term_id or not term_id.startswith('HP:'):
        return None
    
    # 获取rdf:about属性，用于关联Axiom
    rdf_about = class_elem.get(f'{{{NAMESPACES["rdf"]}}}about', '')
    
    # 获取相关的Axiom
    related_axioms = axioms_by_source.get(rdf_about, [])
    
    # 提取基本信息
    label = extract_text(class_elem, 'rdfs:label')
    if not label:
        # 尝试查找带xml:lang="en"的label
        for label_elem in class_elem.findall('.//rdfs:label', NAMESPACES):
            if label_elem.get('{http://www.w3.org/XML/1998/namespace}lang') == 'en':
                label = label_elem.text.strip() if label_elem.text else None
                break
        if not label:
            # 使用第一个label
            label_elem = class_elem.find('.//rdfs:label', NAMESPACES)
            label = label_elem.text.strip() if label_elem is not None and label_elem.text else None
    
    if not label:
        return None  # 必须有label
    
    # 提取定义
    definition = extract_definition(class_elem)
    
    # 提取注释
    comment = extract_text(class_elem, 'rdfs:comment')
    
    # 提取精确同义词
    exact_synonyms = extract_text_list(class_elem, 'oboInOwl:hasExactSynonym')
    # 从Axiom中提取
    exact_synonyms.extend(extract_synonyms_from_axioms(related_axioms, 'hasExactSynonym'))
    exact_synonyms = list(set(exact_synonyms))  # 去重
    
    # 提取相关同义词
    related_synonyms = extract_text_list(class_elem, 'oboInOwl:hasRelatedSynonym')
    # 从Axiom中提取
    related_synonyms.extend(extract_synonyms_from_axioms(related_axioms, 'hasRelatedSynonym'))
    related_synonyms = list(set(related_synonyms))  # 去重
    
    # 提取父类ID
    parent_ids = extract_parent_ids(class_elem)
    
    # 提取替代ID
    alternative_ids = extract_text_list(class_elem, 'oboInOwl:hasAlternativeId')
    
    # 提取废弃标记
    deprecated_elem = class_elem.find('.//owl:deprecated', NAMESPACES)
    deprecated = False
    if deprecated_elem is not None:
        deprecated_text = deprecated_elem.text
        if deprecated_text:
            deprecated = deprecated_text.strip().lower() == 'true'
    
    # 提取被替代的术语ID
    replaced_by = None
    replaced_by_elem = class_elem.find('.//obo:IAO_0100001', NAMESPACES)
    if replaced_by_elem is not None:
        resource = replaced_by_elem.get(f'{{{NAMESPACES["rdf"]}}}resource')
        if resource:
            replaced_by = uri_to_hpo_id(resource)
    
    # 提取创建日期
    creation_date = extract_text(class_elem, 'oboInOwl:creation_date')
    
    # 提取外部引用
    xrefs = extract_xrefs(class_elem)
    
    return {
        'id': term_id,
        'label': label,
        'definition': definition,
        'comment': comment,
        'exact_synonyms': exact_synonyms,
        'related_synonyms': related_synonyms,
        'parent_ids': parent_ids,
        'child_ids': [],  # 稍后计算
        'is_leaf': True,  # 稍后计算
        'alternative_ids': alternative_ids,
        'deprecated': deprecated,
        'replaced_by': replaced_by,
        'creation_date': creation_date,
        'xrefs': xrefs
    }


def extract_version(ontology_elem: ET.Element) -> Optional[str]:
    """从Ontology元素中提取版本号"""
    if ontology_elem is None:
        return None
    
    version_iri = ontology_elem.find('owl:versionIRI', NAMESPACES)
    if version_iri is not None:
        resource = version_iri.get(f'{{{NAMESPACES["rdf"]}}}resource', '')
        # 从URI中提取日期部分，例如: .../releases/2026-01-08/...
        match = re.search(r'releases/(\d{4}-\d{2}-\d{2})', resource)
        if match:
            return match.group(1)
    
    return None


def parse_owl_file(owl_path: str) -> Dict:
    """解析OWL文件并提取所有数据"""
    print(f"开始解析OWL文件: {owl_path}")
    
    # 注册命名空间
    for prefix, uri in NAMESPACES.items():
        ET.register_namespace(prefix, uri)
    
    # 解析XML文件
    tree = ET.parse(owl_path)
    root = tree.getroot()
    
    # 提取元数据
    ontology_elem = root.find('owl:Ontology', NAMESPACES)
    version = extract_version(ontology_elem)
    source = Path(owl_path).name
    extraction_date = datetime.now().strftime('%Y-%m-%d')
    
    print(f"版本: {version}")
    print(f"提取日期: {extraction_date}")
    
    # 先收集所有owl:Axiom，按annotatedSource分组
    print("收集owl:Axiom数据...")
    axioms_by_source = defaultdict(list)
    for axiom in root.findall('.//owl:Axiom', NAMESPACES):
        source_elem = axiom.find('owl:annotatedSource', NAMESPACES)
        if source_elem is not None:
            source_uri = source_elem.get(f'{{{NAMESPACES["rdf"]}}}resource', '')
            if source_uri:
                axioms_by_source[source_uri].append(axiom)
    
    print(f"找到 {len(axioms_by_source)} 个带注解的术语")
    
    # 提取所有术语
    print("提取术语数据...")
    terms = {}
    class_count = 0
    
    for class_elem in root.findall('.//owl:Class', NAMESPACES):
        class_count += 1
        if class_count % 1000 == 0:
            print(f"已处理 {class_count} 个类...")
        
        term_data = extract_term_data(class_elem, axioms_by_source)
        if term_data:
            terms[term_data['id']] = term_data
    
    print(f"成功提取 {len(terms)} 个HPO术语")
    
    # 清理不存在的父类引用（这些可能是其他本体的类）
    print("清理不存在的父类引用...")
    removed_parent_count = 0
    for term_id, term_data in terms.items():
        original_count = len(term_data['parent_ids'])
        term_data['parent_ids'] = [p for p in term_data['parent_ids'] if p in terms]
        removed_count = original_count - len(term_data['parent_ids'])
        if removed_count > 0:
            removed_parent_count += removed_count
    
    if removed_parent_count > 0:
        print(f"已清理 {removed_parent_count} 个不存在的父类引用（可能是其他本体的类）")
    
    # 计算派生字段：child_ids
    print("计算子类关系...")
    for term_id, term_data in terms.items():
        for parent_id in term_data['parent_ids']:
            if parent_id in terms:
                if term_id not in terms[parent_id]['child_ids']:
                    terms[parent_id]['child_ids'].append(term_id)
    
    # 计算派生字段：is_leaf
    print("计算叶子节点...")
    for term_data in terms.values():
        term_data['is_leaf'] = len(term_data['child_ids']) == 0
    
    # 构建索引
    print("构建索引...")
    synonym_index = {}
    alternative_id_mapping = {}
    deprecated_terms = []
    
    for term_id, term_data in terms.items():
        # 同义词索引（包括label和所有同义词）
        if term_data['label']:
            synonym_index[term_data['label']] = term_id
        
        for synonym in term_data['exact_synonyms']:
            if synonym and synonym not in synonym_index:
                synonym_index[synonym] = term_id
        
        for synonym in term_data['related_synonyms']:
            if synonym and synonym not in synonym_index:
                synonym_index[synonym] = term_id
        
        # 替代ID映射
        for alt_id in term_data['alternative_ids']:
            if alt_id:
                alternative_id_mapping[alt_id] = term_id
        
        # 废弃术语列表
        if term_data['deprecated']:
            deprecated_terms.append(term_id)
    
    # 构建最终数据结构
    result = {
        'metadata': {
            'version': version,
            'source': source,
            'extraction_date': extraction_date,
            'total_terms': len(terms),
            'total_synonyms': len(synonym_index)
        },
        'terms': terms,
        'synonym_index': synonym_index,
        'alternative_id_mapping': alternative_id_mapping,
        'deprecated_terms': deprecated_terms
    }
    
    print(f"提取完成！")
    print(f"术语总数: {len(terms)}")
    print(f"同义词总数: {len(synonym_index)}")
    print(f"废弃术语数: {len(deprecated_terms)}")
    
    return result


def validate_data(data: Dict) -> tuple[List[str], List[str]]:
    """验证数据完整性
    返回: (errors, warnings)
    """
    errors = []
    warnings = []
    
    # 检查必需字段
    for term_id, term_data in data['terms'].items():
        if not term_data.get('id'):
            errors.append(f"术语 {term_id} 缺少id字段")
        if not term_data.get('label'):
            errors.append(f"术语 {term_id} 缺少label字段")
        if not term_id.startswith('HP:'):
            errors.append(f"术语ID {term_id} 格式不正确")
        
        # 检查parent_ids中的ID是否存在（在清理后应该都不存在了，但保留检查以防万一）
        for parent_id in term_data.get('parent_ids', []):
            if parent_id not in data['terms']:
                warnings.append(f"术语 {term_id} 的父类 {parent_id} 不存在（应该已被自动清理）")
    
    # 检查索引
    for synonym, term_id in data['synonym_index'].items():
        if term_id not in data['terms']:
            errors.append(f"同义词索引中的术语ID {term_id} 不存在")
    
    for alt_id, term_id in data['alternative_id_mapping'].items():
        if term_id not in data['terms']:
            errors.append(f"替代ID映射中的术语ID {term_id} 不存在")
    
    for term_id in data['deprecated_terms']:
        if term_id not in data['terms']:
            errors.append(f"废弃术语列表中的术语ID {term_id} 不存在")
    
    return errors, warnings


def main():
    """主函数"""
    # 文件路径
    script_dir = Path(__file__).parent
    owl_file = script_dir.parent / '源数据' / 'hp.owl'
    output_file = script_dir / 'hpo_data.json'
    
    if not owl_file.exists():
        print(f"错误: 找不到文件 {owl_file}")
        return
    
    # 解析OWL文件
    try:
        data = parse_owl_file(str(owl_file))
    except Exception as e:
        print(f"解析OWL文件时出错: {e}")
        import traceback
        traceback.print_exc()
        return
    
    # 验证数据
    print("\n验证数据完整性...")
    errors, warnings = validate_data(data)
    
    if warnings:
        print(f"发现 {len(warnings)} 个警告（已自动处理）:")
        for warning in warnings[:5]:  # 只显示前5个警告
            print(f"  - {warning}")
        if len(warnings) > 5:
            print(f"  ... 还有 {len(warnings) - 5} 个警告")
    
    if errors:
        print(f"\n发现 {len(errors)} 个错误:")
        for error in errors[:10]:  # 只显示前10个错误
            print(f"  - {error}")
        if len(errors) > 10:
            print(f"  ... 还有 {len(errors) - 10} 个错误")
    else:
        print("数据验证通过！")
    
    # 保存JSON文件
    print(f"\n保存结果到 {output_file}...")
    try:
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print(f"成功保存到 {output_file}")
        
        # 显示文件大小
        file_size = output_file.stat().st_size / (1024 * 1024)  # MB
        print(f"输出文件大小: {file_size:.2f} MB")
    except Exception as e:
        print(f"保存文件时出错: {e}")
        import traceback
        traceback.print_exc()


if __name__ == '__main__':
    main()

