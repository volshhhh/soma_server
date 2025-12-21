import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import './Header.css'

function Header() {
  const { isAuthenticated, user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <header className="header">
      <div className="header-content">
        <div className="header-left">
          <Link to="/" className="logo">
            <span className="logo-icon">🎵</span>
            <span className="logo-text">Soma</span>
          </Link>
          <nav className="nav-links">
            <Link to="/" className="nav-link">Welcome</Link>
            {isAuthenticated && (
              <>
                <Link to="/home" className="nav-link">Home</Link>
                <Link to="/chat" className="nav-link">Chat</Link>
              </>
            )}
          </nav>
        </div>
        <div className="header-right">
          {isAuthenticated ? (
            <div className="user-menu">
              <span className="user-name">{user?.displayName || user?.username}</span>
              <button onClick={handleLogout} className="logout-btn">
                Logout
              </button>
            </div>
          ) : (
            <Link to="/login" className="nav-link login-link">Login</Link>
          )}
        </div>
      </div>
    </header>
  )
}

export default Header
