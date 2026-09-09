import { Zap, Route, BatteryCharging } from 'lucide-react'

// EV road-trip photo (Unsplash, free to use). If it fails to load, the
// deep-blue background colour set in CSS keeps the page looking fine.
const HERO_IMAGE =
  'https://images.unsplash.com/photo-1560958089-b8a1929cea89?auto=format&fit=crop&w=1600&q=80'

// Full-screen split layout shared by the Login and Register pages:
// left = EV visual with headline + feature highlights, right = the form.
export default function AuthLayout({ children }) {
  return (
    <div className="auth-split fade-in">
      <aside className="auth-visual" style={{ backgroundImage: `url(${HERO_IMAGE})` }}>
        <div className="auth-visual-copy">
          <h2>Plan every journey with confidence.</h2>
          <p>Find the best route, optimize charging stops, and make every EV journey smarter.</p>
        </div>

        <div className="auth-features">
          <div className="auth-feature">
            <span className="f-icon"><Zap size={16} /></span> Smart charging stops
          </div>
          <div className="auth-feature">
            <span className="f-icon"><Route size={16} /></span> Optimized routes
          </div>
          <div className="auth-feature">
            <span className="f-icon"><BatteryCharging size={16} /></span> Battery-aware planning
          </div>
        </div>
      </aside>

      <section className="auth-form-side">
        <div className="auth-form">{children}</div>
      </section>
    </div>
  )
}
