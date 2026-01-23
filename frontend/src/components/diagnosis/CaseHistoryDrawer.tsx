import { Drawer, List, Button, Tag, Space, Typography, Empty, Spin, Input, Select, Checkbox, message } from 'antd'
import { FileTextOutlined, EyeOutlined, PushpinOutlined, PushpinFilled, DeleteOutlined } from '@ant-design/icons'
import { diagnosisApi } from '@/services/diagnosisApi'
import type { DiagnosisRecord, WorkMode, RiskLevel } from '@/types/diagnosis'
import { useState, useEffect, useMemo } from 'react'

const { Text } = Typography
const { Search } = Input

interface CaseHistoryDrawerProps {
  visible: boolean
  onClose: () => void
  onViewCase?: (diagnosisId: string, status: string) => void
}

const CaseHistoryDrawer: React.FC<CaseHistoryDrawerProps> = ({
  visible,
  onClose,
  onViewCase,
}) => {
  const [loading, setLoading] = useState(false)
  const [records, setRecords] = useState<DiagnosisRecord[]>([])
  const [searchText, setSearchText] = useState('')
  const [workModeFilter, setWorkModeFilter] = useState<WorkMode | 'all'>('all')
  const [riskLevelFilter, setRiskLevelFilter] = useState<RiskLevel | 'all'>('all')
  const [showArchived, setShowArchived] = useState(false)

  useEffect(() => {
    if (visible) {
      loadHistory()
    }
  }, [visible])

  const loadHistory = async () => {
    try {
      setLoading(true)
      const response = await diagnosisApi.getHistory('user-1', 1, 20) // TODO: 从用户上下文获取
      setRecords(response.data.list || [])
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
    onViewCase?.(diagnosisId, status)
    onClose()
  }

  const handlePin = async (diagnosisId: string, isPinned: boolean) => {
    try {
      // TODO: 调用API更新置顶状态
      setRecords(prev => prev.map(r => 
        r.diagnosisId === diagnosisId ? { ...r, isPinned: !isPinned } : r
      ))
      message.success(isPinned ? '已取消置顶' : '已置顶')
    } catch (error) {
      message.error('操作失败')
    }
  }

  const handleArchive = async (diagnosisId: string, isArchived: boolean) => {
    try {
      // TODO: 调用API更新归档状态
      setRecords(prev => prev.map(r => 
        r.diagnosisId === diagnosisId ? { ...r, isArchived: !isArchived } : r
      ))
      message.success(isArchived ? '已取消归档' : '已归档')
    } catch (error) {
      message.error('操作失败')
    }
  }

  // 过滤和排序记录
  const filteredRecords = useMemo(() => {
    let filtered = records.filter(r => {
      // 归档过滤
      if (!showArchived && r.isArchived) return false
      if (showArchived && !r.isArchived) return false

      // 搜索过滤
      if (searchText && !r.chiefComplaint?.toLowerCase().includes(searchText.toLowerCase())) {
        return false
      }

      // 工作态过滤
      if (workModeFilter !== 'all' && r.workMode !== workModeFilter) {
        return false
      }

      // 风险等级过滤
      if (riskLevelFilter !== 'all' && r.riskLevel !== riskLevelFilter) {
        return false
      }

      return true
    })

    // 排序：置顶的在前，然后按时间倒序
    return filtered.sort((a, b) => {
      if (a.isPinned && !b.isPinned) return -1
      if (!a.isPinned && b.isPinned) return 1
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    })
  }, [records, searchText, workModeFilter, riskLevelFilter, showArchived])

  const getWorkModeTag = (workMode?: WorkMode) => {
    if (!workMode) return null
    return (
      <Tag color={workMode === 'clinical_mode' ? 'red' : 'green'}>
        {workMode === 'clinical_mode' ? '临床诊疗态' : '健康管理态'}
      </Tag>
    )
  }

  const getRiskLevelTag = (riskLevel?: RiskLevel) => {
    if (!riskLevel) return null
    const colorMap: Record<RiskLevel, string> = {
      L1: 'red',
      L2: 'orange',
      L3: 'blue',
      L4: 'green',
    }
    return <Tag color={colorMap[riskLevel]}>{riskLevel}</Tag>
  }

  return (
    <Drawer
      title="会话/病例工作台"
      placement="right"
      width={800}
      open={visible}
      onClose={onClose}
    >
      {/* 搜索和筛选区域 */}
      <Space direction="vertical" style={{ width: '100%', marginBottom: 16 }} size="middle">
        <Search
          placeholder="搜索病例（主诉、症状等）"
          allowClear
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          style={{ width: '100%' }}
        />
        <Space wrap>
          <Select
            placeholder="工作态"
            value={workModeFilter}
            onChange={setWorkModeFilter}
            style={{ width: 140 }}
            allowClear
          >
            <Select.Option value="all">全部</Select.Option>
            <Select.Option value="clinical_mode">临床诊疗态</Select.Option>
            <Select.Option value="wellness_mode">健康管理态</Select.Option>
          </Select>
          <Select
            placeholder="风险等级"
            value={riskLevelFilter}
            onChange={setRiskLevelFilter}
            style={{ width: 120 }}
            allowClear
          >
            <Select.Option value="all">全部</Select.Option>
            <Select.Option value="L1">L1</Select.Option>
            <Select.Option value="L2">L2</Select.Option>
            <Select.Option value="L3">L3</Select.Option>
            <Select.Option value="L4">L4</Select.Option>
          </Select>
          <Checkbox
            checked={showArchived}
            onChange={(e) => setShowArchived(e.target.checked)}
          >
            显示已归档
          </Checkbox>
        </Space>
      </Space>

      {loading && !records.length ? (
        <div style={{ textAlign: 'center', padding: '50px 0' }}>
          <Spin size="large" />
        </div>
      ) : filteredRecords.length === 0 ? (
        <Empty description={records.length === 0 ? "暂无诊断记录" : "没有匹配的病例"} />
      ) : (
        <List
          loading={loading}
          dataSource={filteredRecords}
          renderItem={(item) => (
            <List.Item
              actions={[
                <Button
                  key="pin"
                  type="text"
                  icon={item.isPinned ? <PushpinFilled /> : <PushpinOutlined />}
                  onClick={() => handlePin(item.diagnosisId, item.isPinned || false)}
                  style={{ color: item.isPinned ? '#1890ff' : undefined }}
                />,
                <Button
                  key="archive"
                  type="text"
                  icon={<DeleteOutlined />}
                  onClick={() => handleArchive(item.diagnosisId, item.isArchived || false)}
                  danger={!item.isArchived}
                >
                  {item.isArchived ? '取消归档' : '归档'}
                </Button>,
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
                    {item.isPinned && <PushpinFilled style={{ color: '#1890ff' }} />}
                    {item.isArchived && <Tag color="default">已归档</Tag>}
                    <Text strong>{item.chiefComplaint || '未填写主诉'}</Text>
                    {getStatusTag(item.status)}
                    {getWorkModeTag(item.workMode)}
                    {getRiskLevelTag(item.riskLevel)}
                  </Space>
                }
                description={
                  <Space wrap>
                    <Tag>{getTypeText(item.diagnosisType)}</Tag>
                    {item.completeness !== undefined && (
                      <Tag color={item.completeness >= 80 ? 'green' : item.completeness >= 60 ? 'orange' : 'red'}>
                        完整度: {Math.round(item.completeness)}%
                      </Tag>
                    )}
                    {item.tags?.map((tag, idx) => (
                      <Tag key={idx}>{tag}</Tag>
                    ))}
                    <Text type="secondary">{formatDate(item.createdAt)}</Text>
                  </Space>
                }
              />
            </List.Item>
          )}
        />
      )}
    </Drawer>
  )
}

export default CaseHistoryDrawer

