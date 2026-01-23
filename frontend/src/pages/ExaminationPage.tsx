import { useState } from 'react'
import { Card, Tabs, message, Input, Button, Table, Tag, Space, Typography, Alert, List, Empty, Divider, Select, Form } from 'antd'
import {
  HistoryOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  SearchOutlined,
} from '@ant-design/icons'

const { TabPane } = Tabs
const { Text, Title, Paragraph } = Typography
const { Option } = Select

interface ExaminationSuggestion {
  examinationName: string
  purpose: '确诊' | '排除' | '评估'
  priority: 'high' | 'medium' | 'low'
  reason: string
}

const ExaminationPage = () => {
  const [suggestions, setSuggestions] = useState<ExaminationSuggestion[]>([])
  const [loadingSuggestions, setLoadingSuggestions] = useState(false)
  const [form] = Form.useForm()
  
  // 从URL参数获取tab
  const urlParams = new URLSearchParams(window.location.search)
  const defaultTab = urlParams.get('tab') || 'suggestion'

  // 检查建议功能
  const handleGetSuggestions = async (_values: { symptom?: string; age?: number; gender?: string }) => {
    setLoadingSuggestions(true)
    try {
      // TODO: 调用后端API获取检查建议
      // const response = await examinationApi.getSuggestions(values)
      
      // 模拟数据
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      const mockSuggestions: ExaminationSuggestion[] = [
        {
          examinationName: '心电图',
          purpose: '排除',
          priority: 'high',
          reason: '胸痛症状需要排除心脏疾病，心电图是最基础的检查',
        },
        {
          examinationName: '心脏彩超',
          purpose: '确诊',
          priority: 'high',
          reason: '评估心脏结构和功能，排除器质性心脏病',
        },
        {
          examinationName: '血常规',
          purpose: '排除',
          priority: 'medium',
          reason: '排除感染性疾病',
        },
        {
          examinationName: '胸部CT',
          purpose: '评估',
          priority: 'low',
          reason: '如果症状持续，可进一步评估肺部情况',
        },
      ]
      
      setSuggestions(mockSuggestions)
      message.success('已为您生成检查建议')
    } catch (error) {
      console.error('获取检查建议失败:', error)
      message.error('获取检查建议失败，请重试')
    } finally {
      setLoadingSuggestions(false)
    }
  }


  return (
    <Card>
      <Tabs defaultActiveKey={defaultTab}>
        {/* Tab1: 检查建议（检查前）- 核心功能 */}
        <TabPane tab="检查建议" key="suggestion">
          <Space direction="vertical" style={{ width: '100%' }} size="large">
            <Card>
              <Title level={4}>根据您的症状，为您推荐合适的检查</Title>
              <Paragraph type="secondary">
                系统会根据您的症状、年龄、性别、既往史，进行医学判断，为您推荐合适的检查项目。
                即使是"没病"的情况，我们也会给出专业的判断依据，说明为什么安全。
              </Paragraph>
              
              <Divider />
              
              <Form
                form={form}
                layout="vertical"
                onFinish={handleGetSuggestions}
                style={{ maxWidth: 600 }}
              >
                <Form.Item
                  label="主要症状或检查需求"
                  name="symptom"
                  rules={[{ required: true, message: '请输入主要症状或检查需求' }]}
                >
                  <Input.TextArea
                    placeholder="例如：胸痛、体检指标异常、想确认是否健康等"
                    rows={3}
                  />
                </Form.Item>
                
                <Form.Item
                  label="年龄"
                  name="age"
                  rules={[{ required: true, message: '请输入年龄' }]}
                >
                  <Input type="number" placeholder="请输入年龄" />
                </Form.Item>
                
                <Form.Item
                  label="性别"
                  name="gender"
                  rules={[{ required: true, message: '请选择性别' }]}
                >
                  <Select placeholder="请选择性别">
                    <Option value="male">男</Option>
                    <Option value="female">女</Option>
                  </Select>
                </Form.Item>
                
                <Form.Item>
                  <Button
                    type="primary"
                    icon={<SearchOutlined />}
                    htmlType="submit"
                    loading={loadingSuggestions}
                    size="large"
                  >
                    获取检查建议
                  </Button>
                </Form.Item>
              </Form>
            </Card>

            {/* 检查建议结果 */}
            {suggestions.length > 0 && (
              <Card title="检查建议结果">
                <Alert
                  message="根据您提供的信息，建议进行以下检查"
                  description="这些建议基于医学判断，旨在帮助确认您的健康状况或排除相关疾病。"
                  type="info"
                  showIcon
                  style={{ marginBottom: 16 }}
                />
                <Table
                  dataSource={suggestions}
                  columns={[
                    {
                      title: '检查项目',
                      dataIndex: 'examinationName',
                      key: 'examinationName',
                      width: 150,
                    },
                    {
                      title: '目的',
                      dataIndex: 'purpose',
                      key: 'purpose',
                      width: 100,
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
                      width: 100,
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
                      title: '建议理由',
                      dataIndex: 'reason',
                      key: 'reason',
                    },
                  ]}
                  pagination={false}
                  rowKey={(_record, index) => `suggestion-${index}`}
                />
              </Card>
            )}
          </Space>
        </TabPane>

        {/* Tab2: 检查方案设计（检查前） */}
        <TabPane tab="检查方案" key="plan">
          <Card>
            <Alert
              message="个性化检查方案设计"
              description="根据您的症状、年龄、性别、既往史，系统会为您设计个性化的检查方案（体检套餐、专项检查等）。"
              type="info"
              showIcon
              style={{ marginBottom: 16 }}
            />
            <List
              dataSource={[
                { name: '基础体检套餐', description: '适合一般健康人群的常规体检', items: ['血常规', '尿常规', '肝功能', '肾功能', '心电图'] },
                { name: '心血管专项检查', description: '针对心血管疾病的专项检查', items: ['心电图', '心脏彩超', '血脂', '血压监测'] },
                { name: '消化系统检查', description: '针对消化系统疾病的检查', items: ['胃镜', '肠镜', '肝功能', '腹部B超'] },
              ]}
              renderItem={(item) => (
                <List.Item>
                  <List.Item.Meta
                    avatar={<FileTextOutlined style={{ fontSize: 24, color: '#1890ff' }} />}
                    title={<Text strong>{item.name}</Text>}
                    description={
                      <Space direction="vertical" size="small">
                        <Text type="secondary">{item.description}</Text>
                        <Space wrap>
                          {item.items.map((exam, index) => (
                            <Tag key={index} icon={<CheckCircleOutlined />}>
                              {exam}
                            </Tag>
                          ))}
                        </Space>
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          </Card>
        </TabPane>

        {/* Tab3: 检查历史 */}
        <TabPane
          tab={
            <Space>
              <HistoryOutlined />
              <span>检查历史</span>
            </Space>
          }
          key="history"
        >
          <Card>
            <Empty
              description="暂无检查历史记录"
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            />
          </Card>
        </TabPane>
      </Tabs>
    </Card>
  )
}

export default ExaminationPage
