import { useState } from 'react'
import api from '../api/client.js'
import MapView from './MapView.jsx'

// Turn a number of minutes into "2h 15m" style text.
function formatMinutes(min) {
  const rounded = Math.round(min)
  const h = Math.floor(rounded / 60)
  const m = rounded % 60
  if (h > 0) return `${h}h ${m}m`
  return `${m}m`
}

export default function TripResult({ plan, stations }) {
  const [aiText, setAiText] = useState(plan.aiExplanation || '')
  const [aiLoading, setAiLoading] = useState(false)
  const [question, setQuestion] = useState('')
  const [answer, setAnswer] = useState('')
  const [asking, setAsking] = useState(false)

  const start = { lat: plan.startLat, lon: plan.startLon, name: plan.startName }
  const destination = { lat: plan.destinationLat, lon: plan.destinationLon, name: plan.destinationName }
  const stops = plan.stops.map((s) => ({ lat: s.latitude, lon: s.longitude, name: s.stationName }))

  async function explainPlan() {
    setAiLoading(true)
    try {
      const res = await api.post(`/api/ai/trips/${plan.tripId}/explain`)
      setAiText(res.data.answer)
    } catch (err) {
      setAiText('Could not load AI explanation.')
    } finally {
      setAiLoading(false)
    }
  }

  async function askQuestion(e) {
    e.preventDefault()
    if (!question.trim()) return
    setAsking(true)
    setAnswer('')
    try {
      const res = await api.post(`/api/ai/trips/${plan.tripId}/ask`, { question })
      setAnswer(res.data.answer)
    } catch (err) {
      setAnswer('Could not get an answer.')
    } finally {
      setAsking(false)
    }
  }

  return (
    <div>
      {/* Outcome banner */}
      <div className="card" style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2 style={{ margin: 0 }}>
            {plan.startName} → {plan.destinationName}
          </h2>
          {plan.feasible ? (
            <span className="badge badge-ok">
              {plan.directReach ? 'Reachable directly' : `Reachable · ${plan.stops.length} stop(s)`}
            </span>
          ) : (
            <span className="badge badge-bad">Not reachable</span>
          )}
        </div>
        <p className="muted" style={{ marginBottom: 0 }}>{plan.message}</p>
      </div>

      {/* Summary stats */}
      <div className="stat-grid">
        <div className="stat">
          <div className="value">{plan.totalDistanceKm} km</div>
          <div className="label">Total distance</div>
        </div>
        <div className="stat">
          <div className="value">{formatMinutes(plan.drivingTimeMinutes)}</div>
          <div className="label">Driving time</div>
        </div>
        <div className="stat">
          <div className="value">{formatMinutes(plan.chargingTimeMinutes)}</div>
          <div className="label">Charging time</div>
        </div>
        <div className="stat">
          <div className="value">₹{plan.totalCost}</div>
          <div className="label">Charging cost</div>
        </div>
      </div>

      {/* Map */}
      <MapView start={start} destination={destination} stations={stations} stops={stops} />

      <div style={{ marginTop: 8 }} className="muted">
        Start battery: {plan.startBatteryPercent}% · Battery on arrival: {plan.arrivalBatteryPercent}%
      </div>

      {/* Charging stops */}
      {plan.stops.length > 0 && (
        <div className="card" style={{ marginTop: 16 }}>
          <div className="section-title">Recommended charging stops</div>
          {plan.stops.map((stop, i) => (
            <div className="stop-item" key={i}>
              <div className="stop-badge">{i + 1}</div>
              <div>
                <b>{stop.stationName}</b>
                <div className="muted" style={{ fontSize: 13 }}>
                  Drive {stop.distanceFromPreviousKm} km · arrive {stop.batteryArrivalPercent}% →
                  charge to {stop.batteryDeparturePercent}% ·
                  {' '}{formatMinutes(stop.chargingTimeMinutes)} · ₹{stop.chargingCost}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Rejected stations (why not the others) */}
      {plan.rejectedStations && plan.rejectedStations.length > 0 && (
        <div className="card" style={{ marginTop: 16 }}>
          <div className="section-title">Why other stations were not chosen</div>
          {plan.rejectedStations.map((r) => (
            <div className="rejected-item" key={r.stationId}>
              <b>{r.stationName}</b> — {r.reason}
            </div>
          ))}
        </div>
      )}

      {/* AI explanation */}
      <div className="card" style={{ marginTop: 16 }}>
        <div className="section-title">AI explanation</div>
        {aiText ? (
          <div className="ai-box">{aiText}</div>
        ) : (
          <button className="btn btn-secondary" onClick={explainPlan} disabled={aiLoading}>
            {aiLoading ? 'Thinking…' : '✨ Explain this plan'}
          </button>
        )}

        <form onSubmit={askQuestion} style={{ marginTop: 16 }}>
          <label>Ask a question about this trip</label>
          <input
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="e.g. Why did we stop at Jaipur?"
          />
          <button className="btn btn-secondary" type="submit" disabled={asking}>
            {asking ? 'Asking…' : 'Ask AI'}
          </button>
        </form>
        {answer && <div className="ai-box" style={{ marginTop: 12 }}>{answer}</div>}
      </div>
    </div>
  )
}
