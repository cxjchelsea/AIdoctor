import React from 'react'
import { Card, List, Tag, Space, Typography, Button, Alert, Collapse, Divider } from 'antd'
import { DownloadOutlined, ShareAltOutlined, FileTextOutlined } from '@ant-design/icons'
import type { DiagnosisResult } from '@/types/diagnosis'

const { Text, Title } = Typography
const { Panel } = Collapse

interface DiagnosisResultCardProps {
  result: DiagnosisResult
  onViewDetail?: () => void
  onSave?: () => void
  onShare?: () => void
}

const DiagnosisResultCard: React.FC<DiagnosisResultCardProps> = ({
  result,
  onViewDetail,
  onSave,
  onShare,
}) => {
  // 导出报告
  const handleExport = () => {
    // TODO: 实现导出功能
    console.log('导出报告', result)
  }

  return (
    <Card
      title={
        <Space>
          <span>诊断结果</span>
          <Tag color={result.conclusion.type === 'confirmable' ? 'green' : 'orange'}>
            {result.conclusion.type === 'confirmable' ? '可确证' : '不可确证'}
          </Tag>
        </Space>
      }
      style={{ marginBottom: 16 }}
      extra={
        onViewDetail && (
          <Button type="link" onClick={onViewDetail}>
            查看详情
          </Button>
        )
      }
    >
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <Text>根据您的症状和健康档案，我为您做了详细分析：</Text>
        
        {/* 首要假设 */}
        <Card size="small" style={{ backgroundColor: '#f0f9ff', borderColor: '#1890ff' }}>
          <Space direction="vertical" style={{ width: '100%' }} size="small">
            <Text strong style={{ color: '#1890ff' }}>【首要假设】</Text>
            <Text strong>{result.conclusion.primaryHypothesis.disease}</Text>
            <Text type="secondary">
              可能性：{Math.round(result.conclusion.primaryHypothesis.confidence * 100)}%
            </Text>
          </Space>
        </Card>
        
        {/* 必须排除的高危诊断（如果有） */}
        {result.conclusion.mustExcludeDiagnosis && (
          <Alert
            message={
              <Space>
                <Text strong>【必须排除的高危诊断】</Text>
                <Text strong style={{ color: '#ff4d4f' }}>
                  {result.conclusion.mustExcludeDiagnosis.disease}
                </Text>
              </Space>
            }
            description={
              <Text>
                虽然可能性较低（{Math.round(result.conclusion.mustExcludeDiagnosis.confidence * 100)}%），
                但一旦漏诊后果严重，需要优先排除
              </Text>
            }
            type="error"
            showIcon
            style={{ marginBottom: 8 }}
          />
        )}
        
        {/* 主要备选诊断 */}
        {result.conclusion.alternativeDiagnoses.length > 0 && (
          <div>
            <Text strong>【主要备选诊断】</Text>
            <List
              size="small"
              dataSource={result.conclusion.alternativeDiagnoses}
              renderItem={(item) => (
                <List.Item>
                  <Text>{item.disease}</Text>
                  <Text type="secondary">
                    （{Math.round(item.confidence * 100)}%）
                  </Text>
                </List.Item>
              )}
            />
          </div>
        )}
        
        {/* 操作按钮 */}
        <Space>
          {onViewDetail && (
            <Button size="small" onClick={onViewDetail}>查看详情</Button>
          )}
          {onSave && (
            <Button size="small" onClick={onSave}>保存报告</Button>
          )}
          {onShare && (
            <Button size="small" onClick={onShare}>分享</Button>
          )}
        </Space>
      </Space>
    </Card>
  )
}

// 保留原有的详细版本（用于抽屉中显示）
export const DiagnosisResultCardDetailed: React.FC<DiagnosisResultCardProps> = ({
  result,
  onViewDetail,
  onSave,
  onShare,
}) => {
  const handleExport = () => {
    console.log('导出报告', result)
  }

  return (
    <Card
      title={
        <Space>
          <FileTextOutlined />
          <span>诊断结果 - 终点结论包</span>
          <Tag color={result.conclusion.type === 'confirmable' ? 'green' : 'orange'}>
            {result.conclusion.type === 'confirmable' ? '可确证' : '不可确证'}
          </Tag>
        </Space>
      }
      style={{ marginBottom: 16 }}
      extra={
        <Space>
          <Button
            type="text"
            icon={<DownloadOutlined />}
            onClick={handleExport}
          >
            导出
          </Button>
          {onShare && (
            <Button
              type="text"
              icon={<ShareAltOutlined />}
              onClick={onShare}
            >
              分享
            </Button>
          )}
        </Space>
      }
    >
      <Collapse defaultActiveKey={['1', '2', '3', '4']} ghost>
        {/* 要素1：结论 */}
        <Panel
          header={
            <Title level={5} style={{ margin: 0 }}>
              【要素1】结论
            </Title>
          }
          key="1"
        >
          <Space direction="vertical" style={{ width: '100%' }} size="middle">
            {/* 首要假设 */}
            <Card
              size="small"
              style={{ backgroundColor: '#f0f9ff', borderColor: '#1890ff' }}
            >
              <Space direction="vertical" style={{ width: '100%' }} size="small">
                <Text strong style={{ color: '#1890ff' }}>
                  首要假设
                </Text>
                <Text strong style={{ fontSize: 16 }}>
                  {result.conclusion.primaryHypothesis.disease}
                </Text>
                <Text type="secondary">
                  可能性：{Math.round(result.conclusion.primaryHypothesis.confidence * 100)}%
                </Text>
              </Space>
            </Card>

            {/* 主要备选诊断 */}
            {result.conclusion.alternativeDiagnoses.length > 0 && (
              <div>
                <Text strong>主要备选诊断：</Text>
                <List
                  size="small"
                  dataSource={result.conclusion.alternativeDiagnoses}
                  renderItem={(item) => (
                    <List.Item>
                      <Text>{item.disease}</Text>
                      <Text type="secondary">
                        （可能性：{Math.round(item.confidence * 100)}%）
                      </Text>
                    </List.Item>
                  )}
                />
              </div>
            )}
          </Space>
        </Panel>

        {/* 要素2：必须排除项 */}
        <Panel
          header={
            <Title level={5} style={{ margin: 0 }}>
              【要素2】必须排除项
            </Title>
          }
          key="2"
        >
          {result.conclusion.mustExcludeDiagnosis ? (
            <Space direction="vertical" style={{ width: '100%' }} size="small">
              <Alert
                message={
                  <Space>
                    <Text strong style={{ color: '#ff4d4f', fontSize: 16 }}>
                      {result.conclusion.mustExcludeDiagnosis.disease}
                    </Text>
                    <Tag color="red">必须排除</Tag>
                  </Space>
                }
                description={
                  <Space direction="vertical" size="small" style={{ width: '100%' }}>
                    <Text>
                      可能性：{Math.round(result.conclusion.mustExcludeDiagnosis.confidence * 100)}%
                    </Text>
                    <Text>
                      虽然可能性较低，但一旦漏诊后果严重，需要优先排除
                    </Text>
                    {result.exclusionStatus && (
                      <div style={{ marginTop: 8 }}>
                        <Text strong>排除状态：</Text>
                        <Tag color={
                          result.exclusionStatus.status === 'excluded' ? 'green' :
                          result.exclusionStatus.status === 'not_excluded' ? 'red' : 'orange'
                        }>
                          {result.exclusionStatus.status === 'excluded' ? '已排除' :
                           result.exclusionStatus.status === 'not_excluded' ? '未排除' : '需线下排除'}
                        </Tag>
                        {result.exclusionStatus.reason && (
                          <Text type="secondary" style={{ display: 'block', marginTop: 4 }}>
                            {result.exclusionStatus.reason}
                          </Text>
                        )}
                      </div>
                    )}
                  </Space>
                }
                type="error"
                showIcon
              />
            </Space>
          ) : (
            <Text type="secondary">暂无必须排除的高危诊断</Text>
          )}
        </Panel>

        {/* 要素3：关键依据 */}
        <Panel
          header={
            <Title level={5} style={{ margin: 0 }}>
              【要素3】关键依据
            </Title>
          }
          key="3"
        >
          <Space direction="vertical" style={{ width: '100%' }} size="middle">
            {/* 阳性证据 */}
            {result.keyEvidence.positiveEvidence.length > 0 && (
              <div>
                <Text strong style={{ color: '#52c41a' }}>支持证据（阳性）：</Text>
                <List
                  size="small"
                  dataSource={result.keyEvidence.positiveEvidence}
                  renderItem={(item) => (
                    <List.Item>
                      <Text>✓ {item}</Text>
                    </List.Item>
                  )}
                />
              </div>
            )}

            {/* 阴性证据 */}
            {result.keyEvidence.negativeEvidence.length > 0 && (
              <div>
                <Text strong style={{ color: '#1890ff' }}>排除证据（阴性）：</Text>
                <List
                  size="small"
                  dataSource={result.keyEvidence.negativeEvidence}
                  renderItem={(item) => (
                    <List.Item>
                      <Text>✗ {item}</Text>
                    </List.Item>
                  )}
                />
              </div>
            )}
          </Space>
        </Panel>

        {/* 要素4：行动与随访 */}
        <Panel
          header={
            <Title level={5} style={{ margin: 0 }}>
              【要素4】行动与随访
            </Title>
          }
          key="4"
        >
          <Space direction="vertical" style={{ width: '100%' }} size="middle">
            {/* 立即行动 */}
            <Alert
              message="立即行动"
              description={result.actionAndFollowUp.immediateAction}
              type="warning"
              showIcon
            />

            {/* 复评时间窗 */}
            {result.actionAndFollowUp.reviewTimeWindow && (
              <div>
                <Text strong>复评时间窗：</Text>
                <Tag color="blue">{result.actionAndFollowUp.reviewTimeWindow}</Tag>
              </div>
            )}

            {/* 升级触发条件 */}
            {result.actionAndFollowUp.upgradeTriggerConditions.length > 0 && (
              <div>
                <Text strong>升级触发条件（出现以下情况需提前复评或立即升级）：</Text>
                <List
                  size="small"
                  dataSource={result.actionAndFollowUp.upgradeTriggerConditions}
                  renderItem={(item) => (
                    <List.Item>
                      <Text>⚠️ {item}</Text>
                    </List.Item>
                  )}
                />
              </div>
            )}

            {/* 就医建议 */}
            {result.medicalAdvice && (
              <div>
                <Divider />
                <Text strong>就医建议：</Text>
                <div style={{ marginTop: 8 }}>
                  <Text>科室：</Text>
                  <Tag color="purple">{result.medicalAdvice.department}</Tag>
                </div>
                <div style={{ marginTop: 8 }}>
                  <Text>时机：</Text>
                  <Text>{result.medicalAdvice.timing}</Text>
                </div>
              </div>
            )}
          </Space>
        </Panel>
      </Collapse>

      {/* 操作按钮 */}
      <Divider />
      <Space>
        {onSave && (
          <Button icon={<FileTextOutlined />} onClick={onSave}>
            保存报告
          </Button>
        )}
        <Button icon={<DownloadOutlined />} onClick={handleExport}>
          导出报告
        </Button>
        {onShare && (
          <Button icon={<ShareAltOutlined />} onClick={onShare}>
            分享给医生
          </Button>
        )}
      </Space>
    </Card>
  )
}

export default DiagnosisResultCard

