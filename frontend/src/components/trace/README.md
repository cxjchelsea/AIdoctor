# 执行追踪可视化组件

## 组件列表

### 1. DataFlowGraph - 数据流转图

展示数据在服务间的流转路径。

**特性**：
- 节点表示服务
- 箭头表示数据流向
- 支持缩放、拖拽
- 显示执行时间和状态

### 2. ServiceCallGraph - 服务调用图

展示服务间的调用关系和统计信息。

**特性**：
- 显示服务调用次数
- 显示平均耗时
- 显示错误率
- 展示服务依赖关系

### 3. ExecutionTimeline - 执行时间线

按时间顺序展示所有追踪事件。

**特性**：
- 时间轴展示
- 事件详情
- 错误高亮
- 支持查看详细信息

### 4. ModuleCallTree - 模块调用树

展示服务内部模块的调用层次。

**特性**：
- 树形结构
- 支持展开/折叠
- 显示调用统计
- 显示错误信息

## 使用方法

```tsx
import { DataFlowGraph, ServiceCallGraph, ExecutionTimeline, ModuleCallTree } from '@/components/trace'

<DataFlowGraph traces={traces} />
<ServiceCallGraph traces={traces} />
<ExecutionTimeline traces={traces} />
<ModuleCallTree traces={traces} />
```

## 数据格式

所有组件接收 `ExecutionTrace[]` 类型的数据：

```typescript
interface ExecutionTrace {
  id: number
  cdpId: string
  eventType: string
  service?: string
  module?: string
  method?: string
  step?: string
  status?: string
  duration?: number
  timestamp: string
  // ...
}
```

