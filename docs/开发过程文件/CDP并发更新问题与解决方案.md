# CDP并发更新问题与解决方案

> **文档版本**: v1.0  
> **创建日期**: 2026-01-27  
> **问题类型**: 并发冲突、数据库更新冲突  
> **影响范围**: diagnosis-service, execution-trace-service  
> **严重程度**: 高（导致服务异常，影响系统稳定性）

---

## 📋 目录

- [问题概述](#问题概述)
- [问题现象](#问题现象)
- [问题分析](#问题分析)
- [根本原因](#根本原因)
- [解决方案](#解决方案)
  - [方案1: 使用悲观锁防止并发冲突](#方案1-使用悲观锁防止并发冲突)
  - [方案2: 添加防抖机制减少更新频率](#方案2-添加防抖机制减少更新频率)
- [代码修改详情](#代码修改详情)
- [验证方法](#验证方法)
- [预期效果](#预期效果)
- [相关文件](#相关文件)

---

## 问题概述

### 问题描述

在执行健康筛查流程时，系统出现大量 `CDPNotFoundException` 错误，导致CDP追踪摘要更新失败。即使只有单个用户执行一次健康筛查流程，也会出现并发更新冲突。

### 错误信息

```
com.aidoctor.diagnosis.exception.CDPNotFoundException: CDP不存在: CDP not found: cdp_10e3a2ef12994795b797e2214b029f49
	at com.aidoctor.diagnosis.service.cdp.CDPManager.lambda$updateCDP$0(CDPManager.java:85)
	at java.util.Optional.orElseThrow(Optional.java:290)
	at com.aidoctor.diagnosis.service.cdp.CDPManager.updateCDP(CDPManager.java:85)
```

### 影响范围

- **diagnosis-service**: CDP更新接口 `/api/v1/cdp/update-trace-summary` 频繁失败
- **execution-trace-service**: 追踪摘要更新失败，但不影响主业务流程
- **用户体验**: 追踪功能异常，但不影响诊断流程本身

---

## 问题现象

### 日志特征

从日志中可以看到：

1. **多个线程同时更新同一个CDP**：
   ```
   2026-01-27 08:37:48 [http-nio-8084-exec-1] INFO  c.a.d.controller.CDPController - 更新CDP追踪摘要: cdpId=cdp_10e3a2ef12994795b797e2214b029f49
   2026-01-27 08:37:48 [http-nio-8084-exec-2] INFO  c.a.d.controller.CDPController - 更新CDP追踪摘要: cdpId=cdp_10e3a2ef12994795b797e2214b029f49
   2026-01-27 08:37:48 [http-nio-8084-exec-3] INFO  c.a.d.controller.CDPController - 更新CDP追踪摘要: cdpId=cdp_10e3a2ef12994795b797e2214b029f49
   ...
   ```

2. **大量并发错误**：
   - 在 08:37:48 这一秒内，有 10+ 个线程同时尝试更新同一个CDP
   - 多个线程出现 `CDPNotFoundException` 错误

3. **时间集中性**：
   - 所有错误都发生在同一时间点（同一秒内）
   - 说明是异步请求几乎同时到达导致的并发冲突

---

## 问题分析

### 为什么单个流程会产生并发问题？

**关键发现**：即使只有一次健康筛查流程，也会产生大量并发更新请求。

#### 1. 单个流程产生大量追踪事件

一次健康筛查流程会产生多个追踪事件：

- **Feign调用事件**：每次调用健康判定服务都会产生 `FEIGN_CALL_START` 和 `FEIGN_CALL_END` 两个事件
- **服务调用事件**：服务方法调用会产生 `SERVICE_CALL_START` 和 `SERVICE_CALL_END` 两个事件
- **事件数量**：从日志看，一次健康筛查流程可能产生 20+ 个追踪事件

#### 2. 每个事件都触发CDP更新

在 `ExecutionTraceService.recordEvent` 方法中：

```java
@Async("traceExecutor")
public void recordEvent(ExecutionTraceEvent event) {
    traceRepository.save(trace);
    updateCDPTraceSummary(event.getCdpId());  // 每个事件都触发更新！
}
```

**问题**：每个追踪事件都会异步调用 `updateCDPTraceSummary`，导致：
- 20+ 个追踪事件 → 20+ 个CDP更新请求
- 这些请求几乎同时执行（异步并发）

#### 3. 异步执行导致并发冲突

- **事件产生**：虽然是顺序产生的，但由于都是 `@Async` 异步执行
- **更新请求**：多个 `updateCDPTraceSummary` 几乎同时执行
- **并发冲突**：多个线程同时尝试更新同一个CDP，导致：
  - 某些线程在查找CDP时找不到（其他线程正在更新，事务还未提交）
  - 数据库事务隔离导致读取不到正在更新的数据

---

## 根本原因

### 1. 缺少并发控制机制

`CDPManager.updateCDP` 方法使用 `@Transactional`，但**没有使用锁机制**：

```java
@Transactional
public CDP updateCDP(String cdpId, Map<String, Object> updates) {
    CDP existingCDP = getCDPById(cdpId)  // 普通查询，没有锁
        .orElseThrow(() -> new CDPNotFoundException("CDP not found: " + cdpId));
    // ... 更新逻辑
}
```

**问题**：
- 多个事务可以同时读取同一个CDP
- 多个事务可以同时更新同一个CDP
- 导致并发冲突和数据不一致

### 2. 更新频率过高

- **每个追踪事件都触发更新**：20+ 个事件 → 20+ 次更新
- **没有防抖机制**：短时间内大量更新请求
- **数据库压力**：频繁的数据库更新操作

### 3. 事务隔离级别问题

- **READ COMMITTED**：默认隔离级别，可能导致读取不到正在更新的数据
- **没有悲观锁**：无法确保同一时间只有一个事务能更新CDP

---

## 解决方案

### 方案1: 使用悲观锁防止并发冲突

#### 实现思路

使用数据库的 `SELECT FOR UPDATE` 悲观锁，确保同一时间只有一个事务能更新CDP。

#### 代码修改

**1. 在 `CDPRepository` 中添加悲观锁查询方法**：

```java
/**
 * 使用悲观锁查询CDP（用于并发更新场景）
 * SELECT FOR UPDATE，确保同一时间只有一个事务能更新CDP
 */
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM CDP c WHERE c.id = :cdpId")
Optional<CDP> findByIdWithLock(@Param("cdpId") String cdpId);
```

**2. 修改 `CDPManager.updateCDP` 使用悲观锁**：

```java
@Transactional
public CDP updateCDP(String cdpId, Map<String, Object> updates) {
    log.info("更新CDP: cdpId={}", cdpId);
    
    // 使用悲观锁获取CDP，防止并发更新冲突
    // SELECT FOR UPDATE会锁定该行，其他事务必须等待
    CDP existingCDP = cdpRepository.findByIdWithLock(cdpId)
        .orElseThrow(() -> new CDPNotFoundException("CDP not found: " + cdpId));
    
    // ... 后续更新逻辑
}
```

#### 工作原理

1. **SELECT FOR UPDATE**：锁定CDP行，其他事务必须等待
2. **串行化执行**：确保同一时间只有一个事务能更新CDP
3. **避免冲突**：其他事务等待当前事务完成后才能继续

#### 优点

- ✅ 彻底解决并发冲突问题
- ✅ 保证数据一致性
- ✅ 实现简单，只需修改查询方法

#### 缺点

- ⚠️ 可能增加等待时间（但影响很小，因为更新操作很快）
- ⚠️ 需要数据库支持行锁（MySQL/PostgreSQL都支持）

---

### 方案2: 添加防抖机制减少更新频率

#### 实现思路

使用防抖（Debounce）机制，在短时间内只执行最后一次更新请求，大幅减少更新频率。

#### 代码修改

**在 `ExecutionTraceService` 中添加防抖机制**：

```java
@Value("${trace.cdp-update-delay-ms:500}")
private long cdpUpdateDelayMs;

// 用于防抖：记录每个CDP的最后更新时间
private final Map<String, Long> lastUpdateTime = new ConcurrentHashMap<>();
// 用于防抖：记录每个CDP的待更新任务
private final Map<String, java.util.TimerTask> pendingUpdates = new ConcurrentHashMap<>();
private final java.util.Timer updateTimer = new java.util.Timer("CDP-Update-Timer", true);

/**
 * 使用防抖机制调度CDP更新（性能优化：减少更新频率）
 * 如果距离上次更新不足delayMs，则延迟更新；否则立即更新
 */
private void scheduleCDPUpdate(String cdpId) {
    long now = System.currentTimeMillis();
    Long lastUpdate = lastUpdateTime.get(cdpId);
    
    // 取消之前的待更新任务
    java.util.TimerTask oldTask = pendingUpdates.remove(cdpId);
    if (oldTask != null) {
        oldTask.cancel();
    }
    
    // 如果距离上次更新超过延迟时间，立即更新
    if (lastUpdate == null || (now - lastUpdate) >= cdpUpdateDelayMs) {
        updateCDPTraceSummary(cdpId);
        lastUpdateTime.put(cdpId, now);
    } else {
        // 否则，延迟更新
        long delay = cdpUpdateDelayMs - (now - lastUpdate);
        java.util.TimerTask task = new java.util.TimerTask() {
            @Override
            public void run() {
                updateCDPTraceSummary(cdpId);
                lastUpdateTime.put(cdpId, System.currentTimeMillis());
                pendingUpdates.remove(cdpId);
            }
        };
        pendingUpdates.put(cdpId, task);
        updateTimer.schedule(task, delay);
    }
}
```

**修改 `recordEvent` 方法**：

```java
@Async("traceExecutor")
public void recordEvent(ExecutionTraceEvent event) {
    try {
        if (event.getCdpId() == null) {
            return;
        }

        ExecutionTrace trace = convertToEntity(event);
        traceRepository.save(trace);

        // 使用防抖机制，减少CDP更新频率（性能优化）
        scheduleCDPUpdate(event.getCdpId());

        if (eventPublisher != null) {
            eventPublisher.publish(event);
        }

    } catch (Exception e) {
        log.error("记录追踪事件失败: cdpId={}, eventType={}",
            event.getCdpId(), event.getEventType(), e);
    }
}
```

#### 工作原理

1. **防抖延迟**：默认500ms，如果500ms内有多个更新请求，只执行最后一次
2. **立即更新**：如果距离上次更新超过500ms，立即执行更新
3. **取消旧任务**：新的更新请求会取消之前的待更新任务

#### 优点

- ✅ 大幅减少更新频率（从每个事件一次 → 每500ms最多一次）
- ✅ 降低数据库压力
- ✅ 减少网络请求
- ✅ 不影响实时性（500ms延迟可接受）

#### 缺点

- ⚠️ 追踪摘要可能有500ms延迟（但可接受）

---

## 代码修改详情

### 修改文件清单

#### 1. diagnosis-service

**文件**: `diagnosis-service/src/main/java/com/aidoctor/diagnosis/repository/CDPRepository.java`

**修改内容**:
- 添加 `findByIdWithLock` 方法，使用 `@Lock(LockModeType.PESSIMISTIC_WRITE)` 和 `@Query` 注解

**文件**: `diagnosis-service/src/main/java/com/aidoctor/diagnosis/service/cdp/CDPManager.java`

**修改内容**:
- 将 `getCDPById(cdpId)` 改为 `cdpRepository.findByIdWithLock(cdpId)`
- 添加注释说明使用悲观锁的原因

#### 2. execution-trace-service

**文件**: `execution-trace-service/src/main/java/com/aidoctor/trace/service/ExecutionTraceService.java`

**修改内容**:
- 添加防抖相关字段：`cdpUpdateDelayMs`、`lastUpdateTime`、`pendingUpdates`、`updateTimer`
- 添加 `scheduleCDPUpdate` 方法实现防抖逻辑
- 修改 `recordEvent` 方法，使用 `scheduleCDPUpdate` 替代直接调用 `updateCDPTraceSummary`
- 添加 `ConcurrentHashMap` 导入

---

## 验证方法

### 1. 功能验证

#### 测试步骤

1. **启动服务**：
   ```bash
   # 启动 diagnosis-service
   # 启动 execution-trace-service
   # 启动 health-state-assessment-service
   ```

2. **执行健康筛查流程**：
   - 通过前端或API触发一次健康筛查流程
   - 观察日志，确认不再出现 `CDPNotFoundException` 错误

3. **检查日志**：
   ```bash
   # 查看 diagnosis-service 日志
   # 应该看到：
   # - "CDP更新成功" 日志
   # - 不再出现 "CDP不存在" 错误
   ```

#### 验证标准

- ✅ 不再出现 `CDPNotFoundException` 错误
- ✅ CDP更新成功日志正常
- ✅ 追踪摘要能够正常更新

### 2. 性能验证

#### 测试步骤

1. **监控更新频率**：
   - 执行一次健康筛查流程
   - 统计 `updateCDPTraceSummary` 的调用次数
   - 对比优化前后的调用次数

2. **监控数据库连接**：
   - 使用数据库监控工具（如MySQL Workbench）
   - 观察连接数和锁等待情况

#### 验证标准

- ✅ 更新频率降低：从 20+ 次/流程 → 2-3 次/流程
- ✅ 无锁等待超时
- ✅ 数据库连接数正常

### 3. 并发验证

#### 测试步骤

1. **模拟并发场景**：
   - 同时执行多个健康筛查流程
   - 观察是否出现并发冲突

2. **压力测试**：
   - 使用工具（如JMeter）模拟高并发请求
   - 观察系统稳定性

#### 验证标准

- ✅ 无并发冲突错误
- ✅ 系统稳定运行
- ✅ 响应时间正常

---

## 预期效果

### 问题解决

| 指标 | 优化前 | 优化后 | 改善 |
|------|--------|--------|------|
| CDP更新错误率 | 高（10+ 次/流程） | 0 | 100% |
| 更新频率 | 20+ 次/流程 | 2-3 次/流程 | 降低 85-90% |
| 数据库压力 | 高 | 低 | 降低 85-90% |
| 并发冲突 | 频繁 | 无 | 完全解决 |

### 性能提升

- **数据库连接**：减少不必要的连接和更新操作
- **网络请求**：减少 HTTP 请求次数
- **系统稳定性**：消除并发冲突，提升系统稳定性

### 用户体验

- **追踪功能**：追踪摘要能够正常更新，功能恢复正常
- **系统响应**：减少数据库压力，提升系统响应速度
- **错误减少**：消除大量错误日志，提升系统可靠性

---

## 相关文件

### 代码文件

1. **diagnosis-service**:
   - `src/main/java/com/aidoctor/diagnosis/repository/CDPRepository.java`
   - `src/main/java/com/aidoctor/diagnosis/service/cdp/CDPManager.java`
   - `src/main/java/com/aidoctor/diagnosis/controller/CDPController.java`

2. **execution-trace-service**:
   - `src/main/java/com/aidoctor/trace/service/ExecutionTraceService.java`

### 配置文件

1. **execution-trace-service**:
   - `src/main/resources/application.yml` (可配置 `trace.cdp-update-delay-ms`)

### 相关文档

- [AI医生系统性能优化方案.md](./AI医生系统性能优化方案.md)
- [执行追踪功能说明.md](../../execution-trace-service/README.md)

---

## 总结

### 问题根源

单个健康筛查流程会产生大量追踪事件（20+ 个），每个事件都会异步触发一次CDP更新，导致多个线程同时更新同一个CDP，产生并发冲突。

### 解决方案

采用**双重保护机制**：

1. **悲观锁**：使用 `SELECT FOR UPDATE` 确保同一时间只有一个事务能更新CDP
2. **防抖机制**：使用500ms防抖延迟，大幅减少更新频率

### 效果

- ✅ 彻底解决并发冲突问题
- ✅ 更新频率降低 85-90%
- ✅ 系统稳定性显著提升

### 注意事项

1. **防抖延迟**：默认500ms，可根据实际情况调整（通过配置 `trace.cdp-update-delay-ms`）
2. **数据库支持**：确保数据库支持行锁（MySQL/PostgreSQL都支持）
3. **监控观察**：实施后持续观察日志，确保问题完全解决

---

**文档维护**: 如发现新问题或需要调整方案，请及时更新本文档。

