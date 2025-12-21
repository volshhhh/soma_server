package com.soma.chat.dto;

/**
 * DTO для событий присоединения/выхода из чата.
 * 
 * Отправляется всем подписчикам комнаты при изменении
 * состава участников.
 */
public class ChatEventDto {

    private EventType type;
    private String roomId;
    private UserDto user;
    private String message;

    /**
     * Типы событий чата.
     */
    public enum EventType {
        /** Пользователь присоединился к комнате */
        USER_JOINED,
        /** Пользователь покинул комнату */
        USER_LEFT,
        /** Пользователь печатает */
        USER_TYPING,
        /** Обновление списка онлайн пользователей */
        ONLINE_USERS_UPDATE,
        /** Пользователь стал онлайн */
        USER_ONLINE,
        /** Пользователь ушел оффлайн */
        USER_OFFLINE
    }

    // Конструкторы
    public ChatEventDto() {}

    public ChatEventDto(EventType type, String roomId, UserDto user, String message) {
        this.type = type;
        this.roomId = roomId;
        this.user = user;
        this.message = message;
    }

    // Геттеры и сеттеры
    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private EventType type;
        private String roomId;
        private UserDto user;
        private String message;

        public Builder type(EventType type) { this.type = type; return this; }
        public Builder roomId(String roomId) { this.roomId = roomId; return this; }
        public Builder user(UserDto user) { this.user = user; return this; }
        public Builder message(String message) { this.message = message; return this; }

        public ChatEventDto build() {
            return new ChatEventDto(type, roomId, user, message);
        }
    }
}
