import apiClient from '@/utils/apiClient'
import type {
  DiagnosisRequest,
  DiagnosisResponse,
  UserAnswer,
  DiagnosisResult,
  DiagnosisRecord,
  ApiResponse,
  PaginatedResponse,
  HealthStateAssessmentResult,
} from '@/types/diagnosis'

export const diagnosisApi = {
  // 健康状态判定（脑区0）
  assessHealthState: async (request: {
    userId: string
    userInput?: string
    basicInfo?: {
      age?: number
      gender?: string
      bmi?: number
    }
    symptoms?: string[]
    vitalSigns?: {
      bp?: { systolic: number; diastolic: number }
      heartRate?: number
    }
  }): Promise<ApiResponse<HealthStateAssessmentResult>> => {
    const response = await apiClient.post<ApiResponse<HealthStateAssessmentResult>>(
      '/health-state-assessment/assess',
      request
    )
    return response.data
  },

  // 开始诊断
  start: async (request: DiagnosisRequest): Promise<ApiResponse<DiagnosisResponse>> => {
    const response = await apiClient.post<ApiResponse<DiagnosisResponse>>('/diagnosis/start', request)
    return response.data
  },

  // 回答追问
  answer: async (
    diagnosisId: string,
    answer: UserAnswer
  ): Promise<ApiResponse<DiagnosisResponse>> => {
    const response = await apiClient.post<ApiResponse<DiagnosisResponse>>(
      `/diagnosis/${diagnosisId}/answer`,
      answer
    )
    return response.data
  },

  // 获取诊断结果
  getResult: async (diagnosisId: string): Promise<ApiResponse<DiagnosisResult>> => {
    const response = await apiClient.get<ApiResponse<DiagnosisResult>>(`/diagnosis/${diagnosisId}`)
    return response.data
  },

  // 获取诊断历史
  getHistory: async (
    userId: string,
    page: number = 1,
    pageSize: number = 20
  ): Promise<ApiResponse<PaginatedResponse<DiagnosisRecord>>> => {
    const response = await apiClient.get<ApiResponse<PaginatedResponse<DiagnosisRecord>>>(
      '/diagnosis/history',
      {
        params: { userId, page, pageSize },
      }
    )
    return response.data
  },

  // 执行诊断分析
  analyze: async (diagnosisId: string): Promise<ApiResponse<void>> => {
    const response = await apiClient.post<ApiResponse<void>>(`/diagnosis/${diagnosisId}/analyze`)
    return response.data
  },
}


