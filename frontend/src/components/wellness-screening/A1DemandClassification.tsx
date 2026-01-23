import React from 'react'
import { Card, Space, Button, Alert, Descriptions } from 'antd'

interface DemandType {
  type: 1 | 2 | 3 | 4
  typeName: string
  confidence?: number
}

interface A1DemandClassificationProps {
  demandType: DemandType
  onNext: () => void
}

const getDemandTypeLabel = (type: number): string => {
  const labels: Record<number, string> = {
    1: '筛查建议',
    3: '健康目标管理',
    4: '计划性健康需求',
  }
  return labels[type] || '未知'
}

const A1DemandClassification: React.FC<A1DemandClassificationProps> = ({
  demandType,
  onNext,
}) => {
  return (
    <Card title="A1｜需求分类" style={{ marginBottom: 16 }}>
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        {/* 需求类型展示 */}
        <Alert
          message={`已识别您的需求：${getDemandTypeLabel(demandType.type)}`}
          type="success"
          showIcon
        />

        {/* 提取的关键信息 */}
        <Card size="small" title="提取的关键信息">
          <Descriptions column={1}>
            <Descriptions.Item label="需求类型">
              {getDemandTypeLabel(demandType.type)}
            </Descriptions.Item>
            {demandType.confidence && (
              <Descriptions.Item label="置信度">
                {Math.round(demandType.confidence * 100)}%
              </Descriptions.Item>
            )}
          </Descriptions>
        </Card>

        {/* 下一步按钮 */}
        <Button type="primary" onClick={onNext} block>
          继续下一步：收集健康画像
        </Button>
      </Space>
    </Card>
  )
}

export default A1DemandClassification

