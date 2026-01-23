/**
 * 执行追踪相关类型定义
 */

export interface ExecutionTraceEvent {
  cdpId: string
  traceId?: string
  type: string
  service?: string
  module?: string
  method?: string
  step?: string
  status?: string
  duration?: number
  timestamp: number
  input?: any
  output?: any
  errorMessage?: string
  url?: string
  attributes?: Record<string, any>
}

export interface ExecutionTrace {
  id: number
  cdpId: string
  traceId?: string
  eventType: string
  service?: string
  module?: string
  method?: string
  step?: string
  status?: string
  duration?: number
  timestamp: string
  inputData?: string
  outputData?: string
  errorMessage?: string
  requestUrl?: string
  createdAt: string
}

export interface TraceSummary {
  steps: Array<{
    step: string
    startTime: string
    services: string[]
  }>
  serviceCalls: Record<string, number>
  totalDuration: number
  errorCount: number
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp?: number
}

