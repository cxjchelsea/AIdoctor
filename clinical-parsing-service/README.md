# 病例理解服务（tool_1）

病例理解服务是AI医生系统的第二个工具，负责将非结构化的患者信息转换为结构化的临床要素。

## 功能

- **医学概念识别**：从文本中识别症状、疾病、药物、检查等医学概念
- **概念归一化**：将口语化表达转换为标准医学术语（CUI/ICD/SNOMED编码）
- **结构化提取**：提取症状、体征、检查、既往史、用药史、过敏史等结构化数据
- **歧义表达判定**：识别模糊表达，生成追问建议

## 技术栈

- Python 3.10+
- FastAPI 0.104.1+
- Pydantic 2.5.0+

## 项目结构

```
clinical-parsing-service/
├── app/
│   ├── main.py                    # FastAPI应用入口
│   ├── api/
│   │   └── routes.py              # API路由
│   ├── services/
│   │   ├── parsing_service.py     # 病例理解服务（主服务）
│   │   ├── concept_recognizer.py # 医学概念识别
│   │   ├── extractor.py          # 结构化提取
│   │   └── ambiguity_detector.py  # 歧义表达判定
│   ├── models/
│   │   ├── request.py             # 请求模型
│   │   └── response.py            # 响应模型
│   ├── utils/
│   │   ├── exceptions.py          # 异常类
│   │   ├── specific_exceptions.py # 具体异常类
│   │   ├── text_processor.py     # 文本处理工具
│   │   └── vocabulary_loader.py  # 词表加载器
│   └── config/
│       └── settings.py            # 配置管理
├── data/
│   └── vocabularies/              # 归一化词表数据
│       ├── symptom_normalization.csv
│       ├── disease_normalization.csv
│       ├── medication_normalization.csv
│       ├── allergy_normalization.csv
│       ├── examination_normalization.csv
│       ├── indicator_normalization.csv
│       └── ambiguity_rules.json
├── docs/
│   └── clinical-parsing-service - 服务实现方案.md
├── requirements.txt
├── Dockerfile
└── README.md
```

## 安装与启动

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 配置环境变量（可选）

创建 `.env` 文件：

```env
# 服务配置
HOST=0.0.0.0
PORT=8082

# OCR服务配置（可选）
OCR_SERVICE_URL=http://localhost:8083
OCR_SERVICE_TIMEOUT=3

# Redis配置（可选）
REDIS_HOST=localhost
REDIS_PORT=6379
```

### 3. 启动服务

```bash
# 方式1：使用run.py
python run.py

# 方式2：使用uvicorn
uvicorn app.main:app --host 0.0.0.0 --port 8082 --reload
```

服务启动后，访问：
- API文档：http://localhost:8082/docs
- 健康检查：http://localhost:8082/health

## API接口

### 1. 病例理解接口

**接口路径**：`POST /api/v1/parsing/parse`

**请求示例**：
```json
{
  "userId": "user_123",
  "sessionId": "session_456",
  "cdpId": "cdp_789",
  "text": "我最近胸口闷，走几步就喘，之前有高血压"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "concepts": [
      {
        "originalText": "胸口闷",
        "normalizedSymptom": "胸闷样不适",
        "cui": "C0232118",
        "confidence": 0.95,
        "conceptType": "symptom"
      }
    ],
    "structuredData": {
      "symptoms": [
        {
          "name": "胸闷样不适",
          "cui": "C0232118",
          "duration": "recent",
          "severity": null,
          "trigger": null
        }
      ],
      "medicalHistory": [
        {
          "disease": "高血压病",
          "icd": "I10",
          "status": "ongoing"
        }
      ]
    },
    "ambiguousExpressions": null
  }
}
```

### 2. 健康检查接口

**接口路径**：`GET /health`

**响应示例**：
```json
{
  "status": "healthy",
  "version": "1.0.0",
  "service": "clinical-parsing-service"
}
```

## 归一化词表

服务使用以下归一化词表：

1. **症状归一化词表** (`symptom_normalization.csv`)
2. **疾病名称归一化词表** (`disease_normalization.csv`)
3. **药物名称归一化词表** (`medication_normalization.csv`)
4. **过敏源归一化词表** (`allergy_normalization.csv`)
5. **检查项目归一化词表** (`examination_normalization.csv`)
6. **指标名称归一化词表** (`indicator_normalization.csv`)

词表文件位于 `data/vocabularies/` 目录，支持CSV格式。

## 错误码

服务使用以下错误码范围（1100-1199）：

| 错误码 | 说明 |
|--------|------|
| 1101 | 医学概念识别失败 |
| 1102 | 概念归一化失败 |
| 1103 | 多模态理解失败 |
| 1104 | 结构化提取失败 |
| 1105 | 歧义表达判定失败 |
| 1106 | OCR识别失败（tool_1调用） |

## 开发

### 运行测试

```bash
# 运行单元测试
pytest tests/unit/

# 运行集成测试
pytest tests/integration/
```

### 代码规范

- 使用Black进行代码格式化
- 使用Pylint进行代码检查

## 参考文档

- [服务实现方案](./docs/clinical-parsing-service%20-%20服务实现方案.md)
- [AI医生系统-业务逻辑详细设计.md](../../docs/AI医生/2.项目前置设计/AI医生系统-业务逻辑详细设计.md)
- [AI医生系统-技术架构设计.md](../../docs/AI医生/1.项目结构设计/AI医生系统-技术架构设计.md)

