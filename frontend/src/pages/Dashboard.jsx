import { useEffect, useState } from 'react'
import api from '../api/client.js'
import { CITIES } from '../data/cities.js'
import VehicleManager from '../components/VehicleManager.jsx'
import TripResult from '../components/TripResult.jsx'

const MODES = ['FASTEST', 'CHEAPEST', 'BALANCED']

export default function Dashboard() {
  const [vehicles, setVehicles] = useState([])
  const [stations, setStations] = useState([])
  const [selectedVehicleId, setSelectedVehicleId] = useState(null)

  const [startName, setStartName] = useState('Delhi')
  const [destName, setDestName] = useState('Mumbai')
  const [battery, setBattery] = useState(80)
  const [mode, setMode] = useState('FASTEST')

  const [plan, setPlan] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  // Load vehicles + stations once when the page opens.
  useEffect(() => {
    loadVehicles()
    loadStations()
  }, [])

  async function loadVehicles() {
    const res = await api.get('/api/vehicles')
    setVehicles(res.data)
    // Auto-select the first vehicle if none is selected yet.
    if (res.data.length > 0 && selectedVehicleId === null) {
      setSelectedVehicleId(res.data[0].id)
    }
  }

  async function loadStations() {
    const res = await api.get('/api/stations')
    setStations(res.data)
  }

  async function planTrip() {
    setError('')
    if (!selectedVehicleId) {
      setError('Please add and select a vehicle first.')
      return
    }
    const start = CITIES.find((c) => c.name === startName)
    const dest = CITIES.find((c) => c.name === destName)

    setLoading(true)
    setPlan(null)
    try {
      const res = await api.post('/api/trips/plan', {
        startName: start.name,
        startLat: start.lat,
        startLon: start.lon,
        destinationName: dest.name,
        destinationLat: dest.lat,
        destinationLon: dest.lon,
        vehicleId: selectedVehicleId,
        batteryPercent: battery,
        mode
      })
      setPlan(res.data)
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to plan trip')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="page">
      <div className="grid-2">
        {/* Left column: planner + vehicles */}
        <div>
          <div className="card" style={{ marginBottom: 20 }}>
            <div className="section-title">Plan a trip</div>

            <label>Start location</label>
            <select value={startName} onChange={(e) => setStartName(e.target.value)}>
              {CITIES.map((c) => <option key={c.name} value={c.name}>{c.name}</option>)}
            </select>

            <label>Destination</label>
            <select value={destName} onChange={(e) => setDestName(e.target.value)}>
              {CITIES.map((c) => <option key={c.name} value={c.name}>{c.name}</option>)}
            </select>

            <label>Current battery: {battery}%</label>
            <input type="range" min="1" max="100" value={battery}
              onChange={(e) => setBattery(parseInt(e.target.value))} />

            <label>Optimization mode</label>
            <div className="mode-row">
              {MODES.map((m) => (
                <div key={m}
                  className={'mode-chip' + (mode === m ? ' active' : '')}
                  onClick={() => setMode(m)}>
                  {m.charAt(0) + m.slice(1).toLowerCase()}
                </div>
              ))}
            </div>

            {error && <div className="error-box">{error}</div>}

            <button className="btn" onClick={planTrip} disabled={loading}>
              {loading ? 'Planning…' : '⚡ Plan Trip'}
            </button>
          </div>

          <div className="card">
            <VehicleManager
              vehicles={vehicles}
              selectedId={selectedVehicleId}
              onSelect={setSelectedVehicleId}
              onChanged={loadVehicles}
            />
          </div>
        </div>

        {/* Right column: result */}
        <div>
          {plan ? (
            <TripResult plan={plan} stations={stations} />
          ) : (
            <div className="card" style={{ textAlign: 'center', padding: 60 }}>
              <div style={{ fontSize: 48 }}>🗺️</div>
              <h3>Plan your first electric trip</h3>
              <p className="muted">
                Choose a start and destination, set your battery level and pick an
                optimization mode. We'll work out whether you can make it and where to charge.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
