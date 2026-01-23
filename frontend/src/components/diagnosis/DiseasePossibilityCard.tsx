import React from 'react'
import { Space, Typography } from 'antd'
import type { DiseasePossibility } from '@/types/diagnosis'

const { Text } = Typography

interface DiseasePossibilityCardProps {
  possibility: DiseasePossibility
  showDetail?: boolean
}

const DiseasePossibilityCard: React.FC<DiseasePossibilityCardProps> = ({
  possibility,
  showDetail = false,
}) => {
  const { disease, confidence, supportingEvidence, opposingEvidence, missingInfo, inclusionBasis } =
    possibility

  if (!showDetail) {
    // 简化显示（用于列表）
    return (
      <Space>
        <Text strong>{disease}</Text>
        <Text type="secondary">（{Math.round(confidence * 100)}%）</Text>
      </Space>
    )
  }

  // 详细显示（用于卡片）
  return (
    <Space direction="vertical" style={{ width: '100%' }} size="small">
      <Space>
        <Text strong>{disease}</Text>
        <Text type="secondary">可能性：{Math.round(confidence * 100)}%</Text>
      </Space>

      {/* 入选依据 */}
      {inclusionBasis && inclusionBasis.length > 0 && (
        <div>
          <Text type="secondary" style={{ fontSize: 12 }}>
            入选依据：{inclusionBasis.join('、')}
          </Text>
        </div>
      )}

      {/* 支持证据 */}
      {supportingEvidence.length > 0 && (
        <div>
          <Text type="secondary" style={{ fontSize: 12 }}>
            支持证据：{supportingEvidence.join('、')}
          </Text>
        </div>
      )}

      {/* 反对证据 */}
      {opposingEvidence.length > 0 && (
        <div>
          <Text type="secondary" style={{ fontSize: 12 }}>
            反对证据：{opposingEvidence.join('、')}
          </Text>
        </div>
      )}

      {/* 缺失信息 */}
      {missingInfo && missingInfo.length > 0 && (
        <div>
          <Text type="secondary" style={{ fontSize: 12 }}>
            缺失信息：{missingInfo.join('、')}
          </Text>
        </div>
      )}
    </Space>
  )
}

export default DiseasePossibilityCard
