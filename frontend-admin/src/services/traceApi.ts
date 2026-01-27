import axios from 'axios'
import type { ApiResponse, ExecutionTrace, TraceSummary } from '@/types/trace'

// 追踪服务API客户端（独立服务，端口8093）
const traceApiClient = axios.create({
  baseURL: '/api/v1/trace',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

export const traceApi = {
  /**
   * 查询CDP的执行追踪记录
   */
  getTracesByCdpId: async (cdpId: string): Promise<ExecutionTrace[]> => {
    const response = await traceApiClient.get<ExecutionTrace[]>(
      `/cdp/${cdpId}`
    )
    return response.data || []
  },

  /**
   * 获取CDP的执行追踪摘要
   */
  getTraceSummary: async (cdpId: string): Promise<TraceSummary> => {
    const response = await traceApiClient.get<TraceSummary>(
      `/cdp/${cdpId}/summary`
    )
    return response.data || {
      steps: [],
      serviceCalls: {},
      totalDuration: 0,
      errorCount: 0
    }
  },

  /**
   * 获取所有有追踪记录的 CDP ID 列表
   */
  getAllCdpIds: async (): Promise<Array<{ cdpId: string; latestTimestamp?: number; traceCount: number }>> => {
    const response = await traceApiClient.get<Array<{ cdpId: string; latestTimestamp?: number; traceCount: number }>>(
      '/cdps'
    )
    return response.data || []
  },
}

