import { MapContainer, TileLayer, Marker, Popup, Polyline, CircleMarker, useMap } from 'react-leaflet'
import L from 'leaflet'

// Build a simple coloured circular pin as an HTML icon (avoids the missing
// default-marker-image issue and keeps the map readable).
function pin(color, label) {
  return L.divIcon({
    className: '',
    html: `<div style="background:${color};width:26px;height:26px;border-radius:50% 50% 50% 0;
           transform:rotate(-45deg);border:2px solid #0f1720;display:flex;align-items:center;
           justify-content:center;box-shadow:0 2px 6px rgba(0,0,0,.4)">
           <span style="transform:rotate(45deg);color:#06231a;font-weight:700;font-size:12px">${label}</span>
           </div>`,
    iconSize: [26, 26],
    iconAnchor: [13, 26]
  })
}

// Small helper component: pans/zooms the map to fit all the given points.
function FitBounds({ points }) {
  const map = useMap()
  if (points.length > 0) {
    const bounds = L.latLngBounds(points.map((p) => [p.lat, p.lon]))
    map.fitBounds(bounds, { padding: [40, 40] })
  }
  return null
}

export default function MapView({ start, destination, stations, stops }) {
  // The route line goes start -> each charging stop -> destination.
  const routePoints = [start, ...stops, destination].map((p) => [p.lat, p.lon])
  const allPoints = [start, destination, ...stops]

  return (
    <div className="map-wrap">
      <MapContainer center={[start.lat, start.lon]} zoom={6} style={{ height: '100%', width: '100%' }}>
        <TileLayer
          attribution="&copy; OpenStreetMap contributors"
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {/* Route line */}
        <Polyline positions={routePoints} pathOptions={{ color: '#2fe089', weight: 4 }} />

        {/* All stations shown as small dots for context */}
        {stations.map((s) => (
          <CircleMarker
            key={s.id}
            center={[s.latitude, s.longitude]}
            radius={5}
            pathOptions={{ color: s.status === 'AVAILABLE' ? '#5fb0ff' : '#888', fillOpacity: 0.7 }}
          >
            <Popup>
              <b>{s.name}</b><br />
              {s.powerKw} kW · ₹{s.pricePerKwh}/kWh<br />
              Status: {s.status}
            </Popup>
          </CircleMarker>
        ))}

        {/* Chosen charging stops, numbered */}
        {stops.map((stop, i) => (
          <Marker key={i} position={[stop.lat, stop.lon]} icon={pin('#ffcf5c', String(i + 1))}>
            <Popup><b>Stop {i + 1}: {stop.name}</b></Popup>
          </Marker>
        ))}

        {/* Start and destination */}
        <Marker position={[start.lat, start.lon]} icon={pin('#2fe089', 'A')}>
          <Popup>Start: {start.name}</Popup>
        </Marker>
        <Marker position={[destination.lat, destination.lon]} icon={pin('#ff6b6b', 'B')}>
          <Popup>Destination: {destination.name}</Popup>
        </Marker>

        <FitBounds points={allPoints} />
      </MapContainer>
    </div>
  )
}
