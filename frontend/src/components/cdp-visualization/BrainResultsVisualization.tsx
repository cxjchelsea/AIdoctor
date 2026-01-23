import React from 'react'
import { Card, Row, Col, Descriptions, Tag, Progress, Typography } from 'antd'

const { Title, Text } = Typography

interface BrainResults {
  brain0?: {
    workMode: 'wellness_mode' | 'clinical_mode'
    riskLevel: string
  }
  brainA?: {
    concepts: any[]
  }
  brainB?: {
    completeness: number
  }
  brainC?: {
    reasoningPaths: any[]
    ddx: any[]
  }
  // 其他脑区...
}

interface BrainResultsVisualizationProps {
  cdpId: string
}

const BrainResultsVisualization: React.FC<BrainResultsVisualizationProps> = ({
  cdpId,
}) => {
  // TODO: 从API获取八个脑区执行结果数据
  const brainResults: BrainResults = {}

  return (
    <Card>
      <Title level={4}>八个脑区执行结果</Title>
      <Row gutter={16}>
        {/* 脑区0：健康状态判定 */}
        {brainResults.brain0 && (
          <Col span={12}>
            <Card size="small" title="脑区0：健康状态判定">
              <Descriptions size="small" column={1}>
                <Descriptions.Item label="工作态">
                  <Tag
                    color={
                      brainResults.brain0.workMode === 'clinical_mode' ? 'red' : 'green'
                    }
                  >
                    {brainResults.brain0.workMode === 'clinical_mode'
                      ? '临床诊疗态'
                      : '健康管理态'}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="风险等级">
                  {brainResults.brain0.riskLevel}
                </Descriptions.Item>
              </Descriptions>
            </Card>
          </Col>
        )}

        {/* 脑区A：病例理解 */}
        {brainResults.brainA && (
          <Col span={12}>
            <Card size="small" title="脑区A：病例理解">
              <Text>识别概念数：{brainResults.brainA.concepts.length}</Text>
            </Card>
          </Col>
        )}

        {/* 脑区B：主动问诊 */}
        {brainResults.brainB && (
          <Col span={12}>
            <Card size="small" title="脑区B：主动问诊">
              <Progress
                percent={brainResults.brainB.completeness * 100}
                format={(percent) => `${percent}%`}
              />
            </Card>
          </Col>
        )}

        {/* 脑区C：鉴别诊断（DR.KNOWS核心） */}
        {brainResults.brainC && (
          <Col span={12}>
            <Card size="small" title="脑区C：鉴别诊断（DR.KNOWS核心）">
              <Text>推理路径数：{brainResults.brainC.reasoningPaths.length}</Text>
              <br />
              <Text>鉴别诊断数：{brainResults.brainC.ddx.length}</Text>
            </Card>
          </Col>
        )}
      </Row>
    </Card>
  )
}

export default BrainResultsVisualization

