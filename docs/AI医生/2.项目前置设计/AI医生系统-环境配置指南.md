# AI医生系统 - 环境配置指南

> **文档定位**：本文档详细说明AI医生系统的环境配置步骤，包括开发环境和生产环境的配置方法。  
> **设计基础**：基于DR.KNOWS论文，采用单主Agent + 多工具Tools架构设计

---

## 目录

1. [开发环境要求](#一开发环境要求)
2. [配置方式选择](#二配置方式选择)
3. [方式一：本地开发环境配置](#三方式一本地开发环境配置)
4. [方式二：Docker环境配置](#四方式二docker环境配置)
5. [验证配置](#五验证配置)
6. [常见问题](#六常见问题)

---

## 一、开发环境要求

### 1.1 必需软件

| 软件 | 版本要求 | 用途 | 下载地址 |
|------|---------|------|---------|
| **JDK** | 8（或11） | Java后端服务（Spring Boot 2.7.x） | [Oracle JDK 8](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html) / [Temurin](https://adoptium.net/temurin/releases/?version=8) |
| **Maven** | 3.8+ | Java项目构建 | [Maven](https://maven.apache.org/download.cgi) |
| **Python** | 3.10+ | Python AI服务 | [Python](https://www.python.org/downloads/) |
| **MySQL** | 8.0+ | 关系数据库（本地开发推荐） | [MySQL](https://dev.mysql.com/downloads/mysql/) |
| **Oracle数据库** | - | 关系数据库（与同事共同生产时使用） | 需连接已有Oracle数据库（由DBA提供） |
| **Redis** | 7+ | 缓存服务 | [Redis](https://redis.io/download) |

### 1.2 可选软件

| 软件 | 版本要求 | 用途 | 说明 |
|------|---------|------|------|
| **Neo4j** | 5+ | 图数据库（知识图谱） | **必需**，DR.KNOWS核心依赖（tool_3：鉴别诊断工具） |
| **Nacos** | 2.2+ | 服务注册中心 | 可选，微服务使用 |
| **Docker** | 20.10+ | 容器化部署 | 推荐使用 |
| **Docker Compose** | 2.0+ | 容器编排 | 推荐使用 |

> **注意**：根据需求分析，系统**不需要向量数据库（Milvus）**。DR.KNOWS方法使用Neo4j图遍历进行路径检索，不需要向量检索。

### 1.3 IDE推荐

- **Java开发**: IntelliJ IDEA / Eclipse
- **Python开发**: PyCharm / VS Code
- **前端开发**: VS Code / WebStorm

---

## 二、配置方式选择

### 2.1 两种配置方式

**方式一：本地开发环境配置**
- ✅ 适合：日常开发、调试
- ✅ 优点：启动快、调试方便、资源占用少
- ❌ 缺点：需要手动安装和配置各个服务

**方式二：Docker环境配置**
- ✅ 适合：快速启动、环境隔离、部署测试
- ✅ 优点：一键启动、环境一致、易于管理
- ❌ 缺点：需要Docker环境，资源占用较大

### 2.2 推荐方案

- **开发阶段**: 使用方式一（本地开发环境）
- **集成测试**: 使用方式二（Docker环境）
- **生产部署**: 使用方式二（Docker环境）

---

## 三、方式一：本地开发环境配置

### 3.1 步骤1：安装基础软件

#### 3.1.1 安装JDK 8

**Windows:**
```bash
# 1. 下载JDK 8安装包
# 2. 运行安装程序
# 3. 配置环境变量
set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_xxx
set PATH=%JAVA_HOME%\bin;%PATH%

# 4. 验证安装
java -version
# 应显示：java version "1.8.0_xxx"
```

**Linux/Mac:**
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-8-jdk

# Mac (使用Homebrew)
brew install openjdk@8

# 配置环境变量（添加到 ~/.bashrc 或 ~/.zshrc）
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64  # Linux
# 或
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)  # Mac

export PATH=$JAVA_HOME/bin:$PATH

# 验证安装
java -version
# 应显示：java version "1.8.0_xxx"
```

#### 3.1.2 安装Maven

**Windows:**
```bash
# 1. 下载Maven压缩包
# 2. 解压到目录（如 C:\Program Files\Apache\maven）
# 3. 配置环境变量
set MAVEN_HOME=C:\Program Files\Apache\maven
set PATH=%MAVEN_HOME%\bin;%PATH%

# 4. 验证安装
mvn -version
```

**Linux/Mac:**
```bash
# Ubuntu/Debian
sudo apt install maven

# Mac (使用Homebrew)
brew install maven

# 验证安装
mvn -version
```

#### 3.1.3 安装Python 3.10+

**Windows:**
```bash
# 1. 下载Python安装包
# 2. 运行安装程序（勾选"Add Python to PATH"）
# 3. 验证安装
python --version
pip --version
```

**Linux/Mac:**
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install python3.10 python3-pip

# Mac (使用Homebrew)
brew install python@3.10

# 验证安装
python3 --version
pip3 --version
```

### 3.2 步骤2：配置数据库连接

**说明：** 项目支持两种数据库配置：
- **本地开发**：推荐使用MySQL（`mysql` profile），简单易用，适合个人开发
- **与同事共同生产**：使用Oracle（`dev` profile），与现有架构统一

> **补充**：AI服务层（Python）通常也需要访问MySQL/Oracle（读写知识库/规则库/计划库）与Redis（会话/缓存/异步任务结果）。建议本地开发统一使用MySQL。

#### 3.2.1 方式A：本地开发使用MySQL（推荐）

**说明：** 本地开发推荐使用MySQL，配置简单，无需额外安装Oracle数据库。

**安装MySQL：**

**Windows:**
```bash
# 1. 下载MySQL安装包
# 地址：https://dev.mysql.com/downloads/mysql/
# 2. 运行安装程序
# 3. 记住设置的root密码
# 4. 验证安装
mysql --version

# 5. 创建数据库和用户
# 打开MySQL Workbench或使用命令行
mysql -u root -p
```

**Linux:**
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install mysql-server

# 启动服务
sudo systemctl start mysql
sudo systemctl enable mysql

# 安全配置（可选）
sudo mysql_secure_installation

# 连接到MySQL
mysql -u root -p
```

**Mac:**
```bash
# 使用Homebrew
brew install mysql@8.0
brew services start mysql@8.0

# 连接到MySQL
mysql -u root -p
```

**创建数据库和用户:**
```sql
-- 在MySQL命令行中执行
CREATE DATABASE aidoctor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'aidoctor'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON aidoctor.* TO 'aidoctor'@'localhost';
FLUSH PRIVILEGES;

-- 使用数据库
USE aidoctor;
```

**配置说明：**
- 项目默认使用 `mysql` profile（MySQL）
- 配置文件：`application-mysql.yml`
- 数据库连接信息已预配置，无需修改

**启动项目（使用MySQL）：**
```bash
# 默认使用 mysql profile
mvn spring-boot:run

# 或明确指定
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

#### 3.2.2 方式B：与同事共同生产时使用Oracle

**说明：** Oracle数据库通常由DBA或运维团队管理，开发者不需要本地安装Oracle，只需配置连接信息。

**获取数据库连接信息：**
- 联系DBA或运维团队获取Oracle数据库连接信息：
  - 数据库主机地址（host）
  - 端口（port，默认1521）
  - 数据库实例名（SID）或服务名（Service Name）
  - 用户名（username）
  - 密码（password）

**配置数据库连接：**

编辑 `diagnosis-service/src/main/resources/application-dev.yml` 和 `examination-service/src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    driver-class-name: oracle.jdbc.driver.OracleDriver
    url: jdbc:oracle:thin:@${DB_HOST:localhost}:${DB_PORT:1521}/${DB_NAME:orcl}
    username: ${DB_USERNAME:your_username}
    password: ${DB_PASSWORD:your_password}
```

**URL格式说明：**
- **SID格式**：`jdbc:oracle:thin:@host:port:sid`
  - 示例：`jdbc:oracle:thin:@192.168.1.100:1521:orcl`
- **服务名格式**：`jdbc:oracle:thin:@host:port/service_name`
  - 示例：`jdbc:oracle:thin:@192.168.1.100:1521/orcl`

**使用环境变量（推荐）：**
```bash
# Windows
set DB_HOST=192.168.1.100
set DB_PORT=1521
set DB_NAME=orcl
set DB_USERNAME=your_username
set DB_PASSWORD=your_password

# Linux/Mac
export DB_HOST=192.168.1.100
export DB_PORT=1521
export DB_NAME=orcl
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
```

**验证数据库连接：**
```bash
# 使用SQL*Plus连接（如果安装了Oracle客户端）
sqlplus username/password@host:port/service_name

# 或使用项目启动时验证
# 启动项目后，查看日志确认数据库连接是否成功
```

**启动项目（使用Oracle）：**
```bash
# 使用 dev profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或修改 application.yml 中的 active 为 dev
# spring.profiles.active=dev
```

#### 3.2.3 Profile切换说明

**配置文件说明：**
- `application.yml` - 主配置文件，设置默认激活的profile
- `application-mysql.yml` - 本地开发配置（MySQL）
- `application-dev.yml` - 开发环境配置（Oracle，与同事共同生产时使用）

**切换方式：**

**方式1：修改 `application.yml`**
```yaml
spring:
  profiles:
    active: mysql  # 本地开发使用
    # active: dev  # 与同事共同生产时使用
```

**方式2：启动时指定（推荐）**
```bash
# 本地开发（MySQL）
mvn spring-boot:run -Dspring-boot.run.profiles=mysql

# 与同事共同生产（Oracle）
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**方式3：使用环境变量**
```bash
# Windows
set SPRING_PROFILES_ACTIVE=mysql  # 或 dev

# Linux/Mac
export SPRING_PROFILES_ACTIVE=mysql  # 或 dev
```

#### 3.2.4 执行数据库迁移脚本

**MySQL（mysql profile）：**
```bash
# 1. 进入diagnosis-service目录
cd diagnosis-service

# 2. 执行数据库迁移脚本
mysql -u aidoctor -p aidoctor < src/main/resources/db/migration/V1__init_diagnosis_tables.sql

# 或项目启动时自动执行（推荐）
# Spring Boot会自动执行Flyway迁移脚本
```

**Oracle（dev profile）：**
```bash
# 1. 进入diagnosis-service目录
cd diagnosis-service

# 2. 执行数据库迁移脚本
# 方式A：使用SQL*Plus命令（需要Oracle客户端）
sqlplus username/password@host:port/service_name @src/main/resources/db/migration/V1__init_diagnosis_tables.sql

# 方式B：使用SQL Developer或其他数据库工具执行SQL脚本

# 方式C：项目启动时自动执行（推荐）
# Spring Boot会自动执行Flyway迁移脚本（需要配置）
# 注意：需要将SQL脚本转换为Oracle兼容的语法
```

#### 3.2.5 安装Redis

**Windows:**
```bash
# 方式1：使用WSL2
wsl --install
# 在WSL2中安装Redis（参考Linux安装方式）

# 方式2：使用Redis for Windows（不推荐，已停止维护）
# 下载：https://github.com/microsoftarchive/redis/releases
```

**Linux:**
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install redis-server

# 启动服务
sudo systemctl start redis-server
sudo systemctl enable redis-server

# 验证安装
redis-cli ping
# 应返回: PONG
```

**Mac:**
```bash
# 使用Homebrew
brew install redis
brew services start redis

# 验证安装
redis-cli ping
# 应返回: PONG
```

### 3.3 步骤3：配置Java服务

#### 3.3.1 检查Maven依赖

```bash
# 进入diagnosis-service目录
cd diagnosis-service

# 清理并下载依赖
mvn clean install -DskipTests

# 如果依赖下载失败，检查网络或配置Maven镜像
```

**配置Maven镜像（可选，提高下载速度）:**

编辑 `~/.m2/settings.xml`（如果不存在则创建）:
```xml
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <name>Aliyun Maven</name>
      <url>https://maven.aliyun.com/repository/public</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
```

#### 3.3.2 配置应用配置文件

**检查配置文件:**
- `diagnosis-service/src/main/resources/application.yml`
- `diagnosis-service/src/main/resources/application-dev.yml`

**确认配置项:**

```yaml
# application-dev.yml 应该包含以下配置
spring:
  datasource:
    driver-class-name: oracle.jdbc.driver.OracleDriver
    url: jdbc:oracle:thin:@localhost:1521/orcl
    username: your_username
    password: your_password
  
  redis:
    host: localhost
    port: 6379
```

**如果需要修改配置:**
- 数据库连接信息
- Redis连接信息
- Nacos地址（如果使用）
- 服务端口

### 3.4 步骤4：配置Python服务

#### 3.4.1 创建Python虚拟环境（推荐）

```bash
# 进入diagnosis-engine-service目录
cd diagnosis-engine-service

# 创建虚拟环境
python -m venv venv

# 激活虚拟环境
# Windows:
venv\Scripts\activate
# Linux/Mac:
source venv/bin/activate

# 安装依赖（包含LangChain）
pip install -r requirements.txt

# 验证LangChain安装
python -c "import langchain; print(langchain.__version__)"
```

**注意**：`requirements.txt` 已包含以下LangChain相关依赖：
- `langchain==0.1.0`
- `langchain-openai==0.0.2`
- `openai==1.10.0`
- `pyyaml==6.0.1`
- `jinja2==3.1.2`

#### 3.4.2 安装OCR服务依赖

```bash
# 进入ocr-service目录
cd ../ocr-service

# 创建虚拟环境（如果还没有）
python -m venv venv

# 激活虚拟环境
# Windows:
venv\Scripts\activate
# Linux/Mac:
source venv/bin/activate

# 安装依赖
pip install -r requirements.txt

# 注意：OCR服务需要Tesseract OCR引擎
# Windows: 下载安装 https://github.com/UB-Mannheim/tesseract/wiki
# Linux: sudo apt install tesseract-ocr
# Mac: brew install tesseract
```

#### 3.4.3 配置Python服务环境变量（可选）

创建各服务的 `.env` 文件（如果需要）:

**diagnosis-engine-service/.env**:
```bash
# Neo4j配置（DR.KNOWS核心依赖）
NEO4J_URI=bolt://localhost:7687
NEO4J_USER=neo4j
NEO4J_PASSWORD=password

# 注意：根据需求分析，系统不需要Milvus向量数据库
# DR.KNOWS方法使用Neo4j图遍历进行路径检索，不需要向量检索

# LangChain LLM配置（用于"路径注入LLM/解释生成/对话策略"等能力）
# LLM后端类型：openai, chatglm, ollama, custom
LLM_BACKEND=openai

# OpenAI配置（如果使用OpenAI）
OPENAI_API_KEY=your_openai_api_key_here
OPENAI_MODEL=gpt-4
OPENAI_TEMPERATURE=0.3
OPENAI_MAX_TOKENS=2000
# OPENAI_BASE_URL=https://api.openai.com/v1  # 可选，自定义API端点

# ChatGLM配置（如果使用ChatGLM）
# CHATGLM_API_URL=http://chatglm-service:8000
# CHATGLM_API_KEY=your_chatglm_key
# LLM_MODEL=chatglm3-6b

# Ollama配置（如果使用本地模型）
# OLLAMA_BASE_URL=http://localhost:11434
# OLLAMA_MODEL=llama2
# LLM_MODEL=llama2

# 自定义HTTP API配置（如果使用自定义LLM服务）
# CUSTOM_LLM_API_URL=http://llm-service:8080/api/generate
# CUSTOM_LLM_API_KEY=your_custom_key
# LLM_MODEL=medical-llm

# 通用LLM配置
LLM_TIMEOUT=30
LLM_MAX_RETRIES=3
LLM_RETRY_DELAY=1.0
```

**workup-planner-service/.env**:
```bash
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=aidoctor
DB_USERNAME=aidoctor
DB_PASSWORD=password
```

**treatment-engine-service/.env**:
```bash
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=aidoctor
DB_USERNAME=aidoctor
DB_PASSWORD=password
```

**risk-assessment-service/.env**:
```bash
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=aidoctor
DB_USERNAME=aidoctor
DB_PASSWORD=password
```

**knowledge-query-service/.env**:
```bash
# Neo4j配置（知识图谱，支持三个知识库区）
NEO4J_URI=bolt://localhost:7687
NEO4J_USER=neo4j
NEO4J_PASSWORD=password

# 数据库配置（知识库元数据）
DB_HOST=localhost
DB_PORT=3306
DB_NAME=aidoctor
DB_USERNAME=aidoctor
DB_PASSWORD=password

# 知识版本配置
DEFAULT_KG_VERSION=v2.1  # 默认使用的知识版本

# 服务配置
SERVICE_PORT=8093
SERVICE_NAME=knowledge-query-service
```

**knowledge-ops-service/.env**:
```bash
# Neo4j配置（三个知识库区：Sandbox/Staging/Production）
NEO4J_URI=bolt://localhost:7687
NEO4J_USER=neo4j
NEO4J_PASSWORD=password

# 数据库配置（知识库元数据）
DB_HOST=localhost
DB_PORT=3306
DB_NAME=aidoctor
DB_USERNAME=aidoctor
DB_PASSWORD=password

# Redis配置（任务队列）
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# 服务配置
SERVICE_PORT=8094
SERVICE_NAME=knowledge-ops-service
WORK_MODE=offline  # 离线模式

# 知识演化配置
SANDBOX_RELEASE_ID=Sandbox
STAGING_RELEASE_ID=Staging
DEFAULT_PRODUCTION_VERSION=v2.1

# Publish Gate配置
GATE_EVALUATION_ENABLED=true
GATE_MANUAL_APPROVAL_REQUIRED=false  # 中高风险需要人工审批

# Agent配置
EXTRACTOR_AGENT_ENABLED=true
VERIFIER_AGENT_ENABLED=true
CONFLICT_RESOLVER_AGENT_ENABLED=true
RELEASE_BUILDER_AGENT_ENABLED=true
SHADOW_EVALUATOR_AGENT_ENABLED=true
ROLLBACK_MONITOR_AGENT_ENABLED=true

# LLM配置（用于知识抽取）
LLM_BACKEND=openai
OPENAI_API_KEY=your_openai_api_key_here
OPENAI_MODEL=gpt-4
OPENAI_TEMPERATURE=0.3
```

---

### 3.6（新增）统一环境变量清单（建议）

> **目标**：让多服务在本地/容器/生产环境下，通过同一批变量完成"数据库/缓存/图数据库/下游服务地址/调用超时重试/LangChain LLM配置"配置。

#### 3.6.1 基础依赖（DB/Redis/Neo4j）

| 变量名 | 示例 | 说明 |
|------|------|------|
| `DB_HOST` | `localhost` | MySQL/Oracle主机 |
| `DB_PORT` | `3306` / `1521` | 端口 |
| `DB_NAME` | `aidoctor` / `orcl` | 数据库名/服务名（按具体驱动） |
| `DB_USERNAME` | `aidoctor` | 用户名 |
| `DB_PASSWORD` | `password` | 密码 |
| `REDIS_HOST` | `localhost` | Redis主机 |
| `REDIS_PORT` | `6379` | Redis端口 |
| `NEO4J_URI` | `bolt://localhost:7687` | Neo4j Bolt URI（知识图谱，支持三个知识库区） |
| `NEO4J_USER` | `neo4j` | 用户名 |
| `NEO4J_PASSWORD` | `password` | 密码 |
| `DEFAULT_KG_VERSION` | `v2.1` | 默认知识版本（knowledge-query-service使用） |
| `SANDBOX_RELEASE_ID` | `Sandbox` | Sandbox发布区域ID（knowledge-ops-service使用） |
| `STAGING_RELEASE_ID` | `Staging` | Staging发布区域ID（knowledge-ops-service使用） |
| `DEFAULT_PRODUCTION_VERSION` | `v2.1` | 默认生产版本（knowledge-ops-service使用） |

#### 3.6.2 下游服务地址（主Agent调用用）

| 变量名 | 示例 |
|------|------|
| `HEALTH_STATE_URL` | `http://localhost:8081` |
| `CLINICAL_PARSING_URL` | `http://localhost:8082` |
| `KNOWLEDGE_QUERY_URL` | `http://localhost:8093` | 知识查询服务地址 |
| `KNOWLEDGE_OPS_URL` | `http://localhost:8094` | 知识运维服务地址 |
| `DIALOG_URL` | `http://localhost:8088` |
| `DIAGNOSIS_ENGINE_URL` | `http://localhost:8086` |
| `WORKUP_PLANNER_URL` | `http://localhost:8090` |
| `TREATMENT_ENGINE_URL` | `http://localhost:8091` |
| `RISK_ASSESSMENT_URL` | `http://localhost:8092` |
| `EXPLANATION_URL` | `http://localhost:8089` |
| `OCR_URL` | `http://localhost:8087` |

#### 3.6.3 调用治理（超时/重试/熔断）

| 变量名 | 示例 | 说明 |
|------|------|------|
| `HTTP_TIMEOUT_MS` | `5000` | 默认超时（ms） |
| `HTTP_RETRY_MAX` | `2` | 默认重试次数 |
| `HTTP_RETRY_BACKOFF_MS` | `1000` | 默认退避（ms） |
| `CIRCUIT_BREAKER_ENABLED` | `true` | 是否启用熔断 |

#### 3.6.4 LangChain LLM配置（diagnosis-engine-service等需要LLM的服务）

| 变量名 | 示例 | 说明 |
|------|------|------|
| `LLM_BACKEND` | `openai` | LLM后端类型：openai, chatglm, ollama, custom |
| `OPENAI_API_KEY` | `sk-xxx` | OpenAI API密钥（如果使用OpenAI） |
| `OPENAI_MODEL` | `gpt-4` | OpenAI模型名称 |
| `OPENAI_TEMPERATURE` | `0.3` | Temperature参数 |
| `OPENAI_MAX_TOKENS` | `2000` | 最大生成长度 |
| `CHATGLM_API_URL` | `http://chatglm-service:8000` | ChatGLM API地址（如果使用ChatGLM） |
| `CHATGLM_API_KEY` | `your_key` | ChatGLM API密钥（可选） |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama服务地址（如果使用Ollama） |
| `OLLAMA_MODEL` | `llama2` | Ollama模型名称 |
| `CUSTOM_LLM_API_URL` | `http://llm-service:8080` | 自定义LLM API地址（如果使用自定义API） |
| `CUSTOM_LLM_API_KEY` | `your_key` | 自定义LLM API密钥（可选） |
| `LLM_TIMEOUT` | `30` | LLM调用超时时间（秒） |
| `LLM_MAX_RETRIES` | `3` | 最大重试次数 |
| `LLM_RETRY_DELAY` | `1.0` | 重试延迟（秒） |

**提示词模板配置**：
- 模板位置：`diagnosis-engine-service/app/config/prompt_templates/`
- 模板格式：Jinja2模板（`.jinja2`文件）
- 配置文件：`templates.yaml`（模板元数据）

### 3.5 步骤5：安装可选服务

#### 3.5.1 安装Neo4j（必需 - DR.KNOWS核心依赖）

> **重要**：Neo4j是DR.KNOWS方法的核心依赖，用于知识图谱路径检索（tool_3：鉴别诊断工具）。系统**必须**安装Neo4j。

**Windows:**
```bash
# 1. 下载Neo4j Desktop
# 2. 安装并启动Neo4j Desktop
# 3. 创建数据库，设置密码
# 4. 默认端口：7474 (HTTP), 7687 (Bolt)
```

**Linux:**
```bash
# 添加Neo4j仓库
wget -O - https://debian.neo4j.com/neotechnology.gpg.key | sudo apt-key add -
echo 'deb https://debian.neo4j.com stable 4.4' | sudo tee /etc/apt/sources.list.d/neo4j.list

# 安装
sudo apt update
sudo apt install neo4j

# 启动服务
sudo systemctl start neo4j
sudo systemctl enable neo4j

# 设置密码（首次启动后）
cypher-shell -u neo4j -p neo4j
# 在命令行中执行：ALTER USER neo4j SET PASSWORD 'password';
```

**Mac:**
```bash
# 使用Homebrew
brew install neo4j

# 启动服务
brew services start neo4j

# 设置密码（首次启动后）
cypher-shell -u neo4j -p neo4j
# 在命令行中执行：ALTER USER neo4j SET PASSWORD 'password';
```

#### 3.5.2 安装Nacos（可选）

**下载Nacos:**
```bash
# 1. 下载Nacos Server
# 地址：https://github.com/alibaba/nacos/releases
# 2. 解压到目录
# 3. 配置数据库（可选，单机模式可跳过）
```

**启动Nacos:**
```bash
# Windows
startup.cmd -m standalone

# Linux/Mac
sh startup.sh -m standalone

# 访问控制台
# 地址：http://localhost:8848/nacos
# 默认用户名/密码：nacos/nacos
```

---

## 四、方式二：Docker环境配置

### 4.1 步骤1：安装Docker和Docker Compose

#### 4.1.1 安装Docker

**Windows:**
```bash
# 1. 下载Docker Desktop for Windows
# 地址：https://www.docker.com/products/docker-desktop
# 2. 安装并启动Docker Desktop
# 3. 验证安装
docker --version
docker-compose --version
```

**Linux:**
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install docker.io docker-compose

# 启动Docker服务
sudo systemctl start docker
sudo systemctl enable docker

# 将当前用户添加到docker组（可选，避免使用sudo）
sudo usermod -aG docker $USER
# 重新登录后生效

# 验证安装
docker --version
docker-compose --version
```

**Mac:**
```bash
# 使用Homebrew
brew install --cask docker

# 或下载Docker Desktop for Mac
# 地址：https://www.docker.com/products/docker-desktop

# 验证安装
docker --version
docker-compose --version
```

### 4.2 步骤2：配置Docker Compose

#### 4.2.1 检查docker-compose.yml文件

确认项目根目录下的 `docker-compose.yml` 文件存在且配置正确。

> **建议**：本地开发的docker-compose至少包含：`mysql`、`redis`、`neo4j`。  
> `nacos` 可选；`oracle` 不建议本地容器化（通常由DBA提供外部实例）。

#### 4.2.2 启动基础服务

```bash
# 进入项目根目录
cd /path/to/AIdoctor

# 启动基础服务（Redis、Neo4j、MySQL等；Oracle数据库由DBA提供，不在Docker中运行）
docker-compose up -d mysql redis neo4j

# 等待服务启动（约30秒-1分钟）
docker-compose ps

# 查看服务日志
docker-compose logs -f redis
docker-compose logs -f neo4j
```

**注意：** Oracle数据库不在Docker Compose中运行，需要连接外部Oracle数据库。请在配置文件中配置Oracle数据库连接信息。
```

#### 4.2.4 构建并启动所有服务

```bash
# 构建所有服务镜像
docker-compose build

# 启动所有服务
docker-compose up -d

# 查看所有服务状态
docker-compose ps

# 查看服务日志
docker-compose logs -f
```

### 4.3 步骤3：配置应用连接Docker服务

#### 4.3.1 修改Java服务配置

如果要让本地运行的Java服务连接Docker中的数据库和Redis，需要修改配置：

**application-dev.yml:**
```yaml
spring:
  datasource:
    driver-class-name: oracle.jdbc.driver.OracleDriver
    url: jdbc:oracle:thin:@${DB_HOST:localhost}:${DB_PORT:1521}/${DB_NAME:orcl}
    username: ${DB_USERNAME:your_username}
    password: ${DB_PASSWORD:your_password}
  
  redis:
    host: localhost
    port: 6379
```

**注意:** 
- Redis服务已经映射到本地端口，所以配置为 `localhost` 即可
- Oracle数据库连接信息需要从DBA或运维团队获取

---

## 五、验证配置

### 5.1 验证数据库连接

#### 5.1.1 MySQL（mysql profile）

**方式1：使用mysql命令行**
```bash
# 连接数据库
mysql -u aidoctor -p aidoctor

# 连接成功后，查看表
SHOW TABLES;

# 应该能看到以下表：
# - diagnosis_records
# - examination_records
# - examination_plans

# 退出
EXIT;
```

**方式2：使用数据库工具**
- 使用MySQL Workbench、DBeaver、DataGrip等工具连接MySQL
- 配置连接信息：host: localhost, port: 3306, database: aidoctor, username: aidoctor, password: password

**方式3：通过项目启动验证**
```bash
# 启动项目后，查看日志确认数据库连接是否成功
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
# 如果连接成功，日志中会显示"HikariPool-1 - Starting..."
# 如果连接失败，会显示连接错误信息
```

#### 5.1.2 Oracle（dev profile）

**方式1：使用SQL*Plus连接（需要Oracle客户端）**
```bash
sqlplus username/password@host:port/service_name

# 连接成功后，查看表
SELECT table_name FROM user_tables;

# 应该能看到以下表：
# - DIAGNOSIS_RECORDS
# - EXAMINATION_RECORDS
# - EXAMINATION_PLANS
```

**方式2：使用SQL Developer或其他数据库工具**
- 使用Oracle SQL Developer、DBeaver等工具连接Oracle数据库
- 配置连接信息：host、port、service_name、username、password

**方式3：通过项目启动验证**
```bash
# 启动项目后，查看日志确认数据库连接是否成功
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# 如果连接成功，日志中会显示"HikariPool-1 - Starting..."
# 如果连接失败，会显示连接错误信息
```

### 5.2 验证Redis连接

```bash
# 连接Redis
redis-cli -h localhost -p 6379

# 测试连接
ping
# 应返回: PONG

# 测试写入
set test "hello"
get test
# 应返回: "hello"
```

### 5.3 验证Java服务

```bash
# 进入diagnosis-service目录
cd diagnosis-service

# 编译项目
mvn clean compile

# 启动服务
mvn spring-boot:run

# 或使用IDE启动
# 启动类：DiagnosisServiceApplication

# 检查服务是否启动成功
# 访问：http://localhost:8084/actuator/health
# 或访问：http://localhost:8084/swagger-ui.html
```

### 5.4 验证Python服务

#### 5.4.1 验证诊断引擎服务

```bash
# 进入diagnosis-engine-service目录
cd diagnosis-engine-service

# 激活虚拟环境
source venv/bin/activate  # Linux/Mac
# 或
venv\Scripts\activate  # Windows

# 启动服务
uvicorn app.main:app --host 0.0.0.0 --port 8086 --reload

# 检查服务是否启动成功
# 访问：http://localhost:8086/docs
# 或访问：http://localhost:8086/health
```

#### 5.4.2 验证检查建议服务

```bash
# 进入workup-planner-service目录
cd workup-planner-service

# 激活虚拟环境
source venv/bin/activate  # Linux/Mac
# 或
venv\Scripts\activate  # Windows

# 启动服务
uvicorn app.main:app --host 0.0.0.0 --port 8090 --reload

# 检查服务是否启动成功
# 访问：http://localhost:8090/docs
# 或访问：http://localhost:8090/health
```

#### 5.4.3 验证治疗推理服务

```bash
# 进入treatment-engine-service目录
cd treatment-engine-service

# 激活虚拟环境
source venv/bin/activate  # Linux/Mac
# 或
venv\Scripts\activate  # Windows

# 启动服务
uvicorn app.main:app --host 0.0.0.0 --port 8091 --reload

# 检查服务是否启动成功
# 访问：http://localhost:8091/docs
# 或访问：http://localhost:8091/health
```

#### 5.4.4 验证风险评估服务

```bash
# 进入risk-assessment-service目录
cd risk-assessment-service

# 激活虚拟环境
source venv/bin/activate  # Linux/Mac
# 或
venv\Scripts\activate  # Windows

# 启动服务
uvicorn app.main:app --host 0.0.0.0 --port 8092 --reload

# 检查服务是否启动成功
# 访问：http://localhost:8092/docs
# 或访问：http://localhost:8092/health
```

#### 5.4.5 验证OCR服务

```bash
# 进入ocr-service目录
cd ocr-service

# 激活虚拟环境
source venv/bin/activate  # Linux/Mac
# 或
venv\Scripts\activate  # Windows

# 启动服务
uvicorn app.main:app --host 0.0.0.0 --port 8087 --reload

# 检查服务是否启动成功
# 访问：http://localhost:8087/docs
# 或访问：http://localhost:8087/health
```

### 5.5 验证服务间通信

```bash
# 1. 确保所有服务都已启动
# - diagnosis-service: http://localhost:8084
# - diagnosis-engine-service: http://localhost:8086
# - ocr-service: http://localhost:8087
# - dialog-service: http://localhost:8088
# - explanation-service: http://localhost:8089

# 2. 测试诊断服务健康检查
curl http://localhost:8084/actuator/health

# 3. 测试诊断引擎服务
curl http://localhost:8086/health

# 4. 测试OCR服务
curl http://localhost:8087/health

# 5. 访问Swagger文档
# diagnosis-service: http://localhost:8084/swagger-ui.html
# diagnosis-engine-service: http://localhost:8086/docs
# ocr-service: http://localhost:8087/docs
```

### 5.6 验证Nacos（如果使用）

```bash
# 访问Nacos控制台
# 地址：http://localhost:8848/nacos
# 用户名/密码：nacos/nacos

# 查看服务列表
# 应该能看到以下服务：
# - diagnosis-service
# - examination-service
```

### 5.6 验证知识查询服务（knowledge-query-service）

```bash
# 进入knowledge-query-service目录
cd knowledge-query-service

# 激活虚拟环境
source venv/bin/activate  # Linux/Mac
# 或
venv\Scripts\activate  # Windows

# 启动服务
uvicorn app.main:app --host 0.0.0.0 --port 8093 --reload

# 检查服务是否启动成功
# 访问：http://localhost:8093/docs
# 或访问：http://localhost:8093/health

# 测试知识查询接口
curl -X GET "http://localhost:8093/api/v1/knowledge/version/current"
```

### 5.7 验证知识运维服务（knowledge-ops-service）

```bash
# 进入knowledge-ops-service目录
cd knowledge-ops-service

# 激活虚拟环境
source venv/bin/activate  # Linux/Mac
# 或
venv\Scripts\activate  # Windows

# 启动服务
uvicorn app.main:app --host 0.0.0.0 --port 8094 --reload

# 检查服务是否启动成功
# 访问：http://localhost:8094/docs
# 或访问：http://localhost:8094/health

# 测试知识提案接口
curl -X POST "http://localhost:8094/api/v1/knowledge/proposal" \
  -H "Content-Type: application/json" \
  -d '{
    "trigger": "知识覆盖缺口",
    "riskLevel": "medium"
  }'
```

### 5.8 验证Neo4j（如果使用）

```bash
# 访问Neo4j Browser
# 地址：http://localhost:7474
# 用户名/密码：neo4j/password

# 测试查询
MATCH (n) RETURN n LIMIT 25
```

---

## 六、配置验证清单（新增）

- **基础依赖**：MySQL/Redis/Neo4j 三者均可连接（能执行简单读写/查询）
- **端口对齐**：`dialog-service=8088`、`explanation-service=8089`、`knowledge-query-service=8093`、`knowledge-ops-service=8094`、其余端口与附录表一致
- **健康检查**：每个服务至少提供 `/health`（FastAPI）或 `/actuator/health`（Spring Boot）
- **跨服务调用**：主Agent能按"超时/重试"策略调用下游并在失败时降级
- **错误契约**：任一服务返回错误时包含 `traceId`（便于日志串联）
- **知识库配置**：Neo4j支持三个知识库区（Sandbox/Staging/Production），ReleaseMetadata表配置正确
- **知识查询服务**：knowledge-query-service能正常查询知识库，支持版本化访问
- **知识运维服务**：knowledge-ops-service能正常处理知识演化提案，Agent正常工作

---

## 七、常见问题

### 6.1 Java服务启动失败

**问题1：端口被占用**
```
Address already in use: bind
```

**解决方法：**
```bash
# Windows
netstat -ano | findstr :8084
taskkill /PID <PID> /F

# Linux/Mac
lsof -i :8084
kill -9 <PID>
```

**问题2：数据库连接失败**
```
com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
```

**解决方法：**
- 检查MySQL服务是否启动
- 检查数据库连接配置（用户名、密码、端口）
- 检查防火墙设置
- 检查数据库是否已创建

**问题3：依赖下载失败**
```
Could not resolve dependencies
```

**解决方法：**
- 检查网络连接
- 配置Maven镜像（参考3.3.1节）
- 清理Maven缓存：`mvn clean` 或删除 `~/.m2/repository`

### 6.2 Python服务启动失败

**问题1：模块导入错误**
```
ModuleNotFoundError: No module named 'xxx'
```

**解决方法：**
- 确认虚拟环境已激活
- 重新安装依赖：`pip install -r requirements.txt`
- 检查Python版本：`python --version`（需要3.10+）

**问题2：端口被占用**
```
Address already in use
```

**解决方法：**
```bash
# 查找占用端口的进程
# Windows
netstat -ano | findstr :8086
taskkill /PID <PID> /F

# Linux/Mac
lsof -i :8086
kill -9 <PID>
```

**问题3：OCR服务Tesseract未找到**
```
TesseractNotFoundError: tesseract is not installed
```

**解决方法：**
- Windows: 下载安装Tesseract OCR，并添加到PATH
- Linux: `sudo apt install tesseract-ocr`
- Mac: `brew install tesseract`

### 6.3 数据库问题

**问题1：数据库连接失败**
```
ORA-12541: TNS:no listener
或
ORA-12514: TNS:listener does not currently know of service requested
```

**解决方法：**
- 检查Oracle数据库服务是否启动（联系DBA）
- 检查数据库连接配置（host、port、service_name是否正确）
- 验证连接字符串格式是否正确
- 检查网络连接（是否能ping通数据库服务器）

**问题2：权限不足**
```
ORA-01031: insufficient privileges
```

**解决方法：**
- 联系DBA授予必要的权限
- 确认用户是否有CREATE TABLE、INSERT、UPDATE、DELETE、SELECT等权限
- 检查用户是否有对应表的访问权限

**问题3：表不存在**
```
ORA-00942: table or view does not exist
```

**解决方法：**
- 执行数据库迁移脚本（参考3.2.2节）
- 检查JPA配置：`ddl-auto: update`（开发环境可用）
- 注意Oracle表名区分大小写，如果表名是小写，需要用双引号：`"diagnosis_records"`
- 检查表是否在正确的schema中

### 6.4 Docker问题

**问题1：Docker服务启动失败**
```
Cannot connect to the Docker daemon
```

**解决方法：**
- 检查Docker Desktop是否启动
- Linux: 检查Docker服务：`sudo systemctl status docker`
- 检查用户权限（Linux: 将用户添加到docker组）

**问题2：端口冲突**
```
Bind for 0.0.0.0:3306 failed: port is already allocated
```

**解决方法：**
- 修改docker-compose.yml中的端口映射
- 或停止占用端口的其他服务

**问题3：容器无法连接数据库**
```
could not translate host name "mysql" to address
```

**解决方法：**
- 确保服务在同一个Docker网络中
- 检查docker-compose.yml中的网络配置
- 使用服务名（如`mysql`）而不是`localhost`连接

### 6.5 其他问题

**问题1：Maven编译失败**
```
[ERROR] Failed to execute goal org.springframework.boot:spring-boot-maven-plugin
```

**解决方法：**
- 检查Java版本：`java -version`（需要1.8+，应显示java version "1.8.0_xxx"）
- 检查Maven版本：`mvn -version`（需要3.8+）
- 清理并重新编译：`mvn clean install`

**问题2：Nacos连接失败**
```
com.alibaba.nacos.api.exception.NacosException: failed to req API
```

**解决方法：**
- 检查Nacos服务是否启动
- 检查Nacos地址配置
- 如果不需要Nacos，可以注释掉相关依赖和配置

**问题3：Neo4j连接失败**
```
Unable to connect to Neo4j
```

**解决方法：**
- 检查Neo4j服务是否启动
- 检查Neo4j地址和端口配置
- 检查用户名和密码
- 首次连接需要修改默认密码

---

## 八、下一步

配置完成后，可以开始：

1. **开发业务逻辑**
   - 实现Service层的业务逻辑
   - 实现Controller层的接口
   - 实现AI引擎的逻辑

2. **编写测试**
   - 单元测试
   - 集成测试
   - API测试

3. **前端开发**
   - 配置前端开发环境
   - 开发前端页面
   - 对接后端API

4. **部署**
   - 配置生产环境
   - 部署到服务器
   - 配置监控和日志

---

## 附录

### A. 端口列表

| 服务 | 端口 | 说明 |
|------|------|------|
| **Java服务** | | |
| diagnosis-service | 8084 | 诊断服务（CDP管理、诊断流程编排） |
| examination-service | 8085 | 检查服务 |
| **Python AI服务（工具服务）** | | |
| health-state-assessment-service | 8081 | tool_0：健康状态判定工具 |
| clinical-parsing-service | 8082 | tool_1：病例理解工具 |
| dialog-service | 8088 | tool_2：主动问诊工具 |
| diagnosis-engine-service | 8086 | tool_3：鉴别诊断工具（DR.KNOWS核心） |
| workup-planner-service | 8090 | tool_4：检查建议工具 |
| treatment-engine-service | 8091 | tool_5：治疗建议工具 |
| risk-assessment-service | 8092 | tool_6：风险评估工具 |
| explanation-service | 8089 | tool_7：证据链工具 |
| ocr-service | 8087 | OCR服务（多模态理解） |
| **数据存储服务** | | |
| MySQL | 3306 | 数据库（本地开发，mysql profile） |
| Oracle | 1521 | 数据库（与同事共同生产，dev profile，外部） |
| Redis | 6379 | 缓存（CDP临时状态、会话状态） |
| Neo4j HTTP | 7474 | Neo4j浏览器（DR.KNOWS核心依赖） |
| Neo4j Bolt | 7687 | Neo4j连接（DR.KNOWS核心依赖） |
| **服务治理** | | |
| Nacos | 8848 | 服务注册中心 |

> **注意**：系统**不需要**Milvus向量数据库。DR.KNOWS方法使用Neo4j图遍历进行路径检索，不需要向量检索。

### B. 默认账户和密码

| 服务 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| MySQL | aidoctor | password | 数据库用户（本地开发） |
| Oracle | your_username | (由DBA提供) | 数据库用户（与同事共同生产） |
| Redis | - | - | 无密码（开发环境） |
| Neo4j | neo4j | password | 首次登录需修改 |
| Nacos | nacos | nacos | 控制台登录 |

**⚠️ 注意：生产环境请务必修改默认密码！**

### C. 配置文件位置

| 服务 | 配置文件 | 位置 |
|------|---------|------|
| diagnosis-service | application.yml | `diagnosis-service/src/main/resources/` |
| diagnosis-service | application-mysql.yml | `diagnosis-service/src/main/resources/`（本地开发，MySQL） |
| diagnosis-service | application-dev.yml | `diagnosis-service/src/main/resources/`（开发环境，Oracle） |
| examination-service | application.yml | `examination-service/src/main/resources/` |
| examination-service | application-mysql.yml | `examination-service/src/main/resources/`（本地开发，MySQL） |
| examination-service | application-dev.yml | `examination-service/src/main/resources/`（开发环境，Oracle） |
| health-state-assessment-service | .env (可选) | `health-state-assessment-service/` |
| clinical-parsing-service | .env (可选) | `clinical-parsing-service/` |
| dialog-service | .env (可选) | `dialog-service/` |
| diagnosis-engine-service | .env (可选) | `diagnosis-engine-service/` |
| workup-planner-service | .env (可选) | `workup-planner-service/` |
| treatment-engine-service | .env (可选) | `treatment-engine-service/` |
| risk-assessment-service | .env (可选) | `risk-assessment-service/` |
| explanation-service | .env (可选) | `explanation-service/` |
| ocr-service | .env (可选) | `ocr-service/` |
| Docker | docker-compose.yml | 项目根目录 |

> **说明**：各工具服务对应的Python服务配置说明请参考各服务的README文档。

---

### E. 服务调用超时与重试（建议默认值，与技术架构对齐）

> **用途**：本节用于把“架构文档中的超时/重试策略”落实为可配置项，避免各服务自定义导致行为不一致。

| 下游服务 | 超时时间 | 重试次数 | 重试间隔 |
|----------|----------|----------|----------|
| health-state-assessment-service | 5s | 2 | 1s |
| clinical-parsing-service | 10s | 2 | 2s |
| diagnosis-engine-service | 30s | 1 | 5s |
| workup-planner-service | 15s | 1 | 3s |
| treatment-engine-service | 15s | 1 | 3s |
| risk-assessment-service | 10s | 2 | 2s |
| explanation-service | 20s | 1 | 3s |

### D. 有用的命令

```bash
# Maven常用命令
mvn clean                    # 清理编译文件
mvn compile                  # 编译项目
mvn test                     # 运行测试
mvn package                  # 打包项目
mvn spring-boot:run          # 运行Spring Boot应用

# Docker常用命令
docker-compose up -d         # 后台启动所有服务
docker-compose down          # 停止并删除所有容器
docker-compose ps            # 查看服务状态
docker-compose logs -f       # 查看日志
docker-compose restart <service>  # 重启指定服务

# Oracle常用命令（需要Oracle客户端）
sqlplus username/password@host:port/service_name  # 连接数据库
SELECT table_name FROM user_tables;               # 查看所有表
DESC table_name;                                  # 查看表结构
EXIT;                                             # 退出

# Redis常用命令
redis-cli                     # 连接Redis
ping                          # 测试连接
keys *                        # 查看所有键
get key                       # 获取值
set key value                 # 设置值
```

---

**文档版本**：v4.0（对齐"多智能体 + 多微服务 + LangChain集成 + 知识演化与维护"）  
**创建日期**：2025年1月  
**更新日期**：2025年1月  
**文档定位**：AI医生系统的环境配置指南（开发环境和生产环境的配置方法）  
**参考文档**：《AI医生系统-系统功能设计.md》、《AI医生系统-技术架构设计.md》、《知识演化与知识维护-完整设计方案.md》  
**设计基础**：基于DR.KNOWS论文，采用单主Agent + 多工具Tools架构设计，集成知识演化与维护系统  
**最后更新**: 2025年1月  
**维护者**: 开发团队  
**更新说明**：
- v4.0：添加知识演化相关服务配置（knowledge-query-service、knowledge-ops-service），添加Neo4j三个知识库区配置，添加知识版本管理配置，更新配置验证清单
- v3.1：添加LangChain配置说明，更新LLM配置方式（从直接HTTP调用改为LangChain统一管理）
- v3.0：修正端口与架构文档一致（dialog=8088）；Docker基础服务补齐mysql/redis/neo4j；新增统一环境变量清单（DB/Redis/Neo4j/下游URL/超时重试/LLM）；新增配置验证清单；补齐"服务调用超时与重试"默认值表。

