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
  DiagnosisConclusion,
} from '@/types/diagnosis'

/**
 * 转换后端返回的诊断结果为前端期望的格式
 */
function convertDiagnosisResult(rawResult: any): DiagnosisResult {
  // 如果已经有 conclusion 字段，直接返回（兼容已转换的数据）
  if (rawResult.conclusion && rawResult.conclusion.type) {
    return rawResult as DiagnosisResult
  }

  const converted: any = {
    ...rawResult,
  }
  
  // 调试日志
  console.log('转换诊断结果，原始数据:', {
    hasConclusionPackage: !!rawResult.conclusionPackage,
    hasThreeLayerResult: !!rawResult.threeLayerResult,
    hasPossibilities: !!(rawResult.possibilities && rawResult.possibilities.length > 0),
    possibilitiesLength: rawResult.possibilities ? rawResult.possibilities.length : 0,
    possibilitiesSample: rawResult.possibilities ? rawResult.possibilities.slice(0, 2) : null,
    rawResultKeys: Object.keys(rawResult),
  })

  // 从 conclusionPackage 或 threeLayerResult 构建 conclusion
  let conclusion: DiagnosisConclusion | null = null

  // 优先使用 conclusionPackage
  if (rawResult.conclusionPackage) {
    const cp = rawResult.conclusionPackage
    const conclusionData = cp.conclusion || {}
    
    // 从 threeLayerResult 获取详细诊断信息
    const threeLayer = rawResult.threeLayerResult || {}
    const primaryHyp = threeLayer.primaryHypothesis || {}
    const mainAlts = threeLayer.mainAlternatives || []
    const mustExclude = threeLayer.mustExclude || {}

    conclusion = {
      type: conclusionData.type === 'CONFIRMED' || conclusionData.type === 'confirmed' ? 'confirmable' : 'unconfirmable',
      primaryHypothesis: {
        disease: primaryHyp.disease || conclusionData.diagnosis || '未知',
        confidence: primaryHyp.score || conclusionData.confidence || 0.5,
        supportingEvidence: primaryHyp.evidence ? [primaryHyp.evidence] : [],
        opposingEvidence: [],
      },
      alternativeDiagnoses: (mainAlts || []).map((alt: any) => ({
        disease: alt.disease || '未知',
        confidence: alt.score || 0.5,
        supportingEvidence: alt.evidence ? [alt.evidence] : [],
        opposingEvidence: [],
      })),
      mustExcludeDiagnosis: mustExclude.disease ? {
        disease: mustExclude.disease,
        confidence: mustExclude.score || 0.3,
        supportingEvidence: [],
        opposingEvidence: [],
      } : undefined,
    }
  } else if (rawResult.threeLayerResult) {
    // 如果没有 conclusionPackage，从 threeLayerResult 构建
    const threeLayer = rawResult.threeLayerResult
    const primaryHyp = threeLayer.primaryHypothesis || {}
    const mainAlts = threeLayer.mainAlternatives || []
    const mustExclude = threeLayer.mustExclude || {}

    conclusion = {
      type: primaryHyp.score && primaryHyp.score >= 0.8 ? 'confirmable' : 'unconfirmable',
      primaryHypothesis: {
        disease: primaryHyp.disease || '未知',
        confidence: primaryHyp.score || 0.5,
        supportingEvidence: primaryHyp.evidence ? [primaryHyp.evidence] : [],
        opposingEvidence: [],
      },
      alternativeDiagnoses: (mainAlts || []).map((alt: any) => ({
        disease: alt.disease || '未知',
        confidence: alt.score || 0.5,
        supportingEvidence: alt.evidence ? [alt.evidence] : [],
        opposingEvidence: [],
      })),
      mustExcludeDiagnosis: mustExclude.disease ? {
        disease: mustExclude.disease,
        confidence: mustExclude.score || 0.3,
        supportingEvidence: [],
        opposingEvidence: [],
      } : undefined,
    }
  } else if (rawResult.possibilities && Array.isArray(rawResult.possibilities) && rawResult.possibilities.length > 0) {
    // 降级方案：从 possibilities 构建
    const primary = rawResult.possibilities[0]
    // 确保 confidence 是数字
    const primaryConfidence = typeof primary.confidence === 'number' 
      ? primary.confidence 
      : (typeof primary.confidence === 'string' ? parseFloat(primary.confidence) || 0.5 : 0.5)
    
    conclusion = {
      type: primaryConfidence >= 0.8 ? 'confirmable' : 'unconfirmable',
      primaryHypothesis: {
        disease: primary.disease || '未知',
        confidence: primaryConfidence,
        supportingEvidence: Array.isArray(primary.supportingEvidence) ? primary.supportingEvidence : [],
        opposingEvidence: Array.isArray(primary.opposingEvidence) ? primary.opposingEvidence : [],
      },
      alternativeDiagnoses: rawResult.possibilities.slice(1, 3).map((p: any) => {
        const pConfidence = typeof p.confidence === 'number' 
          ? p.confidence 
          : (typeof p.confidence === 'string' ? parseFloat(p.confidence) || 0.5 : 0.5)
        return {
          disease: p.disease || '未知',
          confidence: pConfidence,
          supportingEvidence: Array.isArray(p.supportingEvidence) ? p.supportingEvidence : [],
          opposingEvidence: Array.isArray(p.opposingEvidence) ? p.opposingEvidence : [],
        }
      }),
    }
    
    console.log('从 possibilities 构建 conclusion:', conclusion)
  }

  // 设置 conclusion（如果所有转换都失败，使用默认值）
  if (conclusion) {
    converted.conclusion = conclusion
  } else {
    // 如果所有转换都失败，基于summary或其他信息创建一个基本的 conclusion 结构
    console.warn('⚠️ 无法从后端数据构建 conclusion，基于可用信息生成基本结论。可能原因：', {
      hasPossibilities: !!(rawResult.possibilities && rawResult.possibilities.length > 0),
      hasConclusionPackage: !!rawResult.conclusionPackage,
      hasThreeLayerResult: !!rawResult.threeLayerResult,
      summary: rawResult.summary,
    })
    
    // 尝试从summary中提取信息，或使用默认值
    let primaryDisease = '需要进一步检查'
    let confidence = 0.5
    
    if (rawResult.summary) {
      // 如果summary包含具体疾病名称，提取它
      const summaryText = rawResult.summary
      if (summaryText.includes('建议进一步检查') || summaryText.includes('明确诊断')) {
        primaryDisease = '需要进一步检查'
        confidence = 0.3
      } else {
        // 尝试从summary中提取疾病名称（简单匹配）
        primaryDisease = '待诊断'
        confidence = 0.5
      }
    }
    
    converted.conclusion = {
      type: 'unconfirmable',
      primaryHypothesis: {
        disease: primaryDisease,
        confidence: confidence,
        supportingEvidence: rawResult.summary ? [rawResult.summary] : ['症状信息不足，需要进一步检查'],
        opposingEvidence: [],
      },
      alternativeDiagnoses: [],
    }
  }

  // 转换 keyEvidence
  if (rawResult.conclusionPackage?.keyEvidence) {
    const ke = rawResult.conclusionPackage.keyEvidence
    if (Array.isArray(ke) && ke.length > 0) {
      converted.keyEvidence = {
        positiveEvidence: ke
          .filter((e: any) => e.type === 'positive' || e.evidenceType === 'positive')
          .map((e: any) => e.content || e.description || e.evidence || ''),
        negativeEvidence: ke
          .filter((e: any) => e.type === 'negative' || e.evidenceType === 'negative')
          .map((e: any) => e.content || e.description || e.evidence || ''),
      }
    } else {
      converted.keyEvidence = {
        positiveEvidence: [],
        negativeEvidence: [],
      }
    }
  } else if (!converted.keyEvidence) {
    converted.keyEvidence = {
      positiveEvidence: [],
      negativeEvidence: [],
    }
  }

  // 转换 actionAndFollowUp
  if (rawResult.conclusionPackage?.actionAndFollowUp) {
    const afu = rawResult.conclusionPackage.actionAndFollowUp
    converted.actionAndFollowUp = {
      immediateAction: afu.immediateAction || afu.action || '请遵医嘱',
      reviewTimeWindow: afu.reviewTimeWindow || afu.reviewWindow,
      upgradeTriggerConditions: afu.upgradeTriggerConditions || afu.upgradeTriggers || [],
    }
  } else if (!converted.actionAndFollowUp) {
    converted.actionAndFollowUp = {
      immediateAction: '请遵医嘱',
      upgradeTriggerConditions: [],
    }
  }

  // 转换 exclusionStatus
  if (rawResult.conclusionPackage?.mustExcludeStatus) {
    const mes = rawResult.conclusionPackage.mustExcludeStatus
    converted.exclusionStatus = {
      status: mes.status === 'EXCLUDED' ? 'excluded' : 
             mes.status === 'NOT_EXCLUDED' ? 'not_excluded' : 
             'need_offline_exclude',
      reason: mes.reason || mes.excludeReason || '',
    }
  }

  // 确保 examinationSuggestions 存在
  if (!converted.examinationSuggestions) {
    converted.examinationSuggestions = {
      priorityExaminations: rawResult.examinationSuggestion?.priorityExaminations || [],
      optionalExaminations: rawResult.examinationSuggestion?.optionalExaminations || [],
      explanation: rawResult.examinationSuggestion?.explanation,
    }
  }

  return converted as DiagnosisResult
}

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

  // 启动健康筛查流程（A路径，A1-A5）
  startWellnessScreening: async (cdpId: string): Promise<ApiResponse<DiagnosisResponse>> => {
    const response = await apiClient.post<ApiResponse<DiagnosisResponse>>(
      `/diagnosis/${cdpId}/wellness-screening/start`
    )
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
    console.log('🔍 开始获取诊断结果:', diagnosisId)
    const response = await apiClient.get<any>(`/diagnosis/${diagnosisId}/result`)
    console.log('📥 收到后端响应:', response.data)
    
    // 后端返回的结构：{code, message, data: {DiagnosisResponse包含result字段}, timestamp}
    const apiResponse = response.data as any
    
    // 提取 DiagnosisResponse（在 data 字段中）
    const diagnosisResponse = apiResponse.data || apiResponse
    console.log('📋 DiagnosisResponse:', diagnosisResponse)
    
    // 提取 result 字段（DiagnosisResult）
    const rawResult = diagnosisResponse.result
    console.log('📦 提取的原始result数据:', rawResult)
    
    if (rawResult) {
      // 转换后端数据格式为前端期望的格式
      const convertedResult = convertDiagnosisResult(rawResult)
      console.log('✅ 转换后的结果:', convertedResult)
      
      return {
        code: apiResponse.code || 200,
        message: apiResponse.message || 'success',
        data: convertedResult,
        timestamp: apiResponse.timestamp,
      }
    }
    
    // 如果没有result字段，尝试转换整个diagnosisResponse
    console.warn('⚠️ 响应中没有result字段，尝试转换整个diagnosisResponse:', diagnosisResponse)
    if (diagnosisResponse) {
      const convertedResult = convertDiagnosisResult(diagnosisResponse)
      return {
        code: apiResponse.code || 200,
        message: apiResponse.message || 'success',
        data: convertedResult,
        timestamp: apiResponse.timestamp,
      }
    }
    
    // 如果都没有，返回原始数据
    console.error('❌ 无法转换诊断结果，返回原始数据')
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


