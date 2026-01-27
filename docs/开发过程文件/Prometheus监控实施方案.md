# Prometheus + Grafana 监控实施方案

> **文档版本**: v1.0  
> **创建日期**: 2026-01-27  
> **方案类型**: 具体实施方案  
> **技术栈**: Prometheus + Grafana + Prometheus Client  
> **目标**: 为所有服务集成Prometheus监控，建立统一的性能监控体系

---

## 📋 目录

- [方案概述](#方案概述)
- [当前监控功能处理](#当前监控功能处理)
- [实施步骤](#实施步骤)
  - [步骤1: 安装依赖](#步骤1-安装依赖)
  - [步骤2: Python服务集成](#步骤2-python服务集成)
  - [步骤3: Java服务集成](#步骤3-java服务集成)
  - [步骤4: 部署Prometheus](#步骤4-部署prometheus)
  - [步骤5: 配置Grafana](#步骤5-配置grafana)
  - [步骤6: 配置告警](#步骤6-配置告警)
- [代码修改清单](#代码修改清单)
- [验证方法](#验证方法)
- [常见问题](#常见问题)

---

## 方案概述

### 技术架构

```
业务服务 → Prometheus Client (轻量级) → Prometheus Server → Grafana
```

### 核心优势

- ✅ **几乎无性能影响**：客户端库极轻量，指标记录是内存操作
- ✅ **统一监控**：所有服务的指标集中管理
- ✅ **强大可视化**：Grafana提供丰富的图表和仪表板
- ✅ **告警功能**：支持灵活的告警规则
- ✅ **历史数据**：支持长期趋势分析

---

## 当前监控功能处理

### 是否需要删除？

**答案：不需要完全删除，但需要优化和整合**

### 当前监控功能分析

#### 1. 性能监控中间件（`diagnosis-engine-service/app/main.py`）

**当前实现**：
```python
@app.middleware("http")
async def performance_middleware(request: Request, call_next):
    logger.info(f"请求开始: {request.method} {request.url.path}")  # 同步日志
    # ... 记录耗时
    logger.info(f"请求完成: ... 耗时={process_time:.3f}s")  # 同步日志
```

**处理方案**：
- ✅ **保留中间件**：继续记录请求耗时
- ✅ **添加Prometheus指标**：同时记录到Prometheus
- ⚠️ **优化日志**：减少详细日志，只记录关键信息（可选）

**原因**：
- 日志和指标是**互补的**：日志用于详细问题排查，指标用于性能趋势分析
- Prometheus指标可以替代部分日志功能，但日志仍然有价值（错误详情、调试信息等）

#### 2. 系统资源监控API（`/monitoring/resources`）

**当前实现**：
```python
@router.get("/monitoring/resources")
async def get_resources() -> Dict[str, Any]:
    return get_system_resources()  # 返回CPU、内存、磁盘信息
```

**处理方案**：
- ✅ **保留API**：可以继续使用
- ✅ **同时导出Prometheus指标**：Prometheus也可以拉取系统资源指标

**原因**：
- API可以用于快速查看，Prometheus用于长期监控
- 两者可以共存

#### 3. 日志文件输出（`logs/app.log`）

**处理方案**：
- ✅ **保留日志文件**：用于问题排查和审计
- ⚠️ **可选优化**：减少详细日志，只保留关键信息

**原因**：
- 日志文件用于详细的问题排查
- Prometheus指标用于性能监控和趋势分析
- 两者用途不同，可以共存

### 推荐方案

**保留当前功能，同时添加Prometheus指标**：

1. **性能监控中间件**：
   - 保留：继续记录请求耗时到日志（用于问题排查）
   - 添加：同时记录Prometheus指标（用于性能监控）

2. **系统资源监控**：
   - 保留：`/monitoring/resources` API（用于快速查看）
   - 添加：Prometheus系统资源指标（用于长期监控）

3. **日志文件**：
   - 保留：日志文件输出（用于问题排查）
   - 优化：减少详细日志，只保留关键信息（可选）

---

## 实施步骤

### 步骤1: 安装依赖

#### Python服务依赖

**diagnosis-engine-service/requirements.txt**：

```txt
prometheus-client==0.19.0
```

**其他Python服务**（health-state-assessment-service, clinical-parsing-service等）：

```txt
prometheus-client==0.19.0
```

安装命令：
```bash
pip install prometheus-client==0.19.0
```

#### Java服务依赖

**diagnosis-service/pom.xml** 和 **execution-trace-service/pom.xml**：

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

---

### 步骤2: Python服务集成

#### 2.1 诊断引擎服务（diagnosis-engine-service）

**修改 `app/main.py`**：

```python
"""
诊断引擎服务主程序
"""
import time
import logging
from fastapi import FastAPI, Request, Response
from app.api.routes import router
from app.utils.exceptions import setup_exception_handlers
from app.utils.logger import setup_logger

# Prometheus指标
from prometheus_client import Counter, Histogram, generate_latest, REGISTRY
from prometheus_client.openmetrics.exposition import CONTENT_TYPE_LATEST

# 配置日志系统（性能优化：支持文件输出）
logger = setup_logger("diagnosis-engine", "logs/app.log")

# 定义Prometheus指标
http_requests_total = Counter(
    'http_requests_total',
    'Total HTTP requests',
    ['method', 'path', 'status', 'service']
)

http_request_duration_seconds = Histogram(
    'http_request_duration_seconds',
    'HTTP request duration in seconds',
    ['method', 'path', 'service'],
    buckets=[0.1, 0.5, 1.0, 2.0, 5.0, 10.0]  # 自定义分桶
)

http_request_errors_total = Counter(
    'http_request_errors_total',
    'Total HTTP request errors',
    ['method', 'path', 'error_type', 'service']
)

app = FastAPI(
    title="诊断引擎服务",
    version="1.0.0",
    description="智能诊断系统的诊断引擎服务"
)

# 性能监控中间件（性能优化：同时记录日志和Prometheus指标）
@app.middleware("http")
async def performance_middleware(request: Request, call_next):
    """性能监控中间件"""
    start_time = time.time()
    method = request.method
    path = request.url.path
    service = "diagnosis-engine"
    
    # 记录请求开始（简化日志，只记录关键信息）
    if path not in ["/health", "/metrics"]:  # 排除健康检查和指标端点
        logger.debug(f"请求开始: {method} {path}")
    
    try:
        response = await call_next(request)
        
        # 计算处理时间
        process_time = time.time() - start_time
        status = response.status_code
        
        # 记录Prometheus指标（内存操作，几乎无性能影响）
        http_requests_total.labels(
            method=method,
            path=path,
            status=str(status),
            service=service
        ).inc()
        
        http_request_duration_seconds.labels(
            method=method,
            path=path,
            service=service
        ).observe(process_time)
        
        # 记录日志（保留关键信息）
        if process_time > 1.0:  # 只记录慢请求
            logger.warning(
                f"慢请求: {method} {path} 状态码={status} 耗时={process_time:.3f}s"
            )
        
        # 添加响应头
        response.headers["X-Process-Time"] = str(process_time)
        
        return response
    except Exception as e:
        process_time = time.time() - start_time
        
        # 记录错误指标
        http_request_errors_total.labels(
            method=method,
            path=path,
            error_type=type(e).__name__,
            service=service
        ).inc()
        
        # 记录错误日志
        logger.error(
            f"请求失败: {method} {path} 耗时={process_time:.3f}s 错误={str(e)}",
            exc_info=True
        )
        raise

# 注册路由
app.include_router(router, prefix="/api/v1")

# 设置异常处理
setup_exception_handlers(app)

# 暴露Prometheus指标端点
@app.get("/metrics")
async def metrics():
    """Prometheus指标端点"""
    return Response(
        generate_latest(REGISTRY),
        media_type=CONTENT_TYPE_LATEST
    )

@app.get("/health")
async def health():
    return {"status": "ok", "service": "diagnosis-engine-service"}
```

**修改 `app/utils/monitoring.py`**（添加Prometheus系统资源指标）：

```python
"""
系统资源监控工具
性能优化：用于监控系统资源使用情况
同时支持API查询和Prometheus指标导出
"""
import logging
from typing import Dict
from prometheus_client import Gauge

logger = logging.getLogger(__name__)

# Prometheus系统资源指标
system_cpu_usage_percent = Gauge(
    'system_cpu_usage_percent',
    'System CPU usage percentage',
    ['host']
)

system_memory_usage_bytes = Gauge(
    'system_memory_usage_bytes',
    'System memory usage in bytes',
    ['host']
)

system_memory_total_bytes = Gauge(
    'system_memory_total_bytes',
    'System total memory in bytes',
    ['host']
)

system_disk_usage_bytes = Gauge(
    'system_disk_usage_bytes',
    'System disk usage in bytes',
    ['host', 'path']
)

try:
    import psutil
    import socket
    PSUTIL_AVAILABLE = True
    HOSTNAME = socket.gethostname()
except ImportError:
    PSUTIL_AVAILABLE = False
    HOSTNAME = "unknown"
    logger.warning("psutil not installed, resource monitoring will be limited")

def get_system_resources() -> Dict:
    """
    获取系统资源使用情况
    同时更新Prometheus指标
    
    Returns:
        包含CPU、内存、磁盘等信息的字典
    """
    if not PSUTIL_AVAILABLE:
        return {
            "error": "psutil not installed",
            "message": "Please install psutil: pip install psutil"
        }
    
    try:
        # CPU使用率
        cpu_percent = psutil.cpu_percent(interval=1)
        cpu_count = psutil.cpu_count()
        
        # 内存使用
        memory = psutil.virtual_memory()
        memory_percent = memory.percent
        memory_used_gb = memory.used / (1024 ** 3)
        memory_total_gb = memory.total / (1024 ** 3)
        
        # 磁盘使用（Windows使用C:，Linux/Mac使用/）
        import os
        disk_path = 'C:' if os.name == 'nt' else '/'
        try:
            disk = psutil.disk_usage(disk_path)
            disk_percent = disk.percent
            disk_used_gb = disk.used / (1024 ** 3)
            disk_total_gb = disk.total / (1024 ** 3)
        except Exception:
            disk_percent = 0
            disk_used_gb = 0
            disk_total_gb = 0
        
        # 更新Prometheus指标
        system_cpu_usage_percent.labels(host=HOSTNAME).set(cpu_percent)
        system_memory_usage_bytes.labels(host=HOSTNAME).set(memory.used)
        system_memory_total_bytes.labels(host=HOSTNAME).set(memory.total)
        system_disk_usage_bytes.labels(host=HOSTNAME, path=disk_path).set(disk.used)
        
        return {
            "cpu": {
                "percent": cpu_percent,
                "count": cpu_count
            },
            "memory": {
                "percent": memory_percent,
                "used_gb": round(memory_used_gb, 2),
                "total_gb": round(memory_total_gb, 2)
            },
            "disk": {
                "percent": disk_percent,
                "used_gb": round(disk_used_gb, 2),
                "total_gb": round(disk_total_gb, 2)
            }
        }
    except Exception as e:
        logger.error(f"获取系统资源失败: {str(e)}")
        return {"error": str(e)}
```

#### 2.2 其他Python服务

为其他Python服务（health-state-assessment-service, clinical-parsing-service等）添加类似的集成：

**模板代码**（`app/main.py`）：

```python
from prometheus_client import Counter, Histogram, generate_latest, REGISTRY
from prometheus_client.openmetrics.exposition import CONTENT_TYPE_LATEST
from fastapi import Response

# 定义指标
http_requests_total = Counter(
    'http_requests_total',
    'Total HTTP requests',
    ['method', 'path', 'status', 'service']
)

http_request_duration_seconds = Histogram(
    'http_request_duration_seconds',
    'HTTP request duration in seconds',
    ['method', 'path', 'service'],
    buckets=[0.1, 0.5, 1.0, 2.0, 5.0, 10.0]
)

# 中间件（类似diagnosis-engine-service）
@app.middleware("http")
async def metrics_middleware(request: Request, call_next):
    # ... 实现类似diagnosis-engine-service
    pass

# 指标端点
@app.get("/metrics")
async def metrics():
    return Response(generate_latest(REGISTRY), media_type=CONTENT_TYPE_LATEST)
```

---

### 步骤3: Java服务集成

#### 3.1 诊断服务（diagnosis-service）

**修改 `pom.xml`**：

```xml
<dependencies>
    <!-- 其他依赖... -->
    
    <!-- Prometheus监控 -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
</dependencies>
```

**修改 `src/main/resources/application-mysql.yml`**：

```yaml
# 在文件末尾添加
management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,metrics,info
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: diagnosis-service
      environment: production
  endpoint:
    prometheus:
      enabled: true
```

#### 3.2 执行追踪服务（execution-trace-service）

**同样的修改**：
- 在 `pom.xml` 中添加依赖
- 在 `application-mysql.yml` 中添加配置

---

### 步骤4: 部署Prometheus

#### 4.1 创建Prometheus配置文件

**创建 `monitoring/prometheus.yml`**：

```yaml
global:
  scrape_interval: 15s      # 每15秒拉取一次指标
  evaluation_interval: 15s # 每15秒评估一次告警规则
  external_labels:
    cluster: 'aidoctor'
    environment: 'production'

# 告警规则文件
rule_files:
  - 'alert_rules.yml'

# 抓取配置
scrape_configs:
  # 诊断引擎服务（Python）
  - job_name: 'diagnosis-engine-service'
    static_configs:
      - targets: ['host.docker.internal:8086']  # Windows Docker使用host.docker.internal
        labels:
          service: 'diagnosis-engine'
          language: 'python'
          environment: 'production'

  # 健康判定服务（Python）
  - job_name: 'health-state-assessment-service'
    static_configs:
      - targets: ['host.docker.internal:8081']
        labels:
          service: 'health-state-assessment'
          language: 'python'
          environment: 'production'

  # 病例理解服务（Python）
  - job_name: 'clinical-parsing-service'
    static_configs:
      - targets: ['host.docker.internal:8082']
        labels:
          service: 'clinical-parsing'
          language: 'python'
          environment: 'production'

  # 诊断服务（Java）
  - job_name: 'diagnosis-service'
    static_configs:
      - targets: ['host.docker.internal:8084']
        labels:
          service: 'diagnosis'
          language: 'java'
          environment: 'production'
    metrics_path: '/actuator/prometheus'  # Spring Boot Actuator路径

  # 执行追踪服务（Java）
  - job_name: 'execution-trace-service'
    static_configs:
      - targets: ['host.docker.internal:8093']
        labels:
          service: 'execution-trace'
          language: 'java'
          environment: 'production'
    metrics_path: '/actuator/prometheus'
```

**创建 `monitoring/alert_rules.yml`**：

```yaml
groups:
  - name: service_alerts
    interval: 30s
    rules:
      # 慢请求告警
      - alert: HighResponseTime
        expr: |
          histogram_quantile(0.95, 
            sum(rate(http_request_duration_seconds_bucket[5m])) by (le, service, path)
          ) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "服务响应时间过高"
          description: "{{ $labels.service }} 的 {{ $labels.path }} 接口95分位响应时间超过2秒"

      # 错误率告警
      - alert: HighErrorRate
        expr: |
          sum(rate(http_request_errors_total[5m])) by (service) / 
          sum(rate(http_requests_total[5m])) by (service) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "服务错误率过高"
          description: "{{ $labels.service }} 的错误率超过5%"

      # CPU使用率告警
      - alert: HighCPUUsage
        expr: system_cpu_usage_percent > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "CPU使用率过高"
          description: "{{ $labels.host }} 的CPU使用率超过80%"

      # 内存使用率告警
      expr: |
        (system_memory_usage_bytes / system_memory_total_bytes) * 100 > 85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "内存使用率过高"
          description: "{{ $labels.host }} 的内存使用率超过85%"
```

#### 4.2 创建Docker Compose配置

**创建 `monitoring/docker-compose.yml`**：

```yaml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - ./alert_rules.yml:/etc/prometheus/alert_rules.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=30d'  # 保留30天数据
      - '--web.console.libraries=/usr/share/prometheus/console_libraries'
      - '--web.console.templates=/usr/share/prometheus/consoles'
    networks:
      - monitoring
    restart: unless-stopped

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3001:3000"  # 避免与前端冲突
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_INSTALL_PLUGINS=grafana-piechart-panel
    volumes:
      - grafana-data:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning  # 自动配置数据源
    networks:
      - monitoring
    depends_on:
      - prometheus
    restart: unless-stopped

volumes:
  prometheus-data:
  grafana-data:

networks:
  monitoring:
    driver: bridge
```

#### 4.3 启动Prometheus和Grafana

```bash
cd monitoring
docker-compose up -d
```

**访问地址**：
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001 (用户名: admin, 密码: admin)

---

### 步骤5: 配置Grafana

#### 5.1 添加Prometheus数据源

1. 登录 Grafana (http://localhost:3001)
2. 进入 **Configuration** → **Data Sources**
3. 点击 **Add data source**
4. 选择 **Prometheus**
5. 配置：
   - **URL**: `http://prometheus:9090` (Docker内部网络)
   - **Access**: Server (default)
6. 点击 **Save & Test**

#### 5.2 创建仪表板

**创建服务性能仪表板**：

1. 进入 **Dashboards** → **New Dashboard**
2. 添加以下面板：

**面板1: QPS（每秒请求数）**
```promql
sum(rate(http_requests_total[1m])) by (service)
```

**面板2: 响应时间（P95）**
```promql
histogram_quantile(0.95, 
  sum(rate(http_request_duration_seconds_bucket[5m])) by (le, service)
)
```

**面板3: 错误率**
```promql
sum(rate(http_request_errors_total[5m])) by (service) / 
sum(rate(http_requests_total[5m])) by (service) * 100
```

**面板4: 系统资源（CPU）**
```promql
system_cpu_usage_percent
```

**面板5: 系统资源（内存）**
```promql
(system_memory_usage_bytes / system_memory_total_bytes) * 100
```

#### 5.3 导入现成仪表板（可选）

Grafana提供了大量现成的仪表板模板：

1. 进入 **Dashboards** → **Import**
2. 输入模板ID（如：315 - Spring Boot, 11074 - Node Exporter）
3. 选择数据源
4. 导入

---

### 步骤6: 配置告警

#### 6.1 在Grafana中配置告警

1. 进入仪表板，编辑面板
2. 进入 **Alert** 标签
3. 配置告警规则：
   - **Condition**: 当指标超过阈值时
   - **Notifications**: 配置通知渠道（邮件、钉钉、企业微信等）

#### 6.2 使用AlertManager（可选）

如果需要更复杂的告警规则，可以使用AlertManager：

**修改 `docker-compose.yml`**：

```yaml
  alertmanager:
    image: prom/alertmanager:latest
    container_name: alertmanager
    ports:
      - "9093:9093"
    volumes:
      - ./alertmanager.yml:/etc/alertmanager/alertmanager.yml
      - alertmanager-data:/alertmanager
    networks:
      - monitoring
    restart: unless-stopped
```

---

## 代码修改清单

### Python服务

| 服务 | 文件 | 修改内容 |
|------|------|---------|
| diagnosis-engine-service | `requirements.txt` | 添加 `prometheus-client==0.19.0` |
| diagnosis-engine-service | `app/main.py` | 添加Prometheus指标和`/metrics`端点 |
| diagnosis-engine-service | `app/utils/monitoring.py` | 添加Prometheus系统资源指标 |
| health-state-assessment-service | `requirements.txt` | 添加 `prometheus-client==0.19.0` |
| health-state-assessment-service | `app/main.py` | 添加Prometheus指标和`/metrics`端点 |
| clinical-parsing-service | `requirements.txt` | 添加 `prometheus-client==0.19.0` |
| clinical-parsing-service | `app/main.py` | 添加Prometheus指标和`/metrics`端点 |
| 其他Python服务 | 同上 | 同上 |

### Java服务

| 服务 | 文件 | 修改内容 |
|------|------|---------|
| diagnosis-service | `pom.xml` | 添加 `micrometer-registry-prometheus` 依赖 |
| diagnosis-service | `application-mysql.yml` | 添加 `management` 配置 |
| execution-trace-service | `pom.xml` | 添加 `micrometer-registry-prometheus` 依赖 |
| execution-trace-service | `application-mysql.yml` | 添加 `management` 配置 |

### 监控服务

| 文件 | 说明 |
|------|------|
| `monitoring/prometheus.yml` | Prometheus配置文件 |
| `monitoring/alert_rules.yml` | 告警规则文件 |
| `monitoring/docker-compose.yml` | Docker Compose配置 |

---

## 验证方法

### 1. 验证指标收集

#### 检查指标端点

**Python服务**：
```bash
curl http://localhost:8086/metrics
```

应该看到类似输出：
```
# HELP http_requests_total Total HTTP requests
# TYPE http_requests_total counter
http_requests_total{method="GET",path="/health",status="200",service="diagnosis-engine"} 10.0

# HELP http_request_duration_seconds HTTP request duration in seconds
# TYPE http_request_duration_seconds histogram
http_request_duration_seconds_bucket{method="GET",path="/health",service="diagnosis-engine",le="0.1"} 10.0
...
```

**Java服务**：
```bash
curl http://localhost:8084/actuator/prometheus
```

#### 检查Prometheus

1. 访问 http://localhost:9090
2. 进入 **Status** → **Targets**
3. 检查所有服务的状态是否为 **UP**

#### 查询指标

在Prometheus查询界面输入：
```promql
sum(rate(http_requests_total[1m])) by (service)
```

应该能看到各服务的请求速率。

### 2. 验证Grafana

1. 访问 http://localhost:3001
2. 登录（admin/admin）
3. 查看仪表板，应该能看到：
   - 各服务的QPS
   - 响应时间
   - 错误率
   - 系统资源

### 3. 性能影响验证

**测试方法**：
1. 记录集成Prometheus前的请求响应时间
2. 集成Prometheus后，再次测试
3. 对比响应时间差异

**预期结果**：
- 响应时间增加 <0.1ms（几乎无影响）

---

## 常见问题

### Q1: 是否需要删除当前的监控日志功能？

**A**: **不需要完全删除**，建议：

1. **保留日志文件**：用于详细问题排查
2. **优化日志级别**：减少详细日志，只保留关键信息（慢请求、错误）
3. **添加Prometheus指标**：用于性能监控和趋势分析

**原因**：
- 日志和指标是**互补的**，不是替代关系
- 日志用于问题排查，指标用于性能监控
- 两者可以共存，互不影响

### Q2: 日志文件会很大吗？

**A**: 如果担心日志文件过大，可以：

1. **减少日志详细程度**：只记录慢请求和错误
2. **使用日志轮转**：已经配置了日志轮转（10MB，保留5个备份）
3. **调整日志级别**：将详细日志改为DEBUG级别

### Q3: Prometheus会占用很多资源吗？

**A**: 不会：

- **客户端库**：内存占用 <10MB，CPU开销 <0.1%
- **Prometheus Server**：内存 1-2GB，CPU <5%
- **Grafana**：内存 500MB，CPU <2%

### Q4: 如果Prometheus服务挂了，会影响业务服务吗？

**A**: **不会**：

- Prometheus是**主动拉取**，业务服务是被动的
- 业务服务只是暴露`/metrics`端点，不主动连接Prometheus
- 即使Prometheus挂了，业务服务仍然正常运行

### Q5: 如何查看历史数据？

**A**: 

- Prometheus默认保留15天数据（可配置）
- 如果需要更长的历史数据，可以：
  1. 增加Prometheus存储保留时间
  2. 使用长期存储（如Thanos、Cortex）
  3. 导出数据到其他数据库

### Q6: Windows Docker如何访问宿主机服务？

**A**: 

在`prometheus.yml`中使用 `host.docker.internal`：

```yaml
targets: ['host.docker.internal:8086']
```

这是Docker Desktop提供的特殊DNS名称，可以访问宿主机服务。

---

## 总结

### 核心要点

1. **保留当前监控功能**：日志和指标是互补的，可以共存
2. **优化日志**：减少详细日志，只保留关键信息
3. **添加Prometheus指标**：用于性能监控和趋势分析
4. **几乎无性能影响**：Prometheus客户端库极轻量

### 实施建议

1. **逐步实施**：先在一个服务试点，验证后再推广
2. **保留日志**：日志仍然有价值，不要完全删除
3. **优化日志**：减少详细日志，提高性能
4. **统一规范**：所有服务使用统一的指标命名和标签规范

---

**文档维护**: 根据实施情况持续更新本文档。

