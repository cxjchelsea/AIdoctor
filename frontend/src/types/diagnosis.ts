// 诊断相关类型定义

export type DiagnosisType = 'symptom' | 'examination' | 'comprehensive'
export type DiagnosisStatus = 'collecting' | 'questioning' | 'analyzing' | 'completed' | 'cancelled'
export type AnswerType = 'symptom' | 'sign' | 'history' | 'other'
export type WorkMode = 'wellness_mode' | 'clinical_mode'
export type RiskLevel = 'L1' | 'L2' | 'L3' | 'L4'

export interface SymptomInfo {
  chiefComplaint?: string
  duration?: string
  severity?: number
  frequency?: string
  location?: string
  accompanyingSymptoms?: string[]
  features?: {
    trigger?: string
    relief?: string
  }
}

export interface DiagnosisRequest {
  userId: string
  diagnosisType?: DiagnosisType
  symptomInfo?: SymptomInfo
  examinationRecordId?: string
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
}

export interface Question {
  question: string
  questionType: string
  missingInfoType: string
  required: boolean
  options?: string[]
  priority?: 'required' | 'important' | 'optional' // 优先级（基于信息缺口分级）
}

export interface DiagnosisResponse {
  diagnosisId: string
  cdpId?: string
  status: DiagnosisStatus
  completeness?: number
  question?: Question
  workMode?: WorkMode
  currentStep?: string
  nextAction?: {
    type: string
    question?: string
    message?: string
  }
  wellnessPlan?: {
    riskManagement?: string
    lifestyleAdvice?: string
    followUpPlan?: string
    demand_type?: string
    profile?: any
    branch_result?: any
    unified_result?: any
    followup_plan?: any
    next_review_date?: string
    summary?: string
    [key: string]: any
  }
}

export interface UserAnswer {
  answer: string
  answerType?: AnswerType
}

export interface DiseasePossibility {
  disease: string
  confidence: number
  supportingEvidence: string[]
  opposingEvidence: string[]
  missingInfo?: string[]
  inclusionBasis?: string[] // 入选依据（问题清单中的线索）
}

export interface ExaminationItem {
  name: string
  purpose: string
  priority: 'high' | 'medium' | 'low'
  reason: string
}

export interface MedicalAdvice {
  department: string
  timing: string
  preparation: {
    documents: string[]
    questions: string[]
  }
  sbarSummary?: string
}

// 诊断结论结构（三层分层）
export interface DiagnosisConclusion {
  type: 'confirmable' | 'unconfirmable' // 可确证/不可确证
  primaryHypothesis: DiseasePossibility // 首要假设（1个）
  alternativeDiagnoses: DiseasePossibility[] // 主要备选诊断（1-2个）
  mustExcludeDiagnosis?: DiseasePossibility // 必须排除的高危诊断（0-1个）
}

// 必须排除项状态
export interface ExclusionStatus {
  status: 'excluded' | 'not_excluded' | 'need_offline_exclude'
  reason: string // 排除理由
}

// 关键依据
export interface KeyEvidence {
  positiveEvidence: string[] // 阳性证据（支持最可能方向，至少三条）
  negativeEvidence: string[] // 关键阴性证据（排除其他方向）
}

// 行动与随访
export interface ActionAndFollowUp {
  immediateAction: string // 立即行动
  reviewTimeWindow?: string // 复评时间窗（例如："3天后"）
  upgradeTriggerConditions: string[] // 升级触发条件（例如：症状加重、出现新症状）
}

export interface DiagnosisResult {
  diagnosisId: string
  status: DiagnosisStatus
  conclusion: DiagnosisConclusion // 结论（三层分层）
  exclusionStatus?: ExclusionStatus // 必须排除项状态
  keyEvidence: KeyEvidence // 关键依据
  actionAndFollowUp: ActionAndFollowUp // 行动与随访
  examinationSuggestions: {
    priorityExaminations: ExaminationItem[]
    optionalExaminations: ExaminationItem[]
    explanation?: string
  }
  medicalAdvice: MedicalAdvice
  createdAt: string
  completedAt?: string
}

export interface DiagnosisRecord {
  diagnosisId: string
  userId: string
  diagnosisType: DiagnosisType
  status: DiagnosisStatus
  chiefComplaint?: string
  completeness?: number
  createdAt: string
  completedAt?: string
  workMode?: WorkMode // 工作态
  riskLevel?: RiskLevel // 风险等级
  isPinned?: boolean // 是否置顶
  isArchived?: boolean // 是否归档
  tags?: string[] // 标签
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
  errors?: Array<{
    field: string
    message: string
  }>
}

export interface PaginatedResponse<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

// 健康状态判定结果（tool_0）
export interface HealthStateAssessmentResult {
  needsClinicalMode: boolean
  workMode: WorkMode
  riskLevel: RiskLevel
  assessmentReason: string
  redFlags: string[]
  wellnessPlan?: {
    riskManagement: string
    lifestyleAdvice: string
    followUpPlan: string
  }
  cdpId: string
  entryAssessment?: {
    userInput: string
    hasSymptom: boolean
    symptomStatus: 'no_symptom' | 'has_symptom' | 'uncertain'
    clarificationNeeded: boolean
    clarificationResult?: 'A' | 'B'
    redFlagsHit: boolean
    redFlagsList: string[]
    pathSelected: 'A' | 'B' | 'exit'
  }
}

// 入口判定流程（P0模块）相关类型
export interface EntryAssessmentState {
  currentStep: 1 | 2 | 3 | 4 | 5
  userInput: string
  symptomStatus?: {
    status: 'no_symptom' | 'has_symptom' | 'uncertain'
    symptoms?: string[]
  }
  clarificationQuestion?: string
  redFlagsCheck?: {
    redFlagsHit: boolean
    redFlags: Array<{ description: string }>
    safetyMessage?: string
  }
  pathResult?: {
    path: 'A' | 'B' | 'exit'
    pathName: string
    nextStep: string
    message?: string
  }
}

// 健康筛查流程（A路径）相关类型
export type WellnessScreeningStage =
  | 'A1_DEMAND_CLASSIFICATION'
  | 'A2_HEALTH_PROFILE_COLLECTED'
  | 'A3_BRANCH_EXECUTED'
  | 'A4_UNIFIED_RESULT_GENERATED'
  | 'A5_FOLLOW_UP_SETUP'

export interface WellnessScreeningState {
  currentStage: WellnessScreeningStage
  demandType?: {
    type: 1 | 2 | 3 | 4
    typeName: string
    confidence?: number
  }
  healthProfile?: {
    completeness: number
    basicInfo?: {
      age?: number
      gender?: string
      bmi?: number
    }
  }
  branchResult?: {
    demandType: 1 | 2 | 3 | 4
    riskLevel?: string
    recommendations?: Array<{
      name: string
      description: string
      priority: string
      reason: string
    }>
  }
  unifiedResult?: {
    summary: string
    recommendations: string[]
    nextSteps: string[]
  }
  followUpPlan?: {
    followUpDate: string
    reminderContent: string
  }
}

