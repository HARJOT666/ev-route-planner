// Small display helpers shared by several components.

// 135 -> "2h 15m", 24 -> "24 min"
export function formatMinutes(min) {
  const rounded = Math.round(min)
  const h = Math.floor(rounded / 60)
  const m = rounded % 60
  if (h > 0) return `${h}h ${m}m`
  return `${m} min`
}

// 3544.7 -> "₹3,545"
export function formatCost(value) {
  return '₹' + Math.round(value).toLocaleString('en-IN')
}

// ISO string -> "9 Sep 2026"
export function formatDate(iso) {
  return new Date(iso).toLocaleDateString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric'
  })
}

// "FASTEST" -> "Fastest"
export function formatMode(mode) {
  return mode.charAt(0) + mode.slice(1).toLowerCase()
}
