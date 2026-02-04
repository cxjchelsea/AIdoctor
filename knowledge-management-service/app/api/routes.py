"""
知识库管理服务API路由
"""
from fastapi import APIRouter, UploadFile, File, HTTPException
from typing import List, Optional
from app.models.request import ImportRequest, ValidationRequest
from app.models.response import ImportResult, ValidationResult, TaskStatus
from app.services.kg_import_service import KGImportService
from app.services.table_import_service import TableImportService
from app.services.config_import_service import ConfigImportService
from app.services.validation_service import ValidationService
from app.services.schema_service import SchemaService

router = APIRouter()

# 初始化服务
kg_import_service = KGImportService()
table_import_service = TableImportService()
config_import_service = ConfigImportService()
validation_service = ValidationService()
schema_service = SchemaService()


@router.post("/import/kg/coding-standards", response_model=ImportResult)
async def import_coding_standards(file: UploadFile = File(...)):
    """导入第0项：编码规范"""
    try:
        result = await kg_import_service.import_coding_standards(file)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/import/kg/standard-directory", response_model=ImportResult)
async def import_standard_directory(file: UploadFile = File(...)):
    """导入第3项：标准目录主数据"""
    try:
        result = await kg_import_service.import_standard_directory(file)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/import/kg/chief-complaint", response_model=ImportResult)
async def import_chief_complaint(file: UploadFile = File(...)):
    """导入第5项：主诉模板"""
    try:
        result = await kg_import_service.import_chief_complaint(file)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/import/kg/disease", response_model=ImportResult)
async def import_disease(file: UploadFile = File(...)):
    """导入第6项：疾病模板"""
    try:
        result = await kg_import_service.import_disease(file)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/import/table/vocabulary", response_model=ImportResult)
async def import_vocabulary(file: UploadFile = File(...)):
    """导入归一化词表"""
    try:
        result = await table_import_service.import_vocabulary(file)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/import/config/chief-complaint", response_model=ImportResult)
async def import_chief_complaint_config(file: UploadFile = File(...)):
    """导入主诉配置"""
    try:
        result = await config_import_service.import_chief_complaint_config(file)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/import/config/disease", response_model=ImportResult)
async def import_disease_config(file: UploadFile = File(...)):
    """导入疾病配置"""
    try:
        result = await config_import_service.import_disease_config(file)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/validate/kg", response_model=ValidationResult)
async def validate_kg(request: ValidationRequest):
    """验证知识图谱"""
    try:
        result = await validation_service.validate_kg(request)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/validate/table", response_model=ValidationResult)
async def validate_table(request: ValidationRequest):
    """验证表格"""
    try:
        result = await validation_service.validate_table(request)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/validate/config", response_model=ValidationResult)
async def validate_config(request: ValidationRequest):
    """验证配置"""
    try:
        result = await validation_service.validate_config(request)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/status")
async def get_status():
    """获取服务状态"""
    return {
        "status": "running",
        "service": "knowledge-management-service"
    }


@router.get("/tasks", response_model=List[TaskStatus])
async def get_tasks():
    """获取导入任务列表"""
    # TODO: 实现任务列表查询
    return []


@router.get("/tasks/{task_id}", response_model=TaskStatus)
async def get_task(task_id: str):
    """获取任务详情"""
    # TODO: 实现任务详情查询
    raise HTTPException(status_code=404, detail="Task not found")


@router.post("/schema/setup")
async def setup_schema():
    """搭建知识图谱结构（创建约束和索引）"""
    try:
        result = await schema_service.setup_schema()
        return {
            "status": "success",
            "message": "结构搭建完成",
            "result": result
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/schema/validate")
async def validate_schema():
    """验证知识图谱结构"""
    try:
        result = await schema_service.validate_schema()
        return {
            "status": "success",
            "result": result
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/schema/constraints")
async def get_all_constraints():
    """获取所有约束的详细信息"""
    try:
        constraints = await schema_service.get_all_constraints()
        return {
            "status": "success",
            "result": constraints,
            "count": len(constraints)
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/schema/indexes")
async def get_all_indexes():
    """获取所有索引的详细信息"""
    try:
        indexes = await schema_service.get_all_indexes()
        return {
            "status": "success",
            "result": indexes,
            "count": len(indexes)
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/schema/node-labels")
async def get_all_node_labels():
    """获取所有节点类型（Node Labels）的详细信息"""
    try:
        node_labels = await schema_service.get_all_node_labels()
        return {
            "status": "success",
            "result": node_labels,
            "count": len(node_labels)
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/schema/relationship-types")
async def get_all_relationship_types():
    """获取所有关系类型（Relationship Types）的详细信息"""
    try:
        relationship_types = await schema_service.get_all_relationship_types()
        return {
            "status": "success",
            "result": relationship_types,
            "count": len(relationship_types)
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/schema/subgraphs")
async def get_all_subgraphs():
    """获取所有子图的详细信息"""
    try:
        subgraphs = await schema_service.get_all_subgraphs()
        return {
            "status": "success",
            "result": subgraphs,
            "count": len(subgraphs)
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/schema/complete")
async def get_complete_schema():
    """获取完整的Schema信息（节点类型、关系类型、子图、约束、索引）"""
    try:
        schema = await schema_service.get_complete_schema()
        return {
            "status": "success",
            "result": schema
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

