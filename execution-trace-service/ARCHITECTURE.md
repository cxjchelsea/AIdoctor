# 执行追踪服务架构设计说明

## 为什么使用独立数据库？

### 1. 微服务架构原则

`execution-trace-service` 使用独立的 `aidoctor_trace` 数据库，而 `diagnosis-service` 使用 `aidoctor` 数据库。这是典型的**微服务数据隔离**设计：

- **服务解耦**：两个服务可以独立部署、扩展和维护
- **数据隔离**：追踪数据不会影响主业务数据库的性能
- **独立扩展**：可以根据追踪数据量独立扩展数据库资源
- **故障隔离**：追踪服务故障不会影响主业务流程

### 2. 如何基于CDP进行关联？

虽然数据存储在不同的数据库中，但通过 **CDP ID** 作为关联键，可以实现完整的数据关联：

#### 数据关联方式

```
┌─────────────────────────────────────────────────────────────┐
│                    CDP ID (关联键)                           │
│                    "cdp_abc123..."                          │
└─────────────────────────────────────────────────────────────┘
                            │
                            │
        ┌───────────────────┴───────────────────┐
        │                                       │
        ▼                                       ▼
┌─────────────────────┐              ┌─────────────────────┐
│  aidoctor 数据库     │              │ aidoctor_trace 数据库│
│  (diagnosis-service)│              │ (execution-trace-   │
│                     │              │  service)           │
├─────────────────────┤              ├─────────────────────┤
│ cdp 表              │              │ execution_trace 表   │
│ - id: cdp_abc123    │              │ - cdp_id: cdp_abc123│
│ - execution_trace:  │              │ - 详细追踪数据       │
│   {摘要信息}        │              │ - 时间戳、输入输出等 │
└─────────────────────┘              └─────────────────────┘
```

#### 查询方式

**1. 从CDP查询追踪摘要（快速查询）**
```java
// 在 diagnosis-service 中
CDP cdp = cdpManager.getCDPById(cdpId);
Map<String, Object> traceSummary = cdp.getExecutionTrace();
// 直接获取摘要，无需跨库查询
```

**2. 从追踪服务查询详细数据（详细分析）**
```java
// 在 execution-trace-service 中
List<ExecutionTrace> traces = traceRepository.findByCdpIdOrderByTimestampAsc(cdpId);
// 获取完整的执行追踪记录
```

**3. 联合查询（通过API聚合）**
```java
// 前端或管理端
// 1. 获取CDP基本信息
GET /api/v1/diagnosis/{cdpId}/result

// 2. 获取详细追踪数据
GET /api/v1/trace/cdp/{cdpId}

// 3. 获取追踪摘要
GET /api/v1/trace/cdp/{cdpId}/summary
```

### 3. 数据同步机制

#### 摘要更新流程

```
1. 执行追踪事件发生
   ↓
2. execution-trace-service 记录详细数据到 execution_trace 表
   ↓
3. execution-trace-service 异步计算追踪摘要
   ↓
4. execution-trace-service 调用 diagnosis-service API
   POST /api/v1/cdp/update-trace-summary
   ↓
5. diagnosis-service 更新 CDP.execution_trace 字段
```

#### 代码实现

**execution-trace-service 端：**
```java
// ExecutionTraceService.java
@Async("traceExecutor")
private void updateCDPTraceSummary(String cdpId) {
    // 1. 查询详细追踪数据
    List<ExecutionTrace> traces = traceRepository.findByCdpIdOrderByTimestampAsc(cdpId);
    
    // 2. 构建摘要
    Map<String, Object> summary = buildTraceSummary(traces).toSummaryMap();
    
    // 3. 调用 diagnosis-service 更新CDP
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("cdpId", cdpId);
    requestBody.put("executionTrace", summary);
    restTemplate.postForEntity(cdpUpdateUrl, requestBody, Void.class);
}
```

**diagnosis-service 端：**
```java
// CDPController.java
@PostMapping("/update-trace-summary")
public ResponseEntity<Void> updateTraceSummary(@RequestBody Map<String, Object> request) {
    String cdpId = (String) request.get("cdpId");
    Map<String, Object> executionTrace = (Map<String, Object>) request.get("executionTrace");
    
    Map<String, Object> updates = new HashMap<>();
    updates.put("executionTrace", executionTrace);
    cdpManager.updateCDP(cdpId, updates);
    
    return ResponseEntity.ok().build();
}
```

### 4. 数据一致性保证

#### 最终一致性

- **详细数据**：实时写入 `execution_trace` 表
- **摘要数据**：异步更新到 `cdp.execution_trace` 字段
- **一致性**：采用最终一致性模型，摘要可能略有延迟（通常<1秒）

#### 容错机制

- 如果更新CDP摘要失败，不影响详细数据的记录
- 摘要更新失败会记录日志，可以手动触发重新计算
- 主业务流程不受追踪服务影响（追踪失败不影响诊断流程）

### 5. 扩展性优势

#### 独立扩展

- **数据库扩展**：可以根据追踪数据量独立扩展 `aidoctor_trace` 数据库
- **服务扩展**：可以独立扩展 `execution-trace-service` 的实例数量
- **存储策略**：可以为追踪数据设置不同的保留策略（如归档、删除）

#### 查询优化

- **摘要查询**：从CDP直接获取摘要，无需跨库JOIN，性能高
- **详细查询**：从追踪服务查询，可以针对追踪场景优化索引
- **分离查询**：不同场景使用不同的查询路径，避免相互影响

### 6. 实际使用示例

#### 场景1：查看诊断结果（快速）

```java
// 前端调用
GET /api/v1/diagnosis/{cdpId}/result

// 返回的CDP中包含 executionTrace 摘要
{
  "cdpId": "cdp_abc123",
  "ddx": [...],
  "executionTrace": {
    "totalDuration": 1500,
    "serviceCalls": ["diagnosis-service", "clinical-parsing-service"],
    "steps": [...]
  }
}
```

#### 场景2：分析执行路径（详细）

```java
// 前端调用
GET /api/v1/trace/cdp/{cdpId}

// 返回详细的追踪记录
[
  {
    "cdpId": "cdp_abc123",
    "service": "diagnosis-service",
    "method": "executeDiagnosisWorkflow",
    "duration": 500,
    "input": {...},
    "output": {...}
  },
  ...
]
```

#### 场景3：实时监控（WebSocket）

```javascript
// 前端连接WebSocket
const socket = new SockJS('http://localhost:8093/api/v1/trace/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function() {
    // 订阅CDP的追踪事件
    stompClient.subscribe('/topic/trace/cdp_abc123', function(message) {
        const event = JSON.parse(message.body);
        // 实时更新UI
        updateTraceVisualization(event);
    });
});
```

## 总结

使用独立数据库的设计优势：

1. ✅ **服务解耦**：两个服务可以独立开发和部署
2. ✅ **性能隔离**：追踪数据不影响主业务数据库
3. ✅ **独立扩展**：可以根据需求独立扩展资源
4. ✅ **故障隔离**：追踪服务故障不影响诊断流程
5. ✅ **灵活查询**：摘要快速查询，详细数据按需查询

通过 **CDP ID** 作为关联键，即使不在同一个数据库中，也能实现完整的数据关联和查询。

