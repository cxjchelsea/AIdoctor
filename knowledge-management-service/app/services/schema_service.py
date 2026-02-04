"""
知识图谱结构搭建服务
负责创建约束、索引等Schema定义
包括：节点类型、关系类型、子图、约束、索引
"""
from typing import Dict, Any, List
import logging
from app.utils.neo4j_client import Neo4jClient
from app.config.settings import settings

logger = logging.getLogger(__name__)

# 定义所有节点类型（Node Labels）
EXPECTED_NODE_LABELS = {
    # 语义层节点类型
    "Symptom": {"layer": "语义层", "module": "第3项", "description": "标准症状"},
    "ChiefComplaint": {"layer": "语义层", "module": "第3项+第5项", "description": "标准主诉"},
    "Disease": {"layer": "语义层", "module": "第3项+第6项", "description": "标准疾病"},
    "Examination": {"layer": "语义层", "module": "第3项", "description": "标准检查"},
    "Evidence": {"layer": "语义层", "module": "第7项", "description": "证据条目"},
    "Category": {"layer": "语义层", "module": "第3项", "description": "分类"},
    "ReasoningSubgroup": {"layer": "语义层", "module": "第6项", "description": "推理子组"},
    "DifferentialFrameworkSubclass": {"layer": "语义层", "module": "第5项", "description": "鉴别诊断框架子类"},
    "DifferentialPoint": {"layer": "语义层", "module": "第5项+第6项", "description": "关键差异点/鉴别点"},
    "Treatment": {"layer": "语义层", "module": "第0项", "description": "治疗（编码规范）"},
    "Sign": {"layer": "语义层", "module": "第0项", "description": "体征（编码规范）"},
    "Indicator": {"layer": "语义层", "module": "第0项", "description": "指标（编码规范）"},
    "Drug": {"layer": "语义层", "module": "第0项", "description": "药物（编码规范）"},
    # 工程层节点类型
    "Document": {"layer": "工程层", "module": "工程层", "description": "源文档"},
    "Chunk": {"layer": "工程层", "module": "工程层", "description": "文档片段（含向量嵌入）"},
    "Mention": {"layer": "工程层", "module": "工程层", "description": "实体提及"},
    "Source": {"layer": "工程层", "module": "工程层", "description": "数据来源"},
    "Version": {"layer": "工程层", "module": "工程层", "description": "版本信息"},
}

# 定义所有关系类型（Relationship Types）
EXPECTED_RELATIONSHIP_TYPES = {
    # 基础关系
    "BELONGS_TO": {"direction": "→", "from": ["Symptom", "Disease", "Examination"], "to": ["Category"], "module": "第3项", "description": "实体属于分类"},
    "HAS_CATEGORY": {"direction": "→", "from": ["Category"], "to": ["Category"], "module": "第3项", "description": "分类包含子分类"},
    "ALIGNED_WITH": {"direction": "↔", "from": ["Entity"], "to": ["Entity"], "module": "第0项", "description": "编码对齐关系"},
    # 主诉相关关系
    "HAS_CANDIDATE_DISEASE": {"direction": "→", "from": ["ChiefComplaint"], "to": ["Disease"], "module": "第5项", "description": "主诉有候选疾病"},
    "HAS_REASONING_SUBGROUP": {"direction": "→", "from": ["ChiefComplaint"], "to": ["ReasoningSubgroup"], "module": "第5项", "description": "主诉有推理子组"},
    "HAS_DIFFERENTIAL_SUBCLASS": {"direction": "→", "from": ["ChiefComplaint"], "to": ["DifferentialFrameworkSubclass"], "module": "第5项", "description": "主诉有鉴别诊断框架子类"},
    "HAS_DIFFERENTIAL_POINT": {"direction": "→", "from": ["ChiefComplaint"], "to": ["DifferentialPoint"], "module": "第5项", "description": "主诉有关键差异点"},
    "RECOMMENDS_EXAMINATION": {"direction": "→", "from": ["ChiefComplaint"], "to": ["Examination"], "module": "第5项", "description": "主诉推荐检查"},
    # 疾病相关关系
    "SUPPORTED_BY": {"direction": "→", "from": ["Disease"], "to": ["Evidence"], "module": "第6项", "description": "疾病被证据支持"},
    "OPPOSED_BY": {"direction": "→", "from": ["Disease"], "to": ["Evidence"], "module": "第6项", "description": "疾病被证据反对"},
    "REQUIRES": {"direction": "→", "from": ["Disease"], "to": ["Evidence"], "module": "第6项", "description": "疾病需要证据"},
    "DIFFERENTIAL_WITH": {"direction": "↔", "from": ["Disease"], "to": ["Disease"], "module": "第6项", "description": "疾病需要与另一疾病鉴别"},
    "REQUIRES_EXAMINATION": {"direction": "→", "from": ["Disease"], "to": ["Examination"], "module": "第6项", "description": "疾病需要检查"},
    "BELONGS_TO_SUBGROUP": {"direction": "→", "from": ["Disease"], "to": ["ReasoningSubgroup"], "module": "第6项", "description": "疾病属于推理子组"},
    "ESCALATION_TRIGGER": {"direction": "→", "from": ["Disease"], "to": ["Evidence"], "module": "第6项", "description": "疾病升级触发条件"},
    # 组织关系
    "CONTAINS_DISEASE": {"direction": "→", "from": ["DifferentialFrameworkSubclass", "ReasoningSubgroup"], "to": ["Disease"], "module": "第5项+第6项", "description": "子类/子组包含疾病"},
    "AFFECTS": {"direction": "→", "from": ["DifferentialPoint"], "to": ["ReasoningSubgroup", "Disease"], "module": "第5项", "description": "差异点影响子组/疾病"},
    # 工程层关系
    "CONTAINS": {"direction": "→", "from": ["Document", "Chunk"], "to": ["Chunk", "Mention"], "module": "工程层", "description": "文档包含词块/词块包含实体提及"},
    "REFERS_TO": {"direction": "→", "from": ["Mention"], "to": ["Entity"], "module": "工程层", "description": "提及指向实体"},
    "HAS_EVIDENCE": {"direction": "→", "from": ["Edge"], "to": ["Chunk"], "module": "工程层", "description": "关系边链接到证据chunk"},
    "FROM_DOC": {"direction": "→", "from": ["Entity", "Edge"], "to": ["Document"], "module": "工程层", "description": "实体/关系链接到来源文档"},
}

# 定义子图（Subgraph）
SUBGRAPHS = {
    "编码子图": {
        "labels": ["Symptom", "Disease", "Examination", "Treatment", "Sign", "Indicator", "Drug"],
        "module": "第0项",
        "description": "所有包含编码属性的实体节点"
    },
    "标准目录子图": {
        "labels": ["Symptom", "ChiefComplaint", "Disease", "Examination", "Category"],
        "module": "第3项",
        "description": "标准目录主数据及其分类关系"
    },
    "主诉知识子图": {
        "labels": ["ChiefComplaint", "DifferentialFrameworkSubclass", "DifferentialPoint"],
        "module": "第5项",
        "description": "主诉扩展属性、鉴别诊断框架子类、关键差异点及其关系"
    },
    "疾病知识子图": {
        "labels": ["Disease", "ReasoningSubgroup", "DifferentialPoint"],
        "module": "第6项",
        "description": "疾病扩展属性、推理子组、鉴别点及其关系"
    },
    "证据子图": {
        "labels": ["Evidence"],
        "module": "第7项",
        "description": "证据节点及其关系（待设计）"
    },
    "工程子图": {
        "labels": ["Document", "Chunk", "Mention", "Source", "Version"],
        "module": "工程层",
        "description": "文档、词块、提及、来源、版本及其关系"
    },
}


class SchemaService:
    """知识图谱结构搭建服务"""
    
    def __init__(self, neo4j_client: Neo4jClient = None):
        """
        初始化结构搭建服务
        
        Args:
            neo4j_client: Neo4j客户端，如果为None则自动创建
        """
        if neo4j_client:
            self.neo4j_client = neo4j_client
        else:
            self.neo4j_client = Neo4jClient(
                uri=settings.neo4j_uri,
                user=settings.neo4j_user,
                password=settings.neo4j_password,
                max_connection_lifetime=settings.neo4j_max_connection_lifetime,
                max_connection_pool_size=settings.neo4j_max_connection_pool_size,
                connection_acquisition_timeout=settings.neo4j_connection_timeout
            )
        logger.info("结构搭建服务初始化完成")
    
    async def setup_schema(self) -> Dict[str, Any]:
        """
        搭建完整的知识图谱结构
        
        包括：
        1. 创建所有唯一性约束（display_id和uuid）
        2. 创建所有索引
        
        Returns:
            搭建结果
        """
        logger.info("开始搭建知识图谱结构...")
        results = {
            "constraints_created": [],
            "constraints_failed": [],
            "indexes_created": [],
            "indexes_failed": [],
            "errors": []
        }
        
        try:
            # 1. 创建display_id唯一性约束
            display_id_constraints = [
                ("symptom_display_id", "Symptom", "display_id"),
                ("chief_complaint_display_id", "ChiefComplaint", "display_id"),
                ("disease_display_id", "Disease", "display_id"),
                ("examination_display_id", "Examination", "display_id"),
                ("category_display_id", "Category", "display_id"),
                ("evidence_display_id", "Evidence", "display_id"),
                ("reasoning_subgroup_display_id", "ReasoningSubgroup", "display_id"),
                ("differential_framework_subclass_display_id", "DifferentialFrameworkSubclass", "display_id"),
                ("differential_point_display_id", "DifferentialPoint", "display_id"),
            ]
            
            for constraint_name, label, field in display_id_constraints:
                try:
                    query = f"""
                    CREATE CONSTRAINT {constraint_name} IF NOT EXISTS
                    FOR (n:{label}) REQUIRE n.{field} IS UNIQUE
                    """
                    self.neo4j_client.execute_write(query)
                    results["constraints_created"].append(constraint_name)
                    logger.info(f"✅ 创建约束: {constraint_name}")
                except Exception as e:
                    error_msg = f"约束 {constraint_name} 创建失败: {str(e)}"
                    results["constraints_failed"].append(constraint_name)
                    results["errors"].append(error_msg)
                    logger.error(error_msg)
            
            # 2. 创建UUID唯一性约束
            uuid_constraints = [
                ("symptom_uuid", "Symptom", "symptom_uuid"),
                ("chief_complaint_uuid", "ChiefComplaint", "chief_complaint_uuid"),
                ("disease_uuid", "Disease", "disease_uuid"),
                ("examination_uuid", "Examination", "examination_uuid"),
                ("category_uuid", "Category", "category_uuid"),
                ("reasoning_subgroup_uuid", "ReasoningSubgroup", "subgroup_uuid"),
                ("differential_framework_subclass_uuid", "DifferentialFrameworkSubclass", "subclass_uuid"),
                ("differential_point_uuid", "DifferentialPoint", "differential_point_uuid"),
            ]
            
            for constraint_name, label, field in uuid_constraints:
                try:
                    query = f"""
                    CREATE CONSTRAINT {constraint_name} IF NOT EXISTS
                    FOR (n:{label}) REQUIRE n.{field} IS UNIQUE
                    """
                    self.neo4j_client.execute_write(query)
                    results["constraints_created"].append(constraint_name)
                    logger.info(f"✅ 创建UUID约束: {constraint_name}")
                except Exception as e:
                    error_msg = f"UUID约束 {constraint_name} 创建失败: {str(e)}"
                    results["constraints_failed"].append(constraint_name)
                    results["errors"].append(error_msg)
                    logger.error(error_msg)
            
            # 3. 创建节点属性索引
            node_indexes = [
                ("symptom_name", "Symptom", "name"),
                ("disease_name", "Disease", "name"),
                ("disease_category", "Disease", "category"),
                ("disease_icd10", "Disease", "icd10_code"),
                ("disease_icd11", "Disease", "icd11_code"),
                ("disease_tier_level", "Disease", "tier_level"),
                ("chief_complaint_name", "ChiefComplaint", "name"),
                ("examination_name", "Examination", "name"),
                ("category_name", "Category", "name"),
            ]
            
            for index_name, label, field in node_indexes:
                try:
                    query = f"""
                    CREATE INDEX {index_name} IF NOT EXISTS
                    FOR (n:{label}) ON (n.{field})
                    """
                    self.neo4j_client.execute_write(query)
                    results["indexes_created"].append(index_name)
                    logger.info(f"✅ 创建索引: {index_name}")
                except Exception as e:
                    error_msg = f"索引 {index_name} 创建失败: {str(e)}"
                    results["indexes_failed"].append(index_name)
                    results["errors"].append(error_msg)
                    logger.error(error_msg)
            
            # 4. 验证结构
            validation_result = await self.validate_schema()
            results["validation"] = validation_result
            
            logger.info(f"✅ 结构搭建完成！创建了 {len(results['constraints_created'])} 个约束，{len(results['indexes_created'])} 个索引")
            
        except Exception as e:
            error_msg = f"结构搭建失败: {str(e)}"
            results["errors"].append(error_msg)
            logger.error(error_msg)
            raise
        
        return results
    
    async def validate_schema(self) -> Dict[str, Any]:
        """
        验证结构定义
        
        Returns:
            验证结果
        """
        logger.info("开始验证结构定义...")
        validation = {
            "constraints_count": 0,
            "indexes_count": 0,
            "missing_constraints": [],
            "missing_indexes": [],
            "is_valid": True
        }
        
        try:
            # 查询所有约束
            constraints_query = "SHOW CONSTRAINTS"
            constraints = self.neo4j_client.execute_query(constraints_query)
            validation["constraints_count"] = len(constraints)
            
            # 查询所有索引
            indexes_query = "SHOW INDEXES"
            indexes = self.neo4j_client.execute_query(indexes_query)
            validation["indexes_count"] = len(indexes)
            
            # 验证必需的约束是否存在
            required_constraints = [
                "symptom_display_id", "disease_display_id", 
                "chief_complaint_display_id", "category_display_id"
            ]
            
            constraint_names = [c.get("name", "") for c in constraints]
            for req_constraint in required_constraints:
                if req_constraint not in constraint_names:
                    validation["missing_constraints"].append(req_constraint)
                    validation["is_valid"] = False
            
            logger.info(f"✅ 结构验证完成：{validation['constraints_count']} 个约束，{validation['indexes_count']} 个索引")
            
        except Exception as e:
            logger.error(f"结构验证失败: {str(e)}")
            validation["is_valid"] = False
            validation["error"] = str(e)
        
        return validation
    
    async def get_all_constraints(self) -> List[Dict[str, Any]]:
        """
        获取所有约束的详细信息
        
        Returns:
            约束列表
        """
        logger.info("开始查询所有约束...")
        try:
            constraints_query = "SHOW CONSTRAINTS"
            constraints = self.neo4j_client.execute_query(constraints_query)
            
            # 格式化约束信息
            formatted_constraints = []
            for constraint in constraints:
                # Neo4j返回的字段可能是entityType或labelsOrTypes
                entity_type = constraint.get("entityType") or constraint.get("labelsOrTypes", [])
                if isinstance(entity_type, list) and len(entity_type) > 0:
                    entity_type = entity_type[0]
                elif not entity_type:
                    entity_type = ""
                
                properties = constraint.get("properties", [])
                if isinstance(properties, str):
                    properties = [properties]
                
                formatted_constraints.append({
                    "name": constraint.get("name", ""),
                    "type": constraint.get("type", ""),
                    "entityType": entity_type,
                    "properties": properties if isinstance(properties, list) else [],
                    "description": self._get_constraint_description(constraint.get("name", ""))
                })
            
            logger.info(f"✅ 查询到 {len(formatted_constraints)} 个约束")
            return formatted_constraints
            
        except Exception as e:
            logger.error(f"查询约束失败: {str(e)}")
            raise
    
    async def get_all_indexes(self) -> List[Dict[str, Any]]:
        """
        获取所有索引的详细信息
        
        Returns:
            索引列表
        """
        logger.info("开始查询所有索引...")
        try:
            indexes_query = "SHOW INDEXES"
            indexes = self.neo4j_client.execute_query(indexes_query)
            
            # 格式化索引信息
            formatted_indexes = []
            for index in indexes:
                # Neo4j返回的字段可能是entityType或labelsOrTypes
                entity_type = index.get("entityType") or index.get("labelsOrTypes", [])
                if isinstance(entity_type, list) and len(entity_type) > 0:
                    entity_type = entity_type[0]
                elif not entity_type:
                    entity_type = ""
                
                properties = index.get("properties", [])
                if isinstance(properties, str):
                    properties = [properties]
                
                formatted_indexes.append({
                    "name": index.get("name", ""),
                    "type": index.get("type", ""),
                    "entityType": entity_type,
                    "properties": properties if isinstance(properties, list) else [],
                    "state": index.get("state", "UNKNOWN"),
                    "description": self._get_index_description(index.get("name", ""))
                })
            
            logger.info(f"✅ 查询到 {len(formatted_indexes)} 个索引")
            return formatted_indexes
            
        except Exception as e:
            logger.error(f"查询索引失败: {str(e)}")
            raise
    
    def _get_constraint_description(self, name: str) -> str:
        """获取约束的描述信息"""
        descriptions = {
            "symptom_display_id": "症状 display_id 唯一性约束",
            "chief_complaint_display_id": "主诉 display_id 唯一性约束",
            "disease_display_id": "疾病 display_id 唯一性约束",
            "examination_display_id": "检查 display_id 唯一性约束",
            "category_display_id": "分类 display_id 唯一性约束",
            "evidence_display_id": "证据 display_id 唯一性约束",
            "reasoning_subgroup_display_id": "推理子组 display_id 唯一性约束",
            "differential_framework_subclass_display_id": "鉴别框架子类 display_id 唯一性约束",
            "differential_point_display_id": "关键差异点 display_id 唯一性约束",
            "symptom_uuid": "症状 UUID 唯一性约束",
            "chief_complaint_uuid": "主诉 UUID 唯一性约束",
            "disease_uuid": "疾病 UUID 唯一性约束",
            "examination_uuid": "检查 UUID 唯一性约束",
            "category_uuid": "分类 UUID 唯一性约束",
            "reasoning_subgroup_uuid": "推理子组 UUID 唯一性约束",
            "differential_framework_subclass_uuid": "鉴别框架子类 UUID 唯一性约束",
            "differential_point_uuid": "关键差异点 UUID 唯一性约束",
        }
        return descriptions.get(name, "")
    
    def _get_index_description(self, name: str) -> str:
        """获取索引的描述信息"""
        descriptions = {
            "symptom_name": "症状名称索引",
            "disease_name": "疾病名称索引",
            "disease_category": "疾病分类索引",
            "disease_icd10": "疾病 ICD-10 编码索引",
            "disease_icd11": "疾病 ICD-11 编码索引",
            "disease_tier_level": "疾病分层索引",
            "chief_complaint_name": "主诉名称索引",
            "examination_name": "检查名称索引",
            "category_name": "分类名称索引",
        }
        return descriptions.get(name, "")
    
    async def get_all_node_labels(self) -> List[Dict[str, Any]]:
        """
        获取所有节点类型（Node Labels）的详细信息
        
        Returns:
            节点类型列表
        """
        logger.info("开始查询所有节点类型...")
        try:
            # 查询实际存在的节点类型（通过查询所有节点的标签）
            query = """
            MATCH (n)
            RETURN DISTINCT labels(n) as labels
            """
            actual_labels_result = self.neo4j_client.execute_query(query)
            actual_label_names = set()
            for item in actual_labels_result:
                labels = item.get("labels", [])
                if isinstance(labels, list):
                    actual_label_names.update(labels)
                elif labels:
                    actual_label_names.add(labels)
            
            # 格式化节点类型信息
            formatted_labels = []
            for label_name, label_info in EXPECTED_NODE_LABELS.items():
                exists = label_name in actual_label_names
                formatted_labels.append({
                    "name": label_name,
                    "layer": label_info["layer"],
                    "module": label_info["module"],
                    "description": label_info["description"],
                    "exists": exists,
                    "node_count": 0  # 可以通过查询获取实际节点数量
                })
            
            # 添加实际存在但未定义的节点类型
            for label_name in actual_label_names:
                if label_name not in EXPECTED_NODE_LABELS:
                    formatted_labels.append({
                        "name": label_name,
                        "layer": "未知",
                        "module": "未知",
                        "description": "未在Schema中定义的节点类型",
                        "exists": True,
                        "node_count": 0
                    })
            
            logger.info(f"✅ 查询到 {len(formatted_labels)} 个节点类型")
            return formatted_labels
            
        except Exception as e:
            logger.error(f"查询节点类型失败: {str(e)}")
            raise
    
    async def get_all_relationship_types(self) -> List[Dict[str, Any]]:
        """
        获取所有关系类型（Relationship Types）的详细信息
        
        Returns:
            关系类型列表
        """
        logger.info("开始查询所有关系类型...")
        try:
            # 查询实际存在的关系类型（通过查询所有关系的类型）
            query = """
            MATCH ()-[r]->()
            RETURN DISTINCT type(r) as relationshipType
            """
            actual_types = self.neo4j_client.execute_query(query)
            actual_type_names = {item.get("relationshipType", "") for item in actual_types}
            
            # 格式化关系类型信息
            formatted_types = []
            for type_name, type_info in EXPECTED_RELATIONSHIP_TYPES.items():
                exists = type_name in actual_type_names
                formatted_types.append({
                    "name": type_name,
                    "direction": type_info["direction"],
                    "from": type_info["from"],
                    "to": type_info["to"],
                    "module": type_info["module"],
                    "description": type_info["description"],
                    "exists": exists,
                    "relationship_count": 0  # 可以通过查询获取实际关系数量
                })
            
            # 添加实际存在但未定义的关系类型
            for type_name in actual_type_names:
                if type_name not in EXPECTED_RELATIONSHIP_TYPES:
                    formatted_types.append({
                        "name": type_name,
                        "direction": "未知",
                        "from": [],
                        "to": [],
                        "module": "未知",
                        "description": "未在Schema中定义的关系类型",
                        "exists": True,
                        "relationship_count": 0
                    })
            
            logger.info(f"✅ 查询到 {len(formatted_types)} 个关系类型")
            return formatted_types
            
        except Exception as e:
            logger.error(f"查询关系类型失败: {str(e)}")
            raise
    
    async def get_all_subgraphs(self) -> List[Dict[str, Any]]:
        """
        获取所有子图的详细信息
        
        Returns:
            子图列表
        """
        logger.info("开始查询所有子图...")
        try:
            formatted_subgraphs = []
            for subgraph_name, subgraph_info in SUBGRAPHS.items():
                # 查询每个子图的节点数量
                node_counts = {}
                for label in subgraph_info["labels"]:
                    try:
                        count_query = f"MATCH (n:{label}) RETURN count(n) as count"
                        result = self.neo4j_client.execute_query(count_query)
                        node_counts[label] = result[0].get("count", 0) if result else 0
                    except:
                        node_counts[label] = 0
                
                total_nodes = sum(node_counts.values())
                
                formatted_subgraphs.append({
                    "name": subgraph_name,
                    "labels": subgraph_info["labels"],
                    "module": subgraph_info["module"],
                    "description": subgraph_info["description"],
                    "node_counts": node_counts,
                    "total_nodes": total_nodes
                })
            
            logger.info(f"✅ 查询到 {len(formatted_subgraphs)} 个子图")
            return formatted_subgraphs
            
        except Exception as e:
            logger.error(f"查询子图失败: {str(e)}")
            raise
    
    async def get_complete_schema(self) -> Dict[str, Any]:
        """
        获取完整的Schema信息（节点类型、关系类型、子图、约束、索引）
        
        Returns:
            完整的Schema信息
        """
        logger.info("开始查询完整Schema信息...")
        try:
            schema = {
                "node_labels": await self.get_all_node_labels(),
                "relationship_types": await self.get_all_relationship_types(),
                "subgraphs": await self.get_all_subgraphs(),
                "constraints": await self.get_all_constraints(),
                "indexes": await self.get_all_indexes(),
                "summary": {
                    "total_node_labels": len(EXPECTED_NODE_LABELS),
                    "total_relationship_types": len(EXPECTED_RELATIONSHIP_TYPES),
                    "total_subgraphs": len(SUBGRAPHS),
                    "total_constraints": 0,
                    "total_indexes": 0
                }
            }
            
            schema["summary"]["total_constraints"] = len(schema["constraints"])
            schema["summary"]["total_indexes"] = len(schema["indexes"])
            
            logger.info("✅ 完整Schema信息查询完成")
            return schema
            
        except Exception as e:
            logger.error(f"查询完整Schema失败: {str(e)}")
            raise
    
    def close(self):
        """关闭连接"""
        if self.neo4j_client:
            self.neo4j_client.close()

