import apiClient from '@/utils/apiClient'
import type { ApiResponse } from '@/types/diagnosis'

export interface InformationGapItem {
  field: string
  description: string
  reason: string
}

export interface InformationGaps {
  required: InformationGapItem[]
  important: InformationGapItem[]
  optional: InformationGapItem[]
}

export interface FieldConfigResponse {
  fieldConfig: InformationGaps
}

export const dialogApi = {
  // 获取字段配置
  getFieldConfig: async (): Promise<ApiResponse<FieldConfigResponse>> => {
    const response = await apiClient.get<ApiResponse<FieldConfigResponse>>(
      '/dialog/field-config'
    )
    return response.data
  },
}


