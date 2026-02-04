import axios from 'axios'
import type { SchemaSetupResult, SchemaValidationResult, ApiResponse, ConstraintDetail, IndexDetail } from '@/types/knowledge'

// 知识库管理服务API客户端（端口8094）
const knowledgeApiClient = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

export const knowledgeApi = {
  /**
   * 搭建知识图谱结构（创建约束和索引）
   */
  setupSchema: async (): Promise<SchemaSetupResult> => {
    const response = await knowledgeApiClient.post<ApiResponse<SchemaSetupResult>>(
      '/schema/setup'
    )
    return response.data.result || response.data.data || {
      constraints_created: [],
      constraints_failed: [],
      indexes_created: [],
      indexes_failed: [],
      errors: [],
    }
  },

  /**
   * 验证知识图谱结构
   */
  validateSchema: async (): Promise<SchemaValidationResult> => {
    const response = await knowledgeApiClient.get<ApiResponse<SchemaValidationResult>>(
      '/schema/validate'
    )
    return response.data.result || response.data.data || {
      constraints_count: 0,
      indexes_count: 0,
      missing_constraints: [],
      missing_indexes: [],
      is_valid: false,
    }
  },

  /**
   * 获取所有约束的详细信息
   */
  getAllConstraints: async (): Promise<ConstraintDetail[]> => {
    const response = await knowledgeApiClient.get<ApiResponse<ConstraintDetail[]>>(
      '/schema/constraints'
    )
    return response.data.result || response.data.data || []
  },

  /**
   * 获取所有索引的详细信息
   */
  getAllIndexes: async (): Promise<IndexDetail[]> => {
    const response = await knowledgeApiClient.get<ApiResponse<IndexDetail[]>>(
      '/schema/indexes'
    )
    return response.data.result || response.data.data || []
  },
}

