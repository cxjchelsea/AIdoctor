import React from 'react'
import { Layout, Card, Tabs, Button } from 'antd'
import ReasoningPathsVisualization from './ReasoningPathsVisualization'
import EvidenceChainVisualization from './EvidenceChainVisualization'
import BrainResultsVisualization from './BrainResultsVisualization'
import CDPVersionHistory from './CDPVersionHistory'

const { Content } = Layout
const { TabPane } = Tabs

interface CDPVisualizationPageProps {
  cdpId: string
}

const CDPVisualizationPage: React.FC<CDPVisualizationPageProps> = ({ cdpId }) => {
  const handleReplay = () => {
    // TODO: 实现CDP演变过程回放
    console.log('回放CDP演变过程', cdpId)
  }

  return (
    <Layout>
      <Content>
        <Card
          title="CDP可视化"
          extra={<Button onClick={handleReplay}>回放演变过程</Button>}
        >
          <Tabs>
            {/* 推理路径可视化（DR.KNOWS核心） */}
            <TabPane tab="推理路径" key="reasoning-paths">
              <ReasoningPathsVisualization cdpId={cdpId} />
            </TabPane>

            {/* 证据链可视化 */}
            <TabPane tab="证据链" key="evidence-chain">
              <EvidenceChainVisualization cdpId={cdpId} />
            </TabPane>

            {/* 八个脑区执行结果 */}
            <TabPane tab="脑区执行结果" key="brain-results">
              <BrainResultsVisualization cdpId={cdpId} />
            </TabPane>

            {/* CDP版本历史 */}
            <TabPane tab="版本历史" key="versions">
              <CDPVersionHistory cdpId={cdpId} />
            </TabPane>
          </Tabs>
        </Card>
      </Content>
    </Layout>
  )
}

export default CDPVisualizationPage

