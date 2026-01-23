import { Progress, Typography } from 'antd'

interface CompletenessProgressProps {
  completeness: number // 0-1
  status: 'collecting' | 'questioning' | 'analyzing' | 'completed'
}

const CompletenessProgress: React.FC<CompletenessProgressProps> = ({
  completeness,
  status,
}) => {
  const getStatus = (): 'exception' | 'active' | 'success' => {
    if (completeness < 0.6) return 'exception'
    if (completeness < 0.7) return 'active'
    if (completeness < 0.8) return 'active'
    return 'success'
  }

  const getText = (): string => {
    if (completeness < 0.6) return '信息不足，需要补充更多信息（最低要求为60%才能进行诊断分析）'
    if (completeness < 0.7) return '信息基本完整，建议继续补充（达到60%最低要求，建议补充到70%停止追问）'
    if (completeness < 0.8) return '信息完整，可以开始分析（达到停止追问阈值，可以开始分析）'
    return '信息完整，诊断准确性更高（理想状态）'
  }

  const getStrokeColor = () => {
    if (completeness < 0.6) return '#ff4d4f'
    if (completeness < 0.7) return '#faad14'
    if (completeness < 0.8) return '#faad14'
    return '#52c41a'
  }

  return (
    <div style={{ padding: '16px', backgroundColor: '#fafafa', borderTop: '1px solid #e8e8e8' }}>
      <Progress
        percent={Math.round(completeness * 100)}
        status={getStatus()}
        strokeColor={getStrokeColor()}
        format={(percent) => `${percent}%`}
        style={{ marginBottom: 8 }}
      />
      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
        {getText()}
      </Typography.Text>
    </div>
  )
}

export default CompletenessProgress

