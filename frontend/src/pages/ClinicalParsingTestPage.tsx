import { useState } from 'react'
import {
  Card,
  Input,
  Button,
  Typography,
  Space,
  Tag,
  Divider,
  Alert,
  Spin,
  Row,
  Col,
  Collapse,
  List,
  Badge,
} from 'antd'
import {
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  LoadingOutlined,
} from '@ant-design/icons'
import { clinicalParsingApi } from '@/services/clinicalParsingApi'
import type { ClinicalParsingResponse } from '@/types/clinicalParsing'

const { Title, Text, Paragraph } = Typography
const { TextArea } = Input
const { Panel } = Collapse

// 快速测试用例
const quickTests = [
  {
    name: '症状识别',
    text: '我最近胸口闷，走几步就喘',
    description: '测试症状识别和归一化',
  },
  {
    name: '疾病识别',
    text: '我之前有高血压，现在正在吃阿司匹林',
    description: '测试疾病和药物识别',
  },
  {
    name: '药物识别',
    text: '我正在吃阿司匹林和头孢',
    description: '测试药物识别',
  },
  {
    name: '检查识别',
    text: '我做了血常规和心电图检查',
    description: '测试检查项目识别',
  },
  {
    name: '过敏识别',
    text: '我对青霉素过敏，还有海鲜过敏',
    description: '测试过敏源识别',
  },
  {
    name: '歧义判定',
    text: '我最近胸痛',
    description: '测试歧义表达判定',
  },
  {
    name: '混合识别',
    text: '我最近胸口闷，之前有高血压，正在吃阿司匹林，对青霉素过敏',
    description: '测试多类型概念混合识别',
  },
  {
    name: '属性提取',
    text: '我最近3天轻微头痛，活动后加重',
    description: '测试症状属性提取（持续时间、严重度、诱因）',
  },
]

const ClinicalParsingTestPage = () => {
  const [userId, setUserId] = useState('test_user_001')
  const [sessionId, setSessionId] = useState('test_session_001')
  const [text, setText] = useState('')
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<ClinicalParsingResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  const handleParse = async () => {
    if (!text.trim()) {
      setError('请输入测试文本')
      return
    }

    setLoading(true)
    setError(null)
    setResult(null)

    try {
      const response = await clinicalParsingApi.parse({
        userId,
        sessionId,
        text: text.trim(),
      })

      if (response.code === 200 && response.data) {
        setResult(response.data)
      } else {
        setError(response.message || '解析失败')
      }
    } catch (err: any) {
      setError(err.message || '请求失败，请检查服务是否启动')
    } finally {
      setLoading(false)
    }
  }

  const handleQuickTest = (testText: string) => {
    setText(testText)
  }

  const clearResult = () => {
    setResult(null)
    setError(null)
  }

  return (
    <div style={{ padding: '24px', background: '#f5f5f5', minHeight: '100vh' }}>
      <Card>
        <Title level={2}>病例理解服务 - 功能测试</Title>
        <Paragraph>
          测试病例理解服务的所有功能：概念识别、归一化、结构化提取、歧义判定
        </Paragraph>

        <Divider />

        {/* 输入区域 */}
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <div>
            <Text strong>用户ID:</Text>
            <Input
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              style={{ marginTop: 8 }}
            />
          </div>

          <div>
            <Text strong>会话ID:</Text>
            <Input
              value={sessionId}
              onChange={(e) => setSessionId(e.target.value)}
              style={{ marginTop: 8 }}
            />
          </div>

          <div>
            <Text strong>输入文本:</Text>
            <TextArea
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder="输入患者描述，例如：我最近胸口闷，走几步就喘，之前有高血压"
              rows={5}
              style={{ marginTop: 8 }}
            />
          </div>

          {/* 快速测试按钮 */}
          <div>
            <Text strong>快速测试:</Text>
            <div style={{ marginTop: 8, display: 'flex', flexWrap: 'wrap', gap: 8 }}>
              {quickTests.map((test, index) => (
                <Button
                  key={index}
                  size="small"
                  onClick={() => handleQuickTest(test.text)}
                  title={test.description}
                >
                  {test.name}
                </Button>
              ))}
            </div>
          </div>

          <Space>
            <Button
              type="primary"
              onClick={handleParse}
              loading={loading}
              icon={loading ? <LoadingOutlined /> : <CheckCircleOutlined />}
            >
              开始解析
            </Button>
            <Button onClick={clearResult}>清空结果</Button>
          </Space>
        </Space>

        {/* 错误提示 */}
        {error && (
          <Alert
            message="错误"
            description={error}
            type="error"
            showIcon
            style={{ marginTop: 16 }}
            closable
            onClose={() => setError(null)}
          />
        )}

        {/* 结果显示 */}
        {loading && (
          <div style={{ textAlign: 'center', padding: '40px' }}>
            <Spin size="large" />
            <div style={{ marginTop: 16 }}>
              <Text>正在解析...</Text>
            </div>
          </div>
        )}

        {result && (
          <div style={{ marginTop: 24 }}>
            <Alert
              message="解析成功"
              description={`识别到 ${result.concepts.length} 个概念`}
              type="success"
              showIcon
              style={{ marginBottom: 16 }}
            />

            <Collapse defaultActiveKey={['concepts', 'structured']}>
              {/* 概念列表 */}
              <Panel
                header={
                  <span>
                    识别到的概念{' '}
                    <Badge count={result.concepts.length} showZero color="#52c41a" />
                  </span>
                }
                key="concepts"
              >
                {result.concepts.length > 0 ? (
                  <List
                    dataSource={result.concepts}
                    renderItem={(concept, _index) => (
                      <List.Item>
                        <Space direction="vertical" style={{ width: '100%' }}>
                          <div>
                            <Text strong style={{ color: '#1890ff', fontSize: 16 }}>
                              {concept.originalText}
                            </Text>
                            <Text> → </Text>
                            <Text strong>{concept.normalizedSymptom}</Text>
                          </div>
                          <div>
                            <Tag color="blue">{concept.conceptType}</Tag>
                            {concept.cui && <Tag color="green">CUI: {concept.cui}</Tag>}
                            {concept.icd && <Tag color="orange">ICD: {concept.icd}</Tag>}
                            {concept.atc && <Tag color="purple">ATC: {concept.atc}</Tag>}
                            {concept.loinc && <Tag color="cyan">LOINC: {concept.loinc}</Tag>}
                            <Tag color="default">
                              置信度: {(concept.confidence * 100).toFixed(0)}%
                            </Tag>
                          </div>
                        </Space>
                      </List.Item>
                    )}
                  />
                ) : (
                  <Text type="secondary">未识别到任何概念</Text>
                )}
              </Panel>

              {/* 结构化数据 */}
              <Panel header="结构化数据" key="structured">
                <Row gutter={[16, 16]}>
                  {/* 症状 */}
                  {result.structuredData.symptoms.length > 0 && (
                    <Col xs={24} md={12}>
                      <Card size="small" title={`症状 (${result.structuredData.symptoms.length})`}>
                        <List
                          size="small"
                          dataSource={result.structuredData.symptoms}
                          renderItem={(symptom) => (
                            <List.Item>
                              <Space direction="vertical" style={{ width: '100%' }}>
                                <Text strong>{symptom.name}</Text>
                                <div>
                                  {symptom.duration && (
                                    <Tag color="blue">持续时间: {symptom.duration}</Tag>
                                  )}
                                  {symptom.severity && (
                                    <Tag color="orange">严重度: {symptom.severity}</Tag>
                                  )}
                                  {symptom.trigger && (
                                    <Tag color="green">诱因: {symptom.trigger}</Tag>
                                  )}
                                </div>
                              </Space>
                            </List.Item>
                          )}
                        />
                      </Card>
                    </Col>
                  )}

                  {/* 既往史 */}
                  {result.structuredData.medicalHistory.length > 0 && (
                    <Col xs={24} md={12}>
                      <Card
                        size="small"
                        title={`既往史 (${result.structuredData.medicalHistory.length})`}
                      >
                        <List
                          size="small"
                          dataSource={result.structuredData.medicalHistory}
                          renderItem={(disease) => (
                            <List.Item>
                              <Space direction="vertical" style={{ width: '100%' }}>
                                <Text strong>{disease.disease}</Text>
                                <div>
                                  {disease.icd && <Tag color="orange">ICD: {disease.icd}</Tag>}
                                  <Tag
                                    color={disease.status === 'ongoing' ? 'red' : 'default'}
                                  >
                                    {disease.status === 'ongoing' ? '当前疾病' : '既往史'}
                                  </Tag>
                                </div>
                              </Space>
                            </List.Item>
                          )}
                        />
                      </Card>
                    </Col>
                  )}

                  {/* 药物 */}
                  {result.structuredData.medications.length > 0 && (
                    <Col xs={24} md={12}>
                      <Card
                        size="small"
                        title={`药物 (${result.structuredData.medications.length})`}
                      >
                        <List
                          size="small"
                          dataSource={result.structuredData.medications}
                          renderItem={(med) => (
                            <List.Item>
                              <Space direction="vertical" style={{ width: '100%' }}>
                                <Text strong>{med.name}</Text>
                                <div>
                                  {med.atc && <Tag color="purple">ATC: {med.atc}</Tag>}
                                  <Tag color={med.status === 'current' ? 'red' : 'default'}>
                                    {med.status === 'current' ? '当前用药' : '既往用药'}
                                  </Tag>
                                </div>
                              </Space>
                            </List.Item>
                          )}
                        />
                      </Card>
                    </Col>
                  )}

                  {/* 检查 */}
                  {result.structuredData.examinations.length > 0 && (
                    <Col xs={24} md={12}>
                      <Card
                        size="small"
                        title={`检查 (${result.structuredData.examinations.length})`}
                      >
                        <List
                          size="small"
                          dataSource={result.structuredData.examinations}
                          renderItem={(exam) => (
                            <List.Item>
                              <Space direction="vertical" style={{ width: '100%' }}>
                                <Text strong>{exam.name}</Text>
                                {exam.loinc && <Tag color="cyan">LOINC: {exam.loinc}</Tag>}
                              </Space>
                            </List.Item>
                          )}
                        />
                      </Card>
                    </Col>
                  )}

                  {/* 过敏 */}
                  {result.structuredData.allergies.length > 0 && (
                    <Col xs={24} md={12}>
                      <Card
                        size="small"
                        title={`过敏 (${result.structuredData.allergies.length})`}
                      >
                        <List
                          size="small"
                          dataSource={result.structuredData.allergies}
                          renderItem={(allergy) => (
                            <List.Item>
                              <Text strong>{allergy.allergen}</Text>
                              {allergy.reaction && (
                                <Tag color="red" style={{ marginLeft: 8 }}>
                                  反应: {allergy.reaction}
                                </Tag>
                              )}
                            </List.Item>
                          )}
                        />
                      </Card>
                    </Col>
                  )}
                </Row>
              </Panel>

              {/* 歧义表达 */}
              {result.ambiguousExpressions && result.ambiguousExpressions.length > 0 && (
                <Panel
                  header={
                    <span>
                      <ExclamationCircleOutlined style={{ color: '#faad14' }} /> 歧义表达{' '}
                      <Badge count={result.ambiguousExpressions.length} showZero color="#faad14" />
                    </span>
                  }
                  key="ambiguous"
                >
                  <List
                    dataSource={result.ambiguousExpressions}
                    renderItem={(amb) => (
                      <List.Item>
                        <Space direction="vertical" style={{ width: '100%' }}>
                          <div>
                            <Text strong style={{ color: '#faad14' }}>
                              {amb.text}
                            </Text>
                          </div>
                          <div>
                            <Text type="secondary">建议追问：</Text>
                            <List
                              size="small"
                              dataSource={amb.suggestedQuestions}
                              renderItem={(question, index) => (
                                <List.Item style={{ padding: '8px 0' }}>
                                  {index + 1}. {question}
                                </List.Item>
                              )}
                            />
                          </div>
                        </Space>
                      </List.Item>
                    )}
                  />
                </Panel>
              )}
            </Collapse>
          </div>
        )}
      </Card>
    </div>
  )
}

export default ClinicalParsingTestPage

