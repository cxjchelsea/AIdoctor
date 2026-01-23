// 病例理解服务相关类型定义

export interface ClinicalParsingRequest {
  userId: string
  sessionId: string
  text: string
  cdpId?: string
  input?: {
    modality?: 'text' | 'image' | 'mixed'
    images?: string[]
    vitalSigns?: Record<string, any>
    medicalHistory?: Record<string, any>
  }
}

export interface NormalizedConcept {
  originalText: string
  normalizedSymptom: string
  cui?: string
  icd?: string
  snomed?: string
  loinc?: string
  atc?: string
  confidence: number
  conceptType: 'symptom' | 'disease' | 'medication' | 'examination' | 'allergy' | 'indicator'
}

export interface Symptom {
  name: string
  cui?: string
  duration?: string
  severity?: string
  trigger?: string
}

export interface Sign {
  name: string
  value?: string
  unit?: string
}

export interface Examination {
  name: string
  loinc?: string
  result?: string
  abnormal: boolean
}

export interface MedicalHistory {
  disease: string
  icd?: string
  status: 'past' | 'ongoing'
}

export interface Medication {
  name: string
  atc?: string
  status: 'current' | 'past'
}

export interface Allergy {
  allergen: string
  reaction?: string
}

export interface StructuredData {
  symptoms: Symptom[]
  signs: Sign[]
  examinations: Examination[]
  medicalHistory: MedicalHistory[]
  medications: Medication[]
  allergies: Allergy[]
}

export interface AmbiguousExpression {
  text: string
  type: string
  suggestedQuestions: string[]
}

export interface ClinicalParsingResponse {
  concepts: NormalizedConcept[]
  structuredData: StructuredData
  ambiguousExpressions?: AmbiguousExpression[]
}

