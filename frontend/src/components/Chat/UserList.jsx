import './UserList.css'

/**
 * UserList component - displays all users for private messaging.
 * Allows selecting a user to start a private conversation.
 */
function UserList({ users, currentUser, selectedUser, onSelectUser }) {
  // Filter out the current user from the list
  const otherUsers = users.filter(
    (user) => user.username !== currentUser?.username
  )

  return (
    <div className="user-list">
      <div className="user-list-header">
        <h3>💬 Chats</h3>
        <span className="user-count">{otherUsers.length} users</span>
      </div>

      <div className="user-list-items">
        {otherUsers.length === 0 ? (
          <div className="no-users">
            <p>No other users yet</p>
          </div>
        ) : (
          otherUsers.map((user) => (
            <div
              key={user.id || user.username}
              className={`user-item ${
                selectedUser?.username === user.username ? 'selected' : ''
              } ${user.online ? 'online' : 'offline'}`}
              onClick={() => onSelectUser(user)}
            >
              <div className="user-avatar">
                {(user.displayName || user.username).charAt(0).toUpperCase()}
              </div>
              <div className="user-info">
                <span className="user-name">
                  {user.displayName || user.username}
                </span>
                <span className="user-status">
                  {user.online ? 'Online' : 'Offline'}
                </span>
              </div>
              <div className={`status-indicator ${user.online ? 'online' : 'offline'}`}></div>
            </div>
          ))
        )}
      </div>
    </div>
  )
}

export default UserList
