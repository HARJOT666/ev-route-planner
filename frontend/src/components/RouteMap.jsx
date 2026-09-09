import { useEffect } from 'react'
import { MapContainer, TileLayer, Marker, Popup, Polyline, CircleMarker, useMap } from 'react-leaflet'
import L from 'leaflet'

// A small coloured circle pin built from HTML (no image assets needed).
function pin(color, label) {
  return L.divIcon({
    className: '',
    html: `<div class="map-pin" style="background:${color}">${label}</div>`,
    iconSize: [26, 26],
    iconAnchor: [13, 13]
  })
}

// Zooms the map to fit the given points once, when the component mounts.
// The parent gives it a `key` per plan so it re-runs for each new plan.
function FitBounds({ points }) {
  const map = useMap()
  useEffect(() => {
    if (points.length > 0) {
      map.fitBounds(L.latLngBounds(points), { padding: [48, 48] })
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps
  return null
}

// Shows all charging stations always; when a plan exists it also draws the
// route line, start/destination pins and the numbered charging stops.
export default function RouteMap({ plan, stations }) {
  const hasPlan = plan !== null

  let routePoints = []
  let fitPoints = []
  if (hasPlan) {
    const start = [plan.startLat, plan.startLon]
    const end = [plan.destinationLat, plan.destinationLon]
    const stops = plan.stops.map((s) => [s.latitude, s.longitude])
    routePoints = [start, ...stops, end]
    fitPoints = routePoints
  }

  return (
    <div className="map-panel">
      <MapContainer center={[23.5, 76.5]} zoom={5} style={{ height: '100%', width: '100%' }}>
        <TileLayer
          attribution="&copy; OpenStreetMap contributors"
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {/* All stations as small dots for context */}
        {stations.map((s) => (
          <CircleMarker key={s.id} center={[s.latitude, s.longitude]} radius={5}
            pathOptions={{
              color: s.status === 'AVAILABLE' ? '#1d4ed8' : '#9ca3af',
              fillColor: s.status === 'AVAILABLE' ? '#1d4ed8' : '#9ca3af',
              fillOpacity: 0.6, weight: 1
            }}>
            <Popup>
              <b>{s.name}</b><br />
              {s.powerKw} kW · ₹{s.pricePerKwh}/kWh<br />
              <span style={{ color: '#6b7280' }}>{s.status.replace('_', ' ')}</span>
            </Popup>
          </CircleMarker>
        ))}

        {hasPlan && (
          <>
            <Polyline positions={routePoints} pathOptions={{ color: '#1d4ed8', weight: 4, opacity: 0.85 }} />

            {plan.stops.map((stop, i) => (
              <Marker key={i} position={[stop.latitude, stop.longitude]} icon={pin('#15803d', i + 1)}>
                <Popup><b>Stop {i + 1}: {stop.stationName}</b><br />Charge {Math.round(stop.chargingTimeMinutes)} min</Popup>
              </Marker>
            ))}

            <Marker position={[plan.startLat, plan.startLon]} icon={pin('#1d4ed8', 'A')}>
              <Popup>Start: {plan.startName}</Popup>
            </Marker>
            <Marker position={[plan.destinationLat, plan.destinationLon]} icon={pin('#111827', 'B')}>
              <Popup>Destination: {plan.destinationName}</Popup>
            </Marker>

            <FitBounds key={plan.tripId} points={fitPoints} />
          </>
        )}
      </MapContainer>
    </div>
  )
}
