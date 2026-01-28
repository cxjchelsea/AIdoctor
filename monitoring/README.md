# 性能监控服务

基于 Prometheus + Grafana 的统一性能监控方案。

## 架构

- **Prometheus**: 指标收集和存储
- **Grafana**: 可视化展示
- **业务服务**: 通过客户端库暴露指标端点

## 系统架构

```
业务服务
  ├── 指标输出 → Prometheus → Grafana（性能监控）
  └── 日志输出 → Loki → Grafana（日志查看）
```

### 组件说明

- **Prometheus**: 指标收集和存储
- **Loki**: 日志聚合和存储
- **Promtail**: 日志收集代理
- **Grafana**: 统一可视化界面（指标+日志）

## 快速开始

### 1. 安装依赖

**Python服务**（所有服务已集成，只需安装依赖）：
```bash
# 进入任意Python服务目录
cd diagnosis-engine-service
pip install -r requirements.txt
```

**Java服务**（所有服务已集成，只需重新编译）：
```bash
# 进入任意Java服务目录
cd diagnosis-service
mvn clean install
```

### 2. 启动监控服务

```bash
cd monitoring
docker-compose up -d
```

### 3. 访问服务

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3001
  - 用户名: `admin`
  - 密码: `admin`

### 4. 配置 Grafana 数据源

1. 登录 Grafana
2. 进入 Configuration -> Data Sources
3. 添加 Prometheus 数据源
   - URL: `http://prometheus:9090`
   - Access: Server (default)
4. 点击 "Save & Test"

#### 添加Loki数据源（日志查看）

1. 登录 Grafana
2. 进入 Configuration -> Data Sources
3. 添加 Loki 数据源
   - URL: `http://loki:3100`
   - Access: Server (default)
4. 点击 "Save & Test"

### 5. 导入仪表板

可以使用以下 Grafana 仪表板模板：
- Spring Boot: ID `11378`
- Node Exporter: ID `11074`

或创建自定义仪表板。

### 6. 查看日志

1. 进入 Grafana 的 **Explore** 页面
2. 选择 **Loki** 数据源
3. 输入LogQL查询，例如：`{service="diagnosis-engine"}`
4. 点击 "Run query"

**详细使用说明请查看**: 
- [使用指南.md](./使用指南.md) - 指标监控使用说明
- [日志系统使用说明.md](./日志系统使用说明.md) - 日志系统使用说明

## 监控的服务

### Python 服务（9个）

- **健康状态判定服务**: `http://localhost:8081/metrics` (端口8081)
- **病例理解服务**: `http://localhost:8082/metrics` (端口8082)
- **诊断引擎服务**: `http://localhost:8086/metrics` (端口8086)
- **OCR服务**: `http://localhost:8087/metrics` (端口8087)
- **对话服务**: `http://localhost:8088/metrics` (端口8088)
- **解释生成服务**: `http://localhost:8089/metrics` (端口8089)
- **检查建议服务**: `http://localhost:8090/metrics` (端口8090)
- **治疗推理服务**: `http://localhost:8091/metrics` (端口8091)
- **风险评估服务**: `http://localhost:8092/metrics` (端口8092)

### Java 服务（3个）

- **诊断服务**: `http://localhost:8084/actuator/prometheus` (端口8084)
- **检查服务**: `http://localhost:8085/actuator/prometheus` (端口8085)
- **执行追踪服务**: `http://localhost:8093/actuator/prometheus` (端口8093)

## 告警规则

告警规则定义在 `alert_rules.yml` 中，包括：

- 慢请求告警（P95响应时间 > 2秒）
- 错误率告警（错误率 > 5%）
- CPU使用率告警（> 80%）
- 内存使用率告警（> 12GB）
- 服务不可用告警
- JVM堆内存告警（> 85%）
- 数据库连接池告警（> 80%）

## 配置说明

### Prometheus 配置

- **拉取间隔**: 15秒
- **数据保留**: 30天
- **目标服务**: 通过 `host.docker.internal` 访问宿主机服务

### Windows 注意事项

如果 Prometheus 无法访问 `host.docker.internal`，可以：

1. 使用 `localhost` 或 `127.0.0.1`
2. 或者使用 Docker Desktop 的网络模式

### Linux/Mac 注意事项

可以使用 `host.docker.internal` 或直接使用 `localhost`。

## 常用 PromQL 查询

### QPS（每秒请求数）

```promql
sum(rate(http_requests_total[5m])) by (service)
```

### 响应时间（P95）

```promql
histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le, service))
```

### 错误率

```promql
sum(rate(http_request_errors_total[5m])) by (service) / 
sum(rate(http_requests_total[5m])) by (service)
```

### CPU 使用率

```promql
100 - (avg by(instance) (rate(process_cpu_seconds_total[5m])) * 100)
```

### 内存使用

```promql
process_resident_memory_bytes / 1024 / 1024 / 1024
```

## 维护

### 重新加载 Prometheus 配置

```bash
curl -X POST http://localhost:9090/-/reload
```

### 查看 Prometheus 状态

访问 http://localhost:9090/status

### 清理数据

```bash
docker-compose down -v
```

## 故障排查

### Prometheus 无法拉取指标

1. 检查服务是否正常运行
2. 检查端口是否正确
3. 检查网络连接（Windows 使用 `host.docker.internal`）
4. 查看 Prometheus 日志：`docker logs prometheus`

### Grafana 无法连接 Prometheus

1. 检查 Prometheus 是否正常运行
2. 检查数据源配置中的 URL
3. 确保 Grafana 和 Prometheus 在同一网络中

## 性能影响

- **客户端库开销**: <0.01ms（内存操作）
- **指标导出开销**: 0.1-1ms（生成文本）
- **Prometheus 拉取**: 无影响（被动拉取）

总体性能影响极小，几乎可以忽略不计。

