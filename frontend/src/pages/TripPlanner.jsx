import { useEffect, useState } from 'react'
import api from '../services/api.js'
import TripForm from '../components/TripForm.jsx'
import RouteMap from '../components/RouteMap.jsx'
import TripSummary from '../components/TripSummary.jsx'
import ChargingTimeline from '../components/ChargingTimeline.jsx'
import StationAlternatives from '../components/StationAlternatives.jsx'
import AiAssistant from '../components/AiAssistant.jsx'

export default function TripPlanner() {
  const [vehicles, setVehicles] = useState([])
  const [stations, setStations] = useState([])
  const [selectedVehicleId, setSelectedVehicleId] = useState(null)

  const [plan, setPlan] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  // Load vehicles + stations once when the page opens.
  useEffect(() => {
    loadVehicles()
    api.get('/api/stations').then((res) => setStations(res.data))
  }, [])

  async function loadVehicles() {
    const res = await api.get('/api/vehicles')
    setVehicles(res.data)
    // Auto-select the first vehicle if none is selected yet.
    if (res.data.length > 0 && selectedVehicleId === null) {
      setSelectedVehicleId(res.data[0].id)
    }
  }

  async function planTrip({ start, destination, battery, mode }) {
    setError('')
    if (!selectedVehicleId) {
      setError('Please add and select a vehicle first.')
      return
    }
    if (start.name === destination.name) {
      setError('Start and destination must be different.')
      return
    }

    setLoading(true)
    try {
      const res = await api.post('/api/trips/plan', {
        startName: start.name,
        startLat: start.lat,
        startLon: start.lon,
        destinationName: destination.name,
        destinationLat: destination.lat,
        destinationLon: destination.lon,
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
    <main className="container page">
      <div className="page-header">
        <h1>Plan your EV journey</h1>
        <p>Find the fastest and most efficient charging plan for your trip.</p>
      </div>

      <div className="planner-grid">
        {/* Left: booking-style form */}
        <TripForm
          vehicles={vehicles}
          selectedVehicleId={selectedVehicleId}
          onSelectVehicle={setSelectedVehicleId}
          onVehiclesChanged={loadVehicles}
          onPlan={planTrip}
          loading={loading}
          error={error}
        />

        {/* Right: map first, then the results below it */}
        <div>
          <RouteMap plan={plan} stations={stations} />

          {plan ? (
            <div className="fade-in" key={plan.tripId}>
              <div className="section">
                <TripSummary plan={plan} />
              </div>

              <div className="section">
                <div className="section-header"><h2>Your journey</h2></div>
                <ChargingTimeline plan={plan} />
              </div>

              {plan.rejectedStations.length > 0 && (
                <div className="section">
                  <div className="section-header">
                    <h2>Alternative stations considered</h2>
                  </div>
                  <StationAlternatives rejected={plan.rejectedStations} />
                </div>
              )}

              <div className="section">
                <AiAssistant tripId={plan.tripId} initialExplanation={plan.aiExplanation} />
              </div>
            </div>
          ) : (
            <p className="section-hint" style={{ marginTop: 16 }}>
              Enter your trip details and we'll work out whether you can make it and where to charge.
            </p>
          )}
        </div>
      </div>
    </main>
  )
}
