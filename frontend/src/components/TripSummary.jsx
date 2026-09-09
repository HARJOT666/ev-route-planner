import { Route, Clock, BatteryCharging, IndianRupee, ArrowRight, CheckCircle2, XCircle } from 'lucide-react'
import { formatMinutes, formatCost } from '../format.js'

// Route headline + four compact statistics. Typography and spacing only —
// no bordered cards around each number.
export default function TripSummary({ plan }) {
  const totalMinutes = plan.drivingTimeMinutes + plan.chargingTimeMinutes

  return (
    <div>
      <div className="route-title">
        <h2>{plan.startName} <ArrowRight size={18} style={{ verticalAlign: '-3px', color: '#9ca3af' }} /> {plan.destinationName}</h2>
        {plan.feasible ? (
          <span className="pill pill-green">
            <CheckCircle2 size={13} />
            {plan.directReach ? 'Reachable without charging' : `${plan.stops.length} charging stop${plan.stops.length === 1 ? '' : 's'}`}
          </span>
        ) : (
          <span className="pill pill-red"><XCircle size={13} /> Not reachable</span>
        )}
      </div>
      <p className="route-message">{plan.message}</p>

      <div className="stats">
        <div className="stat">
          <div className="stat-value">{Math.round(plan.totalDistanceKm)} km</div>
          <div className="stat-label"><Route size={13} /> Total distance</div>
        </div>
        <div className="stat">
          <div className="stat-value">{formatMinutes(totalMinutes)}</div>
          <div className="stat-label"><Clock size={13} /> Estimated duration</div>
        </div>
        <div className="stat">
          <div className="stat-value">{formatMinutes(plan.chargingTimeMinutes)}</div>
          <div className="stat-label"><BatteryCharging size={13} /> Charging time</div>
        </div>
        <div className="stat">
          <div className="stat-value">{formatCost(plan.totalCost)}</div>
          <div className="stat-label"><IndianRupee size={13} /> Estimated cost</div>
        </div>
      </div>

      <div className="battery-line">
        <BatteryCharging size={15} />
        Battery <b>{Math.round(plan.startBatteryPercent)}%</b> at start
        <ArrowRight size={14} />
        <b>{Math.round(plan.arrivalBatteryPercent)}%</b> on arrival
      </div>
    </div>
  )
}
