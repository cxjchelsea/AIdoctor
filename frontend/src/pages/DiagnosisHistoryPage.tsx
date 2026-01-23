import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, List, Tag, Button, Space, Typography, Pagination, Empty, Spin } from 'antd'
import {
  FileTextOutlined,
  EyeOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import { diagnosisApi } from '@/services/diagnosisApi'
import type { DiagnosisRecord, PaginatedResponse } from '@/types/diagnosis'

const { Text } = Typography

const DiagnosisHistoryPage = () => {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<PaginatedResponse<DiagnosisRecord> | null>(null)
  const [page, setPage] = useState(1)
  const [pageSize] = useState(20)

  useEffect(() => {
    loadHistory()
  }, [page])

  const loadHistory = async () => {
    try {
      setLoading(true)
      const response = await diagnosisApi.getHistory('user-1', page, pageSize) // TODO: 从用户上下文获取
      setData(response.data)
    } catch (error) {
      console.error('加载诊断历史失败:', error)
    } finally {
      setLoading(false)
    }
  }

  const getStatusTag = (status: string) => {
    const statusConfig: Record<string, { color: string; text: string }> = {
      collecting: { color: 'blue', text: '收集中' },
      questioning: { color: 'orange', text: '追问中' },
      analyzing: { color: 'processing', text: '分析中' },
      completed: { color: 'success', text: '已完成' },
      cancelled: { color: 'default', text: '已取消' },
    }
    const config = statusConfig[status] || statusConfig.collecting
    return <Tag color={config.color}>{config.text}</Tag>
  }

  const getTypeText = (type: string) => {
    const typeMap: Record<string, string> = {
      symptom: '症状诊断',
      examination: '检查诊断',
      comprehensive: '综合诊断',
    }
    return typeMap[type] || type
  }

  const formatDate = (dateString: string) => {
    const date = new Date(dateString)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  const handleViewDetail = (diagnosisId: string, status: string) => {
    if (status === 'completed') {
      navigate(`/diagnosis/result/${diagnosisId}`)
    } else {
      navigate(`/diagnosis?diagnosisId=${diagnosisId}`)
    }
  }

  const handleRediagnosis = () => {
    navigate('/')
  }

  return (
    <div>
      <Card
        title="诊断历史"
        extra={
          <Button
            type="primary"
            icon={<ReloadOutlined />}
            onClick={handleRediagnosis}
          >
            开始新诊断
          </Button>
        }
      >
        {loading && !data ? (
          <div style={{ textAlign: 'center', padding: '50px 0' }}>
            <Spin size="large" />
          </div>
        ) : !data || data.list.length === 0 ? (
          <Empty
            description="暂无诊断记录"
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          >
            <Button type="primary" onClick={handleRediagnosis}>
              开始第一次诊断
            </Button>
          </Empty>
        ) : (
          <>
            <List
              loading={loading}
              dataSource={data.list}
              renderItem={(item) => (
                <List.Item
                  actions={[
                    <Button
                      key="view"
                      type="link"
                      icon={<EyeOutlined />}
                      onClick={() => handleViewDetail(item.diagnosisId, item.status)}
                    >
                      {item.status === 'completed' ? '查看结果' : '继续诊断'}
                    </Button>,
                  ]}
                >
                  <List.Item.Meta
                    avatar={<FileTextOutlined style={{ fontSize: 24, color: '#1890ff' }} />}
                    title={
                      <Space>
                        <Text strong>{item.chiefComplaint || '未填写主诉'}</Text>
                        {getStatusTag(item.status)}
                      </Space>
                    }
                    description={
                      <Space>
                        <Tag>{getTypeText(item.diagnosisType)}</Tag>
                        <Text type="secondary">
                          {formatDate(item.createdAt)}
                        </Text>
                        {item.completedAt && (
                          <>
                            <Text type="secondary">·</Text>
                            <Text type="secondary">
                              完成于 {formatDate(item.completedAt)}
                            </Text>
                          </>
                        )}
                        {item.completeness !== undefined && (
                          <>
                            <Text type="secondary">·</Text>
                            <Text type="secondary">
                              完整度: {Math.round(item.completeness)}%
                            </Text>
                          </>
                        )}
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
            {data.totalPages > 1 && (
              <div style={{ textAlign: 'right', marginTop: 16 }}>
                <Pagination
                  current={page}
                  total={data.total}
                  pageSize={pageSize}
                  showTotal={(total) => `共 ${total} 条记录`}
                  onChange={(newPage) => setPage(newPage)}
                />
              </div>
            )}
          </>
        )}
      </Card>
    </div>
  )
}

export default DiagnosisHistoryPage
