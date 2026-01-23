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

- 🧠 **多脑区协同**：基于"五脑思想"架构，实现健康状态判定、病例理解、主动问诊、鉴别诊断等核心功能
- 🔍 **智能诊断**：集成规则引擎、知识图谱推理、统计模型和大语言模型，提供多引擎融合诊断
- 💬 **对话交互**：支持自然语言对话，主动问诊收集患者信息
- 📊 **检查建议**：基于患者症状智能推荐检查方案
- 💊 **治疗推理**：提供个性化治疗建议和风险评估
- 📝 **可解释性**：生成诊断结果的详细解释，提高系统可信度
- 🖼️ **多模态理解**：支持OCR识别，处理医疗报告和检查单
- 🔄 **微服务架构**：采用Spring Boot + FastAPI混合架构，支持独立部署和扩展

## 🏗️ 系统架构

本系统采用微服务架构，基于"五脑思想"设计理念，将诊断流程分解为多个独立的脑区服务：

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
│ 脑区0: 健康  │  │ 脑区A: 病例理解   │  │ 脑区B: 对话  │
│ 状态判定     │  │ (临床解析服务)    │  │ (对话服务)   │
│   :8081      │  │    :8082          │  │   :8088      │
└──────────────┘  └──────────────────┘  └─────────────┘
                            │
                    ┌───────▼────────┐
                    │ 脑区C: 诊断引擎 │
                    │ (多引擎融合)    │
                    │    :8086       │
                    └───────┬────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
┌───────▼──────┐  ┌─────────▼────────┐  ┌──────▼──────┐
│ 脑区D: 检查  │  │ 脑区E: 治疗推理   │  │ 脑区F: 风险  │
│ 建议         │  │                  │  │ 评估        │
│   :8090      │  │    :8091          │  │   :8092     │
└──────────────┘  └──────────────────┘  └─────────────┘
                            │
                    ┌───────▼────────┐
                    │ 脑区G: 解释生成 │
                    │    :8089       │
                    └────────────────┘
```

### 核心服务说明

- **脑区0（健康状态判定）**：评估患者整体健康状态，判断是否需要进一步诊断
- **脑区A（病例理解）**：解析和理解患者提供的病历、症状等信息
- **脑区B（主动问诊）**：通过对话主动收集患者信息，补充诊断所需数据
- **脑区C（鉴别诊断）**：多引擎融合，提供诊断建议和鉴别诊断
- **脑区D（检查建议）**：基于诊断结果推荐合适的检查方案
- **脑区E（治疗推理）**：提供个性化治疗建议
- **脑区F（风险评估）**：评估疾病风险和预后
- **脑区G（解释生成）**：生成诊断结果的详细解释，提高可解释性

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
# 健康状态判定服务（脑区0）
cd health-state-assessment-service
pip install -r requirements.txt
python run.py

# 病例理解服务（脑区A）
cd clinical-parsing-service
pip install -r requirements.txt
python run.py

# 对话服务（脑区B）
cd dialog-service
pip install -r requirements.txt
python run.py

# 诊断引擎服务（脑区C）
cd diagnosis-engine-service
pip install -r requirements.txt
python run.py

# 解释生成服务（脑区G）
cd explanation-service
pip install -r requirements.txt
python run.py

# OCR服务
cd ocr-service
pip install -r requirements.txt
python run.py

# 检查建议服务（脑区D）
cd workup-planner-service
pip install -r requirements.txt
python run.py

# 治疗推理服务（脑区E）
cd treatment-engine-service
pip install -r requirements.txt
python run.py

# 风险评估服务（脑区F）
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
├── health-state-assessment-service/  # 健康状态判定服务（Python）- 脑区0
├── clinical-parsing-service/       # 病例理解服务（Python）- 脑区A
├── dialog-service/                 # 对话管理服务（Python）- 脑区B（主动问诊）
├── diagnosis-engine-service/       # 诊断引擎服务（Python）- 脑区C（鉴别诊断）
│   ├── kg-reasoning-engine/        # 知识图谱推理引擎（DR.KNOWS核心）
│   ├── multi-engine-fusion/        # 多引擎融合（规则/知识图谱/统计/大模型/鉴别）
│   └── ...
├── explanation-service/            # 解释生成服务（Python）- 脑区G（可解释性）
├── ocr-service/                    # OCR服务（Python）- 多模态理解
├── workup-planner-service/         # 检查建议服务（Python）- 脑区D
├── treatment-engine-service/       # 治疗推理服务（Python）- 脑区E
├── risk-assessment-service/        # 风险评估服务（Python）- 脑区F
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
- `health-state-assessment-service`: **8081**（脑区0：健康状态判定）
- `clinical-parsing-service`: **8082**（脑区A：病例理解）
- `dialog-service`: **8088**（脑区B：主动问诊）
- `diagnosis-engine-service`: **8086**（脑区C：鉴别诊断）
- `explanation-service`: **8089**（脑区G：可解释性）
- `ocr-service`: **8087**（多模态理解）
- `workup-planner-service`: **8090**（脑区D：检查建议）
- `treatment-engine-service`: **8091**（脑区E：治疗推理）
- `risk-assessment-service`: **8092**（脑区F：风险评估）

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

## 📚 文档

详细文档请查看 `docs/` 目录：

- [系统设计方案](./docs/AI医生/1.项目结构设计/)
- [技术架构方案](./docs/五脑思想/)
- [环境配置指南](./docs/AI医生/2.项目前置设计/AI医生系统-环境配置指南.md)
- [服务实现方案](./docs/开发过程文件/)
- [开发流程指南](./docs/AI医生/AI医生系统-开发流程.md)
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
