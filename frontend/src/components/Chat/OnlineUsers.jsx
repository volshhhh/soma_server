import './OnlineUsers.css'

function OnlineUsers({ users }) {
  return (
    <div className="online-users">
      <h3 className="online-users-title">
        Online — {users.length}
      </h3>
      {users.length === 0 ? (
        <p className="no-users">No users online</p>
      ) : (
        <ul className="users-list">
          {users.map((user, index) => (
            <li key={user.username || index} className="user-item">
              <div className="user-avatar">
                {(user.displayName || user.username || '?')[0].toUpperCase()}
              </div>
              <span className="user-name">{user.displayName || user.username}</span>
              <span className="online-dot"></span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default OnlineUsers
