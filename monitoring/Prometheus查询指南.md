# Prometheus 查询指南 - 查看各服务资源占用

## 快速访问

访问 Prometheus Web UI: http://localhost:9090

## 查看各服务资源占用

### 1. 查看所有 Java 服务的 JVM 堆内存使用（GB）

```promql
jvm_memory_used_bytes{area="heap"} / 1024 / 1024 / 1024
```

**说明**：
- 显示每个 Java 服务的堆内存使用量（单位：GB）
- 可以看到 `diagnosis-service`、`examination-service`、`execution-trace-service` 的内存使用

### 2. 查看所有服务的进程内存使用（GB）

```promql
process_resident_memory_bytes / 1024 / 1024 / 1024
```

**说明**：
- 显示所有服务（包括 Java 和 Python）的进程内存使用
- 可以看到每个服务实际占用的物理内存

### 3. 查看所有服务的 CPU 使用率（%）

```promql
rate(process_cpu_seconds_total[5m]) * 100
```

**说明**：
- 显示每个服务的 CPU 使用率百分比
- `[5m]` 表示过去 5 分钟的平均值

### 4. 查看各服务的 QPS（每秒请求数）

```promql
sum(rate(http_requests_total[5m])) by (service)
```

**说明**：
- 显示每个服务每秒处理的请求数
- 按服务名称分组

### 5. 查看各服务的响应时间（P95）

```promql
histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le, service))
```

**说明**：
- 显示每个服务 95% 的请求响应时间
- 单位：秒

### 6. 查看各服务的错误率（%）

```promql
sum(rate(http_request_errors_total[5m])) by (service) / 
sum(rate(http_requests_total[5m])) by (service) * 100
```

**说明**：
- 显示每个服务的错误率百分比
- 错误率 = 错误请求数 / 总请求数 * 100

### 7. 查看 Java 服务的 JVM 堆内存使用率（%）

```promql
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

**说明**：
- 显示每个 Java 服务的堆内存使用率
- 可以看到堆内存是否接近上限

### 8. 查看 Java 服务的 GC 次数

```promql
rate(jvm_gc_pause_seconds_count[5m])
```

**说明**：
- 显示每个 Java 服务的垃圾回收频率
- 频率过高可能表示内存压力大

### 9. 查看各服务的总请求数

```promql
sum(increase(http_requests_total[1h])) by (service)
```

**说明**：
- 显示过去 1 小时内每个服务的总请求数

### 10. 查看特定服务的所有指标

**诊断服务（Java）**：
```promql
{service="diagnosis"}
```

**诊断引擎服务（Python）**：
```promql
{service="diagnosis-engine"}
```

## 在 Prometheus 中的操作步骤

### 步骤 1：打开 Prometheus Web UI
1. 访问 http://localhost:9090
2. 点击顶部菜单 **Graph**

### 步骤 2：输入查询
1. 在搜索框中输入上述任一 PromQL 查询
2. 点击 **Execute** 按钮
3. 选择 **Graph** 标签查看图表，或 **Table** 标签查看表格

### 步骤 3：查看所有可用指标
1. 点击顶部菜单 **Status** -> **Targets**
2. 查看所有服务的状态（应该都是 UP）
3. 点击顶部菜单 **Graph**
4. 在搜索框输入指标名称的一部分，Prometheus 会自动补全

### 步骤 4：查看指标详情
1. 在搜索框输入指标名称，如：`jvm_memory_used_bytes`
2. 点击 **Execute**
3. 可以看到所有带有该指标的服务实例

## 常用指标列表

### Java 服务指标（通过 Spring Boot Actuator 自动暴露）

#### CPU 指标
- `process_cpu_seconds_total` - 进程累计 CPU 使用时间（秒，Counter类型）
- `system_cpu_usage` - 系统 CPU 使用率（0-1之间，Gauge类型）
- `process_cpu_usage` - 进程 CPU 使用率（0-1之间，Gauge类型）

#### 内存指标
- `jvm_memory_used_bytes` - JVM 内存使用（字节，按area分类：heap/nonheap）
- `jvm_memory_max_bytes` - JVM 最大内存（字节）
- `jvm_memory_committed_bytes` - JVM 已提交内存（字节）
- `jvm_memory_init_bytes` - JVM 初始内存（字节）
- `process_resident_memory_bytes` - 进程物理内存使用（字节，RSS）
- `jvm_memory_usage` - JVM 内存使用率（0-1之间）

#### GC 指标
- `jvm_gc_pause_seconds_count` - GC 暂停次数
- `jvm_gc_pause_seconds_sum` - GC 暂停总时间
- `jvm_gc_pause_seconds` - GC 暂停时间（Histogram）

#### HTTP 指标
- `http_server_requests_seconds` - HTTP 请求耗时（Histogram）
- `http_server_requests_total` - HTTP 请求总数（Counter）

**注意**：Java服务通过`/actuator/prometheus`端点暴露这些指标，Prometheus会自动抓取。

### Python 服务指标
- `process_resident_memory_bytes` - 进程物理内存使用（字节）
- `process_cpu_seconds_total` - 进程 CPU 使用时间（秒）
- `http_requests_total` - HTTP 请求总数
- `http_request_duration_seconds` - HTTP 请求耗时
- `http_request_errors_total` - HTTP 请求错误数

## 查看服务状态

### 检查所有服务是否正常运行
1. 访问 http://localhost:9090
2. 点击 **Status** -> **Targets**
3. 查看所有服务的状态：
   - **UP** - 服务正常运行
   - **DOWN** - 服务无法访问

### 查看服务标签
在查询结果中，可以看到每个指标都有标签，例如：
- `service` - 服务名称
- `instance` - 服务实例地址
- `job` - Prometheus 任务名称

## 示例：查看诊断服务（diagnosis-service）的资源占用

### CPU 相关查询

1. **查看 CPU 使用率（百分比）**：
   ```promql
   rate(process_cpu_seconds_total{service="diagnosis"}[5m]) * 100
   ```
   - 显示过去5分钟的平均CPU使用率
   - 单位：百分比（%）

2. **查看 CPU 使用率（按处理器核心数）**：
   ```promql
   rate(process_cpu_seconds_total{service="diagnosis"}[5m]) * 100 / 
   count(count by (instance) (process_cpu_seconds_total{service="diagnosis"}))
   ```
   - 如果服务器有多个CPU核心，这个查询会显示相对于单核的使用率

3. **查看进程累计 CPU 时间（秒）**：
   ```promql
   process_cpu_seconds_total{service="diagnosis"}
   ```
   - 显示进程从启动到现在的累计CPU使用时间

### 内存相关查询

1. **查看进程物理内存使用（GB）**：
   ```promql
   process_resident_memory_bytes{service="diagnosis"} / 1024 / 1024 / 1024
   ```
   - 显示进程实际占用的物理内存（RSS）
   - 单位：GB

2. **查看 JVM 堆内存使用（GB）**：
   ```promql
   jvm_memory_used_bytes{service="diagnosis", area="heap"} / 1024 / 1024 / 1024
   ```
   - 显示JVM堆内存的使用量
   - 单位：GB

3. **查看 JVM 堆内存使用率（%）**：
   ```promql
   jvm_memory_used_bytes{service="diagnosis", area="heap"} / 
   jvm_memory_max_bytes{service="diagnosis", area="heap"} * 100
   ```
   - 显示堆内存使用率百分比
   - 接近100%时需要注意GC压力

4. **查看 JVM 非堆内存使用（GB）**：
   ```promql
   jvm_memory_used_bytes{service="diagnosis", area="nonheap"} / 1024 / 1024 / 1024
   ```
   - 显示JVM非堆内存（方法区、元空间等）的使用量

5. **查看 JVM 总内存使用（GB）**：
   ```promql
   (jvm_memory_used_bytes{service="diagnosis", area="heap"} + 
    jvm_memory_used_bytes{service="diagnosis", area="nonheap"}) / 1024 / 1024 / 1024
   ```
   - 显示JVM总内存使用量（堆+非堆）

6. **查看 JVM 已提交内存（GB）**：
   ```promql
   jvm_memory_committed_bytes{service="diagnosis", area="heap"} / 1024 / 1024 / 1024
   ```
   - 显示JVM已向操作系统申请的内存大小

### 综合查询

1. **同时查看 CPU 和内存使用**：
   ```promql
   # CPU使用率
   rate(process_cpu_seconds_total{service="diagnosis"}[5m]) * 100
   
   # 内存使用（GB）
   process_resident_memory_bytes{service="diagnosis"} / 1024 / 1024 / 1024
   ```
   - 在Prometheus中可以分别执行这两个查询，然后在Graph视图中同时查看

2. **查看诊断服务的所有 JVM 指标**：
   ```promql
   {service="diagnosis", __name__=~"jvm_.*"}
   ```
   - 显示所有以`jvm_`开头的指标

3. **查看诊断服务的所有进程指标**：
   ```promql
   {service="diagnosis", __name__=~"process_.*"}
   ```
   - 显示所有以`process_`开头的指标

### 性能指标查询

1. **查看 QPS（每秒请求数）**：
   ```promql
   sum(rate(http_requests_total{service="diagnosis"}[5m]))
   ```

2. **查看响应时间（P95）**：
   ```promql
   histogram_quantile(0.95, 
     sum(rate(http_request_duration_seconds_bucket{service="diagnosis"}[5m])) by (le)
   )
   ```

3. **查看错误率（%）**：
   ```promql
   sum(rate(http_request_errors_total{service="diagnosis"}[5m])) / 
   sum(rate(http_requests_total{service="diagnosis"}[5m])) * 100
   ```

## 时间范围选择

在 Prometheus 查询界面右上角，可以选择时间范围：
- **Last 1 hour** - 过去 1 小时
- **Last 6 hours** - 过去 6 小时
- **Last 24 hours** - 过去 24 小时
- **Custom** - 自定义时间范围

## 导出到 Grafana

查询结果可以导出到 Grafana 进行可视化：
1. 在 Grafana 中创建新的 Dashboard
2. 添加 Panel
3. 在查询框中输入相同的 PromQL 查询
4. 选择 Prometheus 数据源

## 故障排查

### 如果看不到数据
1. 检查服务是否运行：访问 http://localhost:9090/status/targets
2. 检查时间范围：确保选择的时间范围内有数据
3. 检查指标名称：使用 **Status** -> **Targets** 查看服务是否正常

### 如果查询返回空结果
1. 确认服务标签正确：`{service="diagnosis"}` 中的服务名称要匹配
2. 检查时间范围：可能需要选择更长的时间范围
3. 查看原始指标：先查询基础指标，如 `process_resident_memory_bytes`

## 快速查询参考（诊断服务）

### 最常用的查询

**1. CPU 使用率（%）**
```promql
rate(process_cpu_seconds_total{service="diagnosis"}[5m]) * 100
```

**2. 内存使用（GB）**
```promql
process_resident_memory_bytes{service="diagnosis"} / 1024 / 1024 / 1024
```

**3. JVM 堆内存使用（GB）**
```promql
jvm_memory_used_bytes{service="diagnosis", area="heap"} / 1024 / 1024 / 1024
```

**4. JVM 堆内存使用率（%）**
```promql
jvm_memory_used_bytes{service="diagnosis", area="heap"} / 
jvm_memory_max_bytes{service="diagnosis", area="heap"} * 100
```

### 验证服务是否正常暴露指标

1. **直接访问 Actuator 端点**：
   ```
   http://localhost:8084/actuator/prometheus
   ```
   应该能看到所有以`jvm_`、`process_`开头的指标

2. **在 Prometheus 中验证**：
   ```promql
   up{job="diagnosis-service"}
   ```
   如果返回`1`，说明服务正常；返回`0`或没有结果，说明服务无法访问

3. **查看所有可用指标**：
   ```promql
   {service="diagnosis"}
   ```
   这会显示所有带有`service="diagnosis"`标签的指标

