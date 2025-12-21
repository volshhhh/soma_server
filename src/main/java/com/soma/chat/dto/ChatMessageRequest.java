package com.soma.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO для входящего сообщения чата от клиента.
 * 
 * Используется для приема сообщений через WebSocket.
 * Не содержит информации об отправителе — она определяется
 * из Principal (аутентифицированного пользователя).
 */
public class ChatMessageRequest {

    /**
     * Идентификатор комнаты, в которую отправляется сообщение.
     * По умолчанию "public".
     */
    @NotBlank(message = "Room ID обязателен")
    @Size(max = 100, message = "Room ID не должен превышать 100 символов")
    private String roomId = "public";

    /**
     * Текстовое содержимое сообщения.
     */
    @NotBlank(message = "Содержимое сообщения не может быть пустым")
    @Size(max = 4000, message = "Сообщение не должно превышать 4000 символов")
    private String content;

    /**
     * Тип сообщения (опционально).
     * По умолчанию TEXT.
     */
    private String type;

    // Конструкторы
    public ChatMessageRequest() {}

    public ChatMessageRequest(String roomId, String content, String type) {
        this.roomId = roomId;
        this.content = content;
        this.type = type;
    }

    // Геттеры и сеттеры
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String roomId = "public";
        private String content;
        private String type;

        public Builder roomId(String roomId) { this.roomId = roomId; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder type(String type) { this.type = type; return this; }

        public ChatMessageRequest build() {
            return new ChatMessageRequest(roomId, content, type);
        }
    }
}
