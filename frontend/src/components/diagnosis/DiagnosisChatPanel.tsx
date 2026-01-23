import { useState, useEffect, useRef } from 'react'
import { Card, Input, Button, Space, Avatar, Typography, Spin } from 'antd'
import { SendOutlined, UserOutlined, UploadOutlined } from '@ant-design/icons'
import { useDiagnosisStore } from '@/stores/diagnosisStore'
import DiagnosisResultCard from './DiagnosisResultCard'
import HealthStateAssessmentCard from './HealthStateAssessmentCard'
import WellnessPlanCard from './WellnessPlanCard'
import WellnessScreeningFlow from '@/components/wellness-screening/WellnessScreeningFlow'
import Message from './Message'

const { TextArea } = Input
const { Text } = Typography

interface DiagnosisChatPanelProps {
  onViewResultDetail?: () => void
}

const DiagnosisChatPanel: React.FC<DiagnosisChatPanelProps> = ({ onViewResultDetail }) => {
  const { messages, sendMessage, status, healthAssessmentDone, diagnosisId, workMode } = useDiagnosisStore()
  const [inputValue, setInputValue] = useState('')
  const [loading, setLoading] = useState(false)
  const [showWellnessScreening, setShowWellnessScreening] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  const handleSend = async () => {
    if (!inputValue.trim() || loading) return

    const message = inputValue.trim()
    setInputValue('')
    setLoading(true)

    try {
      await sendMessage(message)
    } catch (error) {
      console.error('发送消息失败:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleOptionClick = async (option: string) => {
    if (loading) return
    setLoading(true)
    
    try {
      // 直接发送选项内容
      await sendMessage(option)
    } catch (error) {
      console.error('发送选项失败:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  // 处理文件上传
  const handleFileUpload = async (file: File) => {
    // TODO: 实现文件上传逻辑
    console.log('上传文件:', file)
    // 这里可以调用OCR服务
  }

  // 健康筛查流程完成回调
  const handleWellnessScreeningComplete = () => {
    setShowWellnessScreening(false)
  }

  return (
    <Card
      style={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        borderRadius: '12px',
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
      }}
      styles={{
        body: {
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        padding: '16px',
        overflow: 'hidden',
        },
      }}
    >
      {/* 消息列表 */}
      <div
        style={{
          flex: 1,
          overflowY: 'auto',
          marginBottom: '16px',
          padding: '8px',
        }}
      >
        {messages.length === 0 ? (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              height: '100%',
              color: '#999',
            }}
          >
            <Text type="secondary">开始对话，描述您的问题或需求...</Text>
          </div>
        ) : (
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            {messages.map((msg) => {
              // 诊断结果卡片
              if (msg.type === 'result' && msg.result) {
                return (
                  <div key={msg.id} style={{ margin: '24px 0' }}>
                    <DiagnosisResultCard
                      result={msg.result}
                      onViewDetail={onViewResultDetail}
                      onSave={() => {
                        // TODO: 保存诊断报告
                      }}
                      onShare={() => {
                        // TODO: 分享诊断结果
                      }}
                    />
                  </div>
                )
              }

              // 健康状态判定卡片 - 系统判断结果（重），视觉上更突出
              if (msg.type === 'assessment' && msg.assessment) {
                return (
                  <div 
                    key={msg.id} 
                    style={{ 
                      margin: '32px 0',
                      padding: '16px',
                      backgroundColor: '#fafafa',
                      borderRadius: '12px',
                      border: '1px solid #e8e8e8',
                    }}
                  >
                    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                      <HealthStateAssessmentCard result={msg.assessment} />
                      {msg.assessment.workMode === 'wellness_mode' && msg.assessment.wellnessPlan && (
                        <WellnessPlanCard plan={msg.assessment.wellnessPlan} />
                      )}
                      {/* 健康筛查流程已在后端自动执行完成，不需要显示"开始健康筛查流程"按钮 */}
                      {/* 如果wellnessPlan中包含A1-A5的结果，说明流程已完成 */}
                    </Space>
                  </div>
                )
              }

              // 普通消息 - 对话流（轻）
              // 只显示user、system、question类型的消息
              if (msg.type === 'user' || msg.type === 'system' || msg.type === 'question') {
                return (
                  <div key={msg.id} style={{ opacity: msg.type === 'system' ? 0.85 : 1 }}>
                    <Message
                      type={msg.type === 'question' ? 'question' : msg.type}
                      content={msg.content}
                      timestamp={msg.timestamp}
                      question={msg.question}
                      highlight={msg.type === 'question'}
                      onOptionClick={handleOptionClick}
                    />
                  </div>
                )
              }
              
              // 其他类型的消息（wellness等）不在这里显示
              return null
            })}
            {loading && (
              <div style={{ display: 'flex', justifyContent: 'flex-start', gap: '8px', alignItems: 'flex-start' }}>
                <Avatar icon={<UserOutlined />} style={{ backgroundColor: '#1890ff' }} />
                <Card
                  style={{
                    backgroundColor: '#f5f5f5',
                    border: 'none',
                    borderRadius: 8,
                  }}
                  styles={{ body: { padding: '12px' } }}
                >
                  <Spin size="small" />
                </Card>
              </div>
            )}
            <div ref={messagesEndRef} />
          </Space>
        )}

        {/* 健康筛查流程（健康管理态时显示） */}
        {showWellnessScreening && workMode === 'wellness_mode' && (
          <div style={{ marginTop: 24 }}>
            <WellnessScreeningFlow onComplete={handleWellnessScreeningComplete} />
          </div>
        )}
      </div>

      {/* 输入区域 */}
      <div style={{ borderTop: '1px solid #f0f0f0', paddingTop: '16px' }}>
        {/* 引导文本：在输入框上方 - 仅在健康状态评估完成后且未开始结构化提问时显示 */}
        {healthAssessmentDone && !diagnosisId && messages.length > 0 && status !== 'analyzing' && (
          <div style={{ marginBottom: '12px' }}>
            <Text type="secondary" style={{ fontSize: 13, color: '#8c8c8c' }}>
              为进一步判断风险，请补充以下信息之一：持续时间 / 是否放射 / 是否伴随呼吸困难
            </Text>
          </div>
        )}
        {/* 文件上传输入（隐藏） */}
        <input
          type="file"
          ref={fileInputRef}
          style={{ display: 'none' }}
          accept="image/*,.pdf"
          onChange={(e) => {
            const file = e.target.files?.[0]
            if (file) {
              handleFileUpload(file)
            }
          }}
        />
        <Space.Compact style={{ width: '100%' }}>
          <Button
            type="text"
            icon={<UploadOutlined />}
            onClick={() => fileInputRef.current?.click()}
            disabled={loading || status === 'analyzing'}
            title="上传报告"
          />
          <TextArea
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="请描述您的问题或需求..."
            autoSize={{ minRows: 1, maxRows: 4 }}
            disabled={loading || status === 'analyzing'}
            style={{ resize: 'none' }}
          />
          <Button
            type="primary"
            icon={<SendOutlined />}
            onClick={handleSend}
            loading={loading}
            disabled={!inputValue.trim() || status === 'analyzing'}
            style={{ height: 'auto' }}
          >
            发送
          </Button>
        </Space.Compact>
      </div>
    </Card>
  )
}

export default DiagnosisChatPanel
