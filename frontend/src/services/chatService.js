import { Client } from '@stomp/stompjs'

class ChatService {
  constructor() {
    this.client = null
    this.subscriptions = new Map()
    this.isConnected = false
    this.messageHandlers = new Set()
    this.eventHandlers = new Set()
    this.connectionHandlers = new Set()
  }

  connect(username) {
    return new Promise((resolve, reject) => {
      const auth = localStorage.getItem('soma_auth')
      
      // Use the proxy - in dev mode, Vite proxies ws://localhost:3000/ws-chat to backend
      const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const wsHost = window.location.host
      
      // Use pure WebSocket endpoint with auth token in query string
      const authParam = auth ? `?token=${auth}` : ''
      const brokerURL = `${wsProtocol}//${wsHost}/ws-chat${authParam}`
      
      console.log('[ChatService] Connecting to:', brokerURL.replace(/token=.*/, 'token=***'))
      
      this.client = new Client({
        brokerURL: brokerURL,
        connectHeaders: {
          // Pass auth token in STOMP headers as well for SockJS fallback
          'Authorization': auth ? `Basic ${auth}` : '',
          login: username || '',
          passcode: '',
        },
        debug: (str) => {
          console.log('[STOMP]', str)
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: (frame) => {
          console.log('Connected to WebSocket')
          this.isConnected = true
          this.notifyConnectionHandlers(true)
          resolve(frame)
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame.headers['message'])
          this.isConnected = false
          this.notifyConnectionHandlers(false)
          reject(new Error(frame.headers['message']))
        },
        onDisconnect: () => {
          console.log('Disconnected from WebSocket')
          this.isConnected = false
          this.notifyConnectionHandlers(false)
        },
        onWebSocketClose: () => {
          console.log('WebSocket connection closed')
          this.isConnected = false
          this.notifyConnectionHandlers(false)
        },
      })

      this.client.activate()
    })
  }

  disconnect() {
    if (this.client) {
      this.subscriptions.forEach((sub) => sub.unsubscribe())
      this.subscriptions.clear()
      this.client.deactivate()
      this.isConnected = false
    }
  }

  subscribeToRoom(roomId, onMessage, onEvent) {
    if (!this.client || !this.isConnected) {
      console.error('Not connected to WebSocket')
      return null
    }

    const topic = roomId ? `/topic/room.${roomId}` : '/topic/public'
    const eventTopic = roomId ? `/topic/room.${roomId}.events` : '/topic/public.events'

    // Subscribe to messages
    const messageSub = this.client.subscribe(topic, (message) => {
      const data = JSON.parse(message.body)
      if (onMessage) onMessage(data)
      this.notifyMessageHandlers(data)
    })

    // Subscribe to events (join/leave)
    const eventSub = this.client.subscribe(eventTopic, (message) => {
      const data = JSON.parse(message.body)
      if (onEvent) onEvent(data)
      this.notifyEventHandlers(data)
    })

    const subscriptionKey = roomId || 'public'
    this.subscriptions.set(subscriptionKey, { messageSub, eventSub })

    return subscriptionKey
  }

  unsubscribeFromRoom(roomId) {
    const key = roomId || 'public'
    const subs = this.subscriptions.get(key)
    if (subs) {
      subs.messageSub?.unsubscribe()
      subs.eventSub?.unsubscribe()
      this.subscriptions.delete(key)
    }
  }

  /**
   * Subscribe to user online/offline status updates
   */
  subscribeToUserStatus(onStatusUpdate) {
    if (!this.client || !this.isConnected) {
      console.error('Not connected to WebSocket')
      return null
    }

    const statusSub = this.client.subscribe('/topic/users.status', (message) => {
      const data = JSON.parse(message.body)
      console.log('[ChatService] User status update:', data)
      if (onStatusUpdate) onStatusUpdate(data)
    })

    this.subscriptions.set('userStatus', { messageSub: statusSub })
    return 'userStatus'
  }

  sendMessage(content, roomId = null) {
    if (!this.client || !this.isConnected) {
      console.error('Not connected to WebSocket')
      return false
    }

    const destination = roomId ? `/app/chat.send.${roomId}` : '/app/chat.send'
    
    this.client.publish({
      destination,
      body: JSON.stringify({ content }),
    })

    return true
  }

  /**
   * Send a private message to a specific user
   */
  sendPrivateMessage(content, recipientUsername) {
    if (!this.client || !this.isConnected) {
      console.error('Not connected to WebSocket')
      return false
    }

    this.client.publish({
      destination: '/app/chat.private',
      body: JSON.stringify({ 
        content, 
        recipientUsername 
      }),
    })

    return true
  }

  /**
   * Subscribe to private messages for the current user
   */
  subscribeToPrivateMessages(onMessage) {
    if (!this.client || !this.isConnected) {
      console.error('Not connected to WebSocket')
      return null
    }

    // Subscribe to private queue for this user
    const privateSub = this.client.subscribe('/user/queue/private', (message) => {
      const data = JSON.parse(message.body)
      if (onMessage) onMessage(data)
      this.notifyMessageHandlers(data)
    })

    this.subscriptions.set('private', { messageSub: privateSub })
    return 'private'
  }

  /**
   * Request private message history from the server
   */
  requestPrivateHistory(recipientUsername) {
    if (!this.client || !this.isConnected) {
      console.error('Not connected to WebSocket')
      return false
    }

    this.client.publish({
      destination: '/app/chat.private.history',
      body: JSON.stringify({ recipientUsername }),
    })

    return true
  }

  /**
   * Subscribe to private history response
   */
  subscribeToPrivateHistory(onHistory) {
    if (!this.client || !this.isConnected) {
      console.error('Not connected to WebSocket')
      return null
    }

    const historySub = this.client.subscribe('/user/queue/private.history', (message) => {
      const data = JSON.parse(message.body)
      if (onHistory) onHistory(data)
    })

    this.subscriptions.set('privateHistory', { messageSub: historySub })
    return 'privateHistory'
  }

  joinRoom(roomId = null) {
    if (!this.client || !this.isConnected) {
      console.error('Not connected to WebSocket')
      return false
    }

    this.client.publish({
      destination: '/app/chat.join',
      body: JSON.stringify({ roomId }),
    })

    return true
  }

  async getMessageHistory(roomId = null, limit = 50) {
    const auth = localStorage.getItem('soma_auth')
    // Use 'public' as the default room ID (matches backend)
    const actualRoomId = roomId || 'public'
    const url = `/api/chat/rooms/${actualRoomId}/messages?limit=${limit}`

    try {
      const response = await fetch(url, {
        headers: {
          Authorization: auth ? `Basic ${auth}` : '',
          'Content-Type': 'application/json',
        },
      })

      if (!response.ok) {
        throw new Error('Failed to fetch message history')
      }

      return await response.json()
    } catch (error) {
      console.error('Error fetching message history:', error)
      return []
    }
  }

  /**
   * Get private message history with a specific user via REST API
   */
  async getPrivateMessageHistory(recipientUsername, limit = 50) {
    const auth = localStorage.getItem('soma_auth')
    const url = `/api/chat/private/${recipientUsername}/messages?limit=${limit}`

    try {
      const response = await fetch(url, {
        headers: {
          Authorization: auth ? `Basic ${auth}` : '',
          'Content-Type': 'application/json',
        },
      })

      if (!response.ok) {
        throw new Error('Failed to fetch private message history')
      }

      return await response.json()
    } catch (error) {
      console.error('Error fetching private message history:', error)
      return []
    }
  }

  /**
   * Get all users from the server
   */
  async getAllUsers() {
    const auth = localStorage.getItem('soma_auth')
    const url = '/api/chat/users'

    try {
      const response = await fetch(url, {
        headers: {
          Authorization: auth ? `Basic ${auth}` : '',
          'Content-Type': 'application/json',
        },
      })

      if (!response.ok) {
        throw new Error('Failed to fetch users')
      }

      return await response.json()
    } catch (error) {
      console.error('Error fetching users:', error)
      return []
    }
  }

  // Handler management
  addMessageHandler(handler) {
    this.messageHandlers.add(handler)
  }

  removeMessageHandler(handler) {
    this.messageHandlers.delete(handler)
  }

  addEventHandler(handler) {
    this.eventHandlers.add(handler)
  }

  removeEventHandler(handler) {
    this.eventHandlers.delete(handler)
  }

  addConnectionHandler(handler) {
    this.connectionHandlers.add(handler)
  }

  removeConnectionHandler(handler) {
    this.connectionHandlers.delete(handler)
  }

  notifyMessageHandlers(message) {
    this.messageHandlers.forEach((handler) => handler(message))
  }

  notifyEventHandlers(event) {
    this.eventHandlers.forEach((handler) => handler(event))
  }

  notifyConnectionHandlers(isConnected) {
    this.connectionHandlers.forEach((handler) => handler(isConnected))
  }
}

// Singleton instance
const chatService = new ChatService()
export default chatService
