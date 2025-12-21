# Soma Chat Frontend

A React-based frontend for the Soma Chat application, built with Vite and styled to match the Soma app design.

## Features

- 🎨 **Dark theme** with Spotify-inspired green accents
- 💬 **Real-time messaging** via WebSocket/STOMP
- 🏠 **Multiple chat rooms** (General, Music, Playlist, Support)
- 👥 **Online user tracking**
- 📜 **Message history** persistence
- 🔐 **Authentication** with session management
- 📱 **Responsive design** for mobile and desktop

## Tech Stack

- **React 18** - UI library
- **Vite** - Build tool and dev server
- **React Router 6** - Client-side routing
- **@stomp/stompjs** - WebSocket STOMP client
- **SockJS** - WebSocket fallback

## Project Structure

```
frontend/
├── public/
│   └── vite.svg          # App icon
├── src/
│   ├── components/
│   │   ├── Chat/
│   │   │   ├── MessageList.jsx
│   │   │   ├── MessageInput.jsx
│   │   │   ├── RoomList.jsx
│   │   │   └── OnlineUsers.jsx
│   │   └── Header/
│   │       └── Header.jsx
│   ├── context/
│   │   └── AuthContext.jsx
│   ├── pages/
│   │   ├── Chat/
│   │   │   └── Chat.jsx
│   │   ├── Home/
│   │   │   └── Home.jsx
│   │   ├── Login/
│   │   │   └── Login.jsx
│   │   └── Welcome/
│   │       └── Welcome.jsx
│   ├── services/
│   │   └── chatService.js
│   ├── App.jsx
│   ├── App.css
│   ├── main.jsx
│   └── index.css
├── index.html
├── package.json
└── vite.config.js
```

## Getting Started

### Prerequisites

- Node.js 18+ and npm
- Backend server running on port 8080

### Installation

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

The app will be available at `http://localhost:3000`

### Build for Production

```bash
npm run build
```

The built files will be in the `dist/` directory.

## Configuration

The Vite dev server is configured to proxy API and WebSocket requests to the backend:

- `/api/*` → `http://localhost:8080`
- `/ws-chat` → `http://localhost:8080` (WebSocket)

## Demo Accounts

| Username | Password | Role  |
|----------|----------|-------|
| alice    | password | User  |
| bob      | password | User  |
| admin    | admin    | Admin |

## Pages

### Welcome Page
Landing page with features overview and call-to-action buttons.

### Login Page
Authentication form matching Soma app design with demo account info.

### Home Page
Dashboard with quick access to different chat rooms.

### Chat Page
Real-time chat interface with:
- Sidebar with room list and online users
- Message display with timestamps
- Message input with send button
- Join/leave event notifications

## Styling

The app uses CSS custom properties for theming:

```css
--primary-green: #1DB954       /* Spotify green */
--dark-bg: #121212             /* Main background */
--dark-surface: #1e1e1e        /* Card backgrounds */
--header-bg: #1a7f37           /* Header green */
--gold-accent: #c4a747         /* Secondary accent */
```

## WebSocket Events

The chat service handles:
- **Connection management** - Auto-reconnect on disconnect
- **Room subscriptions** - Subscribe/unsubscribe to specific rooms
- **Message sending** - Send messages to current room
- **Event handling** - User join/leave notifications

## License

MIT License - Soma Chat Application
