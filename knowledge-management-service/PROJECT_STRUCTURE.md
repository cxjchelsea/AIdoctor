# 知识库管理服务 - 项目结构

## 📁 目录结构

```
knowledge-management-service/
├── app/                              # 应用主目录
│   ├── __init__.py
│   ├── main.py                       # FastAPI应用入口
│   ├── api/                          # API路由
│   │   ├── __init__.py
│   │   └── routes.py                 # API路由定义
│   ├── services/                     # 业务服务层
│   │   ├── __init__.py
│   │   ├── kg_import_service.py      # 知识图谱导入服务
│   │   ├── table_import_service.py  # 表格文件导入服务
│   │   ├── config_import_service.py  # 配置文件导入服务
│   │   └── validation_service.py    # 数据验证服务
│   ├── importers/                    # 导入器模块
│   │   ├── __init__.py
│   │   ├── kg_importers/             # 知识图谱导入器
│   │   │   ├── __init__.py
│   │   │   ├── coding_importer.py           # 第0项：编码规范导入
│   │   │   ├── standard_dir_importer.py      # 第3项：标准目录导入
│   │   │   ├── chief_complaint_importer.py   # 第5项：主诉模板导入
│   │   │   └── disease_importer.py           # 第6项：疾病模板导入
│   │   ├── table_importers/          # 表格文件导入器
│   │   │   ├── __init__.py
│   │   │   └── vocabulary_importer.py        # 归一化词表导入
│   │   └── config_importers/         # 配置文件导入器
│   │       ├── __init__.py
│   │       ├── chief_complaint_config_importer.py  # 主诉配置导入
│   │       └── disease_config_importer.py           # 疾病配置导入
│   ├── cleaners/                     # 数据清洗器
│   │   ├── __init__.py
│   │   └── data_cleaner.py           # 数据清洗器
│   ├── validators/                    # 数据验证器
│   │   ├── __init__.py
│   │   ├── kg_validator.py           # 知识图谱验证器
│   │   ├── table_validator.py        # 表格验证器
│   │   └── config_validator.py       # 配置验证器
│   ├── models/                        # 数据模型
│   │   ├── __init__.py
│   │   ├── request.py                # 请求模型
│   │   ├── response.py                # 响应模型
│   │   └── import_task.py             # 导入任务模型
│   ├── utils/                         # 工具类
│   │   ├── __init__.py
│   │   ├── neo4j_client.py           # Neo4j客户端
│   │   ├── exceptions.py             # 异常处理
│   │   ├── logger.py                 # 日志工具
│   │   └── metrics.py                # 指标监控
│   └── config/                        # 配置管理
│       ├── __init__.py
│       └── settings.py                # 应用配置
├── data/                              # 数据目录
│   ├── source/                        # 源数据
│   │   ├── tables/                    # 表格文件
│   │   ├── configs/                    # 配置文件
│   │   └── kg/                         # 知识图谱数据
│   └── processed/                      # 处理后数据
├── scripts/                            # 导入脚本
│   ├── __init__.py
│   ├── import_all.py                   # 全量导入脚本
│   └── import_item.py                  # 单项导入脚本
├── tests/                              # 测试文件
│   ├── __init__.py
│   └── test_importers.py               # 导入器测试
├── logs/                               # 日志目录（运行时创建）
├── Dockerfile                          # Docker镜像构建文件
├── requirements.txt                    # Python依赖
├── run.py                              # 启动脚本
├── README.md                           # 项目说明
├── .gitignore                          # Git忽略文件
├── .env.example                        # 环境变量示例
└── PROJECT_STRUCTURE.md                # 项目结构说明（本文件）
```

## 📋 模块说明

### 1. app/api/ - API路由层
- **routes.py**: 定义所有API端点，包括导入、验证、查询等接口

### 2. app/services/ - 业务服务层
- **kg_import_service.py**: 知识图谱导入服务，协调各个知识图谱导入器
- **table_import_service.py**: 表格文件导入服务
- **config_import_service.py**: 配置文件导入服务
- **validation_service.py**: 数据验证服务，协调各个验证器

### 3. app/importers/ - 导入器模块
- **kg_importers/**: 知识图谱导入器
  - `coding_importer.py`: 导入第0项编码规范
  - `standard_dir_importer.py`: 导入第3项标准目录主数据
  - `chief_complaint_importer.py`: 导入第5项主诉模板
  - `disease_importer.py`: 导入第6项疾病模板
- **table_importers/**: 表格文件导入器
  - `vocabulary_importer.py`: 导入归一化词表
- **config_importers/**: 配置文件导入器
  - `chief_complaint_config_importer.py`: 导入主诉配置
  - `disease_config_importer.py`: 导入疾病配置

### 4. app/cleaners/ - 数据清洗器
- **data_cleaner.py**: 数据清洗器，负责数据去重、标准化等

### 5. app/validators/ - 数据验证器
- **kg_validator.py**: 知识图谱验证器
- **table_validator.py**: 表格验证器
- **config_validator.py**: 配置验证器

### 6. app/models/ - 数据模型
- **request.py**: API请求模型
- **response.py**: API响应模型
- **import_task.py**: 导入任务模型

### 7. app/utils/ - 工具类
- **neo4j_client.py**: Neo4j数据库客户端
- **exceptions.py**: 异常处理
- **logger.py**: 日志工具
- **metrics.py**: Prometheus指标监控

### 8. app/config/ - 配置管理
- **settings.py**: 应用配置，使用pydantic-settings管理环境变量

### 9. scripts/ - 导入脚本
- **import_all.py**: 全量导入脚本（CLI工具）
- **import_item.py**: 单项导入脚本（CLI工具）

### 10. tests/ - 测试文件
- **test_importers.py**: 导入器测试

## 🔄 数据流转

```
用户上传文件
  ↓
API路由 (routes.py)
  ↓
业务服务 (services/)
  ↓
导入器 (importers/)
  ↓
数据清洗 (cleaners/)
  ↓
数据验证 (validators/)
  ↓
Neo4j/文件系统
```

## 📝 下一步工作

1. **完善服务实现**：实现各个service和importer的具体逻辑
2. **实现数据清洗**：完善data_cleaner.py的清洗逻辑
3. **实现数据验证**：完善各个validator的验证逻辑
4. **添加版本管理**：实现版本管理服务
5. **完善测试**：编写单元测试和集成测试

---

**最后更新**：2026-01-28

