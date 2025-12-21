import { createContext, useContext, useState, useEffect } from 'react'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    // Check for saved session
    const savedUser = localStorage.getItem('soma_user')
    const savedAuth = localStorage.getItem('soma_auth')
    
    if (savedUser && savedAuth) {
      setUser(JSON.parse(savedUser))
      setIsAuthenticated(true)
    }
    setLoading(false)
  }, [])

  const login = async (username, password) => {
    setLoading(true)
    setError(null)
    
    try {
      // Create Basic Auth header
      const credentials = btoa(`${username}:${password}`)
      
      const response = await fetch('/api/chat/users/me', {
        method: 'GET',
        headers: {
          'Authorization': `Basic ${credentials}`,
          'Content-Type': 'application/json',
        },
      })
      
      if (!response.ok) {
        throw new Error('Invalid username or password')
      }
      
      const userData = await response.json()
      
      // Save to state and localStorage
      setUser(userData)
      setIsAuthenticated(true)
      localStorage.setItem('soma_user', JSON.stringify(userData))
      localStorage.setItem('soma_auth', credentials)
      
      return { success: true }
    } catch (err) {
      setError(err.message)
      return { success: false, error: err.message }
    } finally {
      setLoading(false)
    }
  }

  const logout = () => {
    setUser(null)
    setIsAuthenticated(false)
    localStorage.removeItem('soma_user')
    localStorage.removeItem('soma_auth')
  }

  const getAuthHeader = () => {
    const auth = localStorage.getItem('soma_auth')
    return auth ? `Basic ${auth}` : null
  }

  const value = {
    user,
    isAuthenticated,
    loading,
    error,
    login,
    logout,
    getAuthHeader,
  }

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
