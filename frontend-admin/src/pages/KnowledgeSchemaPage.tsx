import React, { useState, useEffect } from 'react'
import {
  Card,
  Button,
  Space,
  Typography,
  Tag,
  Alert,
  Statistic,
  Row,
  Col,
  message,
  Table,
  Descriptions,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  ReloadOutlined,
  DatabaseOutlined,
  SettingOutlined,
  CheckOutlined,
} from '@ant-design/icons'
import { knowledgeApi } from '@/services/knowledgeApi'
import type { SchemaSetupResult, SchemaValidationResult, ConstraintDetail, IndexDetail } from '@/types/knowledge'

const { Title, Text } = Typography

/**
 * 知识图谱结构管理页面
 */
const KnowledgeSchemaPage: React.FC = () => {
  const [loading, setLoading] = useState(false)
  const [validating, setValidating] = useState(false)
  const [loadingConstraints, setLoadingConstraints] = useState(false)
  const [loadingIndexes, setLoadingIndexes] = useState(false)
  const [setupResult, setSetupResult] = useState<SchemaSetupResult | null>(null)
  const [validationResult, setValidationResult] = useState<SchemaValidationResult | null>(null)
  const [allConstraints, setAllConstraints] = useState<ConstraintDetail[]>([])
  const [allIndexes, setAllIndexes] = useState<IndexDetail[]>([])

  // 加载验证结果
  const loadValidation = async () => {
    setValidating(true)
    try {
      const result = await knowledgeApi.validateSchema()
      setValidationResult(result)
    } catch (error: any) {
      message.error(`验证失败: ${error.message || '未知错误'}`)
    } finally {
      setValidating(false)
    }
  }

  // 搭建结构
  const handleSetup = async () => {
    setLoading(true)
    try {
      const result = await knowledgeApi.setupSchema()
      setSetupResult(result)
      
      if (result.errors.length === 0) {
        message.success('结构搭建成功！')
      } else {
        message.warning(`结构搭建完成，但有 ${result.errors.length} 个错误`)
      }
      
      // 自动刷新验证结果和列表
      await loadValidation()
      await loadAllConstraints()
      await loadAllIndexes()
    } catch (error: any) {
      message.error(`搭建失败: ${error.message || '未知错误'}`)
    } finally {
      setLoading(false)
    }
  }

  // 加载所有约束
  const loadAllConstraints = async () => {
    setLoadingConstraints(true)
    try {
      const constraints = await knowledgeApi.getAllConstraints()
      setAllConstraints(constraints)
    } catch (error: any) {
      message.error(`加载约束失败: ${error.message || '未知错误'}`)
    } finally {
      setLoadingConstraints(false)
    }
  }

  // 加载所有索引
  const loadAllIndexes = async () => {
    setLoadingIndexes(true)
    try {
      const indexes = await knowledgeApi.getAllIndexes()
      setAllIndexes(indexes)
    } catch (error: any) {
      message.error(`加载索引失败: ${error.message || '未知错误'}`)
    } finally {
      setLoadingIndexes(false)
    }
  }

  // 组件加载时自动验证和加载
  useEffect(() => {
    loadValidation()
    loadAllConstraints()
    loadAllIndexes()
  }, [])

  // 所有约束表格列
  const allConstraintColumns = [
    {
      title: '约束名称',
      dataIndex: 'name',
      key: 'name',
      width: 250,
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 150,
      render: (type: string) => <Tag>{type}</Tag>,
    },
    {
      title: '实体类型',
      dataIndex: 'entityType',
      key: 'entityType',
      width: 200,
    },
    {
      title: '属性',
      dataIndex: 'properties',
      key: 'properties',
      render: (properties: string[]) => properties.join(', '),
    },
    {
      title: '说明',
      dataIndex: 'description',
      key: 'description',
    },
  ]

  // 所有索引表格列
  const allIndexColumns = [
    {
      title: '索引名称',
      dataIndex: 'name',
      key: 'name',
      width: 250,
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 150,
      render: (type: string) => <Tag>{type}</Tag>,
    },
    {
      title: '实体类型',
      dataIndex: 'entityType',
      key: 'entityType',
      width: 200,
    },
    {
      title: '属性',
      dataIndex: 'properties',
      key: 'properties',
      render: (properties: string[]) => properties.join(', '),
    },
    {
      title: '状态',
      dataIndex: 'state',
      key: 'state',
      width: 100,
      render: (state: string) => {
        if (state === 'ONLINE') {
          return <Tag color="success">在线</Tag>
        }
        return <Tag>{state}</Tag>
      },
    },
    {
      title: '说明',
      dataIndex: 'description',
      key: 'description',
    },
  ]

  type SchemaNameRow = { key: string; name: string }

  // 搭建结果中的约束表格列（保留原有功能）
  const constraintColumns: ColumnsType<SchemaNameRow> = [
    {
      title: '约束名称',
      dataIndex: 'name',
      key: 'name',
      width: 250,
    },
    {
      title: '状态',
      key: 'status',
      width: 100,
      render: (_: any, record) => {
        const isCreated = setupResult?.constraints_created.includes(record.name) || false
        const isFailed = setupResult?.constraints_failed.includes(record.name) || false
        if (isCreated) {
          return <Tag color="success" icon={<CheckCircleOutlined />}>已创建</Tag>
        }
        if (isFailed) {
          return <Tag color="error" icon={<CloseCircleOutlined />}>失败</Tag>
        }
        return <Tag>未知</Tag>
      },
    },
  ]

  // 搭建结果中的索引表格列（保留原有功能）
  const indexColumns: ColumnsType<SchemaNameRow> = [
    {
      title: '索引名称',
      dataIndex: 'name',
      key: 'name',
      width: 250,
    },
    {
      title: '状态',
      key: 'status',
      width: 100,
      render: (_: any, record) => {
        const isCreated = setupResult?.indexes_created.includes(record.name) || false
        const isFailed = setupResult?.indexes_failed.includes(record.name) || false
        if (isCreated) {
          return <Tag color="success" icon={<CheckCircleOutlined />}>已创建</Tag>
        }
        if (isFailed) {
          return <Tag color="error" icon={<CloseCircleOutlined />}>失败</Tag>
        }
        return <Tag>未知</Tag>
      },
    },
  ]

  return (
    <div style={{ padding: '24px' }}>
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        {/* 页面标题 */}
        <Card>
          <Space>
            <DatabaseOutlined style={{ fontSize: 24, color: '#1890ff' }} />
            <Title level={3} style={{ margin: 0 }}>知识图谱结构管理</Title>
          </Space>
        </Card>
          {/* 操作区域 */}
          <Card>
            <Space size="large">
              <Button
                type="primary"
                icon={<SettingOutlined />}
                loading={loading}
                onClick={handleSetup}
                size="large"
              >
                搭建结构
              </Button>
              <Button
                icon={<CheckOutlined />}
                loading={validating}
                onClick={loadValidation}
                size="large"
              >
                验证结构
              </Button>
              <Button
                icon={<ReloadOutlined />}
                onClick={() => {
                  setSetupResult(null)
                  loadValidation()
                  loadAllConstraints()
                  loadAllIndexes()
                }}
                size="large"
              >
                刷新
              </Button>
            </Space>
          </Card>

          {/* 验证结果统计 */}
          {validationResult && (
            <Card title="结构验证结果">
              <Row gutter={16}>
                <Col span={6}>
                  <Statistic
                    title="约束数量"
                    value={validationResult.constraints_count}
                    prefix={<DatabaseOutlined />}
                  />
                </Col>
                <Col span={6}>
                  <Statistic
                    title="索引数量"
                    value={validationResult.indexes_count}
                    prefix={<DatabaseOutlined />}
                  />
                </Col>
                <Col span={6}>
                  <Statistic
                    title="验证状态"
                    value={validationResult.is_valid ? '通过' : '未通过'}
                    valueStyle={{ color: validationResult.is_valid ? '#3f8600' : '#cf1322' }}
                    prefix={validationResult.is_valid ? <CheckCircleOutlined /> : <CloseCircleOutlined />}
                  />
                </Col>
                <Col span={6}>
                  <Statistic
                    title="缺失约束"
                    value={validationResult.missing_constraints.length}
                    valueStyle={{ color: validationResult.missing_constraints.length > 0 ? '#cf1322' : '#3f8600' }}
                  />
                </Col>
              </Row>

              {validationResult.missing_constraints.length > 0 && (
                <Alert
                  message="缺少约束"
                  description={
                    <Space direction="vertical" size="small">
                      {validationResult.missing_constraints.map((name, index) => (
                        <Text key={index} code>{name}</Text>
                      ))}
                    </Space>
                  }
                  type="warning"
                  style={{ marginTop: 16 }}
                />
              )}

              {validationResult.error && (
                <Alert
                  message="验证错误"
                  description={validationResult.error}
                  type="error"
                  style={{ marginTop: 16 }}
                />
              )}
            </Card>
          )}

          {/* 所有约束列表 */}
          <Card 
            title={
              <Space>
                <span>所有约束列表</span>
                <Tag color="blue">{allConstraints.length} 个</Tag>
              </Space>
            }
            extra={
              <Button 
                size="small" 
                icon={<ReloadOutlined />} 
                onClick={loadAllConstraints}
                loading={loadingConstraints}
              >
                刷新
              </Button>
            }
          >
            <Table
              columns={allConstraintColumns}
              dataSource={allConstraints}
              rowKey="name"
              pagination={{ pageSize: 10 }}
              loading={loadingConstraints}
              size="small"
            />
          </Card>

          {/* 所有索引列表 */}
          <Card 
            title={
              <Space>
                <span>所有索引列表</span>
                <Tag color="blue">{allIndexes.length} 个</Tag>
              </Space>
            }
            extra={
              <Button 
                size="small" 
                icon={<ReloadOutlined />} 
                onClick={loadAllIndexes}
                loading={loadingIndexes}
              >
                刷新
              </Button>
            }
          >
            <Table
              columns={allIndexColumns}
              dataSource={allIndexes}
              rowKey="name"
              pagination={{ pageSize: 10 }}
              loading={loadingIndexes}
              size="small"
            />
          </Card>

          {/* 搭建结果 */}
          {setupResult && (
            <>
              <Card title="搭建结果统计">
                <Row gutter={16}>
                  <Col span={6}>
                    <Statistic
                      title="成功创建约束"
                      value={setupResult.constraints_created.length}
                      valueStyle={{ color: '#3f8600' }}
                      prefix={<CheckCircleOutlined />}
                    />
                  </Col>
                  <Col span={6}>
                    <Statistic
                      title="约束创建失败"
                      value={setupResult.constraints_failed.length}
                      valueStyle={{ color: setupResult.constraints_failed.length > 0 ? '#cf1322' : '#3f8600' }}
                      prefix={<CloseCircleOutlined />}
                    />
                  </Col>
                  <Col span={6}>
                    <Statistic
                      title="成功创建索引"
                      value={setupResult.indexes_created.length}
                      valueStyle={{ color: '#3f8600' }}
                      prefix={<CheckCircleOutlined />}
                    />
                  </Col>
                  <Col span={6}>
                    <Statistic
                      title="索引创建失败"
                      value={setupResult.indexes_failed.length}
                      valueStyle={{ color: setupResult.indexes_failed.length > 0 ? '#cf1322' : '#3f8600' }}
                      prefix={<CloseCircleOutlined />}
                    />
                  </Col>
                </Row>
              </Card>

              {/* 约束列表 */}
              <Card title="约束列表">
                <Table
                  columns={constraintColumns}
                  dataSource={[
                    ...setupResult.constraints_created.map(name => ({ key: name, name })),
                    ...setupResult.constraints_failed.map(name => ({ key: name, name })),
                  ]}
                  pagination={false}
                  size="small"
                />
              </Card>

              {/* 索引列表 */}
              <Card title="索引列表">
                <Table
                  columns={indexColumns}
                  dataSource={[
                    ...setupResult.indexes_created.map(name => ({ key: name, name })),
                    ...setupResult.indexes_failed.map(name => ({ key: name, name })),
                  ]}
                  pagination={false}
                  size="small"
                />
              </Card>

              {/* 错误信息 */}
              {setupResult.errors.length > 0 && (
                <Card title="错误信息" style={{ borderColor: '#ff4d4f' }}>
                  <Alert
                    message={`共 ${setupResult.errors.length} 个错误`}
                    type="error"
                    description={
                      <Space direction="vertical" size="small" style={{ marginTop: 8 }}>
                        {setupResult.errors.map((error, index) => (
                          <Text key={index} type="danger" style={{ display: 'block' }}>
                            {error}
                          </Text>
                        ))}
                      </Space>
                    }
                  />
                </Card>
              )}
            </>
          )}

          {/* 帮助信息 */}
          <Card title="使用说明">
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="搭建结构">
                创建所有约束和索引。如果约束或索引已存在，会被跳过（使用 IF NOT EXISTS）。
              </Descriptions.Item>
              <Descriptions.Item label="验证结构">
                验证当前知识图谱的结构是否完整，包括约束和索引的数量。
              </Descriptions.Item>
              <Descriptions.Item label="约束">
                包括 display_id 唯一性约束（9个）和 uuid 唯一性约束（8个），共17个。
              </Descriptions.Item>
              <Descriptions.Item label="索引">
                包括常用查询字段的索引，如 name、category、icd10_code 等，共9个。
              </Descriptions.Item>
            </Descriptions>
          </Card>
        </Space>
      </div>
  )
}

export default KnowledgeSchemaPage

