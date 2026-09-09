import { useEffect, useState } from 'react'
import api from '../api/client.js'
import TripResult from '../components/TripResult.jsx'

function formatDate(iso) {
  return new Date(iso).toLocaleString()
}

export default function History() {
  const [trips, setTrips] = useState([])
  const [stations, setStations] = useState([])
  const [selectedPlan, setSelectedPlan] = useState(null)

  useEffect(() => {
    loadTrips()
    api.get('/api/stations').then((res) => setStations(res.data))
  }, [])

  async function loadTrips() {
    const res = await api.get('/api/trips')
    setTrips(res.data)
  }

  // Load the full detail of one trip when its row is clicked.
  async function openTrip(id) {
    const res = await api.get(`/api/trips/${id}`)
    setSelectedPlan(res.data)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  return (
    <div className="page">
      {selectedPlan && (
        <div style={{ marginBottom: 24 }}>
          <button className="btn btn-secondary btn-small" onClick={() => setSelectedPlan(null)}>
            ← Back to list
          </button>
          <div style={{ marginTop: 16 }}>
            <TripResult plan={selectedPlan} stations={stations} />
          </div>
        </div>
      )}

      {!selectedPlan && (
        <>
          <h2>Trip history</h2>
          {trips.length === 0 && <p className="muted">No trips yet. Plan one from the Trip Planner.</p>}

          {trips.map((t) => (
            <div className="trip-row" key={t.id} onClick={() => openTrip(t.id)}>
              <div>
                <b>{t.startName} → {t.destinationName}</b>
                <div className="muted" style={{ fontSize: 13 }}>
                  {formatDate(t.createdAt)} · {t.mode} · {t.numberOfStops} stop(s)
                </div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div>{t.totalDistanceKm} km</div>
                <div className="muted" style={{ fontSize: 13 }}>
                  {Math.round(t.estimatedDurationMinutes)} min ·{' '}
                  {t.feasible
                    ? <span className="badge badge-ok">OK</span>
                    : <span className="badge badge-bad">Unreachable</span>}
                </div>
              </div>
            </div>
          ))}
        </>
      )}
    </div>
  )
}
