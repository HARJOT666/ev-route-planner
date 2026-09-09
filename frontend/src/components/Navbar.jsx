import { NavLink } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

export default function Navbar() {
  const { user, logout } = useAuth()

  return (
    <div className="navbar">
      <div className="brand">
        <span className="logo">⚡</span>
        <span>EV Route Planner</span>
      </div>
      <div className="links">
        <NavLink to="/" end>Trip Planner</NavLink>
        <NavLink to="/history">History</NavLink>
        <span className="muted">Hi, {user.name}</span>
        <button className="btn btn-secondary btn-small" onClick={logout}>
          Logout
        </button>
      </div>
    </div>
  )
}
