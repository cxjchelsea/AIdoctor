import { Avatar, Button, Space, Typography, Card } from 'antd'
import { MedicineBoxOutlined } from '@ant-design/icons'
import type { Question } from '@/types/diagnosis'

const { Text } = Typography

interface MessageProps {
  type: 'user' | 'system' | 'question'
  content: string
  timestamp: Date
  question?: Question
  highlight?: boolean
  onOptionClick?: (option: string) => void
}

const Message: React.FC<MessageProps> = ({
  type,
  content,
  timestamp,
  question,
  highlight,
  onOptionClick,
}) => {
  const isUser = type === 'user'

  const formatTime = (date: Date) => {
    return date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  return (
    <div
      className={`message message-${type} ${highlight ? 'message-highlight' : ''}`}
      style={{
        display: 'flex',
        justifyContent: isUser ? 'flex-end' : 'flex-start',
        marginBottom: 16,
        padding: '0 8px',
      }}
    >
      {!isUser && (
        <Avatar
          icon={<MedicineBoxOutlined />}
          style={{ marginRight: 8, backgroundColor: '#1890ff' }}
        />
      )}

      <div
        style={{
          maxWidth: '70%',
          display: 'flex',
          flexDirection: 'column',
          alignItems: isUser ? 'flex-end' : 'flex-start',
        }}
      >
        <Card
          style={{
            padding: '12px 16px',
            borderRadius: 8,
            backgroundColor: isUser ? '#1890ff' : highlight ? '#e6f7ff' : '#fff',
            color: isUser ? '#fff' : '#000',
            boxShadow: '0 1px 2px rgba(0,0,0,0.1)',
            border: highlight ? '2px solid #1890ff' : '1px solid #e8e8e8',
          }}
          styles={{ body: { padding: 0 } }}
        >
          <Text style={{ color: isUser ? '#fff' : 'inherit' }}>{content}</Text>

          {question && question.options && question.options.length > 0 && (
            <Space
              style={{
                marginTop: 8,
                display: 'flex',
                flexWrap: 'wrap',
                width: '100%',
              }}
              size="small"
            >
              {question.options.map((option) => (
                <Button
                  key={option}
                  size="small"
                  type={isUser ? 'default' : 'primary'}
                  ghost={isUser}
                  onClick={() => onOptionClick?.(option)}
                  style={{
                    borderRadius: 16,
                    fontSize: 12,
                  }}
                >
                  {option}
                </Button>
              ))}
            </Space>
          )}
        </Card>

        <Text
          type="secondary"
          style={{ fontSize: 12, marginTop: 4, color: '#8c8c8c' }}
        >
          {formatTime(timestamp)}
        </Text>
      </div>

      {isUser && (
        <Avatar
          style={{ marginLeft: 8, backgroundColor: '#87d068' }}
        >
          我
        </Avatar>
      )}
    </div>
  )
}

export default Message
