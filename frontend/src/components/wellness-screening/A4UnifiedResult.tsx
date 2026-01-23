import React from 'react'
import { Card, Space, Button, Alert, List } from 'antd'

interface UnifiedResult {
  summary: string
  recommendations: string[]
  nextSteps: string[]
}

interface A4UnifiedResultProps {
  unifiedResult: UnifiedResult
  onNext: () => void
}

const A4UnifiedResult: React.FC<A4UnifiedResultProps> = ({
  unifiedResult,
  onNext,
}) => {
  return (
    <Card title="A4｜统一结果" style={{ marginBottom: 16 }}>
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        {/* 摘要 */}
        <Alert message={unifiedResult.summary} type="success" showIcon />

        {/* 建议列表 */}
        <Card size="small" title="建议">
          <List
            dataSource={unifiedResult.recommendations}
            renderItem={(item) => <List.Item>{item}</List.Item>}
          />
        </Card>

        {/* 下一步操作 */}
        <Card size="small" title="下一步操作">
          <List
            dataSource={unifiedResult.nextSteps}
            renderItem={(item) => <List.Item>{item}</List.Item>}
          />
        </Card>

        {/* 下一步按钮 */}
        <Button type="primary" onClick={onNext} block>
          继续下一步：设置随访
        </Button>
      </Space>
    </Card>
  )
}

export default A4UnifiedResult

