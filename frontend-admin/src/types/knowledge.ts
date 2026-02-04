/**
 * 知识图谱结构管理相关类型定义
 */

export interface SchemaConstraint {
  name: string
  type: string
  entityType: string
  property: string
  description?: string
}

export interface SchemaIndex {
  name: string
  type: string
  entityType: string
  property: string
  description?: string
}

export interface SchemaSetupResult {
  constraints_created: string[]
  constraints_failed: string[]
  indexes_created: string[]
  indexes_failed: string[]
  errors: string[]
  validation?: SchemaValidationResult
}

export interface SchemaValidationResult {
  constraints_count: number
  indexes_count: number
  missing_constraints: string[]
  missing_indexes: string[]
  is_valid: boolean
  error?: string
}

export interface ConstraintDetail {
  name: string
  type: string
  entityType: string
  properties: string[]
  description?: string
}

export interface IndexDetail {
  name: string
  type: string
  entityType: string
  properties: string[]
  state: string
  description?: string
}

export interface ApiResponse<T> {
  status: string
  message?: string
  result?: T
  data?: T
}

