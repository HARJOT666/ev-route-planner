import { useState } from 'react'
import { ChevronDown, ChevronUp } from 'lucide-react'

const DEFAULT_VISIBLE = 3

// "Why other stations were not chosen". Shows only a few relevant
// alternatives by default; the full technical list is behind a toggle.
export default function StationAlternatives({ rejected }) {
  const [showAll, setShowAll] = useState(false)

  if (!rejected || rejected.length === 0) {
    return null
  }

  // Put the "close call" stations (lost only on the mode's criteria) first —
  // those are the most interesting alternatives for the driver.
  const closeCalls = rejected.filter((r) => r.reason.startsWith('another reachable'))
  const others = rejected.filter((r) => !r.reason.startsWith('another reachable'))
  const ordered = [...closeCalls, ...others]

  const visible = showAll ? ordered : ordered.slice(0, DEFAULT_VISIBLE)

  return (
    <div>
      {visible.map((r) => (
        <div className="alt-row" key={r.stationId}>
          <span className="alt-name">{r.stationName}</span>
          <span className="alt-reason">{r.reason}</span>
        </div>
      ))}

      {ordered.length > DEFAULT_VISIBLE && (
        <button className="btn-link" style={{ marginTop: 12, display: 'inline-flex', alignItems: 'center', gap: 4 }}
          onClick={() => setShowAll(!showAll)}>
          {showAll
            ? <><ChevronUp size={14} /> Show fewer</>
            : <><ChevronDown size={14} /> View all {ordered.length} evaluated stations</>}
        </button>
      )}
    </div>
  )
}
