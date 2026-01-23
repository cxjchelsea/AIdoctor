# 服务拆分迁移说明

## 已迁移的模块

以下模块已迁移到独立服务，请更新相关引用：

### 1. workup-planner → workup-planner-service
- **迁移内容**：`app/workup-planner/` 目录
- **新服务地址**：http://workup-planner-service:8090
- **API路径**：`/api/v1/workup/plan`

### 2. treatment-engine → treatment-engine-service
- **迁移内容**：`app/treatment-engine/` 目录
- **新服务地址**：http://treatment-engine-service:8091
- **API路径**：`/api/v1/treatment/plan`

### 3. risk-assessment-engine → risk-assessment-service
- **迁移内容**：`app/risk-assessment-engine/` 目录
- **新服务地址**：http://risk-assessment-service:8092
- **API路径**：`/api/v1/risk/assess`

### 4. verification_plan_builder → workup-planner-service
- **迁移内容**：`app/builders/verification_plan_builder.py`
- **新位置**：`workup-planner-service/app/services/verification_planner.py`

### 5. conclusion_package_builder → risk-assessment-service
- **迁移内容**：`app/builders/conclusion_package_builder.py`
- **新位置**：`risk-assessment-service/app/builders/conclusion_package_builder.py`

## 需要更新的代码

1. **diagnosis-service** 中的服务调用需要更新为新的服务地址
2. **API路由** 需要更新为调用新服务的API
3. **导入语句** 需要更新为新的服务客户端

## 保留的模块

以下模块保留在 diagnosis-engine-service 中：
- `kg-reasoning-engine/`：知识图谱推理引擎（DR.KNOWS核心）
- `multi-engine-fusion/`：多引擎融合诊断
- `engines/`：各个引擎实现（将整合到 multi-engine-fusion/）
- `classifiers/`：三层分层分类器
- `analyzers/`：证据分析器和推理组织器

