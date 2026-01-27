import type { ExecutionTrace } from '@/types/trace'

/**
 * 过滤掉失败的服务调用
 * 1. status === 'ERROR' 的追踪记录
 * 2. 只有 START 事件（没有 duration 且 status === 'IN_PROGRESS'）且没有对应成功 END 事件的追踪记录
 * 3. 如果对应的 END 事件是 ERROR，则 START 事件也应该被过滤掉
 */
export function filterSuccessfulTraces(traces: ExecutionTrace[]): ExecutionTrace[] {
  return traces.filter(trace => {
    // 过滤掉错误状态的追踪记录
    if (trace.status === 'ERROR') {
      return false
    }
    
    // 如果只有 START 事件（没有 duration），检查是否有对应的成功 END 事件
    if (trace.status === 'IN_PROGRESS' && !trace.duration) {
      // 检查是否有对应的成功 END 事件（通过 traceId 和 method 匹配）
      // 必须确保 END 事件不是 ERROR 状态
      const hasSuccessfulEndEvent = traces.some(t => 
        t.traceId === trace.traceId && 
        t.method === trace.method && 
        t.service === trace.service &&
        t.status !== 'IN_PROGRESS' &&
        t.status !== 'ERROR' &&
        t.duration != null
      )
      return hasSuccessfulEndEvent
    }
    
    // 对于有 duration 的事件（END 事件），如果 status 不是 SUCCESS，也过滤掉
    if (trace.duration && trace.status !== 'SUCCESS') {
      return false
    }
    
    return true
  })
}

