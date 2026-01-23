import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Card, Typography, Button, Space, Spin, message } from 'antd'
import {
  ArrowLeftOutlined,
  SaveOutlined,
  ShareAltOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import { diagnosisApi } from '@/services/diagnosisApi'
import type { DiagnosisResult } from '@/types/diagnosis'
import DiseasePossibilityCard from '@/components/diagnosis/DiseasePossibilityCard'
import ExaminationSuggestion from '@/components/diagnosis/ExaminationSuggestion'
import MedicalAdvice from '@/components/diagnosis/MedicalAdvice'

const { Title, Text } = Typography

const DiagnosisResultPage = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [result, setResult] = useState<DiagnosisResult | null>(null)

  useEffect(() => {
    if (id) {
      loadResult()
    }
  }, [id])

  const loadResult = async () => {
    try {
      setLoading(true)
      const response = await diagnosisApi.getResult(id!)
      setResult(response.data)
    } catch (error) {
      console.error('加载诊断结果失败:', error)
      message.error('加载诊断结果失败')
    } finally {
      setLoading(false)
    }
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

  const handleSave = () => {
    message.success('报告已保存')
    // TODO: 实现保存功能
  }

  const handleShare = () => {
    // TODO: 实现分享功能
    message.info('分享功能开发中')
  }

  const handleRediagnosis = () => {
    navigate('/')
  }

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '100px 0' }}>
        <Spin size="large" />
        <div style={{ marginTop: 16 }}>
          <Text type="secondary">加载诊断结果中...</Text>
        </div>
      </div>
    )
  }

  if (!result) {
    return (
      <Card>
        <Title level={2}>诊断结果</Title>
        <Text type="secondary">未找到诊断结果</Text>
      </Card>
    )
  }

  return (
    <div>
      {/* 返回按钮 */}
      <Button
        icon={<ArrowLeftOutlined />}
        onClick={() => navigate('/')}
        style={{ marginBottom: 16 }}
      >
        返回
      </Button>

      {/* 结果概览卡片 */}
      <Card style={{ marginBottom: 16 }}>
        <Title level={3}>诊断结果</Title>
        <Text>
          根据您的症状和健康档案，我考虑以下几个可能的方向：
        </Text>
        <div style={{ marginTop: 16 }}>
          <Text type="secondary">
            诊断时间：{formatDate(result.createdAt)}
          </Text>
          {result.completedAt && (
            <>
              <Text type="secondary" style={{ marginLeft: 16 }}>
                完成时间：{formatDate(result.completedAt)}
              </Text>
            </>
          )}
        </div>
      </Card>

      {/* 可能性列表 */}
      {result.possibilities && result.possibilities.length > 0 && (
        <Card title="可能性分析" style={{ marginBottom: 16 }}>
          {result.possibilities.map((possibility, index) => (
            <DiseasePossibilityCard key={index} possibility={possibility} />
          ))}
        </Card>
      )}

      {/* 建议检查 */}
      {result.examinationSuggestions && (
        <ExaminationSuggestion
          priorityExaminations={result.examinationSuggestions.priorityExaminations}
          optionalExaminations={result.examinationSuggestions.optionalExaminations}
          explanation={result.examinationSuggestions.explanation}
        />
      )}

      {/* 就医建议 */}
      {result.medicalAdvice && (
        <MedicalAdvice advice={result.medicalAdvice} />
      )}

      {/* 操作按钮 */}
      <Card>
        <Space>
          <Button
            type="primary"
            icon={<SaveOutlined />}
            onClick={handleSave}
          >
            保存报告
          </Button>
          <Button
            icon={<ShareAltOutlined />}
            onClick={handleShare}
          >
            分享
          </Button>
          <Button
            icon={<ReloadOutlined />}
            onClick={handleRediagnosis}
          >
            重新诊断
          </Button>
        </Space>
      </Card>
    </div>
  )
}

export default DiagnosisResultPage
