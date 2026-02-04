# 知识图谱结构搭建 - 快速开始

## ✅ 已实现功能

1. **SchemaService** (`app/services/schema_service.py`)
   - 创建所有唯一性约束（display_id 和 uuid）
   - 创建所有索引
   - 验证结构完整性

2. **CLI脚本** (`scripts/setup_schema.py`)
   - 命令行工具，可直接执行结构搭建
   - 支持参数配置
   - 详细的输出信息

3. **API接口** (`app/api/routes.py`)
   - `POST /api/v1/schema/setup` - 搭建结构
   - `GET /api/v1/schema/validate` - 验证结构

## 🚀 使用方法

### 方式1：CLI脚本（推荐）

```bash
# 进入项目目录
cd knowledge-management-service

# 执行结构搭建
python scripts/setup_schema.py --password password
```

### 方式2：API接口

```bash
# 启动服务
uvicorn app.main:app --host 0.0.0.0 --port 8094

# 搭建结构
curl -X POST http://localhost:8094/api/v1/schema/setup

# 验证结构
curl http://localhost:8094/api/v1/schema/validate
```

## 📋 创建的约束和索引

### 约束（17个）
- 9个 display_id 唯一性约束
- 8个 uuid 唯一性约束

### 索引（9个）
- 常用查询字段的索引（name, category, icd10_code等）

## 📚 详细文档

查看 [结构搭建使用指南](docs/结构搭建使用指南.md) 获取更多信息。

## ⚠️ 注意事项

1. **必须先搭建结构**：在导入数据之前，必须先执行结构搭建
2. **幂等性**：脚本可以安全地重复执行
3. **Neo4j连接**：确保Neo4j已启动并可连接

## 🔄 下一步

结构搭建完成后，可以：
1. 创建节点实例（Category、Symptom、Disease等）
2. 建立关系（BELONGS_TO、HAS_CATEGORY等）

