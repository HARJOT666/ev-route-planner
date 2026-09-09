import { useState } from 'react'
import api from '../api/client.js'
import { useAuth } from '../context/AuthContext.jsx'

// One page handles both login and register (toggled with a link).
export default function Login() {
  const { login } = useAuth()
  const [isRegister, setIsRegister] = useState(false)
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const url = isRegister ? '/api/auth/register' : '/api/auth/login'
      const body = isRegister ? { name, email, password } : { email, password }
      const response = await api.post(url, body)
      login(response.data) // stores token + redirects (via context state change)
    } catch (err) {
      setError(err.response?.data?.error || 'Something went wrong')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-wrap">
      <div className="card auth-card">
        <div className="brand" style={{ fontSize: 24, marginBottom: 6 }}>
          <span className="logo" style={{ fontSize: 28 }}>⚡</span> EV Route Planner
        </div>
        <p className="muted" style={{ marginTop: 0 }}>
          Plan electric road trips with smart charging stops.
        </p>

        <form onSubmit={handleSubmit}>
          {isRegister && (
            <>
              <label>Name</label>
              <input value={name} onChange={(e) => setName(e.target.value)} required />
            </>
          )}

          <label>Email</label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />

          <label>Password</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />

          {error && <div className="error-box">{error}</div>}

          <button className="btn" type="submit" disabled={loading}>
            {loading ? 'Please wait…' : isRegister ? 'Create account' : 'Log in'}
          </button>
        </form>

        <div className="toggle">
          {isRegister ? 'Already have an account? ' : "Don't have an account? "}
          <span onClick={() => { setIsRegister(!isRegister); setError('') }}>
            {isRegister ? 'Log in' : 'Register'}
          </span>
        </div>
      </div>
    </div>
  )
}
