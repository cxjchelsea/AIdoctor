import React from 'react'
import { Steps } from 'antd'

interface WellnessScreeningProgressProps {
  currentStage: string
}

const WellnessScreeningProgress: React.FC<WellnessScreeningProgressProps> = ({
  currentStage,
}) => {
  const steps = [
    { title: '需求分类', stage: 'A1_DEMAND_CLASSIFICATION' },
    { title: '收集健康画像', stage: 'A2_HEALTH_PROFILE_COLLECTED' },
    { title: '执行分支', stage: 'A3_BRANCH_EXECUTED' },
    { title: '生成统一结果', stage: 'A4_UNIFIED_RESULT_GENERATED' },
    { title: '设置随访', stage: 'A5_FOLLOW_UP_SETUP' },
  ]

  const currentIndex = steps.findIndex((s) => s.stage === currentStage)

  return (
    <Steps current={currentIndex} style={{ marginBottom: 24 }}>
      {steps.map((step) => (
        <Steps.Step key={step.stage} title={step.title} />
      ))}
    </Steps>
  )
}

export default WellnessScreeningProgress

