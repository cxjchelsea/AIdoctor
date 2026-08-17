import React from 'react'
import { Card, Row, Col, Descriptions, Tag, Progress, Typography } from 'antd'

const { Title, Text } = Typography

interface ToolResults {
  tool0?: {
    workMode: 'wellness_mode' | 'clinical_mode'
    riskLevel: string
  }
  tool1?: {
    concepts: any[]
  }
  tool2?: {
    completeness: number
  }
  tool3?: {
    reasoningPaths: any[]
    ddx: any[]
  }
  // 其他工具...
}

interface ToolResultsVisualizationProps {
  cdpId: string
}

const ToolResultsVisualization: React.FC<ToolResultsVisualizationProps> = ({
  cdpId: _cdpId,
}) => {
  // TODO: 从API获取八个工具执行结果数据
  const toolResults: ToolResults = {}

  return (
    <Card>
      <Title level={4}>八个工具执行结果</Title>
      <Row gutter={16}>
        {/* tool_0：健康状态判定 */}
        {toolResults.tool0 && (
          <Col span={12}>
            <Card size="small" title="tool_0：健康状态判定">
              <Descriptions size="small" column={1}>
                <Descriptions.Item label="工作态">
                  <Tag
                    color={
                      toolResults.tool0.workMode === 'clinical_mode' ? 'red' : 'green'
                    }
                  >
                    {toolResults.tool0.workMode === 'clinical_mode'
                      ? '临床诊疗态'
                      : '健康管理态'}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="风险等级">
                  {toolResults.tool0.riskLevel}
                </Descriptions.Item>
              </Descriptions>
            </Card>
          </Col>
        )}

        {/* tool_1：病例理解 */}
        {toolResults.tool1 && (
          <Col span={12}>
            <Card size="small" title="tool_1：病例理解">
              <Text>识别概念数：{toolResults.tool1.concepts.length}</Text>
            </Card>
          </Col>
        )}

        {/* tool_2：主动问诊 */}
        {toolResults.tool2 && (
          <Col span={12}>
            <Card size="small" title="tool_2：主动问诊">
              <Progress
                percent={toolResults.tool2.completeness * 100}
                format={(percent) => `${percent}%`}
              />
            </Card>
          </Col>
        )}

        {/* tool_3：鉴别诊断（DR.KNOWS核心） */}
        {toolResults.tool3 && (
          <Col span={12}>
            <Card size="small" title="tool_3：鉴别诊断（DR.KNOWS核心）">
              <Text>推理路径数：{toolResults.tool3.reasoningPaths.length}</Text>
              <br />
              <Text>鉴别诊断数：{toolResults.tool3.ddx.length}</Text>
            </Card>
          </Col>
        )}
      </Row>
    </Card>
  )
}

export default ToolResultsVisualization

