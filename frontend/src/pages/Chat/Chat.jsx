import { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import chatService from '../../services/chatService'
import MessageList from '../../components/Chat/MessageList'
import MessageInput from '../../components/Chat/MessageInput'
import RoomList from '../../components/Chat/RoomList'
import OnlineUsers from '../../components/Chat/OnlineUsers'
import './Chat.css'

const ROOMS = [
  { id: null, name: 'General', icon: '💬' },
  { id: 'music', name: 'Music', icon: '🎵' },
  { id: 'playlist', name: 'Playlist', icon: '📝' },
  { id: 'support', name: 'Support', icon: '🛟' },
]

function Chat() {
  const { roomId } = useParams()
  const navigate = useNavigate()
  const { user, isAuthenticated } = useAuth()
  const [messages, setMessages] = useState([])
  const [isConnected, setIsConnected] = useState(false)
  const [isConnecting, setIsConnecting] = useState(true)
  const [onlineUsers, setOnlineUsers] = useState([])
  const [events, setEvents] = useState([])
  const messagesEndRef = useRef(null)

  const currentRoom = ROOMS.find((r) => r.id === (roomId || null)) || ROOMS[0]

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

  // Subscribe to room when connected or room changes
  useEffect(() => {
    if (!isConnected) return

    // Clear messages when changing rooms
    setMessages([])
    setEvents([])

    // Load message history
    const loadHistory = async () => {
      const history = await chatService.getMessageHistory(roomId, 50)
      // Backend already returns messages in chronological order (oldest first)
      setMessages(history)
    }
    loadHistory()

    // Subscribe to room
    const handleMessage = (message) => {
      setMessages((prev) => [...prev, message])
    }

    const handleEvent = (event) => {
      setEvents((prev) => [...prev.slice(-9), event])
      
      // Update online users based on events
      if (event.type === 'JOIN') {
        setOnlineUsers((prev) => {
          if (!prev.find((u) => u.username === event.username)) {
            return [...prev, { username: event.username, displayName: event.displayName }]
          }
          return prev
        })
      } else if (event.type === 'LEAVE') {
        setOnlineUsers((prev) => prev.filter((u) => u.username !== event.username))
      }
    }

    chatService.subscribeToRoom(roomId, handleMessage, handleEvent)
    chatService.joinRoom(roomId)

    return () => {
      chatService.unsubscribeFromRoom(roomId)
    }
  }, [isConnected, roomId])

  const handleSendMessage = (content) => {
    if (content.trim() && isConnected) {
      chatService.sendMessage(content.trim(), roomId)
    }
  }

  const handleRoomChange = (newRoomId) => {
    if (newRoomId === null) {
      navigate('/chat')
    } else {
      navigate(`/chat/${newRoomId}`)
    }
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
          <RoomList
            rooms={ROOMS}
            currentRoomId={roomId}
            onRoomChange={handleRoomChange}
          />
          <OnlineUsers users={onlineUsers} />
        </aside>

        <main className="chat-main">
          <div className="chat-header">
            <div className="room-info">
              <span className="room-icon">{currentRoom.icon}</span>
              <h2>{currentRoom.name}</h2>
            </div>
            <div className="connection-status">
              <span className={`status-dot ${isConnected ? 'connected' : 'disconnected'}`}></span>
              <span>{isConnected ? 'Connected' : 'Disconnected'}</span>
            </div>
          </div>

          <MessageList
            messages={messages}
            events={events}
            currentUser={user}
            messagesEndRef={messagesEndRef}
          />

          <MessageInput
            onSend={handleSendMessage}
            disabled={!isConnected}
            placeholder={isConnected ? `Message #${currentRoom.name.toLowerCase()}` : 'Connecting...'}
          />
        </main>
      </div>
    </div>
  )
}

export default Chat
