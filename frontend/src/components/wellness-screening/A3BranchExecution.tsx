import React from 'react'
import { Card, Space, Button, Tag, List } from 'antd'

interface BranchExecutionResult {
  demandType: 1 | 2 | 3 | 4
  riskLevel?: string
  recommendations?: Array<{
    name: string
    description: string
    priority: string
    reason: string
  }>
}

interface A3BranchExecutionProps {
  branchResult: BranchExecutionResult
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

const A3BranchExecution: React.FC<A3BranchExecutionProps> = ({
  branchResult,
  onNext,
}) => {
  return (
    <Card
      title={`A3｜执行分支 - ${getDemandTypeLabel(branchResult.demandType)}`}
      style={{ marginBottom: 16 }}
    >
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        {/* 根据需求类型展示不同内容 */}
        {branchResult.demandType === 1 && branchResult.recommendations && (
          <Card size="small" title="筛查建议">
            <Space direction="vertical" style={{ width: '100%' }} size="middle">
              {branchResult.riskLevel && (
                <Tag color="blue">风险等级：{branchResult.riskLevel}</Tag>
              )}
              <List
                dataSource={branchResult.recommendations}
                renderItem={(item) => (
                  <List.Item>
                    <List.Item.Meta
                      title={item.name}
                      description={
                        <div>
                          <div>{item.description}</div>
                          <div style={{ marginTop: 8 }}>
                            <span style={{ color: '#8c8c8c' }}>优先级：{item.priority}</span>
                          </div>
                          <div>
                            <span style={{ color: '#8c8c8c' }}>原因：{item.reason}</span>
                          </div>
                        </div>
                      }
                    />
                  </List.Item>
                )}
              />
            </Space>
          </Card>
        )}

        {/* 下一步按钮 */}
        <Button type="primary" onClick={onNext} block>
          继续下一步：生成统一结果
        </Button>
      </Space>
    </Card>
  )
}

export default A3BranchExecution

