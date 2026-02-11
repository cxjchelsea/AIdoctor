# 健康状态判定服务（tool_0）

健康状态判定服务是AI医生系统的第一个工具，负责判断"这个人，现在需要被当成'病人'对待吗？"

## 功能

### 核心功能

1. **入口判定流程（P0模块）**
   - Step 1：接收用户输入
   - Step 2：识别用户是否"有症状/困扰"
   - Step 3：方向澄清（仅对"情况C"触发）
   - Step 4：危险信号检查（所有用户都要过一次）
   - Step 5：输出路径结果并跳转

2. **工作态判定**
   - 健康管理态（wellness_mode）：无症状或症状轻微，进入健康筛查路径（A）
   - 临床诊疗态（clinical_mode）：存在症状/困扰，进入症状诊断路径（B）

3. **风险评估**
   - 症状严重程度评估
   - 早期风险信号识别
   - 红旗信号识别
   - 风险等级计算（L1/L2/L3/L4）

4. **健康管理计划生成**
   - 仅在健康管理态时生成
   - 包含风险识别、生活方式建议、随访计划

## 技术栈

- **Python**: 3.10+
- **FastAPI**: 0.104+
- **Pydantic**: 2.0+

## 项目结构

```
health-state-assessment-service/
├── app/
│   ├── main.py                    # FastAPI应用入口
│   ├── api/                       # API路由
│   │   └── routes.py
│   ├── services/                  # 业务服务
│   │   ├── health_state_assessment.py  # 健康状态判定服务
│   │   ├── wellness_plan_generator.py  # 健康管理计划生成
│   │   └── entry_assessment/     # 入口判定流程（P0模块）
│   │       ├── step1_receive_input.py
│   │       ├── step2_identify_symptom.py
│   │       ├── step3_clarification.py
│   │       ├── step4_red_flag_check.py
│   │       └── step5_path_selection.py
│   ├── detectors/                 # 检测器
│   │   ├── symptom_severity_detector.py
│   │   ├── risk_signal_detector.py
│   │   └── red_flag_detector.py
│   ├── rules/                     # 规则库
│   │   ├── severity_rules.py      # 严重程度判定规则（0.1）
│   │   ├── risk_screening_rules.py # 风险筛查规则（0.2）
│   │   ├── red_flag_rules.py      # 红旗信号库（0.3）
│   │   ├── work_mode_rules.py     # 工作态判定规则（0.4）
│   │   └── wellness_plan_rules.py # 健康管理计划规则（0.5）
│   ├── models/                    # 数据模型
│   │   ├── request.py
│   │   └── response.py
│   ├── utils/                     # 工具类
│   │   ├── exceptions.py          # 异常处理
│   │   ├── specific_exceptions.py # 特定异常
│   │   └── logger.py              # 日志配置
│   └── config/                    # 配置
│       └── settings.py
├── docs/                          # 文档
│   └── health-state-assessment-service - 服务实现方案.md
├── requirements.txt               # 依赖
├── run.py                         # 启动脚本
└── README.md                      # 说明文档
```

## 启动

### 开发环境

```bash
# 安装依赖
pip install -r requirements.txt

# 启动服务
python run.py
# 或
uvicorn app.main:app --host 0.0.0.0 --port 8081 --reload
```

### 生产环境

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8081 --workers 4
```

## API接口

### 健康状态判定

**接口路径**: `POST /api/v1/health-state-assessment/assess`

**请求示例**:
```json
{
  "userId": "user-1",
  "userInput": "我最近胸痛",
  "basicInfo": {
    "age": 30,
    "gender": "男",
    "bmi": 26.5
  },
  "symptoms": ["胸痛"],
  "vitalSigns": {
    "bp": {"systolic": 130, "diastolic": 85},
    "heartRate": 75
  }
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "cdpId": "cdp-123456",
    "needsClinicalMode": true,
    "workMode": "clinical_mode",
    "riskLevel": "L3",
    "assessmentReason": "检测到症状，建议进入临床诊疗态",
    "entryAssessment": {
      "userInput": "我最近胸痛",
      "hasSymptom": true,
      "symptomStatus": "has_symptom",
      "symptoms": ["胸痛"],
      "clarificationNeeded": false,
      "redFlagsHit": false,
      "redFlagsList": [],
      "pathSelected": "B"
    },
    "redFlags": [],
    "wellnessPlan": null
  },
  "timestamp": 1705123456789
}
```

## 错误码

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 1001 | 入口判定失败 | 500 |
| 1002 | 危险信号检查失败 | 500 |
| 1003 | 工作态判定失败 | 500 |
| 1004 | 健康管理计划生成失败 | 500 |
| 1005 | 用户输入为空 | 400 |
| 1006 | 用户ID不能为空 | 400 |
| 1007 | 参数验证失败 | 400 |

## 测试

### 基础功能测试

```bash
python test_basic.py
```

### API文档

启动服务后，访问：
- Swagger UI: http://localhost:8081/docs
- ReDoc: http://localhost:8081/redoc

## 开发流程

参考实现方案文档：`docs/health-state-assessment-service - 服务实现方案.md`

## 参考文档

- 《AI医生系统-业务逻辑详细设计.md》- 业务逻辑
- 《AI医生系统-入口设计规范.md》- 入口设计
- 《AI医生系统-API接口规范.md》- API规范
- 《AI医生系统-错误处理规范.md》- 错误处理
