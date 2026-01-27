import type { ExecutionTrace } from '@/types/trace'
import { filterSuccessfulTraces } from './traceFilter'

/**
 * 从traces计算统计数据
 */
export interface TraceStatistics {
  totalDuration: number
  serviceCalls: Record<string, number>
  serviceCount: number
  totalServiceCalls: number
  errorCount: number
  steps: Array<{
    step: string
    startTime: number
    services: string[]
  }>
}

/**
 * 计算追踪统计数据
 * 包含 SERVICE_CALL 和 FEIGN_CALL 事件
 */
export function calculateTraceStatistics(traces: ExecutionTrace[]): TraceStatistics {
  if (traces.length === 0) {
    return {
      totalDuration: 0,
      serviceCalls: {},
      serviceCount: 0,
      totalServiceCalls: 0,
      errorCount: 0,
      steps: [],
    }
  }

  // 统计服务调用（包含 SERVICE_CALL 和 FEIGN_CALL）
  const serviceCallCounts = new Map<string, number>()
  const successfulTraces = filterSuccessfulTraces(traces)
  
  successfulTraces.forEach(trace => {
    if (trace.service && 
        trace.eventType && 
        (trace.eventType.startsWith('SERVICE_CALL') || 
         trace.eventType.startsWith('FEIGN_CALL'))) {
      const currentCount = serviceCallCounts.get(trace.service) || 0
      serviceCallCounts.set(trace.service, currentCount + 1)
    }
  })

  // 计算总执行时间（从第一个事件到最后一个事件的时间差）
  const timestamps = traces.map(t => new Date(t.timestamp).getTime())
  const totalDuration = timestamps.length > 0 
    ? Math.max(...timestamps) - Math.min(...timestamps)
    : 0

  // 统计错误数量
  const errorCount = traces.filter(t => t.status === 'ERROR').length

  // 统计步骤（如果有）
  const stepMap = new Map<string, { startTime: number; services: Set<string> }>()
  traces.forEach(trace => {
    if (trace.step) {
      if (!stepMap.has(trace.step)) {
        stepMap.set(trace.step, { 
          startTime: new Date(trace.timestamp).getTime(), 
          services: new Set() 
        })
      }
      if (trace.service) {
        stepMap.get(trace.step)!.services.add(trace.service)
      }
      // 更新最早的时间戳
      const stepTime = new Date(trace.timestamp).getTime()
      if (stepTime < stepMap.get(trace.step)!.startTime) {
        stepMap.get(trace.step)!.startTime = stepTime
      }
    }
  })

  const serviceCalls = Object.fromEntries(serviceCallCounts)
  const totalServiceCalls = Array.from(serviceCallCounts.values()).reduce((sum, count) => sum + count, 0)

  return {
    totalDuration,
    serviceCalls,
    serviceCount: serviceCallCounts.size,
    totalServiceCalls,
    errorCount,
    steps: Array.from(stepMap.entries())
      .map(([step, data]) => ({
        step,
        startTime: data.startTime,
        services: Array.from(data.services)
      }))
      .sort((a, b) => a.startTime - b.startTime) // 按时间排序
  }
}

