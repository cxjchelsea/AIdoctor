import React, { useState, useEffect } from 'react'
import {
  Card,
  Table,
  Tag,
  Button,
  Space,
  Typography,
  Descriptions,
  Tabs,
  Tree,
  Statistic,
  Row,
  Col,
  Alert,
  Spin,
  message,
  Modal,
  Select,
} from 'antd'
import {
  BookOutlined,
  FileTextOutlined,
  ReloadOutlined,
  EyeOutlined,
  EditOutlined,
} from '@ant-design/icons'
import { bookApi } from '@/services/bookApi'
import type { BookInfo, BookStructure, Chapter, TocItem, IndexItem } from '@/types/book'

const { Title, Text } = Typography
const { Option } = Select

/**
 * 书籍管理页面
 */
const BookManagementPage: React.FC = () => {
  const [books, setBooks] = useState<BookInfo[]>([])
  const [loading, setLoading] = useState(false)
  const [selectedBook, setSelectedBook] = useState<BookInfo | null>(null)
  const [structure, setStructure] = useState<BookStructure | null>(null)
  const [loadingStructure, setLoadingStructure] = useState(false)
  const [statusModalVisible, setStatusModalVisible] = useState(false)
  const [updatingBookId, setUpdatingBookId] = useState<string | null>(null)

  // 加载书籍列表
  const loadBooks = async () => {
    setLoading(true)
    try {
      const booksList = await bookApi.getAllBooks()
      setBooks(booksList)
    } catch (error: any) {
      console.error('加载书籍列表失败:', error)
      message.error('加载书籍列表失败: ' + (error.message || '未知错误'))
    } finally {
      setLoading(false)
    }
  }

  // 加载书籍结构
  const loadStructure = async (bookId: string) => {
    setLoadingStructure(true)
    try {
      const structureData = await bookApi.getBookStructure(bookId)
      setStructure(structureData)
    } catch (error: any) {
      console.error('加载书籍结构失败:', error)
      message.error('加载书籍结构失败: ' + (error.message || '未知错误'))
      setStructure(null)
    } finally {
      setLoadingStructure(false)
    }
  }

  // 更新书籍状态
  const updateBookStatus = async (bookId: string, status: '启用' | '停用' | '重跑中') => {
    try {
      await bookApi.updateBookStatus(bookId, status)
      message.success('状态更新成功')
      setStatusModalVisible(false)
      setUpdatingBookId(null)
      loadBooks()
      // 如果当前选中的书籍状态被更新，也更新选中书籍
      if (selectedBook?.book_id === bookId) {
        setSelectedBook({ ...selectedBook, status })
      }
    } catch (error: any) {
      console.error('更新状态失败:', error)
      message.error('更新状态失败: ' + (error.message || '未知错误'))
    }
  }

  // 处理查看书籍详情
  const handleViewBook = (book: BookInfo) => {
    setSelectedBook(book)
    loadStructure(book.book_id)
  }

  // 处理打开状态更新模态框
  const handleOpenStatusModal = (bookId: string) => {
    setUpdatingBookId(bookId)
    setStatusModalVisible(true)
  }

  // 组件挂载时加载书籍列表
  useEffect(() => {
    loadBooks()
  }, [])

  // 构建目录树数据
  const buildTocTree = (tocItems: TocItem[]) => {
    if (!tocItems || tocItems.length === 0) return []
    
    const tree: any[] = []
    const stack: any[] = []
    
    tocItems.forEach((item) => {
      const node = {
        title: `${item.title} (第${item.page}页)`,
        key: `${item.page}-${item.title}`,
        page: item.page,
      }
      
      // 根据层级调整栈
      while (stack.length > 0 && stack[stack.length - 1].level >= item.level) {
        stack.pop()
      }
      
      if (stack.length === 0) {
        tree.push(node)
      } else {
        if (!stack[stack.length - 1].children) {
          stack[stack.length - 1].children = []
        }
        stack[stack.length - 1].children.push(node)
      }
      
      node['level'] = item.level
      stack.push(node)
    })
    
    return tree
  }

  // 提取章节号（用于排序）
  const extractChapterNumber = (title: string): number => {
    // 尝试匹配各种章节号格式
    const patterns = [
      /第[一二三四五六七八九十\d]+章/,
      /第[一二三四五六七八九十\d]+部分/,
      /Chapter\s*(\d+)/i,
      /CHAPTER\s*(\d+)/i,
      /^(\d+)[\.\s]/,
      /^(\d+)[\.\s]/,
    ]
    
    for (const pattern of patterns) {
      const match = title.match(pattern)
      if (match) {
        // 如果是中文数字，转换为阿拉伯数字
        const numStr = match[1] || match[0]
        if (/[一二三四五六七八九十]/.test(numStr)) {
          const chineseNumbers: Record<string, number> = {
            '一': 1, '二': 2, '三': 3, '四': 4, '五': 5,
            '六': 6, '七': 7, '八': 8, '九': 9, '十': 10,
          }
          return chineseNumbers[numStr] || 0
        }
        return parseInt(numStr.replace(/[^\d]/g, '')) || 0
      }
    }
    
    // 如果没找到章节号，返回一个很大的数字，排在后面
    return 9999
  }

  // 构建章节表格数据（按章节号排序）
  const buildChapterTableData = (chapters: Chapter[]) => {
    if (!chapters || chapters.length === 0) return []
    
    return chapters
      .map((chapter, index) => ({
        key: `chapter-${index}`,
        index: index + 1,
        chapterNumber: extractChapterNumber(chapter.title),
        title: chapter.title,
        startPage: chapter.start_page,
        endPage: chapter.end_page,
        pageRange: `${chapter.start_page}-${chapter.end_page}`,
        pageCount: chapter.end_page - chapter.start_page + 1,
        level: chapter.level,
      }))
      .sort((a, b) => {
        // 先按章节号排序
        if (a.chapterNumber !== b.chapterNumber) {
          return a.chapterNumber - b.chapterNumber
        }
        // 如果章节号相同，按页码排序
        return a.startPage - b.startPage
      })
  }

  // 格式化文件大小
  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
  }

  // 获取状态颜色
  const getStatusColor = (status: string) => {
    switch (status) {
      case '启用':
        return 'success'
      case '停用':
        return 'default'
      case '重跑中':
        return 'processing'
      default:
        return 'default'
    }
  }

  // 获取学科领域颜色
  const getFieldColor = (field?: string | null) => {
    if (!field) return 'default'
    const colorMap: Record<string, string> = {
      内科: 'blue',
      外科: 'red',
      儿科: 'green',
      妇科: 'purple',
      全科: 'orange',
      其他: 'default',
    }
    return colorMap[field] || 'default'
  }

  return (
    <div style={{ padding: '24px', background: '#f0f2f5', minHeight: '100vh' }}>
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <Title level={4} style={{ margin: 0 }}>
            <BookOutlined /> 书籍管理
          </Title>
          <Button
            icon={<ReloadOutlined />}
            onClick={loadBooks}
            loading={loading}
          >
            刷新
          </Button>
        </div>

        {!selectedBook ? (
          <Table
            dataSource={books}
            loading={loading}
            rowKey="book_id"
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showTotal: (total) => `共 ${total} 本书籍`,
            }}
            columns={[
              {
                title: '书籍ID',
                dataIndex: 'book_id',
                key: 'book_id',
                width: 250,
                ellipsis: true,
              },
              {
                title: '书名',
                dataIndex: 'name',
                key: 'name',
                width: 200,
                ellipsis: true,
              },
              {
                title: '版本',
                dataIndex: 'version',
                key: 'version',
                width: 120,
              },
              {
                title: '学科领域',
                dataIndex: 'field',
                key: 'field',
                width: 100,
                render: (field: string | null) =>
                  field ? (
                    <Tag color={getFieldColor(field)}>{field}</Tag>
                  ) : (
                    <Text type="secondary">-</Text>
                  ),
              },
              {
                title: '状态',
                dataIndex: 'status',
                key: 'status',
                width: 100,
                render: (status: string) => (
                  <Tag color={getStatusColor(status)}>{status}</Tag>
                ),
              },
              {
                title: '文件大小',
                dataIndex: 'file_size',
                key: 'file_size',
                width: 100,
                render: (size: number) => formatFileSize(size),
              },
              {
                title: '注册时间',
                dataIndex: 'registered_at',
                key: 'registered_at',
                width: 180,
                render: (time: string) => new Date(time).toLocaleString(),
              },
              {
                title: '操作',
                key: 'action',
                width: 200,
                fixed: 'right',
                render: (_: any, record: BookInfo) => (
                  <Space>
                    <Button
                      type="primary"
                      size="small"
                      icon={<EyeOutlined />}
                      onClick={() => handleViewBook(record)}
                    >
                      查看
                    </Button>
                    <Button
                      size="small"
                      icon={<EditOutlined />}
                      onClick={() => handleOpenStatusModal(record.book_id)}
                    >
                      状态
                    </Button>
                  </Space>
                ),
              },
            ]}
          />
        ) : (
          <div>
            <div style={{ marginBottom: 16 }}>
              <Button onClick={() => setSelectedBook(null)}>返回列表</Button>
            </div>

            <Card title="书籍信息" style={{ marginBottom: 16 }}>
              <Descriptions column={2} bordered>
                <Descriptions.Item label="书籍ID">{selectedBook.book_id}</Descriptions.Item>
                <Descriptions.Item label="书名">{selectedBook.name}</Descriptions.Item>
                <Descriptions.Item label="版本">{selectedBook.version}</Descriptions.Item>
                <Descriptions.Item label="ISBN">
                  {selectedBook.isbn || <Text type="secondary">-</Text>}
                </Descriptions.Item>
                <Descriptions.Item label="学科领域">
                  {selectedBook.field ? (
                    <Tag color={getFieldColor(selectedBook.field)}>{selectedBook.field}</Tag>
                  ) : (
                    <Text type="secondary">-</Text>
                  )}
                </Descriptions.Item>
                <Descriptions.Item label="状态">
                  <Tag color={getStatusColor(selectedBook.status)}>{selectedBook.status}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="文件路径" span={2}>
                  <Text code>{selectedBook.pdf_path}</Text>
                </Descriptions.Item>
                <Descriptions.Item label="文件大小">
                  {formatFileSize(selectedBook.file_size)}
                </Descriptions.Item>
                <Descriptions.Item label="注册时间">
                  {new Date(selectedBook.registered_at).toLocaleString()}
                </Descriptions.Item>
                <Descriptions.Item label="更新时间">
                  {new Date(selectedBook.updated_at).toLocaleString()}
                </Descriptions.Item>
              </Descriptions>
            </Card>

            <Spin spinning={loadingStructure}>
              {structure ? (
                <Card title="书籍结构解析">
                  <Row gutter={16} style={{ marginBottom: 16 }}>
                    <Col span={6}>
                      <Statistic
                        title="总页数"
                        value={structure.total_pages}
                        suffix="页"
                      />
                    </Col>
                    <Col span={6}>
                      <Statistic
                        title="章节数"
                        value={structure.summary.chapter_count}
                        suffix="个"
                      />
                    </Col>
                    <Col span={6}>
                      <Statistic
                        title="目录项"
                        value={structure.summary.toc_item_count}
                        suffix="个"
                      />
                    </Col>
                    <Col span={6}>
                      <Statistic
                        title="索引项"
                        value={structure.summary.index_item_count}
                        suffix="个"
                      />
                    </Col>
                  </Row>

                  <Tabs
                    defaultActiveKey="chapters"
                    items={[
                      {
                        key: 'chapters',
                        label: `章节结构 (${structure.chapters.length})`,
                        children: (
                          <Table
                            dataSource={buildChapterTableData(structure.chapters)}
                            rowKey="key"
                            pagination={{ pageSize: 20, showSizeChanger: true, showTotal: (total) => `共 ${total} 个章节` }}
                            columns={[
                              {
                                title: '序号',
                                dataIndex: 'index',
                                key: 'index',
                                width: 80,
                                align: 'center',
                              },
                              {
                                title: '章节标题',
                                dataIndex: 'title',
                                key: 'title',
                                ellipsis: true,
                                render: (text: string) => (
                                  <Text strong={text.includes('CHAPTER') || text.includes('Chapter')}>
                                    {text}
                                  </Text>
                                ),
                              },
                              {
                                title: '起始页',
                                dataIndex: 'startPage',
                                key: 'startPage',
                                width: 100,
                                align: 'center',
                                sorter: (a, b) => a.startPage - b.startPage,
                              },
                              {
                                title: '结束页',
                                dataIndex: 'endPage',
                                key: 'endPage',
                                width: 100,
                                align: 'center',
                                sorter: (a, b) => a.endPage - b.endPage,
                              },
                              {
                                title: '页码范围',
                                dataIndex: 'pageRange',
                                key: 'pageRange',
                                width: 120,
                                align: 'center',
                              },
                              {
                                title: '页数',
                                dataIndex: 'pageCount',
                                key: 'pageCount',
                                width: 80,
                                align: 'center',
                                sorter: (a, b) => a.pageCount - b.pageCount,
                                render: (count: number) => (
                                  <Tag color="blue">{count} 页</Tag>
                                ),
                              },
                              {
                                title: '层级',
                                dataIndex: 'level',
                                key: 'level',
                                width: 80,
                                align: 'center',
                                render: (level: number) => (
                                  <Tag color={level === 1 ? 'red' : level === 2 ? 'orange' : 'green'}>
                                    {level === 1 ? '一级' : level === 2 ? '二级' : '三级'}
                                  </Tag>
                                ),
                              },
                            ]}
                            size="middle"
                          />
                        ),
                      },
                      {
                        key: 'toc',
                        label: `目录 (${structure.toc_structure.length})`,
                        children: structure.toc_structure.length > 0 ? (
                          <Tree
                            treeData={buildTocTree(structure.toc_structure)}
                            defaultExpandAll={false}
                            showLine
                          />
                        ) : (
                          <Alert
                            message="暂无目录信息"
                            description="目录为空可能是因为：1. PDF文件没有标准的目录页；2. 目录页格式不标准，无法自动识别；3. 目录页不在前20页内。您可以手动检查PDF文件的前几页是否有目录。"
                            type="info"
                            showIcon
                          />
                        ),
                      },
                      {
                        key: 'index',
                        label: `索引 (${structure.index_info.length})`,
                        children: structure.index_info.length > 0 ? (
                          <Table
                            dataSource={structure.index_info.map((item, index) => ({ ...item, key: index }))}
                            rowKey="key"
                            pagination={{ pageSize: 20, showSizeChanger: true, showTotal: (total) => `共 ${total} 个索引项` }}
                            columns={[
                              {
                                title: '关键词',
                                dataIndex: 'keyword',
                                key: 'keyword',
                                ellipsis: true,
                              },
                              {
                                title: '页码',
                                dataIndex: 'pages',
                                key: 'pages',
                                render: (pages: number[]) => pages.join(', '),
                              },
                            ]}
                            size="small"
                          />
                        ) : (
                          <Alert
                            message="暂无索引信息"
                            description="索引为空可能是因为：1. PDF文件没有索引页；2. 索引页格式不标准，无法自动识别；3. 索引页不在最后30页内。您可以手动检查PDF文件的最后几页是否有索引。"
                            type="info"
                            showIcon
                          />
                        ),
                      },
                    ]}
                  />
                </Card>
              ) : (
                <Alert
                  message="未找到结构解析结果"
                  description="该书籍可能尚未进行结构解析，请先运行粗解析脚本。"
                  type="warning"
                />
              )}
            </Spin>
          </div>
        )}
      </Card>

      {/* 状态更新模态框 */}
      <Modal
        title="更新书籍状态"
        open={statusModalVisible}
        onOk={() => {
          // 这里需要从表单获取新状态，简化处理
        }}
        onCancel={() => {
          setStatusModalVisible(false)
          setUpdatingBookId(null)
        }}
        footer={null}
      >
        <Space direction="vertical" style={{ width: '100%' }}>
          <Text>选择新状态：</Text>
          <Select
            style={{ width: '100%' }}
            placeholder="选择状态"
            defaultValue={books.find(b => b.book_id === updatingBookId)?.status}
            onChange={(value) => {
              if (updatingBookId) {
                updateBookStatus(updatingBookId, value)
              }
            }}
          >
            <Option value="启用">启用</Option>
            <Option value="停用">停用</Option>
            <Option value="重跑中">重跑中</Option>
          </Select>
        </Space>
      </Modal>
    </div>
  )
}

export default BookManagementPage

