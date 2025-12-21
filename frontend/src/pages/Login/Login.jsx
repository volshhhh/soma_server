import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import './Login.css'

function Login() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setIsLoading(true)

    const result = await login(username, password)
    
    if (result.success) {
      navigate('/chat')
    } else {
      setError(result.error || 'Login failed. Please try again.')
    }
    
    setIsLoading(false)
  }

  return (
    <div className="login-page">
      <div className="login-container">
        <h1 className="login-title">Login to Soma Chat</h1>
        <p className="login-subtitle">
          Connect with your music community in real-time
        </p>

        <div className="feature-grid">
          <div className="feature-item">
            <h3>Real-time Chat</h3>
            <p>Instant message delivery</p>
          </div>
          <div className="feature-item">
            <h3>Multiple Rooms</h3>
            <p>Join different chat rooms</p>
          </div>
          <div className="feature-item">
            <h3>Message History</h3>
            <p>Access previous conversations</p>
          </div>
          <div className="feature-item">
            <h3>Free to Use</h3>
            <p>No hidden costs or limits</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="login-form">
          {error && <div className="error-message">{error}</div>}
          
          <div className="form-group">
            <input
              type="text"
              placeholder="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="form-input"
              required
              autoComplete="username"
            />
          </div>
          
          <div className="form-group">
            <input
              type="password"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="form-input"
              required
              autoComplete="current-password"
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary login-btn"
            disabled={isLoading}
          >
            {isLoading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>

        <Link to="/" className="btn btn-secondary back-btn">
          Go Back
        </Link>

        <div className="demo-accounts">
          <p className="demo-title">Demo Accounts:</p>
          <div className="demo-list">
            <code>misha / misha123</code>
            <code>dima / dima123</code>
            <code>sasha / sasha123</code>
          </div>
        </div>
      </div>
    </div>
  )
}

export default Login
