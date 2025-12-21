import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import chatService from '../../services/chatService'
import MessageList from '../../components/Chat/MessageList'
import MessageInput from '../../components/Chat/MessageInput'
import UserList from '../../components/Chat/UserList'
import './Chat.css'

function Chat() {
  const navigate = useNavigate()
  const { user, isAuthenticated } = useAuth()
  const [messages, setMessages] = useState([])
  const [isConnected, setIsConnected] = useState(false)
  const [isConnecting, setIsConnecting] = useState(true)
  const [allUsers, setAllUsers] = useState([])
  const [selectedUser, setSelectedUser] = useState(null)
  const messagesEndRef = useRef(null)

  // Scroll to bottom when new messages arrive
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  // Connect to WebSocket on mount
  useEffect(() => {
    if (!isAuthenticated) return

    const connectToChat = async () => {
      setIsConnecting(true)
      try {
        await chatService.connect(user?.username)
        setIsConnected(true)
        
        // Load all users
        const users = await chatService.getAllUsers()
        setAllUsers(users)
      } catch (error) {
        console.error('Failed to connect:', error)
        setIsConnected(false)
      } finally {
        setIsConnecting(false)
      }
    }

    connectToChat()

    // Connection status handler
    const handleConnectionChange = (connected) => {
      setIsConnected(connected)
      if (!connected) {
        setIsConnecting(false)
      }
    }

    chatService.addConnectionHandler(handleConnectionChange)

    return () => {
      chatService.removeConnectionHandler(handleConnectionChange)
      chatService.disconnect()
    }
  }, [isAuthenticated, user])

  // Subscribe to user status updates when connected
  useEffect(() => {
    if (!isConnected) return

    // Subscribe to user status updates
    const handleStatusUpdate = (event) => {
      console.log('User status update:', event)
      const { user: updatedUser, type } = event
      if (!updatedUser) return

      setAllUsers((prevUsers) => {
        return prevUsers.map((u) => {
          if (u.username === updatedUser.username) {
            return {
              ...u,
              online: type === 'USER_ONLINE',
            }
          }
          return u
        })
      })
    }

    chatService.subscribeToUserStatus(handleStatusUpdate)

    // Also join the chat to trigger online status
    chatService.joinRoom()

    return () => {
      const statusSub = chatService.subscriptions.get('userStatus')
      if (statusSub?.messageSub) {
        statusSub.messageSub.unsubscribe()
        chatService.subscriptions.delete('userStatus')
      }
    }
  }, [isConnected])

  // Subscribe to private messages when connected
  useEffect(() => {
    if (!isConnected) return

    // Subscribe to private messages
    const handlePrivateMessage = (message) => {
      // Only add message if it's for the currently selected conversation
      const senderUsername = message.sender?.username
      const recipientUsername = message.recipient?.username
      const currentUsername = user?.username
      
      // Check if this message belongs to the current conversation
      if (selectedUser) {
        const isRelevant = 
          (senderUsername === selectedUser.username && recipientUsername === currentUsername) ||
          (senderUsername === currentUsername && recipientUsername === selectedUser.username)
        
        if (isRelevant) {
          setMessages((prev) => [...prev, message])
        }
      }
    }

    chatService.subscribeToPrivateMessages(handlePrivateMessage)

    return () => {
      // Clean up subscription
      const privateSub = chatService.subscriptions.get('private')
      if (privateSub?.messageSub) {
        privateSub.messageSub.unsubscribe()
        chatService.subscriptions.delete('private')
      }
    }
  }, [isConnected, selectedUser, user])

  // Load conversation history when selecting a user
  useEffect(() => {
    if (!isConnected || !selectedUser) return

    const loadConversation = async () => {
      setMessages([])
      try {
        const history = await chatService.getPrivateMessageHistory(
          selectedUser.username,
          50
        )
        setMessages(history)
      } catch (error) {
        console.error('Failed to load conversation:', error)
      }
    }

    loadConversation()
  }, [isConnected, selectedUser])

  const handleSendMessage = (content) => {
    if (content.trim() && isConnected && selectedUser) {
      chatService.sendPrivateMessage(content.trim(), selectedUser.username)
    }
  }

  const handleSelectUser = (userToChat) => {
    setSelectedUser(userToChat)
    setMessages([])
  }

  if (isConnecting) {
    return (
      <div className="chat-page">
        <div className="connecting-screen">
          <div className="spinner"></div>
          <p>Connecting to chat...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="chat-page">
      <div className="chat-layout">
        <aside className="chat-sidebar">
          <UserList
            users={allUsers}
            currentUser={user}
            selectedUser={selectedUser}
            onSelectUser={handleSelectUser}
          />
        </aside>

        <main className="chat-main">
          {selectedUser ? (
            <>
              <div className="chat-header">
                <div className="room-info">
                  <div className="recipient-avatar">
                    {(selectedUser.displayName || selectedUser.username)
                      .charAt(0)
                      .toUpperCase()}
                  </div>
                  <div className="recipient-info">
                    <h2>{selectedUser.displayName || selectedUser.username}</h2>
                    <span className="recipient-username">
                      @{selectedUser.username}
                    </span>
                  </div>
                </div>
                <div className="connection-status">
                  <span
                    className={`status-dot ${
                      isConnected ? 'connected' : 'disconnected'
                    }`}
                  ></span>
                  <span>{isConnected ? 'Connected' : 'Disconnected'}</span>
                </div>
              </div>

              <MessageList
                messages={messages}
                events={[]}
                currentUser={user}
                messagesEndRef={messagesEndRef}
              />

              <MessageInput
                onSend={handleSendMessage}
                disabled={!isConnected}
                placeholder={
                  isConnected
                    ? `Message @${selectedUser.username}`
                    : 'Connecting...'
                }
              />
            </>
          ) : (
            <div className="no-chat-selected">
              <div className="no-chat-icon">💬</div>
              <h2>Select a user to start chatting</h2>
              <p>Choose someone from the list on the left to begin a private conversation</p>
            </div>
          )}
        </main>
      </div>
    </div>
  )
}

export default Chat
