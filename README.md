# AI医生智能诊断系统

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Python](https://img.shields.io/badge/Python-3.10+-blue.svg)](https://www.python.org/)
[![Java](https://img.shields.io/badge/Java-8-orange.svg)](https://www.java.com/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.8-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-61dafb.svg)](https://reactjs.org/)

> 一个基于"五脑思想"架构的专科医生级智能诊断系统，提供专业的诊断分析、健康检查和医疗建议服务。

## 📋 目录

- [功能特性](#功能特性)
- [系统架构](#系统架构)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [项目结构](#项目结构)
- [服务端口](#服务端口)
- [环境配置](#环境配置)
- [开发指南](#开发指南)
- [文档](#文档)
- [贡献指南](#贡献指南)
- [许可证](#许可证)

## ✨ 功能特性

- 🧠 **多工具协同**：基于"五脑思想"架构，实现健康状态判定、病例理解、主动问诊、鉴别诊断等核心功能
- 🔍 **智能诊断**：集成规则引擎、知识图谱推理、统计模型和大语言模型，提供多引擎融合诊断
- 💬 **对话交互**：支持自然语言对话，主动问诊收集患者信息
- 📊 **检查建议**：基于患者症状智能推荐检查方案
- 💊 **治疗推理**：提供个性化治疗建议和风险评估
- 📝 **可解释性**：生成诊断结果的详细解释，提高系统可信度
- 🖼️ **多模态理解**：支持OCR识别，处理医疗报告和检查单
- 🔄 **微服务架构**：采用Spring Boot + FastAPI混合架构，支持独立部署和扩展

## 🏗️ 系统架构

本系统采用微服务架构，基于"五脑思想"设计理念，将诊断流程分解为多个独立的工具服务：

```
┌─────────────────────────────────────────────────────────┐
│                    前端界面 (React)                      │
│                  React 18 + TypeScript                  │
└─────────────────────────────────────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────┐
│              诊断服务 (Java - 流程编排)                   │
│            Spring Boot + CDP管理                         │
└─────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
┌───────▼──────┐  ┌─────────▼────────┐  ┌──────▼──────┐
│ tool_0: 健康  │  │ tool_1: 病例理解   │  │ tool_2: 对话  │
│ 状态判定     │  │ (临床解析服务)    │  │ (对话服务)   │
│   :8081      │  │    :8082          │  │   :8088      │
└──────────────┘  └──────────────────┘  └─────────────┘
                            │
                    ┌───────▼────────┐
                    │ tool_3: 诊断引擎 │
                    │ (多引擎融合)    │
                    │    :8086       │
                    └───────┬────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
┌───────▼──────┐  ┌─────────▼────────┐  ┌──────▼──────┐
│ tool_4: 检查  │  │ tool_5: 治疗推理   │  │ tool_6: 风险  │
│ 建议         │  │                  │  │ 评估        │
│   :8090      │  │    :8091          │  │   :8092     │
└──────────────┘  └──────────────────┘  └─────────────┘
                            │
                    ┌───────▼────────┐
                    │ tool_7: 解释生成 │
                    │    :8089       │
                    └────────────────┘
```

### 核心服务说明

- **tool_0（健康状态判定）**：评估患者整体健康状态，判断是否需要进一步诊断
- **tool_1（病例理解）**：解析和理解患者提供的病历、症状等信息
- **tool_2（主动问诊）**：通过对话主动收集患者信息，补充诊断所需数据
- **tool_3（鉴别诊断）**：多引擎融合，提供诊断建议和鉴别诊断
- **tool_4（检查建议）**：基于诊断结果推荐合适的检查方案
- **tool_5（治疗推理）**：提供个性化治疗建议
- **tool_6（风险评估）**：评估疾病风险和预后
- **tool_7（解释生成）**：生成诊断结果的详细解释，提高可解释性

## 🛠️ 技术栈

### 后端服务
- **Java服务**：Spring Boot 2.7.8 + Java 8
- **Python服务**：FastAPI + Python 3.10+
- **服务治理**：Spring Cloud Gateway + Nacos
- **数据库**：
  - MySQL 8.0（本地开发推荐）
  - Oracle（生产环境，使用 ojdbc8 驱动）
- **缓存**：Redis 7
- **图数据库**：Neo4j 5（知识图谱存储）
- **向量数据库**：Milvus（可选，用于语义检索）

### 前端
- **框架**：React 18 + TypeScript
- **UI组件库**：Ant Design
- **构建工具**：Vite

### 基础设施
- **容器化**：Docker + Docker Compose
- **服务发现**：Nacos
- **API网关**：Spring Cloud Gateway

### AI/ML
- **大语言模型**：支持 OpenAI、ChatGLM、Ollama 等多种后端
- **知识图谱**：基于 DR.KNOWS 的医疗知识图谱推理
- **OCR**：支持医疗报告和检查单识别

## 🚀 快速开始

### 前置要求

- **JDK 8+**
- **Python 3.10+**
- **Maven 3.8+**
- **MySQL 8.0**（本地开发推荐）或 **Oracle数据库**（生产环境）
- **Docker & Docker Compose**（可选，用于快速启动基础设施）
- **Node.js 16+**（前端开发需要）

### 安装步骤

#### 1. 克隆项目

```bash
git clone https://github.com/cxjchelsea/AIdoctor.git
cd AIdoctor
```

#### 2. 配置环境变量

在各服务目录下创建 `.env` 文件（参考各服务的配置说明）：

```bash
# 数据库配置
DATABASE_URL=jdbc:mysql://localhost:3306/aidoctor
DATABASE_USER=root
DATABASE_PASSWORD=your_password

# Redis配置
REDIS_HOST=localhost
REDIS_PORT=6379

# Neo4j配置
NEO4J_URI=bolt://localhost:7687
NEO4J_USER=neo4j
NEO4J_PASSWORD=your_password

# LLM配置（可选）
LLM_BACKEND=openai
OPENAI_API_KEY=your_api_key_here
OPENAI_MODEL=gpt-4
```

> ⚠️ **注意**：`.env` 文件包含敏感信息，已添加到 `.gitignore`，不会提交到仓库。

#### 3. 启动基础设施

使用 Docker Compose 启动 MySQL、Redis、Neo4j：

```bash
docker-compose up -d mysql redis neo4j
```

#### 4. 初始化数据库

执行数据库初始化脚本（位于各服务的 `src/main/resources/` 目录）。

#### 5. 启动服务

**启动Java服务：**

```bash
# 诊断服务
cd diagnosis-service
mvn spring-boot:run

# 检查服务（新终端）
cd examination-service
mvn spring-boot:run
```

**启动Python服务：**

**方式一：使用启动脚本（推荐，适用于PyCharm）**
```bash
# 在PyCharm中直接运行各服务的 run.py 文件
# 例如：dialog-service/run.py
#      diagnosis-engine-service/run.py
#      ocr-service/run.py
```

**方式二：使用命令行**
```bash
# 健康状态判定服务（tool_0）
cd health-state-assessment-service
pip install -r requirements.txt
python run.py

# 病例理解服务（tool_1）
cd clinical-parsing-service
pip install -r requirements.txt
python run.py

# 对话服务（tool_2）
cd dialog-service
pip install -r requirements.txt
python run.py

# 诊断引擎服务（tool_3）
cd diagnosis-engine-service
pip install -r requirements.txt
python run.py

# 解释生成服务（tool_7）
cd explanation-service
pip install -r requirements.txt
python run.py

# OCR服务
cd ocr-service
pip install -r requirements.txt
python run.py

# 检查建议服务（tool_4）
cd workup-planner-service
pip install -r requirements.txt
python run.py

# 治疗推理服务（tool_5）
cd treatment-engine-service
pip install -r requirements.txt
python run.py

# 风险评估服务（tool_6）
cd risk-assessment-service
pip install -r requirements.txt
python run.py
```

#### 6. 启动前端

```bash
cd frontend
npm install
npm run dev
```

### 使用Docker Compose启动所有服务

```bash
# 启动所有服务（包括应用服务）
docker-compose up -d

# 仅启动基础设施
docker-compose up -d mysql redis neo4j nacos
```

## 📁 项目结构

```
AIdoctor/
├── diagnosis-service/              # 诊断服务（Java）- CDP管理、诊断流程编排
├── examination-service/            # 检查服务（Java）- 检查方案、报告识别
├── health-state-assessment-service/  # 健康状态判定服务（Python）- tool_0
├── clinical-parsing-service/       # 病例理解服务（Python）- tool_1
├── dialog-service/                 # 对话管理服务（Python）- tool_2（主动问诊）
├── diagnosis-engine-service/       # 诊断引擎服务（Python）- tool_3（鉴别诊断）
│   ├── kg-reasoning-engine/        # 知识图谱推理引擎（DR.KNOWS核心）
│   ├── multi-engine-fusion/        # 多引擎融合（规则/知识图谱/统计/大模型/鉴别）
│   └── ...
├── explanation-service/            # 解释生成服务（Python）- tool_7（可解释性）
├── ocr-service/                    # OCR服务（Python）- 多模态理解
├── workup-planner-service/         # 检查建议服务（Python）- tool_4
├── treatment-engine-service/       # 治疗推理服务（Python）- tool_5
├── risk-assessment-service/        # 风险评估服务（Python）- tool_6
├── frontend/                       # 前端应用（React + TypeScript）
├── common/                         # 公共模块
│   └── aidoctor_llm/              # LLM客户端封装
├── docker-compose.yml             # Docker编排配置
└── docs/                          # 文档目录
    ├── AI医生/                    # 系统设计文档
    ├── 五脑思想/                  # 架构设计文档
    └── 开发过程文件/              # 开发文档
```

## 🔌 服务端口

### Java服务
- `diagnosis-service`: **8084**
- `examination-service`: **8085**

### Python服务
- `health-state-assessment-service`: **8081**（tool_0：健康状态判定）
- `clinical-parsing-service`: **8082**（tool_1：病例理解）
- `dialog-service`: **8088**（tool_2：主动问诊）
- `diagnosis-engine-service`: **8086**（tool_3：鉴别诊断）
- `explanation-service`: **8089**（tool_7：可解释性）
- `ocr-service`: **8087**（多模态理解）
- `workup-planner-service`: **8090**（tool_4：检查建议）
- `treatment-engine-service`: **8091**（tool_5：治疗推理）
- `risk-assessment-service`: **8092**（tool_6：风险评估）

### 管理服务
- `execution-trace-service`: **8093**（执行追踪服务）

### 基础设施
- `MySQL`: **3306**
- `Redis`: **6379**
- `Neo4j HTTP`: **7474**
- `Neo4j Bolt`: **7687**
- `Nacos`: **8848**

## ⚙️ 环境配置

### 配置文件位置

| 服务 | 配置文件 | 位置 |
|------|---------|------|
| diagnosis-service | application.yml | `diagnosis-service/src/main/resources/` |
| diagnosis-service | application-mysql.yml | `diagnosis-service/src/main/resources/`（本地开发） |
| diagnosis-service | application-dev.yml | `diagnosis-service/src/main/resources/`（开发环境） |
| Python服务 | .env | 各服务根目录（可选） |

### 环境变量说明

详细的环境变量配置请参考：
- [环境配置指南](./docs/AI医生/2.项目前置设计/AI医生系统-环境配置指南.md)
- [LLM配置说明](./docs/LLM配置说明.md)

### 默认账户和密码

> ⚠️ **警告**：生产环境请务必修改默认密码！

| 服务 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| MySQL | aidoctor | password | 数据库用户（本地开发） |
| Neo4j | neo4j | password | 首次登录需修改 |
| Nacos | nacos | nacos | 控制台登录 |

## ✅ 已实现功能清单

### 核心服务实现状态

| 服务名称 | 工具 | 端口 | 实现状态 | 核心功能 |
|---------|------|------|---------|---------|
| **diagnosis-service** | 流程编排 | 8084 | ✅ 完整实现 | CDP管理、诊断流程编排、健康筛查流程编排 |
| **health-state-assessment-service** | tool_0 | 8081 | ✅ 完整实现 | 入口判定、健康状态判定、工作态判定、健康筛查流程（A1-A5） |
| **clinical-parsing-service** | tool_1 | 8082 | ✅ 完整实现 | 医学概念识别、概念归一化、结构化提取、歧义表达判定 |
| **dialog-service** | tool_2 | 8088 | ✅ 完整实现 | 信息缺口识别、智能追问生成、NLU/NLG、对话上下文管理 |
| **diagnosis-engine-service** | tool_3 | 8086 | ✅ 完整实现 | 知识图谱推理（DR.KNOWS）、五引擎融合诊断、三层分层分类 |
| **workup-planner-service** | tool_4 | 8090 | ✅ 基础实现 | 检查建议生成、验证计划构建 |
| **treatment-engine-service** | tool_5 | 8091 | ✅ 基础实现 | 治疗建议生成、药物推荐 |
| **risk-assessment-service** | tool_6 | 8092 | ✅ 基础实现 | 风险评估、分诊评估、升级规则、终点结论包构建 |
| **explanation-service** | tool_7 | 8089 | ✅ 完整实现 | 证据链构建、推理路径可视化、终点结论包生成、自然语言解释 |
| **ocr-service** | 多模态 | 8087 | ✅ 基础实现 | OCR识别、报告解析 |
| **examination-service** | 检查业务 | 8085 | 🟡 框架搭建 | 检查报告上传、OCR编排（待完善） |

### 详细功能清单

#### 1. diagnosis-service（诊断服务 - 流程编排）

**实现状态**: ✅ 核心框架完整实现

**已实现功能**:
- ✅ CDP管理（创建、更新、查询、版本控制、回退、回放）
- ✅ 服务间调用框架（Feign客户端，支持9个下游服务）
- ✅ 诊断流程编排框架（5步AI循证诊断流程）
- ✅ 健康筛查流程编排（A1-A5完整实现）
- ✅ 诊断流程启动、继续、状态查询、结果获取
- ✅ 响应解析方法（部分实现：Step 1、健康筛查流程）

**API接口**:
- ✅ `POST /api/v1/diagnosis/start` - 启动诊断流程
- ✅ `POST /api/v1/diagnosis/continue` - 继续诊断流程
- ✅ `GET /api/v1/diagnosis/{cdpId}/status` - 获取诊断状态
- ✅ `GET /api/v1/diagnosis/{cdpId}/result` - 获取诊断结果

**详细文档**: [diagnosis-service - 已实现功能清单.md](./diagnosis-service/docs/diagnosis-service%20-%20已实现功能清单.md)

---

#### 2. health-state-assessment-service（健康状态判定服务 - tool_0）

**实现状态**: ✅ 核心功能完整实现

**已实现功能**:
- ✅ 入口判定流程（5个步骤：接收输入、识别症状、方向澄清、危险信号检查、路径选择）
- ✅ 健康状态判定（症状严重程度评估、早期风险信号识别、红旗信号识别、工作态判定、风险等级计算）
- ✅ 健康管理计划生成
- ✅ 健康筛查流程（A1-A5：需求分类、健康画像收集、分支执行、统一结果生成、随访管理）

**API接口**:
- ✅ `POST /api/v1/health-state-assessment/assess` - 健康状态判定
- ✅ `POST /api/v1/wellness-screening/a1-demand-classification` - A1需求分类
- ✅ `POST /api/v1/wellness-screening/a2-health-profile-collection` - A2收集健康画像
- ✅ `POST /api/v1/wellness-screening/a3-branch-execution` - A3执行分支
- ✅ `POST /api/v1/wellness-screening/a4-unified-result-generation` - A4生成统一结果
- ✅ `POST /api/v1/wellness-screening/a5-follow-up-setup` - A5设置随访

**详细文档**: [health-state-assessment-service - 已实现功能清单.md](./health-state-assessment-service/docs/health-state-assessment-service%20-%20已实现功能清单.md)

---

#### 3. clinical-parsing-service（病例理解服务 - tool_1）

**实现状态**: ✅ 核心功能完整实现

**已实现功能**:
- ✅ 归一化词表加载与管理（6类词表：症状、疾病、药物、过敏源、检查、指标）
- ✅ 医学概念识别（基于规则匹配，支持6类概念）
- ✅ 概念归一化（映射到标准编码：CUI/ICD/SNOMED/LOINC/ATC）
- ✅ 结构化提取（症状、疾病、药物、检查、过敏）
- ✅ 歧义表达判定（生成追问问题）

**API接口**:
- ✅ `POST /api/v1/parsing/parse` - 病例理解（核心接口）

**详细文档**: [clinical-parsing-service - 已实现功能清单.md](./clinical-parsing-service/docs/clinical-parsing-service%20-%20已实现功能清单.md)

---

#### 4. dialog-service（对话管理服务 - tool_2）

**实现状态**: ✅ 核心功能完整实现

**已实现功能**:
- ✅ 信息缺口识别（必填/重要/可选三级分类）
- ✅ 完整度计算（基于权重的加权计算）
- ✅ 智能追问生成（优先级排序、避免重复）
- ✅ 自然语言生成（NLG，集成LLM）
- ✅ 自然语言理解（NLU，集成LLM）
- ✅ 对话上下文管理（Redis存储）
- ✅ CDP数据集成（获取和更新CDP）

**API接口**:
- ✅ `POST /api/v1/dialog/generate-question` - 生成追问问题
- ✅ `POST /api/v1/dialog/understand` - 理解用户输入
- ✅ `POST /api/v1/dialog/identify-gaps` - 识别信息缺口
- ✅ `WS /api/v1/dialog/ws/{cdp_id}` - WebSocket实时对话

**详细文档**: [dialog-service - 已实现功能清单.md](./dialog-service/docs/dialog-service%20-%20已实现功能清单.md)

---

#### 5. diagnosis-engine-service（诊断引擎服务 - tool_3）

**实现状态**: ✅ 核心功能完整实现

**已实现功能**:
- ✅ 知识图谱推理引擎（DR.KNOWS核心方法）
  - Neo4j客户端、路径检索器、路径评分器、路径注入器
- ✅ 多引擎融合诊断
  - 规则引擎（框架已实现，规则库待完善）
  - 知识图谱引擎（完整实现）
  - 统计模型引擎（框架已实现，模型待训练）
  - 大模型引擎（完整实现，集成公共LLM库）
  - 鉴别诊断引擎（框架已实现，规则库待完善）
  - 融合引擎（完整实现，加权融合算法）
- ✅ 三层分层分类器（首要假设、主要备选、必须排除）
- ✅ 推理组织器（推理子组组织、分流路径设计）
- ✅ 证据分析器（证据强度分析、证据链构建）

**API接口**:
- ✅ `POST /api/v1/engine/diagnose` - 五引擎融合诊断（完整流程）
- ✅ `POST /api/v1/engine/rule-based` - 规则引擎诊断
- ✅ `POST /api/v1/engine/knowledge-graph` - 知识图谱引擎诊断
- ✅ `POST /api/v1/engine/statistical` - 统计模型引擎诊断
- ✅ `POST /api/v1/engine/llm` - 大模型引擎诊断
- ✅ `POST /api/v1/engine/differential` - 鉴别诊断引擎诊断
- ✅ `POST /api/v1/classify/three-layer` - 三层分层分类
- ✅ `POST /api/v1/kg/paths/retrieve` - 知识图谱路径检索

**详细文档**: [diagnosis-engine-service - 已实现功能清单.md](./diagnosis-engine-service/docs/diagnosis-engine-service%20-%20已实现功能清单.md)

---

#### 6. workup-planner-service（检查建议服务 - tool_4）

**实现状态**: ✅ 基础实现

**已实现功能**:
- ✅ 检查建议生成（WorkupPlanner）
- ✅ 验证计划构建（VerificationPlanner）

**API接口**:
- ✅ `POST /api/v1/workup/plan` - 生成检查建议
- ✅ `POST /api/v1/workup/verification-plan` - 构建验证计划

---

#### 7. treatment-engine-service（治疗推理服务 - tool_5）

**实现状态**: ✅ 基础实现

**已实现功能**:
- ✅ 治疗建议生成（TreatmentEngine）
- ✅ 药物推荐（MedicationRecommender）

**API接口**:
- ✅ `POST /api/v1/treatment/plan` - 生成治疗建议
- ✅ `POST /api/v1/treatment/medication` - 推荐药物

---

#### 8. risk-assessment-service（风险评估服务 - tool_6）

**实现状态**: ✅ 基础实现

**已实现功能**:
- ✅ 风险评估（RiskAssessmentEngine）
- ✅ 分诊评估（TriageEngine）
- ✅ 升级规则（UpgradeRuleEngine）
- ✅ 终点结论包构建（ConclusionPackageBuilder）

**API接口**:
- ✅ `POST /api/v1/risk/assess` - 风险评估
- ✅ `POST /api/v1/risk/triage` - 分诊评估
- ✅ `POST /api/v1/risk/upgrade-rules` - 获取升级规则
- ✅ `POST /api/v1/risk/conclusion-package` - 构建终点结论包

---

#### 9. explanation-service（解释生成服务 - tool_7）

**实现状态**: ✅ 核心功能完整实现

**已实现功能**:
- ✅ 证据链构建（证据提取、分类、强度评估、证据-疾病关联）
- ✅ 推理路径可视化（路径格式化、可视化数据生成）
- ✅ 终点结论包生成（结论构建、必须排除项状态、关键依据、行动与随访）
- ✅ 自然语言解释生成（集成公共LLM库，带降级策略）

**API接口**:
- ✅ `POST /api/v1/explain` - 生成完整解释
- ✅ `POST /api/v1/explain/evidence-chain` - 生成证据链
- ✅ `POST /api/v1/explain/conclusion-package` - 生成终点结论包
- ✅ `POST /api/v1/explain/natural-language` - 生成自然语言解释

**详细文档**: [explanation-service - 已实现功能清单.md](./explanation-service/docs/explanation-service%20-%20已实现功能清单.md)

---

#### 10. ocr-service（OCR服务 - 多模态理解）

**实现状态**: ✅ 基础实现

**已实现功能**:
- ✅ OCR识别（报告识别）

**API接口**:
- ✅ `POST /api/v1/ocr/recognize` - OCR识别报告

---

#### 11. examination-service（检查服务 - 检查业务）

**实现状态**: 🟡 框架搭建

**已实现功能**:
- ✅ 基础框架搭建（Spring Boot）
- ⚠️ 检查报告上传（标记为TODO）
- ⚠️ OCR识别编排（标记为TODO）
- ⚠️ 检查历史查询（标记为TODO）
- ⚠️ 检查方案设计（标记为TODO）

**API接口**:
- ⚠️ `POST /api/v1/examination/upload` - 上传检查报告（TODO）
- ⚠️ `POST /api/v1/examination/ocr` - OCR识别报告（TODO）
- ⚠️ `GET /api/v1/examination/history` - 获取检查历史（TODO）
- ⚠️ `POST /api/v1/examination/plan` - 设计检查方案（TODO）

---

### 功能实现统计

| 服务 | 核心功能完成度 | API接口完成度 | 文档完整度 |
|------|---------------|--------------|-----------|
| diagnosis-service | 90% | 80% | ✅ |
| health-state-assessment-service | 100% | 100% | ✅ |
| clinical-parsing-service | 85% | 100% | ✅ |
| dialog-service | 100% | 100% | ✅ |
| diagnosis-engine-service | 90% | 100% | ✅ |
| workup-planner-service | 60% | 100% | ❌ |
| treatment-engine-service | 60% | 100% | ❌ |
| risk-assessment-service | 60% | 100% | ❌ |
| explanation-service | 100% | 100% | ✅ |
| ocr-service | 50% | 100% | ❌ |
| examination-service | 20% | 0% | ❌ |

### 说明

- ✅ **完整实现**: 核心功能已完整实现，可用于生产环境
- 🟡 **基础实现**: 核心框架已搭建，部分功能待完善
- ⚠️ **待实现**: 功能标记为TODO，需要后续开发

**详细功能清单文档**: 各服务的详细功能清单请查看各服务目录下的 `docs/` 文件夹。

---

## 📚 文档

详细文档请查看 `docs/` 目录：

- [系统设计方案](./docs/AI医生/1.项目结构设计/)
- [技术架构方案](./docs/五脑思想/)
- [环境配置指南](./docs/AI医生/2.项目前置设计/AI医生系统-环境配置指南.md)
- [服务实现方案](./docs/开发过程文件/)
- [开发流程指南](./docs/AI医生/AI医生系统-开发流程.md)

## 🔍 管理端功能

### 执行追踪管理

访问地址：`http://localhost:3000/admin/trace`

功能特性：
- ✅ 实时追踪：WebSocket实时推送执行事件
- ✅ 数据流转图：可视化展示数据在服务间的流转
- ✅ 服务调用图：展示服务间的调用关系和统计
- ✅ 执行时间线：按时间顺序展示所有事件
- ✅ 模块调用树：展示服务内部模块的调用层次
- ✅ 统计信息：总耗时、调用次数、错误统计等

详细说明：[执行追踪管理页面使用说明](./frontend/src/pages/TraceManagementPage.md)
- [LLM配置说明](./docs/LLM配置说明.md)

## 👥 贡献指南

我们欢迎所有形式的贡献！请遵循以下步骤：

1. **Fork 本仓库**
2. **创建特性分支** (`git checkout -b feature/AmazingFeature`)
3. **提交更改** (`git commit -m 'Add some AmazingFeature'`)
4. **推送到分支** (`git push origin feature/AmazingFeature`)
5. **开启 Pull Request**

### 开发规范

- **代码规范**：
  - Java: 遵循 Google Java Style Guide
  - Python: 遵循 PEP 8 规范
  - TypeScript: 遵循 ESLint 规则
- **提交信息**：使用清晰的提交信息，遵循 [Conventional Commits](https://www.conventionalcommits.org/)
- **测试**：编写单元测试，确保所有测试通过
- **文档**：更新相关文档，包括 README、API 文档等

### 代码审查

- 所有 Pull Request 需要至少一名维护者审查
- 确保代码通过所有 CI 检查
- 确保没有引入新的安全漏洞

## 📝 许可证

本项目采用 [MIT License](LICENSE) 许可证。

## 🙏 致谢

- [DR.KNOWS](https://github.com/your-repo/DRKnows) - 知识图谱推理引擎
- [FastAPI](https://fastapi.tiangolo.com/) - 现代Python Web框架
- [Spring Boot](https://spring.io/projects/spring-boot) - Java应用框架
- [React](https://reactjs.org/) - 前端UI框架
- [Ant Design](https://ant.design/) - 企业级UI设计语言

## 📮 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 [Issue](https://github.com/cxjchelsea/AIdoctor/issues)

---

⭐ 如果这个项目对你有帮助，请给个 Star！
