import { useState } from 'react'
import api from '../api/client.js'

// Lists the user's vehicles, lets them pick one, add a new one, or delete.
// `vehicles`, `selectedId` and the callbacks come from the Dashboard page.
export default function VehicleManager({ vehicles, selectedId, onSelect, onChanged }) {
  const [showForm, setShowForm] = useState(false)
  const [name, setName] = useState('')
  const [capacity, setCapacity] = useState('')
  const [efficiency, setEfficiency] = useState('')
  const [power, setPower] = useState('')

  async function addVehicle(e) {
    e.preventDefault()
    await api.post('/api/vehicles', {
      name,
      batteryCapacityKwh: parseFloat(capacity),
      efficiencyKmPerKwh: parseFloat(efficiency),
      maxChargingPowerKw: parseFloat(power)
    })
    setName(''); setCapacity(''); setEfficiency(''); setPower('')
    setShowForm(false)
    onChanged() // ask the parent to reload the vehicle list
  }

  async function remove(id) {
    await api.delete(`/api/vehicles/${id}`)
    onChanged()
  }

  return (
    <div>
      <div className="section-title">Your vehicles</div>

      {vehicles.length === 0 && (
        <p className="muted">No vehicles yet. Add one to start planning.</p>
      )}

      {vehicles.map((v) => (
        <div
          key={v.id}
          className={'vehicle-item' + (v.id === selectedId ? ' selected' : '')}
          onClick={() => onSelect(v.id)}
        >
          <div>
            <b>{v.name}</b>
            <div className="muted" style={{ fontSize: 12 }}>
              {v.batteryCapacityKwh} kWh · {v.efficiencyKmPerKwh} km/kWh · {v.maxChargingPowerKw} kW
            </div>
          </div>
          <button className="btn btn-secondary btn-small" onClick={(e) => { e.stopPropagation(); remove(v.id) }}>
            ✕
          </button>
        </div>
      ))}

      {showForm ? (
        <form onSubmit={addVehicle} style={{ marginTop: 12 }}>
          <label>Vehicle name</label>
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Long Range EV" required />
          <label>Battery capacity (kWh)</label>
          <input type="number" step="any" value={capacity} onChange={(e) => setCapacity(e.target.value)} placeholder="60" required />
          <label>Efficiency (km per kWh)</label>
          <input type="number" step="any" value={efficiency} onChange={(e) => setEfficiency(e.target.value)} placeholder="6" required />
          <label>Max charging power (kW)</label>
          <input type="number" step="any" value={power} onChange={(e) => setPower(e.target.value)} placeholder="150" required />
          <button className="btn" type="submit">Save vehicle</button>
          <button className="btn btn-secondary" type="button" onClick={() => setShowForm(false)}>Cancel</button>
        </form>
      ) : (
        <button className="btn btn-secondary" onClick={() => setShowForm(true)}>+ Add vehicle</button>
      )}
    </div>
  )
}
