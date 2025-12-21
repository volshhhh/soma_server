import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import './Home.css'

function Home() {
  const { user } = useAuth()

  return (
    <div className="home-page">
      <div className="home-container">
        <div className="welcome-section">
          <h1>Welcome, {user?.displayName || user?.username}!</h1>
          <p>What would you like to do today?</p>
        </div>

        <div className="action-cards">
          <Link to="/chat" className="action-card">
            <div className="action-icon">💬</div>
            <h3>General Chat</h3>
            <p>Join the main chat room and connect with others</p>
          </Link>

          <Link to="/chat/music" className="action-card">
            <div className="action-icon">🎵</div>
            <h3>Music Room</h3>
            <p>Discuss your favorite tracks and artists</p>
          </Link>

          <Link to="/chat/playlist" className="action-card">
            <div className="action-icon">📝</div>
            <h3>Playlist Room</h3>
            <p>Share and discover new playlists</p>
          </Link>

          <Link to="/chat/support" className="action-card">
            <div className="action-icon">🛟</div>
            <h3>Support</h3>
            <p>Get help with transfers and technical issues</p>
          </Link>
        </div>

        <div className="quick-stats">
          <div className="stat-item">
            <span className="stat-value">Online</span>
            <span className="stat-label">Status</span>
          </div>
          <div className="stat-item">
            <span className="stat-value">{user?.username}</span>
            <span className="stat-label">Username</span>
          </div>
        </div>
      </div>
    </div>
  )
}

export default Home
