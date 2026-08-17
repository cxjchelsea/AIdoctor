import { Card, Alert, Tabs, Table, Tag, Badge } from 'antd'
import type { ExaminationItem } from '@/types/diagnosis'

const { TabPane } = Tabs

interface ExaminationSuggestionProps {
  priorityExaminations: ExaminationItem[]
  optionalExaminations: ExaminationItem[]
  explanation?: string
}

const ExaminationSuggestion: React.FC<ExaminationSuggestionProps> = ({
  priorityExaminations,
  optionalExaminations,
  explanation,
}) => {
  const columns = [
    {
      title: '检查项目',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '目的',
      dataIndex: 'purpose',
      key: 'purpose',
      render: (purpose: string) => {
        const colors: Record<string, string> = {
          确诊: 'red',
          排除: 'orange',
          评估: 'blue',
        }
        return <Tag color={colors[purpose] || 'default'}>{purpose}</Tag>
      },
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      render: (priority: string) => {
        const colors: Record<string, string> = {
          high: 'red',
          medium: 'orange',
          low: 'blue',
        }
        const texts: Record<string, string> = {
          high: '高',
          medium: '中',
          low: '低',
        }
        return <Tag color={colors[priority] || 'default'}>{texts[priority] || priority}</Tag>
      },
    },
    {
      title: '说明',
      dataIndex: 'reason',
      key: 'reason',
    },
  ]

  return (
    <Card title="建议检查" style={{ marginBottom: 16 }}>
      {explanation && (
        <Alert
          message={explanation}
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}

      <Tabs>
        <TabPane
          tab={
            <Badge count={priorityExaminations.length} offset={[10, 0]}>
              <span>优先检查</span>
            </Badge>
          }
          key="priority"
        >
          <Table
            dataSource={priorityExaminations}
            columns={columns}
            pagination={false}
            size="small"
            rowKey={(_record, index) => `priority-${index}`}
          />
        </TabPane>

        <TabPane
          tab={
            <Badge count={optionalExaminations.length} offset={[10, 0]}>
              <span>可选检查</span>
            </Badge>
          }
          key="optional"
        >
          <Table
            dataSource={optionalExaminations}
            columns={columns}
            pagination={false}
            size="small"
            rowKey={(_record, index) => `optional-${index}`}
          />
        </TabPane>
      </Tabs>
    </Card>
  )
}

export default ExaminationSuggestion

