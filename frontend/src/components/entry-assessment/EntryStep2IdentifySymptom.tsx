import React from 'react'
import { Card, Space, Button, Alert } from 'antd'

interface SymptomStatus {
  status: 'no_symptom' | 'has_symptom' | 'uncertain'
  symptoms?: string[]
}

interface EntryStep2IdentifySymptomProps {
  symptomStatus: SymptomStatus
  onNext: () => void
}

const EntryStep2IdentifySymptom: React.FC<EntryStep2IdentifySymptomProps> = ({
  symptomStatus,
  onNext,
}) => {
  return (
    <Card title="识别您的情况" style={{ marginBottom: 16 }}>
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        {/* 识别结果 */}
        {symptomStatus.status === 'no_symptom' && (
          <Alert
            message="未识别到症状/困扰"
            description="您似乎是想进行健康管理或体检规划"
            type="info"
            showIcon
          />
        )}

        {symptomStatus.status === 'has_symptom' && (
          <Alert
            message="已识别到症状/困扰"
            description={`识别到的症状：${symptomStatus.symptoms?.join('、') || ''}`}
            type="warning"
            showIcon
          />
        )}

        {symptomStatus.status === 'uncertain' && (
          <Alert
            message="需要进一步确认"
            description="您的描述不够明确，需要进一步确认您的需求"
            type="info"
            showIcon
          />
        )}

        {/* 下一步按钮 */}
        <Button type="primary" onClick={onNext} block>
          继续
        </Button>
      </Space>
    </Card>
  )
}

export default EntryStep2IdentifySymptom

