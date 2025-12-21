import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import './Welcome.css'

function Welcome() {
  const { isAuthenticated } = useAuth()

  return (
    <div className="welcome-page">
      <div className="welcome-container">
        <h1 className="welcome-title">Welcome to Soma Chat</h1>
        <p className="welcome-subtitle">
          Connect with your music community in real-time
        </p>
        
        <div className="feature-grid">
          <div className="feature-card">
            <h3>Real-time Messaging</h3>
            <p>Instant message delivery with WebSocket technology</p>
          </div>
          <div className="feature-card">
            <h3>Multiple Rooms</h3>
            <p>Create and join different chat rooms for various topics</p>
          </div>
          <div className="feature-card">
            <h3>Message History</h3>
            <p>Access your previous conversations anytime</p>
          </div>
          <div className="feature-card">
            <h3>Secure</h3>
            <p>Your messages are protected with authentication</p>
          </div>
        </div>

        <div className="welcome-actions">
          {isAuthenticated ? (
            <Link to="/chat" className="btn btn-primary">
              Go to Chat
            </Link>
          ) : (
            <>
              <Link to="/login" className="btn btn-primary">
                Get Started
              </Link>
            </>
          )}
        </div>
      </div>
    </div>
  )
}

export default Welcome
