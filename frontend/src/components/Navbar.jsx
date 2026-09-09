import { NavLink } from 'react-router-dom'
import { Zap, LogOut } from 'lucide-react'
import { useAuth } from '../context/AuthContext.jsx'

export default function Navbar() {
  const { user, logout } = useAuth()
  const initial = user.name ? user.name.charAt(0).toUpperCase() : '?'

  return (
    <header className="nav">
      <div className="container nav-inner">
        <div className="brand">
          <span className="brand-mark"><Zap size={18} /></span>
          EV Route Planner
        </div>

        <nav className="nav-links">
          <NavLink to="/" end>Trip Planner</NavLink>
          <NavLink to="/history">Trip History</NavLink>
        </nav>

        <div className="nav-right">
          <span className="avatar">{initial}</span>
          <span className="nav-user">{user.name}</span>
          <button className="icon-btn" onClick={logout} title="Log out">
            <LogOut size={18} />
          </button>
        </div>
      </div>
    </header>
  )
}
