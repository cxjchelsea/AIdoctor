import React from 'react'
import { Card, List, Tag, Space, Typography } from 'antd'

const { Title } = Typography

interface ReasoningPath {
  description: string
  nodes: any[]
  relationships: any[]
  relevanceScore: number
  evidenceStrength: number
  compositeScore: number
}

interface ReasoningPathsVisualizationProps {
  cdpId: string
}

const ReasoningPathsVisualization: React.FC<ReasoningPathsVisualizationProps> = ({
  cdpId,
}) => {
  // TODO: 从API获取推理路径数据
  const reasoningPaths: ReasoningPath[] = []

  return (
    <Card>
      <Title level={4}>推理路径（DR.KNOWS核心）</Title>
      {reasoningPaths.length === 0 ? (
        <div>暂无推理路径数据</div>
      ) : (
        <List
          dataSource={reasoningPaths}
          renderItem={(path) => (
            <List.Item>
              <Card size="small" style={{ width: '100%' }}>
                {/* 路径描述 */}
                <Typography.Text>{path.description}</Typography.Text>

                {/* 路径评分 */}
                <Space style={{ marginTop: 8 }}>
                  <Tag>相关性: {path.relevanceScore.toFixed(2)}</Tag>
                  <Tag>证据强度: {path.evidenceStrength.toFixed(2)}</Tag>
                  <Tag>综合评分: {path.compositeScore.toFixed(2)}</Tag>
                </Space>
              </Card>
            </List.Item>
          )}
        />
      )}
    </Card>
  )
}

export default ReasoningPathsVisualization

