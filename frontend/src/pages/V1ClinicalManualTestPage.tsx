import { useState } from 'react'

type StartResult = {
  consultationId: string
  cdpId: string
  runId: string
  checkpointId: string
  waitEffectId: string
  entryStatus: string
  patientDeliveryAllowed: boolean
}
type AnswerResult = { consultationId: string; newRunId: string; status: string }
const base = '/internal/nonproduction/v1/clinical'

async function postJson<T>(endpoint: string, payload: unknown): Promise<T> {
  const response = await fetch(base + endpoint, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  if (!response.ok) {
    const text = await response.text()
    throw new Error('HTTP ' + response.status + ': ' + text.slice(0, 800))
  }
  return response.json() as Promise<T>
}

/** Synthetic-data-only manual smoke UI. Not a patient-facing diagnosis interface. */
export default function V1ClinicalManualTestPage() {
  const [text, setText] = useState('测试症状：头痛两天')
  const [answer, setAnswer] = useState('测试回答：没有发烧')
  const [userId, setUserId] = useState('synthetic-manual-test')
  const [started, setStarted] = useState<StartResult | null>(null)
  const [response, setResponse] = useState<AnswerResult | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  async function start() {
    setBusy(true); setError(''); setStarted(null); setResponse(null)
    try {
      const id = crypto.randomUUID()
      const result = await postJson<StartResult>('/start-through-wait', {
        eventId: 'manual-' + id, idempotencyKey: 'manual-' + id,
        userId, rawText: text,
      })
      setStarted(result)
    } catch (e) { setError(String(e)) } finally { setBusy(false) }
  }
  async function continueAnswer() {
    if (!started) return
    setBusy(true); setError('')
    try {
      setResponse(await postJson<AnswerResult>('/answer-through-u04', {
        consultationId: started.consultationId,
        checkpointId: started.checkpointId, answerText: answer,
      }))
    } catch (e) { setError(String(e)) } finally { setBusy(false) }
  }
  return <main style={{ maxWidth: 760, margin: '32px auto', padding: 24, fontFamily: 'sans-serif' }}>
    <h1>V1 Agent 本地人工验证（非生产）</h1>
    <p>仅使用合成测试数据。此页面不用于真实患者，不提供诊断或真实问题交付。</p>
    <p>需在后端独立启用受控测试宿主；404 表示接口未装配，拒绝错误可能表示缺少可信临床授权。</p>
    <label>合成用户 ID<input value={userId} onChange={e=>setUserId(e.target.value)} style={{display:'block',width:'100%'}} /></label>
    <label>首轮合成症状<textarea value={text} onChange={e=>setText(e.target.value)} style={{display:'block',width:'100%',minHeight:70}} /></label>
    <button disabled={busy||!text.trim()||!userId.trim()} onClick={start}>启动首轮并建立 WAIT</button>
    {started && <section><h2>首轮返回</h2><pre>{JSON.stringify(started,null,2)}</pre>
      <p>说明：后端目前仅返回 WAIT 凭据，尚未提供患者可见的追问文本。</p>
      <label>合成回答<textarea value={answer} onChange={e=>setAnswer(e.target.value)} style={{display:'block',width:'100%',minHeight:70}} /></label>
      <button disabled={busy||!answer.trim()} onClick={continueAnswer}>提交回答并重新评估 U03/U04</button>
    </section>}
    {response && <section><h2>第二轮返回</h2><pre>{JSON.stringify(response,null,2)}</pre></section>}
    {error && <div role="alert" style={{color:'#a11',whiteSpace:'pre-wrap',marginTop:16}}>请求失败：{error}</div>}
  </main>
}
