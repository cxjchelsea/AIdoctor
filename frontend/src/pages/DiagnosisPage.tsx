import { useState, useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Row, Col, Drawer, Button, Typography } from 'antd'
import { MenuOutlined } from '@ant-design/icons'
import DiagnosisChatPanel from '@/components/diagnosis/DiagnosisChatPanel'
import DiagnosisInfoPanel from '@/components/diagnosis/DiagnosisInfoPanel'
import HealthProfileDrawer from '@/components/diagnosis/HealthProfileDrawer'
import CaseHistoryDrawer from '@/components/diagnosis/CaseHistoryDrawer'
import DiagnosisResultDetail from '@/components/diagnosis/DiagnosisResultDetail'
import StructuredIntakePanel from '@/components/diagnosis/StructuredIntakePanel'
import { useDiagnosisStore } from '@/stores/diagnosisStore'

const { Title } = Typography

const DiagnosisPage = () => {
  const location = useLocation()
  const navigate = useNavigate()
  const [drawerVisible, setDrawerVisible] = useState(false)
  const [profileDrawerVisible, setProfileDrawerVisible] = useState(false)
  const [caseDrawerVisible, setCaseDrawerVisible] = useState(false)
  const [resultDetailDrawerVisible, setResultDetailDrawerVisible] = useState(false)
  const [structuredIntakeVisible, setStructuredIntakeVisible] = useState(false)
  const [isMobile, setIsMobile] = useState(false)
  const { diagnosisResult, setWorkMode, addSystemMessage, healthAssessmentDone: _healthAssessmentDone, workMode: _workMode, cdpId: _cdpId } = useDiagnosisStore()

  // 从路由 state 获取工作态信息并设置到 store
  useEffect(() => {
    if (location.state) {
      const state = location.state as {
        workMode?: 'wellness_mode' | 'clinical_mode'
        cdpId?: string
        skipAssessment?: boolean
      }
      
      if (state.workMode) {
        setWorkMode(state.workMode, state.cdpId)
      } else if (state.skipAssessment) {
        // 如果跳过了健康状态判定，设置默认工作态（临床诊疗态）
        setWorkMode('clinical_mode')
        console.info('已跳过健康状态判定，使用默认临床诊疗态')
      }
    }
  }, [location.state, setWorkMode])

  useEffect(() => {
    const checkMobile = () => {
      setIsMobile(window.innerWidth < 768)
    }
    checkMobile()
    window.addEventListener('resize', checkMobile)
    return () => window.removeEventListener('resize', checkMobile)
  }, [])

  // 初始化欢迎消息（只在首次加载且消息列表为空时）
  useEffect(() => {
    const { messages: currentMessages } = useDiagnosisStore.getState()
    if (currentMessages.length === 0) {
      addSystemMessage('您好，我是智能诊断助手。请描述您的问题或需求，我会根据您提供的信息为您提供帮助。')
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []) // 只在组件挂载时执行一次

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* 标题栏（无导航栏） */}
      <div
        style={{
          background: '#fff',
          padding: '16px 24px',
          borderBottom: '1px solid #f0f0f0',
        }}
      >
        <Title level={3} style={{ margin: 0 }}>
          智能诊断系统
        </Title>
      </div>

      {/* 主体内容 */}
      <div style={{ flex: 1, padding: '24px', background: '#f5f5f5' }}>
        {/* 统一对话界面：入口判定在对话中无感完成 */}
        <Row gutter={[24, 24]} style={{ height: '100%', minHeight: 'calc(100vh - 120px)' }}>
          {/* 左侧：对话区域 */}
          <Col xs={24} md={16} lg={17}>
            <div style={{ position: 'relative', height: '100%' }}>
              {/* 手机端显示信息面板按钮 */}
              {isMobile && (
                <Button
                  type="primary"
                  icon={<MenuOutlined />}
                  onClick={() => setDrawerVisible(true)}
                  style={{
                    position: 'absolute',
                    top: 16,
                    right: 16,
                    zIndex: 10,
                    borderRadius: '20px',
                    boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
                  }}
                >
                  信息面板
                </Button>
              )}
              <DiagnosisChatPanel onViewResultDetail={() => setResultDetailDrawerVisible(true)} />
            </div>
          </Col>
          {/* 右侧：信息面板（PC和平板端显示） */}
          <Col xs={0} md={8} lg={7}>
            <DiagnosisInfoPanel
              onStartNewDiagnosis={() => {
                // 开始新诊断的逻辑已在store中处理
              }}
              onViewCases={() => setCaseDrawerVisible(true)}
              onViewProfile={() => setProfileDrawerVisible(true)}
              onOpenStructuredIntake={() => setStructuredIntakeVisible(true)}
              onViewCDP={(cdpId) => {
                navigate(`/cdp/${cdpId}`)
              }}
            />
          </Col>
        </Row>
      </div>

      {/* 手机端：信息面板抽屉 */}
      <Drawer
        title="诊断信息"
        placement="right"
        onClose={() => setDrawerVisible(false)}
        open={drawerVisible}
        width={320}
        getContainer={false}
      >
        <DiagnosisInfoPanel
          onStartNewDiagnosis={() => {
            setDrawerVisible(false)
          }}
          onViewCases={() => {
            setDrawerVisible(false)
            setCaseDrawerVisible(true)
          }}
          onViewProfile={() => {
            setDrawerVisible(false)
            setProfileDrawerVisible(true)
          }}
          onOpenStructuredIntake={() => {
            setDrawerVisible(false)
            setStructuredIntakeVisible(true)
          }}
          onViewCDP={(cdpId) => {
            setDrawerVisible(false)
            navigate(`/cdp/${cdpId}`)
          }}
        />
      </Drawer>

      {/* 健康档案抽屉 */}
      <HealthProfileDrawer
        visible={profileDrawerVisible}
        onClose={() => setProfileDrawerVisible(false)}
        onEdit={() => {
          // TODO: 打开编辑健康档案的对话框
        }}
      />

      {/* 病例历史抽屉 */}
      <CaseHistoryDrawer
        visible={caseDrawerVisible}
        onClose={() => setCaseDrawerVisible(false)}
        onViewCase={(_diagnosisId: string, status: string) => {
          // TODO: 根据状态处理查看病例或继续诊断
          if (status === 'completed') {
            // 查看诊断结果
            // 可以在信息面板中展示结果，或加载到对话中
          } else {
            // 继续诊断
            // 恢复诊断状态
          }
        }}
      />

      {/* 诊断结果详情抽屉 */}
      {diagnosisResult && (
        <Drawer
          title="诊断结果详情"
          placement="right"
          onClose={() => setResultDetailDrawerVisible(false)}
          open={resultDetailDrawerVisible}
          width={isMobile ? '100%' : 600}
          getContainer={false}
        >
          <DiagnosisResultDetail result={diagnosisResult} />
        </Drawer>
      )}

      {/* 结构化采集面板 */}
      <StructuredIntakePanel
        visible={structuredIntakeVisible}
        onClose={() => setStructuredIntakeVisible(false)}
      />
    </div>
  )
}

export default DiagnosisPage
