import { Card, List, Tag, Space, Typography, Collapse, Button, Tooltip } from 'antd'
import { QuestionCircleOutlined, CheckCircleOutlined, CloseCircleOutlined, ExclamationCircleOutlined } from '@ant-design/icons'
import type { DiseasePossibility } from '@/types/diagnosis'

const { Text, Title } = Typography
const { Panel } = Collapse

interface DifferentialDiagnosisPanelProps {
  differentialDiagnoses: DiseasePossibility[]
  onSelectDiagnosis?: (disease: string) => void
}

const DifferentialDiagnosisPanel: React.FC<DifferentialDiagnosisPanelProps> = ({
  differentialDiagnoses,
  onSelectDiagnosis,
}) => {
  if (!differentialDiagnoses || differentialDiagnoses.length === 0) {
    return (
      <Card title="鉴别诊断 (DDx)">
        <Text type="secondary">暂无鉴别诊断结果</Text>
      </Card>
    )
  }

  // 按置信度排序
  const sortedDiagnoses = [...differentialDiagnoses].sort(
    (a, b) => b.confidence - a.confidence
  )

  return (
    <Card
      title={
        <Space>
          <span>鉴别诊断 (DDx)</span>
          <Tag color="blue">Top-{sortedDiagnoses.length}</Tag>
        </Space>
      }
    >
      <Collapse ghost>
        {sortedDiagnoses.map((diagnosis, index) => (
          <Panel
            header={
              <Space>
                <Text strong style={{ fontSize: 15 }}>
                  {index + 1}. {diagnosis.disease}
                </Text>
                <Tag
                  color={
                    diagnosis.confidence >= 0.7
                      ? 'green'
                      : diagnosis.confidence >= 0.5
                      ? 'orange'
                      : 'default'
                  }
                >
                  可能性: {Math.round(diagnosis.confidence * 100)}%
                </Tag>
              </Space>
            }
            key={diagnosis.disease}
          >
            <Space direction="vertical" style={{ width: '100%' }} size="middle">
              {/* 支持证据 */}
              {diagnosis.supportingEvidence && diagnosis.supportingEvidence.length > 0 && (
                <div>
                  <Space>
                    <CheckCircleOutlined style={{ color: '#52c41a' }} />
                    <Text strong style={{ color: '#52c41a' }}>
                      支持证据 (Pros)
                    </Text>
                  </Space>
                  <List
                    size="small"
                    dataSource={diagnosis.supportingEvidence}
                    renderItem={(item) => (
                      <List.Item>
                        <Text>✓ {item}</Text>
                      </List.Item>
                    )}
                    style={{ marginTop: 8 }}
                  />
                </div>
              )}

              {/* 反证 */}
              {diagnosis.opposingEvidence && diagnosis.opposingEvidence.length > 0 && (
                <div>
                  <Space>
                    <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
                    <Text strong style={{ color: '#ff4d4f' }}>
                      反证 (Cons)
                    </Text>
                  </Space>
                  <List
                    size="small"
                    dataSource={diagnosis.opposingEvidence}
                    renderItem={(item) => (
                      <List.Item>
                        <Text>✗ {item}</Text>
                      </List.Item>
                    )}
                    style={{ marginTop: 8 }}
                  />
                </div>
              )}

              {/* 缺失证据 */}
              {diagnosis.missingInfo && diagnosis.missingInfo.length > 0 && (
                <div>
                  <Space>
                    <ExclamationCircleOutlined style={{ color: '#faad14' }} />
                    <Text strong style={{ color: '#faad14' }}>
                      缺失证据 (Missing)
                    </Text>
                  </Space>
                  <List
                    size="small"
                    dataSource={diagnosis.missingInfo}
                    renderItem={(item) => (
                      <List.Item>
                        <Text>? {item}</Text>
                      </List.Item>
                    )}
                    style={{ marginTop: 8 }}
                  />
                </div>
              )}

              {/* 入选依据 */}
              {diagnosis.inclusionBasis && diagnosis.inclusionBasis.length > 0 && (
                <div>
                  <Space>
                    <QuestionCircleOutlined style={{ color: '#1890ff' }} />
                    <Text strong>入选依据</Text>
                  </Space>
                  <List
                    size="small"
                    dataSource={diagnosis.inclusionBasis}
                    renderItem={(item) => (
                      <List.Item>
                        <Text type="secondary">• {item}</Text>
                      </List.Item>
                    )}
                    style={{ marginTop: 8 }}
                  />
                </div>
              )}

              {/* 操作按钮 */}
              {onSelectDiagnosis && (
                <Button
                  type="primary"
                  size="small"
                  onClick={() => onSelectDiagnosis(diagnosis.disease)}
                >
                  查看需要补充的问题
                </Button>
              )}
            </Space>
          </Panel>
        ))}
      </Collapse>
    </Card>
  )
}

export default DifferentialDiagnosisPanel

