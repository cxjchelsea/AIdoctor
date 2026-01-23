import apiClient from '@/utils/apiClient'
import type { ApiResponse } from '@/types/diagnosis'
import type {
  ClinicalParsingRequest,
  ClinicalParsingResponse,
} from '@/types/clinicalParsing'

export const clinicalParsingApi = {
  /**
   * 病例理解与结构化
   * 将非结构化的患者信息转换为结构化的临床要素
   */
  parse: async (
    request: ClinicalParsingRequest
  ): Promise<ApiResponse<ClinicalParsingResponse>> => {
    const response = await apiClient.post<ApiResponse<ClinicalParsingResponse>>(
      '/parsing/parse',
      request
    )
    return response.data
  },

  /**
   * 健康检查
   */
  health: async (): Promise<ApiResponse<{ status: string; service: string }>> => {
    const response = await apiClient.get<ApiResponse<{ status: string; service: string }>>(
      '/parsing/health'
    )
    return response.data
  },
}

