import { create } from 'zustand'
import { diagnosisApi } from '@/services/diagnosisApi'
import type {
  DiagnosisRequest,
  DiagnosisStatus,
  Question,
  DiagnosisResult,
  HealthStateAssessmentResult,
} from '@/types/diagnosis'

export interface ChatMessage {
  id: string
  type: 'user' | 'system' | 'question' | 'result' | 'assessment' | 'wellness'
  content: string
  timestamp: Date
  question?: Question
  result?: DiagnosisResult
  assessment?: HealthStateAssessmentResult
  wellnessPlan?: any
}

// 生成唯一消息ID的辅助函数
let messageIdCounter = 0
const generateMessageId = () => {
  messageIdCounter += 1
  return `msg-${Date.now()}-${messageIdCounter}-${Math.random().toString(36).substr(2, 9)}`
}

interface DiagnosisState {
  // 状态
  diagnosisId: string | null
  messages: ChatMessage[]
  completeness: number
  status: 'idle' | DiagnosisStatus
  currentQuestion: Question | null
  diagnosisResult: DiagnosisResult | null
  workMode?: 'wellness_mode' | 'clinical_mode' // 工作态（从健康状态判定获取）
  cdpId?: string // CDP ID（从健康状态判定获取）
  healthAssessmentDone: boolean // 是否已完成健康状态判定
  collectedInfo: {
    chiefComplaint?: string
    duration?: string
    severity?: number
    frequency?: string
    location?: string
    accompanyingSymptoms?: string[]
  }

  // Actions
  setWorkMode: (workMode: 'wellness_mode' | 'clinical_mode', cdpId?: string) => void
  startDiagnosis: (request: DiagnosisRequest) => Promise<void>
  sendMessage: (content: string) => Promise<void>
  answerQuestion: (answer: string) => Promise<void>
  analyze: () => Promise<void>
  resetDiagnosis: () => void
  addSystemMessage: (content: string) => void
  performHealthAssessment: (userInput: string) => Promise<void>
}

export const useDiagnosisStore = create<DiagnosisState>((set, get) => ({
  diagnosisId: null,
  messages: [],
  completeness: 0,
  status: 'idle',
  currentQuestion: null,
  diagnosisResult: null,
  workMode: undefined,
  cdpId: undefined,
  healthAssessmentDone: false,
  collectedInfo: {},

  startDiagnosis: async (request) => {
    try {
      const response = await diagnosisApi.start(request)
      const data = response.data

      // 验证必要字段
      if (!data) {
        throw new Error('诊断服务返回的数据为空')
      }

      // 如果没有 diagnosisId，生成一个临时ID或使用默认值
      const diagnosisId = data.diagnosisId || data.cdpId || `temp-${Date.now()}`
      const cdpId = data.cdpId || data.diagnosisId || diagnosisId

      // 从响应中提取已收集信息
      let collectedInfo = request.symptomInfo || {}
      
      // 如果响应中包含patientState，提取信息
      const patientState = (data as any).patientState
      
      if (patientState) {
        // 提取主诉
        if (patientState.chiefComplaint && !collectedInfo.chiefComplaint) {
          collectedInfo.chiefComplaint = patientState.chiefComplaint
        }
        
        // 提取症状信息
        const symptoms = patientState.symptoms
        if (Array.isArray(symptoms) && symptoms.length > 0) {
          const firstSymptom = symptoms[0]
          if (typeof firstSymptom === 'object') {
            collectedInfo = {
              ...collectedInfo,
              chiefComplaint: firstSymptom.name || collectedInfo.chiefComplaint,
              duration: firstSymptom.duration || collectedInfo.duration,
              severity: firstSymptom.severity ? 
                       (String(firstSymptom.severity).toLowerCase() === 'mild' ? 3 : 
                        String(firstSymptom.severity).toLowerCase() === 'moderate' ? 5 :
                        String(firstSymptom.severity).toLowerCase() === 'severe' ? 8 : 
                        typeof firstSymptom.severity === 'number' ? firstSymptom.severity : collectedInfo.severity) :
                       collectedInfo.severity,
              location: firstSymptom.location || collectedInfo.location,
              frequency: firstSymptom.frequency || collectedInfo.frequency,
            }
          }
        }
        
        // 从structuredData中提取（如果存在）
        const structuredData = patientState.structuredData
        if (structuredData && typeof structuredData === 'object') {
          const structuredSymptoms = (structuredData as any).symptoms
          if (Array.isArray(structuredSymptoms) && structuredSymptoms.length > 0) {
            const firstSymptom = structuredSymptoms[0]
            if (typeof firstSymptom === 'object') {
              collectedInfo = {
                ...collectedInfo,
                chiefComplaint: firstSymptom.name || collectedInfo.chiefComplaint,
                duration: firstSymptom.duration || collectedInfo.duration,
                severity: firstSymptom.severity ? 
                         (String(firstSymptom.severity).toLowerCase() === 'mild' ? 3 : 
                          String(firstSymptom.severity).toLowerCase() === 'moderate' ? 5 :
                          String(firstSymptom.severity).toLowerCase() === 'severe' ? 8 : 
                          typeof firstSymptom.severity === 'number' ? firstSymptom.severity : collectedInfo.severity) :
                         collectedInfo.severity,
                location: firstSymptom.location || collectedInfo.location,
                frequency: firstSymptom.frequency || collectedInfo.frequency,
              }
            }
          }
        }
      }

      set({
        diagnosisId,
        cdpId,
        status: data.status || 'collecting',
        completeness: (data.completeness || 0) / 100,
        currentQuestion: data.question || null,
        collectedInfo,
        workMode: data.workMode,
      })

      // 如果有问题，添加问题消息（支持 nextAction 和 question 两种格式）
      let questionText = null
      if (data.question) {
        questionText = data.question.question || data.question
      } else if (data.nextAction && data.nextAction.type === 'question') {
        questionText = data.nextAction.question || data.nextAction.message
      }
      
      if (questionText) {
        const questionMessage: ChatMessage = {
          id: generateMessageId(),
          type: 'question',
          content: questionText,
          timestamp: new Date(),
          question: data.question || { question: questionText },
        }
        set((state) => ({
          messages: [...state.messages, questionMessage],
        }))
      }
    } catch (error: any) {
      console.error('开始诊断失败:', error)
      // 添加错误提示消息
      const errorMessage: ChatMessage = {
        id: generateMessageId(),
        type: 'system',
        content: error?.response?.data?.message || error?.message || '开始诊断失败，请稍后重试。',
        timestamp: new Date(),
      }
      set((state) => ({
        messages: [...state.messages, errorMessage],
      }))
      throw error
    }
  },

  sendMessage: async (content: string) => {
    const state = get()
    
    // 添加用户消息
    const userMessage: ChatMessage = {
      id: generateMessageId(),
      type: 'user',
      content,
      timestamp: new Date(),
    }

    set((state) => ({
      messages: [...state.messages, userMessage],
    }))

    // 如果还没有进行健康状态判定，先进行判定（在对话中无感完成）
    if (!state.healthAssessmentDone) {
      await get().performHealthAssessment(content)
      // 判定完成后，根据工作态继续对话流程
      const newState = get()
      if (newState.workMode === 'clinical_mode') {
        // 临床诊疗态：继续诊断流程
        // 如果还没有诊断ID，开始诊断流程
        if (!newState.diagnosisId) {
          // 调用开始诊断API
          try {
            await get().startDiagnosis({
              userId: 'user-1', // TODO: 从用户上下文获取
              diagnosisType: 'symptom', // 症状诊断
              symptomInfo: {
                chiefComplaint: content,
                duration: newState.collectedInfo.duration,
                severity: newState.collectedInfo.severity,
                frequency: newState.collectedInfo.frequency,
                location: newState.collectedInfo.location,
                accompanyingSymptoms: newState.collectedInfo.accompanyingSymptoms,
              },
            })
          } catch (error) {
            console.error('启动诊断失败:', error)
            // 如果启动失败，显示提示消息
          const systemMessage: ChatMessage = {
            id: generateMessageId(),
            type: 'system',
            content: '我将为您进行详细的诊断分析。请继续描述您的症状，或回答我的问题。',
            timestamp: new Date(),
          }
          set((state) => ({
            messages: [...state.messages, systemMessage],
          }))
          }
        } else {
          // 已有诊断ID，继续回答
          await get().answerQuestion(content)
        }
      } else {
        // 健康管理态：健康筛查流程已在后端自动执行完成
        // wellnessPlan已经在performHealthAssessment中处理
        // 如果还有问题需要用户回答，会在上面的question处理中显示
        if (!newState.currentQuestion) {
          // 如果没有问题，说明健康筛查流程已完成
          const systemMessage: ChatMessage = {
            id: generateMessageId(),
            type: 'system',
            content: '健康筛查流程已完成。您可以查看健康管理计划，或开始新的诊断。',
            timestamp: new Date(),
          }
          set((state) => ({
            messages: [...state.messages, systemMessage],
          }))
        }
      }
      return
    }

    // 如果已经完成健康状态判定，根据工作态继续相应流程
    if (state.workMode === 'clinical_mode') {
      // 临床诊疗态：诊断流程
      if (!state.diagnosisId) {
        // 如果还没有诊断ID，开始诊断流程
        // TODO: 调用开始诊断API
        const systemMessage: ChatMessage = {
          id: generateMessageId(),
          type: 'system',
          content: '我将为您进行详细的诊断分析。请继续描述您的症状，或回答我的问题。',
          timestamp: new Date(),
        }
        set((state) => ({
          messages: [...state.messages, systemMessage],
        }))
      } else {
        // 已有诊断ID，发送回答
        await get().answerQuestion(content)
      }
    } else {
      // 健康管理态：健康筛查流程
      // 系统会在对话中自然引导用户完成健康筛查
      const systemMessage: ChatMessage = {
        id: generateMessageId(),
        type: 'system',
        content: '我将继续为您提供健康管理服务。请回答我的问题。',
        timestamp: new Date(),
      }
      set((state) => ({
        messages: [...state.messages, systemMessage],
      }))
    }
  },

  answerQuestion: async (answer: string) => {
    const state = get()
    if (!state.diagnosisId && !state.cdpId) return

    try {
      const response = await diagnosisApi.answer(state.diagnosisId || state.cdpId || '', {
        cdpId: state.cdpId || state.diagnosisId || '',
        questionId: state.currentQuestion?.questionId || 'default',
        answer,
        answerType: (state.currentQuestion?.missingInfoType as 'symptom' | 'sign' | 'history' | 'other') || undefined,
      })

      const data = response.data

      // 处理问题（支持 nextAction 和 question 两种格式）
      let currentQuestion = data.question || null
      if (!currentQuestion && data.nextAction && data.nextAction.type === 'question') {
        currentQuestion = {
          question: data.nextAction.question || data.nextAction.message,
          questionType: 'text',
        }
      }

      // 从响应中提取已收集信息（如果有patientState或structuredData）
      let updatedCollectedInfo = { ...state.collectedInfo }
      
      // 如果响应中包含patientState，提取信息
      const patientState = (data as any).patientState
      
      if (patientState) {
        // 提取主诉
        if (patientState.chiefComplaint && !updatedCollectedInfo.chiefComplaint) {
          updatedCollectedInfo.chiefComplaint = patientState.chiefComplaint
        }
        
        // 提取症状信息
        const symptoms = patientState.symptoms
        if (Array.isArray(symptoms) && symptoms.length > 0) {
          const firstSymptom = symptoms[0]
          if (typeof firstSymptom === 'object') {
            // 提取主诉（从症状名称）
            if (firstSymptom.name && !updatedCollectedInfo.chiefComplaint) {
              updatedCollectedInfo.chiefComplaint = firstSymptom.name
            }
            // 提取持续时间
            if (firstSymptom.duration) {
              updatedCollectedInfo.duration = firstSymptom.duration
            }
            // 提取严重程度（将字符串转换为数字）
            if (firstSymptom.severity) {
              const severityStr = String(firstSymptom.severity).toLowerCase()
              updatedCollectedInfo.severity = severityStr === 'mild' ? 3 : 
                                             severityStr === 'moderate' ? 5 :
                                             severityStr === 'severe' ? 8 : 
                                             typeof firstSymptom.severity === 'number' ? firstSymptom.severity : undefined
            }
            // 提取部位
            if (firstSymptom.location) {
              updatedCollectedInfo.location = firstSymptom.location
            }
            // 提取频率
            if (firstSymptom.frequency) {
              updatedCollectedInfo.frequency = firstSymptom.frequency
            }
          }
        }
        
        // 从structuredData中提取（如果存在）
        const structuredData = patientState.structuredData
        if (structuredData && typeof structuredData === 'object') {
          const structuredSymptoms = (structuredData as any).symptoms
          if (Array.isArray(structuredSymptoms) && structuredSymptoms.length > 0) {
            const firstSymptom = structuredSymptoms[0]
            if (typeof firstSymptom === 'object') {
              if (firstSymptom.name && !updatedCollectedInfo.chiefComplaint) {
                updatedCollectedInfo.chiefComplaint = firstSymptom.name
              }
              if (firstSymptom.duration && !updatedCollectedInfo.duration) {
                updatedCollectedInfo.duration = firstSymptom.duration
              }
              if (firstSymptom.severity && updatedCollectedInfo.severity === undefined) {
                const severityStr = String(firstSymptom.severity).toLowerCase()
                updatedCollectedInfo.severity = severityStr === 'mild' ? 3 : 
                                               severityStr === 'moderate' ? 5 :
                                               severityStr === 'severe' ? 8 : undefined
              }
              if (firstSymptom.location && !updatedCollectedInfo.location) {
                updatedCollectedInfo.location = firstSymptom.location
              }
              if (firstSymptom.frequency && !updatedCollectedInfo.frequency) {
                updatedCollectedInfo.frequency = firstSymptom.frequency
              }
            }
          }
        }
      }

      set({
        status: data.status,
        completeness: (data.completeness || 0) / 100,
        currentQuestion,
        cdpId: data.cdpId || state.cdpId,
        diagnosisId: data.diagnosisId || data.cdpId || state.diagnosisId,
        workMode: data.workMode || state.workMode,
        collectedInfo: updatedCollectedInfo,
      })

      // 如果有新问题，添加问题消息（支持 nextAction 和 question 两种格式）
      let questionText = null
      if (data.question) {
        questionText = data.question.question || data.question
      } else if (data.nextAction && data.nextAction.type === 'question') {
        questionText = data.nextAction.question || data.nextAction.message
      }
      
      if (questionText) {
        const questionMessage: ChatMessage = {
          id: generateMessageId(),
          type: 'question',
          content: questionText,
          timestamp: new Date(),
          question: data.question || { question: questionText },
        }
        set((state) => ({
          messages: [...state.messages, questionMessage],
        }))
      } else if (data.status === 'analyzing' || data.status === 'completed') {
        // 如果状态是分析中或已完成，添加系统消息
        const systemMessage: ChatMessage = {
          id: generateMessageId(),
          type: 'system',
          content: '好的，我了解了。让我为您分析一下可能的原因...',
          timestamp: new Date(),
        }
        set((state) => ({
          messages: [...state.messages, systemMessage],
        }))

        // 如果状态是分析中，自动触发分析
        if (data.status === 'analyzing') {
          await get().analyze()
        }
      }
    } catch (error) {
      console.error('回答问题失败:', error)
      throw error
    }
  },

  analyze: async () => {
    const state = get()
    if (!state.diagnosisId) return

    try {
      await diagnosisApi.analyze(state.diagnosisId)

      // 轮询检查结果（性能优化：增加间隔和最大次数限制）
      const POLLING_INTERVAL = 5000 // 5秒间隔（从2秒改为5秒）
      const MAX_POLLING_ATTEMPTS = 60 // 最多轮询60次（5分钟）
      let pollingAttempts = 0
      
      const checkResult = async () => {
        if (pollingAttempts >= MAX_POLLING_ATTEMPTS) {
          console.warn('轮询超时，停止检查')
          set({ status: 'timeout' })
          return
        }
        
        pollingAttempts++
        
        try {
          const resultResponse = await diagnosisApi.getResult(state.diagnosisId!)
          if (resultResponse.data.status === 'completed') {
            const result = resultResponse.data
            set({ status: 'completed', diagnosisResult: result })
            
            // 在对话中添加诊断结果消息
            const resultMessage: ChatMessage = {
              id: generateMessageId(),
              type: 'result',
              content: '诊断结果',
              timestamp: new Date(),
              result: result,
            }
            set((state) => ({
              messages: [...state.messages, resultMessage],
            }))
          } else {
            // 继续轮询（使用优化后的间隔）
            setTimeout(checkResult, POLLING_INTERVAL)
          }
        } catch (error) {
          console.error('获取结果失败:', error)
          // 出错后也继续轮询，但增加间隔
          setTimeout(checkResult, POLLING_INTERVAL * 2)
        }
      }

      setTimeout(checkResult, POLLING_INTERVAL)
    } catch (error) {
      console.error('分析失败:', error)
      throw error
    }
  },

  setWorkMode: (workMode: 'wellness_mode' | 'clinical_mode', cdpId?: string) => {
    set({ workMode, cdpId })
  },

  resetDiagnosis: () => {
    set({
      diagnosisId: null,
      messages: [],
      completeness: 0,
      status: 'idle',
      currentQuestion: null,
      diagnosisResult: null,
      workMode: undefined,
      cdpId: undefined,
      healthAssessmentDone: false,
      collectedInfo: {},
    })
  },

  addSystemMessage: (content: string) => {
    const systemMessage: ChatMessage = {
      id: generateMessageId(),
      type: 'system',
      content,
      timestamp: new Date(),
    }
    set((state) => ({
      messages: [...state.messages, systemMessage],
    }))
  },

  performHealthAssessment: async (userInput: string) => {
    try {
      // 调用 diagnosis-service 的 start 接口
      // 后端会自动调用健康状态判定服务，如果是健康管理态会自动执行健康筛查流程
      const response = await diagnosisApi.start({
        userId: 'user-1', // TODO: 从用户上下文获取
        diagnosisType: 'symptom', // 默认症状诊断类型
        symptomInfo: {
          chiefComplaint: userInput, // 将用户输入作为主诉
        },
        userInput, // 也传递userInput，供后端使用
        basicInfo: {},
        symptoms: [],
        vitalSigns: {},
      })

      const data = response.data

      // 设置工作态和CDP ID（无感完成）
      const diagnosisId = data.diagnosisId || data.cdpId || `temp-${Date.now()}`
      const cdpId = data.cdpId || data.diagnosisId || diagnosisId

      // 从响应中提取已收集信息
      let collectedInfo: any = {}
      
      // 如果响应中包含patientState，提取信息
      const patientState = (data as any).patientState
      
      if (patientState) {
        // 提取主诉
        if (patientState.chiefComplaint) {
          collectedInfo.chiefComplaint = patientState.chiefComplaint
        }
        
        // 提取症状信息
        const symptoms = patientState.symptoms
        if (Array.isArray(symptoms) && symptoms.length > 0) {
          const firstSymptom = symptoms[0]
          if (typeof firstSymptom === 'object') {
            collectedInfo = {
              chiefComplaint: firstSymptom.name || collectedInfo.chiefComplaint,
              duration: firstSymptom.duration,
              severity: firstSymptom.severity ? 
                       (String(firstSymptom.severity).toLowerCase() === 'mild' ? 3 : 
                        String(firstSymptom.severity).toLowerCase() === 'moderate' ? 5 :
                        String(firstSymptom.severity).toLowerCase() === 'severe' ? 8 : undefined) :
                       undefined,
              location: firstSymptom.location,
              frequency: firstSymptom.frequency,
            }
          }
        }
        
        // 从structuredData中提取（如果存在）
        const structuredData = patientState.structuredData
        if (structuredData && typeof structuredData === 'object') {
          const structuredSymptoms = (structuredData as any).symptoms
          if (Array.isArray(structuredSymptoms) && structuredSymptoms.length > 0) {
            const firstSymptom = structuredSymptoms[0]
            if (typeof firstSymptom === 'object') {
              collectedInfo = {
                chiefComplaint: firstSymptom.name || collectedInfo.chiefComplaint,
                duration: firstSymptom.duration || collectedInfo.duration,
                severity: firstSymptom.severity ? 
                         (String(firstSymptom.severity).toLowerCase() === 'mild' ? 3 : 
                          String(firstSymptom.severity).toLowerCase() === 'moderate' ? 5 :
                          String(firstSymptom.severity).toLowerCase() === 'severe' ? 8 : undefined) :
                         collectedInfo.severity,
                location: firstSymptom.location || collectedInfo.location,
                frequency: firstSymptom.frequency || collectedInfo.frequency,
              }
            }
          }
        }
      }

      set({
        diagnosisId,
        cdpId,
        workMode: data.workMode,
        status: data.status || 'collecting',
        completeness: (data.completeness || 0) / 100,
        healthAssessmentDone: true,
        collectedInfo,
      })

      // 构建健康状态判定结果用于展示（从诊断响应中提取）
      const assessmentResult: HealthStateAssessmentResult = {
        needsClinicalMode: data.workMode === 'clinical_mode',
        workMode: data.workMode || 'clinical_mode',
        riskLevel: data.riskLevel || 'L4', // 从后端返回
        assessmentReason: data.assessmentReason || (data.workMode === 'wellness_mode' 
          ? '症状在正常范围，建议健康管理' 
          : '建议进入临床诊疗态'), // 从后端返回，如果没有则使用默认值
        redFlags: data.redFlags || [], // 从后端返回
        cdpId: cdpId,
        wellnessPlan: data.wellnessPlan,
        entryAssessment: data.entryAssessment, // 从后端返回入口判定结果
      }

      // 将健康状态判定结果（包含入口判定结果）保存为消息，用于展示
      const assessmentMessage: ChatMessage = {
        id: generateMessageId(),
        type: 'assessment',
        content: '健康状态评估完成',
        timestamp: new Date(),
        assessment: assessmentResult,
      }
      set((state) => ({
        messages: [...state.messages, assessmentMessage],
      }))

      // 如果是健康管理态，显示健康管理计划
      if (data.workMode === 'wellness_mode' && data.wellnessPlan) {
        const wellnessMessage: ChatMessage = {
          id: generateMessageId(),
          type: 'system',
          content: '健康筛查流程已完成，已为您生成个性化健康管理计划。',
          timestamp: new Date(),
        }
        set((state) => ({
          messages: [...state.messages, wellnessMessage],
        }))
      }

      // 如果有问题，添加问题消息（支持 nextAction 和 question 两种格式）
      let questionText = null
      if (data.question) {
        questionText = data.question.question || data.question
      } else if (data.nextAction && data.nextAction.type === 'question') {
        questionText = data.nextAction.question || data.nextAction.message
      }
      
      if (questionText) {
        const questionMessage: ChatMessage = {
          id: generateMessageId(),
          type: 'question',
          content: questionText,
          timestamp: new Date(),
          question: data.question || { question: questionText },
        }
        set((state) => ({
          messages: [...state.messages, questionMessage],
        }))
        set({ currentQuestion: data.question || { question: questionText } })
      }

      // 根据工作态，系统会在后续对话中自然引导用户
      // 不显示明显的"评估完成"消息，让流程无感切换
    } catch (error: any) {
      console.error('健康状态判定失败:', error)
      
      // 判定失败，默认进入临床诊疗态（无感）
      set({
        workMode: 'clinical_mode',
        healthAssessmentDone: true,
      })

      // 不显示错误消息，让流程自然继续
    }
  },
}))

