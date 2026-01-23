import React, { useState } from 'react'
import {
  EntryStep1ReceiveInput,
  EntryStep2IdentifySymptom,
  EntryStep3Clarification,
  EntryStep4RedFlagCheck,
  EntryStep5PathSelection,
} from './index'

interface EntryAssessmentFlowProps {
  onComplete: (path: 'A' | 'B') => void
  onExit: () => void
}

const EntryAssessmentFlow: React.FC<EntryAssessmentFlowProps> = ({
  onComplete,
  onExit,
}) => {
  const [currentStep, setCurrentStep] = useState<1 | 2 | 3 | 4 | 5>(1)
  const [userInput, setUserInput] = useState('')
  const [symptomStatus, setSymptomStatus] = useState<{
    status: 'no_symptom' | 'has_symptom' | 'uncertain'
    symptoms?: string[]
  }>()
  const [clarificationQuestion, setClarificationQuestion] = useState('')
  const [redFlagsCheck, setRedFlagsCheck] = useState<{
    redFlagsHit: boolean
    redFlags: Array<{ description: string }>
    safetyMessage?: string
  }>()
  const [pathResult, setPathResult] = useState<{
    path: 'A' | 'B' | 'exit'
    pathName: string
    nextStep: string
    message?: string
  }>()

  const handleStep1Submit = async () => {
    // TODO: 调用API进行Step 2：识别症状/困扰
    // 这里模拟API调用
    const mockSymptomStatus = {
      status: userInput.includes('痛') || userInput.includes('不舒服')
        ? 'has_symptom'
        : userInput.includes('体检') || userInput.includes('筛查')
        ? 'no_symptom'
        : 'uncertain',
      symptoms: userInput.includes('痛') ? ['胸痛'] : undefined,
    }
    setSymptomStatus(mockSymptomStatus)
    setCurrentStep(2)
  }

  const handleStep2Next = () => {
    if (symptomStatus?.status === 'uncertain') {
      // 需要方向澄清
      setClarificationQuestion('请确认您的主要需求是什么？')
      setCurrentStep(3)
    } else {
      // 跳过澄清，直接进入危险信号检查
      setCurrentStep(4)
    }
  }

  const handleStep3Clarification = (direction: 'A' | 'B') => {
    // TODO: 调用API进行Step 4：危险信号检查
    const mockRedFlagsCheck = {
      redFlagsHit: false,
      redFlags: [],
    }
    setRedFlagsCheck(mockRedFlagsCheck)
    setCurrentStep(4)
  }

  const handleStep4Next = () => {
    // TODO: 调用API进行Step 5：输出路径结果
    const mockPathResult = {
      path: symptomStatus?.status === 'no_symptom' ? 'A' : 'B',
      pathName: symptomStatus?.status === 'no_symptom' ? '健康筛查路径' : '症状诊断路径',
      nextStep: symptomStatus?.status === 'no_symptom' ? 'A1｜需求分类' : '阶段1｜问诊',
    }
    setPathResult(mockPathResult)
    setCurrentStep(5)
  }

  const handleStep5Navigate = (path: 'A' | 'B') => {
    onComplete(path)
  }

  return (
    <div>
      {currentStep === 1 && (
        <EntryStep1ReceiveInput
          userInput={userInput}
          onInputChange={setUserInput}
          onSubmit={handleStep1Submit}
        />
      )}
      {currentStep === 2 && symptomStatus && (
        <EntryStep2IdentifySymptom
          symptomStatus={symptomStatus}
          onNext={handleStep2Next}
        />
      )}
      {currentStep === 3 && (
        <EntryStep3Clarification
          clarificationQuestion={clarificationQuestion}
          onClarification={handleStep3Clarification}
        />
      )}
      {currentStep === 4 && redFlagsCheck && (
        <EntryStep4RedFlagCheck
          redFlagsCheck={redFlagsCheck}
          onExit={onExit}
        />
      )}
      {currentStep === 5 && pathResult && (
        <EntryStep5PathSelection
          pathResult={pathResult}
          onNavigateToPath={handleStep5Navigate}
          onExit={onExit}
        />
      )}
    </div>
  )
}

export default EntryAssessmentFlow

