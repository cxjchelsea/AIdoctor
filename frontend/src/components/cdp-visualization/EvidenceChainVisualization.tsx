import React from 'react'
import { Card, Timeline, Space, Tag, Typography } from 'antd'

const { Title, Text } = Typography

interface Evidence {
  evidence: string
  type: 'support' | 'oppose' | 'neutral'
  strength: string
  affectedDisease?: string
}

interface EvidenceChainVisualizationProps {
  cdpId: string
}

const EvidenceChainVisualization: React.FC<EvidenceChainVisualizationProps> = ({
  cdpId: _cdpId,
}) => {
  // TODO: 从API获取证据链数据
  const evidenceChain: Evidence[] = []

  return (
    <Card>
      <Title level={4}>证据链</Title>
      {evidenceChain.length === 0 ? (
        <div>暂无证据链数据</div>
      ) : (
        <Timeline>
          {evidenceChain.map((evidence, index) => (
            <Timeline.Item
              key={index}
              color={
                evidence.type === 'support'
                  ? 'green'
                  : evidence.type === 'oppose'
                  ? 'red'
                  : 'gray'
              }
            >
              <Space direction="vertical">
                <Text strong>{evidence.evidence}</Text>
                <Tag>{evidence.strength}</Tag>
                {evidence.affectedDisease && (
                  <Text type="secondary">影响：{evidence.affectedDisease}</Text>
                )}
              </Space>
            </Timeline.Item>
          ))}
        </Timeline>
      )}
    </Card>
  )
}

export default EvidenceChainVisualization

