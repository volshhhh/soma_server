import './RoomList.css'

function RoomList({ rooms, currentRoomId, onRoomChange }) {
  return (
    <div className="room-list">
      <h3 className="room-list-title">Chat Rooms</h3>
      <ul className="rooms">
        {rooms.map((room) => (
          <li key={room.id || 'general'}>
            <button
              className={`room-item ${currentRoomId === room.id ? 'active' : ''}`}
              onClick={() => onRoomChange(room.id)}
            >
              <span className="room-item-icon">{room.icon}</span>
              <span className="room-item-name">{room.name}</span>
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}

export default RoomList
