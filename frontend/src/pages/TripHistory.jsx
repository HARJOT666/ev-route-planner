import { useEffect, useState } from 'react'
import { Search, ChevronRight, ArrowLeft, ArrowRight } from 'lucide-react'
import api from '../services/api.js'
import { formatMinutes, formatDate, formatMode } from '../format.js'
import RouteMap from '../components/RouteMap.jsx'
import TripSummary from '../components/TripSummary.jsx'
import ChargingTimeline from '../components/ChargingTimeline.jsx'
import AiAssistant from '../components/AiAssistant.jsx'

export default function TripHistory() {
  const [trips, setTrips] = useState([])
  const [stations, setStations] = useState([])
  const [search, setSearch] = useState('')
  const [selectedPlan, setSelectedPlan] = useState(null)

  useEffect(() => {
    api.get('/api/trips').then((res) => setTrips(res.data))
    api.get('/api/stations').then((res) => setStations(res.data))
  }, [])

  // Load the full detail of one trip when its row is clicked.
  async function openTrip(id) {
    const res = await api.get(`/api/trips/${id}`)
    setSelectedPlan(res.data)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  // Simple client-side filter on route names and mode.
  const term = search.toLowerCase()
  const filtered = trips.filter((t) =>
    t.startName.toLowerCase().includes(term) ||
    t.destinationName.toLowerCase().includes(term) ||
    t.mode.toLowerCase().includes(term)
  )

  // ----- Detail view -----
  if (selectedPlan) {
    return (
      <main className="container page fade-in">
        <button className="btn-link" style={{ display: 'inline-flex', alignItems: 'center', gap: 6, marginBottom: 20 }}
          onClick={() => setSelectedPlan(null)}>
          <ArrowLeft size={15} /> Back to trip history
        </button>

        <TripSummary plan={selectedPlan} />

        <div className="section">
          <RouteMap plan={selectedPlan} stations={stations} />
        </div>

        <div className="planner-grid section" style={{ gridTemplateColumns: 'minmax(0,1fr) minmax(0,1fr)' }}>
          <div>
            <div className="section-header"><h2>Your journey</h2></div>
            <ChargingTimeline plan={selectedPlan} />
          </div>
          <div>
            <AiAssistant tripId={selectedPlan.tripId} initialExplanation={selectedPlan.aiExplanation} />
          </div>
        </div>
      </main>
    )
  }

  // ----- List view -----
  return (
    <main className="container page">
      <div className="page-header">
        <h1>Trip History</h1>
        <p>{trips.length === 0 ? 'Your planned trips will appear here.' : `${trips.length} trip${trips.length === 1 ? '' : 's'} planned`}</p>
      </div>

      {trips.length > 0 && (
        <div className="history-toolbar">
          <div className="input-wrap">
            <span className="input-icon"><Search size={16} /></span>
            <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search trips…" />
          </div>
        </div>
      )}

      {trips.length === 0 && (
        <div className="empty-state">
          <h3>No trips yet</h3>
          <p>Plan a trip from the Trip Planner and it will be saved here.</p>
        </div>
      )}

      {filtered.map((t) => (
        <div className="trip-row" key={t.id} onClick={() => openTrip(t.id)}>
          <div>
            <div className="trip-route">
              {t.startName} <ArrowRight size={14} style={{ verticalAlign: '-2px', color: '#9ca3af' }} /> {t.destinationName}
            </div>
            <div className="trip-sub">
              {formatDate(t.createdAt)} · {formatMode(t.mode)} · {t.numberOfStops} charging stop{t.numberOfStops === 1 ? '' : 's'}
            </div>
          </div>
          <div className="trip-metric hide-mobile">
            {Math.round(t.totalDistanceKm)} km
            <span className="small">Distance</span>
          </div>
          <div className="trip-metric hide-mobile">
            {formatMinutes(t.estimatedDurationMinutes)}
            <span className="small">Duration</span>
          </div>
          <div className="hide-mobile">
            {t.feasible
              ? <span className="pill pill-green">Completed</span>
              : <span className="pill pill-red">Unreachable</span>}
          </div>
          <span className="chevron"><ChevronRight size={18} /></span>
        </div>
      ))}

      {trips.length > 0 && filtered.length === 0 && (
        <p className="section-hint" style={{ marginTop: 16 }}>No trips match "{search}".</p>
      )}
    </main>
  )
}
