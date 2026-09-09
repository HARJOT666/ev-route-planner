import { useState } from 'react'
import { Car, Trash2, Plus } from 'lucide-react'
import api from '../services/api.js'

// Shows the selected vehicle in one compact line. "Change vehicle" reveals
// the full list (selectable rows), a delete action and an "add vehicle" form.
export default function VehiclePicker({ vehicles, selectedId, onSelect, onChanged }) {
  const [expanded, setExpanded] = useState(false)
  const [showAdd, setShowAdd] = useState(false)
  const [name, setName] = useState('')
  const [capacity, setCapacity] = useState('')
  const [efficiency, setEfficiency] = useState('')
  const [power, setPower] = useState('')

  const selected = vehicles.find((v) => v.id === selectedId)

  function spec(v) {
    return `${v.batteryCapacityKwh} kWh · ${v.efficiencyKmPerKwh} km/kWh · ${v.maxChargingPowerKw} kW max charging`
  }

  async function addVehicle(e) {
    e.preventDefault()
    const res = await api.post('/api/vehicles', {
      name,
      batteryCapacityKwh: parseFloat(capacity),
      efficiencyKmPerKwh: parseFloat(efficiency),
      maxChargingPowerKw: parseFloat(power)
    })
    setName(''); setCapacity(''); setEfficiency(''); setPower('')
    setShowAdd(false)
    setExpanded(false)
    await onChanged()          // parent reloads the list
    onSelect(res.data.id)      // select the vehicle we just added
  }

  async function removeVehicle(e, id) {
    e.stopPropagation()
    await api.delete(`/api/vehicles/${id}`)
    onChanged()
  }

  // Compact view: one vehicle line + "Change vehicle".
  if (selected && !expanded) {
    return (
      <div className="vehicle-selected">
        <span className="v-icon"><Car size={18} /></span>
        <div>
          <div className="v-name">{selected.name}</div>
          <div className="v-spec">{spec(selected)}</div>
        </div>
        <button type="button" className="btn-link" onClick={() => setExpanded(true)}>
          Change vehicle
        </button>
      </div>
    )
  }

  // Expanded view: selectable rows + add form.
  return (
    <div>
      {vehicles.length > 0 && (
        <div className="vehicle-list">
          {vehicles.map((v) => (
            <div key={v.id}
              className={'vehicle-row' + (v.id === selectedId ? ' selected' : '')}
              onClick={() => { onSelect(v.id); setExpanded(false) }}>
              <span className="radio" />
              <div>
                <div className="v-name">{v.name}</div>
                <div className="v-spec">{spec(v)}</div>
              </div>
              <button type="button" className="icon-btn" title="Remove vehicle"
                onClick={(e) => removeVehicle(e, v.id)}>
                <Trash2 size={15} />
              </button>
            </div>
          ))}
        </div>
      )}

      {vehicles.length === 0 && !showAdd && (
        <p className="muted small" style={{ marginBottom: 10 }}>
          Add your EV to start planning.
        </p>
      )}

      {showAdd ? (
        <form onSubmit={addVehicle} style={{ marginTop: 12 }}>
          <div className="field">
            <label>Vehicle name</label>
            <input value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Long Range EV" required />
          </div>
          <div className="form-grid-2">
            <div className="field">
              <label>Battery (kWh)</label>
              <input type="number" step="any" value={capacity} onChange={(e) => setCapacity(e.target.value)} placeholder="60" required />
            </div>
            <div className="field">
              <label>Efficiency (km/kWh)</label>
              <input type="number" step="any" value={efficiency} onChange={(e) => setEfficiency(e.target.value)} placeholder="6" required />
            </div>
          </div>
          <div className="field">
            <label>Max charging power (kW)</label>
            <input type="number" step="any" value={power} onChange={(e) => setPower(e.target.value)} placeholder="150" required />
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-primary btn-sm" type="submit">Save vehicle</button>
            <button className="btn btn-ghost btn-sm" type="button" onClick={() => setShowAdd(false)}>Cancel</button>
          </div>
        </form>
      ) : (
        <button type="button" className="btn btn-ghost btn-sm" style={{ marginTop: 10 }}
          onClick={() => setShowAdd(true)}>
          <Plus size={14} /> Add vehicle
        </button>
      )}
    </div>
  )
}
