"""
CDP数据读取工具
通过HTTP调用diagnosis-service获取CDP数据
"""
import logging
import httpx
from typing import Dict, Any, List, Optional
import os

logger = logging.getLogger(__name__)

# diagnosis-service的URL（从环境变量或配置中读取）
DIAGNOSIS_SERVICE_URL = os.getenv("DIAGNOSIS_SERVICE_URL", "http://localhost:8080")


async def read_cdp_fields(
    cdp_id: str,
    cdp_version: int,
    read_fields: List[str],
    diagnosis_service_url: Optional[str] = None
) -> Dict[str, Any]:
    """
    从CDP读取指定字段的数据
    
    Args:
        cdp_id: CDP ID
        cdp_version: CDP版本号
        read_fields: 需要读取的字段路径列表（如：["cdp.patient_state", "cdp.ddx"]）
        diagnosis_service_url: diagnosis-service的URL（可选，默认从配置读取）
    
    Returns:
        Dict[str, Any]: 字段路径到字段值的映射
    """
    if diagnosis_service_url is None:
        diagnosis_service_url = DIAGNOSIS_SERVICE_URL
    
    try:
        # 调用diagnosis-service的CDP查询接口
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(
                f"{diagnosis_service_url}/api/v1/diagnosis/cdp/{cdp_id}"
            )
            response.raise_for_status()
            
            result = response.json()
            if result.get("code") != 200:
                raise Exception(f"获取CDP失败: {result.get('message')}")
            
            cdp_data = result.get("data", {})
            
            # 根据read_fields提取字段值
            extracted_data = {}
            for field_path in read_fields:
                value = get_cdp_field_value(cdp_data, field_path)
                extracted_data[field_path] = value
            
            logger.info(f"成功读取CDP字段: cdpId={cdp_id}, fields={read_fields}")
            return extracted_data
            
    except httpx.HTTPError as e:
        logger.error(f"HTTP请求失败: {str(e)}")
        raise Exception(f"获取CDP数据失败: {str(e)}")
    except Exception as e:
        logger.error(f"读取CDP字段失败: cdpId={cdp_id}, error={str(e)}")
        raise


def get_cdp_field_value(cdp_data: Dict[str, Any], field_path: str) -> Any:
    """
    根据字段路径从CDP数据中提取字段值
    
    支持的字段路径格式：
    - cdp.patient_state -> cdp_data["patientState"]
    - cdp.ddx -> cdp_data["ddx"]
    - cdp.triage -> cdp_data["triage"]
    - cdp.workup_plan -> cdp_data["workupPlan"]
    - cdp.management_plan -> cdp_data["managementPlan"]
    - cdp.evidence_graph -> cdp_data["evidenceGraph"]
    - cdp.uncertainty.missing_critical_info -> cdp_data["uncertainty"]["missing_critical_info"]
    
    Args:
        cdp_data: CDP数据（从diagnosis-service获取的完整CDP）
        field_path: 字段路径（如：cdp.patient_state）
    
    Returns:
        Any: 字段值，如果字段不存在则返回None
    """
    # 移除"cdp."前缀
    if field_path.startswith("cdp."):
        field_path = field_path[4:]
    
    # 处理嵌套路径（如：uncertainty.missing_critical_info）
    parts = field_path.split(".")
    value = cdp_data
    
    # 字段名映射（Java驼峰命名 -> Python下划线命名）
    field_name_map = {
        "patient_state": "patientState",
        "workup_plan": "workupPlan",
        "management_plan": "managementPlan",
        "evidence_graph": "evidenceGraph",
        "health_state_assessment": "healthStateAssessment",
        "wellness_plan": "wellnessPlan",
    }
    
    try:
        for i, part in enumerate(parts):
            # 如果是第一层，尝试映射字段名
            if i == 0 and part in field_name_map:
                part = field_name_map[part]
            
            if isinstance(value, dict):
                value = value.get(part)
                if value is None:
                    return None
            else:
                return None
        
        return value
    except (KeyError, TypeError, AttributeError):
        logger.warning(f"字段路径不存在: {field_path}")
        return None

