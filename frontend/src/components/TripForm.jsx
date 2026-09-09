import { useState } from 'react'
import { MapPin, Flag, BatteryCharging, Zap, IndianRupee, Scale, Route } from 'lucide-react'
import { CITIES } from '../data/cities.js'
import VehiclePicker from './VehiclePicker.jsx'

const MODES = [
  { key: 'FASTEST', label: 'Fastest', Icon: Zap },
  { key: 'CHEAPEST', label: 'Cheapest', Icon: IndianRupee },
  { key: 'BALANCED', label: 'Balanced', Icon: Scale }
]

// The trip planning form. It owns its own input state and hands the chosen
// values to the page through onPlan(). Vehicle state lives in the page.
export default function TripForm({
  vehicles, selectedVehicleId, onSelectVehicle, onVehiclesChanged,
  onPlan, loading, error
}) {
  const [startName, setStartName] = useState('Delhi')
  const [destName, setDestName] = useState('Mumbai')
  const [battery, setBattery] = useState(90)
  const [mode, setMode] = useState('FASTEST')

  function handleSubmit(e) {
    e.preventDefault()
    const start = CITIES.find((c) => c.name === startName)
    const destination = CITIES.find((c) => c.name === destName)
    onPlan({ start, destination, battery, mode })
  }

  return (
    <form className="panel" onSubmit={handleSubmit}>
      <div className="field">
        <label>Start</label>
        <div className="input-wrap">
          <span className="input-icon"><MapPin size={16} /></span>
          <select value={startName} onChange={(e) => setStartName(e.target.value)}>
            {CITIES.map((c) => <option key={c.name} value={c.name}>{c.name}</option>)}
          </select>
        </div>
      </div>

      <div className="field">
        <label>Destination</label>
        <div className="input-wrap">
          <span className="input-icon"><Flag size={16} /></span>
          <select value={destName} onChange={(e) => setDestName(e.target.value)}>
            {CITIES.map((c) => <option key={c.name} value={c.name}>{c.name}</option>)}
          </select>
        </div>
      </div>

      <div className="field">
        <label>Vehicle</label>
        <VehiclePicker
          vehicles={vehicles}
          selectedId={selectedVehicleId}
          onSelect={onSelectVehicle}
          onChanged={onVehiclesChanged}
        />
      </div>

      <div className="field">
        <label>Current battery</label>
        <div className="slider-row">
          <BatteryCharging size={18} color="#15803d" />
          <input type="range" min="1" max="100" value={battery}
            onChange={(e) => setBattery(parseInt(e.target.value))} />
          <span className="slider-value">{battery}%</span>
        </div>
      </div>

      <div className="field">
        <label>Optimization preference</label>
        <div className="segmented">
          {MODES.map(({ key, label, Icon }) => (
            <button key={key} type="button"
              className={'seg-btn' + (mode === key ? ' active' : '')}
              onClick={() => setMode(key)}>
              <Icon size={14} /> {label}
            </button>
          ))}
        </div>
      </div>

      {error && <div className="error">{error}</div>}

      <button className="btn btn-primary btn-block" type="submit" disabled={loading}>
        <Route size={16} />
        {loading ? 'Planning your trip…' : 'Plan your trip'}
      </button>
    </form>
  )
}
