import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext.jsx'
import Login from './pages/Login.jsx'
import Dashboard from './pages/Dashboard.jsx'
import History from './pages/History.jsx'
import Navbar from './components/Navbar.jsx'

export default function App() {
  const { user } = useAuth()

  // Not logged in -> only the login/register page is available.
  if (!user) {
    return (
      <Routes>
        <Route path="*" element={<Login />} />
      </Routes>
    )
  }

  // Logged in -> show the navbar and the app pages.
  return (
    <>
      <Navbar />
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/history" element={<History />} />
        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </>
  )
}
