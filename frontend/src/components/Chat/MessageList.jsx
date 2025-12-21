import './MessageList.css'

function MessageList({ messages, events, currentUser, messagesEndRef }) {
  const formatTime = (timestamp) => {
    if (!timestamp) return ''
    const date = new Date(timestamp)
    return date.toLocaleTimeString('en-US', { 
      hour: '2-digit', 
      minute: '2-digit',
      hour12: false
    })
  }

  const formatDate = (timestamp) => {
    if (!timestamp) return ''
    const date = new Date(timestamp)
    const today = new Date()
    
    if (date.toDateString() === today.toDateString()) {
      return 'Today'
    }
    
    const yesterday = new Date(today)
    yesterday.setDate(yesterday.getDate() - 1)
    if (date.toDateString() === yesterday.toDateString()) {
      return 'Yesterday'
    }
    
    return date.toLocaleDateString('en-US', { 
      month: 'short', 
      day: 'numeric' 
    })
  }

  // Group messages by date
  const groupedMessages = messages.reduce((groups, message) => {
    const dateKey = message.createdAt ? formatDate(message.createdAt) : 'Today'
    if (!groups[dateKey]) {
      groups[dateKey] = []
    }
    groups[dateKey].push(message)
    return groups
  }, {})

  // Helper to get sender info - handles both nested and flat structures
  const getSenderUsername = (message) => {
    return message.sender?.username || message.senderUsername || ''
  }

  const getSenderDisplayName = (message) => {
    return message.sender?.displayName || message.senderDisplayName || getSenderUsername(message)
  }

  const isOwnMessage = (message) => {
    const senderUsername = getSenderUsername(message)
    return senderUsername === currentUser?.username
  }

  const isSystemMessage = (message) => {
    return message.type === 'SYSTEM' || message.type === 'JOIN' || message.type === 'LEAVE'
  }

  return (
    <div className="message-list">
      {Object.keys(groupedMessages).length === 0 && (
        <div className="no-messages">
          <p>No messages yet. Be the first to say hello! 👋</p>
        </div>
      )}

      {Object.entries(groupedMessages).map(([date, msgs]) => (
        <div key={date} className="message-group">
          {msgs.length > 0 && (
            <div className="date-separator">
              <span>{date}</span>
            </div>
          )}
          
          {msgs.map((message, index) => {
            // Skip rendering if message content is empty
            if (!message.content || message.content.trim() === '') {
              return null
            }

            const displayName = getSenderDisplayName(message)
            const isOwn = isOwnMessage(message)
            const isSystem = isSystemMessage(message)

            return (
              <div
                key={message.id || index}
                className={`message ${isOwn ? 'own' : ''} ${isSystem ? 'system' : ''}`}
              >
                {isSystem ? (
                  <div className="system-message">
                    <span>{message.content}</span>
                  </div>
                ) : (
                  <>
                    {!isOwn && (
                      <div className="message-avatar">
                        {(displayName || '?')[0].toUpperCase()}
                      </div>
                    )}
                    <div className="message-content">
                      {!isOwn && (
                        <div className="message-header">
                          <span className="message-author">
                            {displayName}
                          </span>
                          <span className="message-time">
                            {formatTime(message.createdAt)}
                          </span>
                        </div>
                      )}
                      <div className="message-text">{message.content}</div>
                      {isOwn && (
                        <span className="message-time own-time">
                          {formatTime(message.createdAt)}
                        </span>
                      )}
                    </div>
                  </>
                )}
              </div>
            )
          })}
        </div>
      ))}

      {/* Recent events */}
      {events.slice(-3).map((event, index) => (
        <div key={`event-${index}`} className="event-notification">
          {event.type === 'JOIN' ? (
            <span>👋 <strong>{event.displayName || event.username}</strong> joined the chat</span>
          ) : (
            <span>👋 <strong>{event.displayName || event.username}</strong> left the chat</span>
          )}
        </div>
      ))}

      <div ref={messagesEndRef} />
    </div>
  )
}

export default MessageList
