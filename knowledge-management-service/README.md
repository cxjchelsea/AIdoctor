# 知识库管理服务 (knowledge-management-service)

## 服务概述

知识库管理服务是AI医生系统的核心服务之一，负责知识库的导入、清洗、验证和管理。

## 主要功能

1. **知识图谱导入**
   - 第0项：编码规范导入
   - 第3项：标准目录主数据导入
   - 第5项：主诉模板导入
   - 第6项：疾病模板导入

2. **表格文件导入**
   - 归一化词表导入
   - 标准目录导入

3. **配置文件导入**
   - 主诉配置导入
   - 疾病配置导入
   - 规则配置导入

4. **数据验证**
   - 知识图谱验证
   - 表格验证
   - 配置验证

5. **数据清洗**
   - 数据去重
   - 数据标准化
   - 数据补全

6. **版本管理**
   - 版本创建
   - 版本回滚
   - 版本对比

## 技术栈

- **框架**: FastAPI 0.104.1
- **数据库**: Neo4j 5.14.0（知识图谱）
- **文件处理**: pandas, openpyxl, PyYAML
- **其他**: Pydantic 2.7.0, Prometheus Client

## 项目结构

```
knowledge-management-service/
├── app/
│   ├── main.py                    # FastAPI应用入口
│   ├── api/
│   │   └── routes.py              # API路由
│   ├── services/
│   │   ├── kg_import_service.py  # 知识图谱导入服务
│   │   ├── table_import_service.py # 表格文件导入服务
│   │   ├── config_import_service.py # 配置文件导入服务
│   │   └── validation_service.py # 数据验证服务
│   ├── importers/                 # 导入器
│   ├── cleaners/                  # 清洗器
│   ├── validators/                # 验证器
│   ├── models/                    # 数据模型
│   ├── utils/                     # 工具类
│   └── config/                    # 配置管理
├── data/                          # 数据目录
│   ├── source/                    # 源数据
│   └── processed/                 # 处理后数据
├── scripts/                       # 导入脚本
├── tests/                         # 测试文件
├── Dockerfile
├── requirements.txt
└── README.md
```

## 快速开始

### 安装依赖

```bash
pip install -r requirements.txt
```

### 配置环境变量

创建 `.env` 文件：

```env
NEO4J_URI=bolt://localhost:7687
NEO4J_USER=neo4j
NEO4J_PASSWORD=password
DATA_DIR=data
LOG_LEVEL=INFO
```

### 启动服务

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8094
```

### Docker启动

```bash
docker build -t knowledge-management-service .
docker run -p 8094:8094 knowledge-management-service
```

## API接口

### 导入接口

- `POST /api/v1/import/kg/coding-standards` - 导入编码规范
- `POST /api/v1/import/kg/standard-directory` - 导入标准目录
- `POST /api/v1/import/kg/chief-complaint` - 导入主诉模板
- `POST /api/v1/import/kg/disease` - 导入疾病模板
- `POST /api/v1/import/table/vocabulary` - 导入归一化词表
- `POST /api/v1/import/config/chief-complaint` - 导入主诉配置
- `POST /api/v1/import/config/disease` - 导入疾病配置

### 验证接口

- `POST /api/v1/validate/kg` - 验证知识图谱
- `POST /api/v1/validate/table` - 验证表格
- `POST /api/v1/validate/config` - 验证配置

### 查询接口

- `GET /api/v1/status` - 获取服务状态
- `GET /api/v1/tasks` - 获取导入任务列表
- `GET /api/v1/tasks/{task_id}` - 获取任务详情

## 开发计划

- [x] Phase 1: 基础框架搭建
- [ ] Phase 2: 导入功能实现
- [ ] Phase 3: 清洗和验证功能
- [ ] Phase 4: 可视化和版本管理

## 许可证

MIT License

