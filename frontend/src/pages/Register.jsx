import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Zap, User, Mail, Lock } from 'lucide-react'
import api from '../services/api.js'
import { useAuth } from '../context/AuthContext.jsx'
import AuthLayout from '../components/AuthLayout.jsx'

export default function Register() {
  const { login } = useAuth()
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
      const response = await api.post('/api/auth/register', { name, email, password })
      login(response.data)
    } catch (err) {
      setError(err.response?.data?.error || 'Something went wrong')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout>
      <div className="brand" style={{ marginBottom: 36 }}>
        <span className="brand-mark"><Zap size={18} /></span>
        EV Route Planner
      </div>

      <h1>Create your account</h1>
      <p className="auth-sub">Start planning smarter electric road trips in minutes.</p>

      <form onSubmit={handleSubmit}>
        <div className="field">
          <label>Name</label>
          <div className="input-wrap">
            <span className="input-icon"><User size={16} /></span>
            <input value={name} onChange={(e) => setName(e.target.value)}
              placeholder="Your name" required />
          </div>
        </div>

        <div className="field">
          <label>Email</label>
          <div className="input-wrap">
            <span className="input-icon"><Mail size={16} /></span>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)}
              placeholder="your@email.com" required />
          </div>
        </div>

        <div className="field">
          <label>Password</label>
          <div className="input-wrap">
            <span className="input-icon"><Lock size={16} /></span>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
              placeholder="At least 6 characters" required />
          </div>
        </div>

        {error && <div className="error">{error}</div>}

        <button className="btn btn-primary btn-block btn-lg" type="submit" disabled={loading}>
          {loading ? 'Creating account…' : 'Create account'}
        </button>
      </form>

      <p className="auth-switch">
        Already have an account? <Link to="/login">Sign in</Link>
      </p>
    </AuthLayout>
  )
}
