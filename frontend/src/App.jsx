import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import TripPlanner from './pages/TripPlanner.jsx'
import TripHistory from './pages/TripHistory.jsx'
import Navbar from './components/Navbar.jsx'

export default function App() {
  const { user } = useAuth()

  // Not logged in -> only login / register are available.
  if (!user) {
    return (
      <Routes>
        <Route path="/register" element={<Register />} />
        <Route path="*" element={<Login />} />
      </Routes>
    )
  }

  // Logged in -> navbar + app pages.
  return (
    <>
      <Navbar />
      <Routes>
        <Route path="/" element={<TripPlanner />} />
        <Route path="/history" element={<TripHistory />} />
        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </>
  )
}
