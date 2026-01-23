# 执行追踪服务 (Execution Trace Service)

执行追踪服务是AI医生系统的一个独立管理端服务，用于实时记录、存储和可视化整个系统（包括Java和Python服务）的执行流程、服务调用链和数据流转。它为管理员提供了一个直观的界面，以便理解系统内部的工作机制，进行故障排查和性能分析。

## 核心功能

1. **统一事件收集**: 接收来自所有Java和Python业务服务的执行追踪事件。
2. **详细数据存储**: 将每个执行事件的详细信息（服务、模块、方法、输入、输出、耗时、状态等）存储到独立的数据库表中。
3. **CDP摘要更新**: 异步更新`diagnosis-service`中CDP（Clinical Decision Package）的`executionTrace`字段，存储执行路径的摘要信息，以便快速查询。
4. **实时事件推送**: 通过WebSocket向管理端前端实时推送执行事件，实现动态可视化。
5. **查询API**: 提供RESTful API供管理端前端查询历史追踪数据和统计信息。
6. **数据清理**: 定时清理过期追踪数据，防止数据库过大。

## 技术栈

- **Java**: 1.8
- **Spring Boot**: 2.7.8
- **Spring Data JPA**: 数据库访问
- **MySQL**: 数据库
- **Spring WebSocket**: 实时通信
- **Lombok**: 简化Java代码
- **Flyway**: 数据库迁移
- **Jackson**: JSON处理
- **RestTemplate**: 用于调用`diagnosis-service`更新CDP

## 项目结构

```
execution-trace-service/
├── src/
│   ├── main/
│   │   ├── java/com/aidoctor/trace/
│   │   │   ├── ExecutionTraceServiceApplication.java # 应用启动类
│   │   │   ├── config/                               # 配置类
│   │   │   │   ├── DatabaseInitializer.java          # 数据库自动初始化器
│   │   │   │   ├── TraceAsyncConfig.java             # 异步线程池配置
│   │   │   │   ├── WebSocketConfig.java              # WebSocket配置
│   │   │   │   └── RestTemplateConfig.java           # RestTemplate配置
│   │   │   ├── controller/                           # RESTful API控制器
│   │   │   │   └── ExecutionTraceController.java     # 接收追踪事件和查询API
│   │   │   ├── dto/                                  # 数据传输对象
│   │   │   │   ├── ExecutionTraceEvent.java          # 追踪事件DTO
│   │   │   │   └── TraceSummary.java                 # 追踪摘要DTO
│   │   │   ├── entity/                               # JPA实体
│   │   │   │   └── ExecutionTrace.java               # 详细追踪记录实体
│   │   │   ├── repository/                           # JPA Repository
│   │   │   │   └── ExecutionTraceRepository.java     # 追踪记录Repository
│   │   │   ├── service/                              # 业务逻辑服务
│   │   │   │   ├── ExecutionTraceService.java        # 核心追踪服务
│   │   │   │   └── ExecutionTraceEventPublisher.java # WebSocket事件发布器
│   │   │   └── util/                                 # 工具类
│   │   │       └── JsonUtil.java                     # JSON工具类
│   │   └── resources/
│   │       ├── application.yml                       # 基础配置
│   │       ├── application-mysql.yml                 # MySQL数据库配置
│   │       └── db/migration/
│   │           └── V1__init_execution_trace_table.sql # Flyway数据库迁移脚本
│   └── test/...
├── pom.xml                                           # Maven项目文件
└── README.md                                         # 服务说明文档
```

## 部署与运行

### 1. 数据库准备

**自动创建（推荐）**：
- 服务启动时会自动检查数据库`aidoctor_trace`是否存在
- 如果不存在，会自动创建数据库（需要MySQL用户有CREATE DATABASE权限）
- 然后Flyway会自动执行`V1__init_execution_trace_table.sql`创建`execution_trace`表
- 可通过配置`execution.trace.auto-create-database=false`禁用自动创建

**手动创建**：
如果禁用自动创建或自动创建失败，可以手动创建数据库：
```sql
CREATE DATABASE IF NOT EXISTS aidoctor_trace 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci
    COMMENT 'AI医生系统执行追踪服务数据库';
```

### 2. 配置

修改`src/main/resources/application-mysql.yml`中的数据库连接信息：
- `spring.datasource.url`: 数据库连接URL
- `spring.datasource.username`: 数据库用户名
- `spring.datasource.password`: 数据库密码

**注意**：如果启用自动创建数据库，确保MySQL用户有`CREATE DATABASE`权限。

### 3. 启动服务

```bash
# 在项目根目录执行
cd execution-trace-service
mvn spring-boot:run
```

服务将默认运行在`8093`端口。

## API接口

### 1. 接收追踪事件

**POST** `/api/v1/trace/events`

- **功能**: 接收来自其他服务的执行追踪事件。
- **请求体**: `ExecutionTraceEvent` 对象。

### 2. 查询CDP的执行追踪

**GET** `/api/v1/trace/cdp/{cdpId}`

- **功能**: 根据CDP ID查询该CDP的所有详细执行追踪记录。
- **响应**: `List<ExecutionTrace>`。

### 3. 获取CDP的追踪摘要

**GET** `/api/v1/trace/cdp/{cdpId}/summary`

- **功能**: 根据CDP ID获取该CDP的执行追踪摘要信息（总耗时、服务调用统计、步骤列表等）。
- **响应**: `TraceSummary` 对象。

### 4. WebSocket实时推送

**WS** `/api/v1/trace/ws`

- **功能**: 客户端连接此WebSocket端点，并订阅`/topic/trace/{cdpId}`路径，即可实时接收指定CDP的执行追踪事件。

## 与其他服务的集成

### Java服务 (`diagnosis-service`)

1. **添加`TraceServiceClient`**: 在`diagnosis-service`中添加`TraceServiceClient` Feign客户端，指向`execution-trace-service`的地址（默认`http://localhost:8093`）。
2. **修改AOP切面和Feign拦截器**: 将原先直接调用本地`ExecutionTraceService`的逻辑改为通过`TraceServiceClient`调用远程的`execution-trace-service`。
3. **更新`application.yml`**: 配置`trace.service-url`指向`execution-trace-service`的地址。

### Python服务

1. **更新`aidoctor_trace`库**: 确保Python服务使用的`aidoctor_trace`库已更新，其内部的`TraceClient`会默认或通过环境变量`TRACE_SERVICE_URL`配置调用`execution-trace-service`的`/api/v1/trace/events`接口。
2. **设置环境变量**: 在Python服务的`.env`文件中设置`TRACE_SERVICE_URL=http://localhost:8093`。

## 贡献

欢迎对`execution-trace-service`进行贡献。请遵循项目贡献指南。


