import React, { useState } from 'react'
import {
  WellnessScreeningProgress,
  A1DemandClassification,
  A2HealthProfileCollection,
  A3BranchExecution,
  A4UnifiedResult,
  A5FollowUpSetup,
} from './index'
import type { WellnessScreeningStage } from '@/types/diagnosis'

interface WellnessScreeningFlowProps {
  onComplete: () => void
}

const WellnessScreeningFlow: React.FC<WellnessScreeningFlowProps> = ({ onComplete: _onComplete }) => {
  const [currentStage, setCurrentStage] = useState<WellnessScreeningStage>('A1_DEMAND_CLASSIFICATION')
  const [demandType, setDemandType] = useState<{
    type: 1 | 2 | 3 | 4
    typeName: string
    confidence?: number
  }>()
  const [healthProfile, setHealthProfile] = useState<{
    completeness: number
    basicInfo?: {
      age?: number
      gender?: string
      bmi?: number
    }
  }>()
  const [branchResult, setBranchResult] = useState<{
    demandType: 1 | 2 | 3 | 4
    riskLevel?: string
    recommendations?: Array<{
      name: string
      description: string
      priority: string
      reason: string
    }>
  }>()
  const [unifiedResult, setUnifiedResult] = useState<{
    summary: string
    recommendations: string[]
    nextSteps: string[]
  }>()
  const [followUpPlan, setFollowUpPlan] = useState<{
    followUpDate: string
    reminderContent: string
  }>()

  const handleA1Next = () => {
    // TODO: 调用API进行A1需求分类
    // 这里模拟API调用
    const mockDemandType = {
      type: 1 as const,
      typeName: '筛查建议',
      confidence: 0.9,
    }
    setDemandType(mockDemandType)
    setCurrentStage('A2_HEALTH_PROFILE_COLLECTED')
  }

  const handleA2Next = () => {
    // TODO: 调用API进行A2收集健康画像
    // 这里模拟API调用
    const mockHealthProfile = {
      completeness: 0.8,
      basicInfo: {
        age: 30,
        gender: '男',
        bmi: 22.5,
      },
    }
    setHealthProfile(mockHealthProfile)
    setCurrentStage('A3_BRANCH_EXECUTED')
  }

  const handleA3Next = () => {
    // TODO: 调用API进行A3执行分支
    // 这里模拟API调用
    const mockBranchResult = {
      demandType: demandType?.type || 1,
      riskLevel: 'L3',
      recommendations: [
        {
          name: '血压检查',
          description: '建议每年检查一次血压',
          priority: '高',
          reason: '年龄和生活方式因素',
        },
      ],
    }
    setBranchResult(mockBranchResult)
    setCurrentStage('A4_UNIFIED_RESULT_GENERATED')
  }

  const handleA4Next = () => {
    // TODO: 调用API进行A4生成统一结果
    // 这里模拟API调用
    const mockUnifiedResult = {
      summary: '根据您的健康画像，建议进行定期健康筛查',
      recommendations: ['定期体检', '保持健康生活方式'],
      nextSteps: ['3个月后复查', '如有异常及时就医'],
    }
    setUnifiedResult(mockUnifiedResult)
    setCurrentStage('A5_FOLLOW_UP_SETUP')
  }

  // handleA5Complete 尚未接线到 A5 UI，不在本次类型基线修复中实现或连接

  return (
    <div>
      <WellnessScreeningProgress currentStage={currentStage} />

      {currentStage === 'A1_DEMAND_CLASSIFICATION' && demandType && (
        <A1DemandClassification demandType={demandType} onNext={handleA1Next} />
      )}

      {currentStage === 'A2_HEALTH_PROFILE_COLLECTED' && healthProfile && (
        <A2HealthProfileCollection
          healthProfile={healthProfile}
          isComplete={healthProfile.completeness >= 0.6}
          missingRequiredFields={[]}
          followUpQuestions={[]}
          onAnswer={() => {}}
          onNext={handleA2Next}
        />
      )}

      {currentStage === 'A3_BRANCH_EXECUTED' && branchResult && (
        <A3BranchExecution branchResult={branchResult} onNext={handleA3Next} />
      )}

      {currentStage === 'A4_UNIFIED_RESULT_GENERATED' && unifiedResult && (
        <A4UnifiedResult unifiedResult={unifiedResult} onNext={handleA4Next} />
      )}

      {currentStage === 'A5_FOLLOW_UP_SETUP' && followUpPlan && (
        <A5FollowUpSetup
          followUpPlan={followUpPlan}
          onViewRecord={() => {}}
          onNewScreening={() => {
            // 重置流程
            setCurrentStage('A1_DEMAND_CLASSIFICATION')
            setDemandType(undefined)
            setHealthProfile(undefined)
            setBranchResult(undefined)
            setUnifiedResult(undefined)
            setFollowUpPlan(undefined)
          }}
        />
      )}
    </div>
  )
}

export default WellnessScreeningFlow

