import React from 'react'
import { Card, Space, Button, Alert } from 'antd'

interface EntryStep3ClarificationProps {
  clarificationQuestion: string
  onClarification: (direction: 'A' | 'B') => void
}

const EntryStep3Clarification: React.FC<EntryStep3ClarificationProps> = ({
  clarificationQuestion,
  onClarification,
}) => {
  return (
    <Card title="需要确认一下" style={{ marginBottom: 16 }}>
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        {/* 澄清问题 */}
        <Alert message={clarificationQuestion} type="info" showIcon />

        {/* 选择按钮 */}
        <Space style={{ width: '100%', justifyContent: 'center' }}>
          <Button
            size="large"
            onClick={() => onClarification('A')}
            style={{ width: 200 }}
          >
            健康筛查/体检规划
          </Button>
          <Button
            size="large"
            type="primary"
            onClick={() => onClarification('B')}
            style={{ width: 200 }}
          >
            症状诊断
          </Button>
        </Space>
      </Space>
    </Card>
  )
}

export default EntryStep3Clarification

