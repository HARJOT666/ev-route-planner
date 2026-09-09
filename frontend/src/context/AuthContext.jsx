import { createContext, useContext, useState } from 'react'

// A tiny auth context. It keeps the logged-in user in React state and mirrors
// the token + name in localStorage so a page refresh stays logged in.
const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const token = localStorage.getItem('token')
    const name = localStorage.getItem('name')
    return token ? { token, name } : null
  })

  // Called after a successful register/login.
  function login(authResponse) {
    localStorage.setItem('token', authResponse.token)
    localStorage.setItem('name', authResponse.name)
    setUser({ token: authResponse.token, name: authResponse.name })
  }

  function logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('name')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
