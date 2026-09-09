import { MapPin, Flag, Zap, Car } from 'lucide-react'
import { formatMinutes, formatCost } from '../format.js'

const AVG_SPEED_KMH = 60 // same assumption as the backend optimizer

// A driving leg between two points on the timeline.
function Leg({ km }) {
  const minutes = (km / AVG_SPEED_KMH) * 60
  return (
    <div className="tl-leg">
      <Car size={14} /> {Math.round(km)} km · about {formatMinutes(minutes)} of driving
    </div>
  )
}

// Vertical journey itinerary: Start → (drive → charge)* → drive → Destination.
export default function ChargingTimeline({ plan }) {
  // Distance of the final leg = total − everything driven before the last stop.
  let drivenBeforeFinal = 0
  for (const stop of plan.stops) {
    drivenBeforeFinal += stop.distanceFromPreviousKm
  }
  const finalLegKm = plan.totalDistanceKm - drivenBeforeFinal

  return (
    <div className="timeline">
      {/* Start */}
      <div className="tl-item">
        <span className="tl-dot start"><MapPin size={13} /></span>
        <div className="tl-title">{plan.startName}</div>
        <div className="tl-sub">Departing with {Math.round(plan.startBatteryPercent)}% battery</div>
        {plan.feasible && <Leg km={plan.stops.length > 0 ? plan.stops[0].distanceFromPreviousKm : finalLegKm} />}
      </div>

      {/* Charging stops */}
      {plan.stops.map((stop, i) => {
        const isLast = i === plan.stops.length - 1
        const nextLegKm = isLast ? finalLegKm : plan.stops[i + 1].distanceFromPreviousKm
        return (
          <div className="tl-item" key={i}>
            <span className="tl-dot charge"><Zap size={12} /></span>
            <div className="tl-title">{stop.stationName}</div>
            <div className="tl-sub">
              Charge for {formatMinutes(stop.chargingTimeMinutes)} · Battery {Math.round(stop.batteryArrivalPercent)}% → {Math.round(stop.batteryDeparturePercent)}%
            </div>
            <div className="tl-badges">
              <span className="pill pill-green">+{stop.energyAddedKwh} kWh</span>
              <span className="pill pill-grey">{formatCost(stop.chargingCost)}</span>
            </div>
            {plan.feasible && <Leg km={nextLegKm} />}
          </div>
        )
      })}

      {/* Destination */}
      <div className="tl-item">
        <span className="tl-dot end"><Flag size={12} /></span>
        <div className="tl-title">{plan.destinationName}</div>
        <div className="tl-sub">
          {plan.feasible
            ? `Arriving with about ${Math.round(plan.arrivalBatteryPercent)}% battery`
            : 'Cannot be reached with the current plan'}
        </div>
      </div>
    </div>
  )
}
