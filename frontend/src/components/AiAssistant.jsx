import { useState } from 'react'
import { Sparkles, MessageCircle } from 'lucide-react'
import api from '../services/api.js'

// "EV Trip Assistant": explains the plan and answers questions about it.
// It is a section inside the trip result, not a separate chat page.
export default function AiAssistant({ tripId, initialExplanation }) {
  const [explanation, setExplanation] = useState(initialExplanation || '')
  const [explaining, setExplaining] = useState(false)
  const [question, setQuestion] = useState('')
  const [answer, setAnswer] = useState('')
  const [asking, setAsking] = useState(false)

  async function explainRoute() {
    setExplaining(true)
    try {
      const res = await api.post(`/api/ai/trips/${tripId}/explain`)
      setExplanation(res.data.answer)
    } catch (err) {
      setExplanation('The assistant could not explain this route right now.')
    } finally {
      setExplaining(false)
    }
  }

  async function askQuestion(e) {
    e.preventDefault()
    if (!question.trim()) return
    setAsking(true)
    setAnswer('')
    try {
      const res = await api.post(`/api/ai/trips/${tripId}/ask`, { question })
      setAnswer(res.data.answer)
    } catch (err) {
      setAnswer('The assistant could not answer right now.')
    } finally {
      setAsking(false)
    }
  }

  return (
    <div>
      <div className="assistant-head">
        <span className="a-icon"><Sparkles size={16} /></span>
        <h2>EV Trip Assistant</h2>
      </div>

      {explanation ? (
        <div className="ai-answer fade-in">{explanation}</div>
      ) : (
        <div>
          <p className="section-hint" style={{ marginBottom: 12 }}>
            Get a plain-English explanation of why this route and these charging stops were chosen.
          </p>
          <button className="btn btn-ghost" onClick={explainRoute} disabled={explaining}>
            <Sparkles size={15} /> {explaining ? 'Thinking…' : 'Explain my route'}
          </button>
        </div>
      )}

      <form className="ask-row" onSubmit={askQuestion}>
        <div className="input-wrap">
          <span className="input-icon"><MessageCircle size={16} /></span>
          <input value={question} onChange={(e) => setQuestion(e.target.value)}
            placeholder="Ask anything about your journey…" />
        </div>
        <button className="btn btn-primary" type="submit" disabled={asking}>
          {asking ? 'Asking…' : 'Ask EV Assistant'}
        </button>
      </form>
      {answer && <div className="ai-answer fade-in" style={{ marginTop: 12 }}>{answer}</div>}
    </div>
  )
}
