package com.soma.chat.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Сущность сообщения чата.
 * 
 * Представляет одно сообщение в чате, связанное с отправителем
 * и определенной комнатой (roomId).
 */
@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_message_room", columnList = "room_id"),
    @Index(name = "idx_message_created", columnList = "created_at"),
    @Index(name = "idx_message_sender", columnList = "sender_id")
})
public class ChatMessage {

    /**
     * Уникальный идентификатор сообщения.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Отправитель сообщения.
     * ManyToOne связь с User.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * Текстовое содержимое сообщения.
     */
    @NotBlank(message = "Содержимое сообщения не может быть пустым")
    @Size(max = 4000, message = "Сообщение не должно превышать 4000 символов")
    @Column(nullable = false, length = 4000)
    private String content;

    /**
     * Идентификатор комнаты чата.
     * Позволяет организовать сообщения по разным каналам/комнатам.
     * По умолчанию "public" - публичный канал.
     */
    @NotBlank(message = "Room ID обязателен")
    @Column(name = "room_id", nullable = false, length = 100)
    private String roomId = "public";

    /**
     * Тип сообщения.
     * TEXT - обычное текстовое сообщение
     * SYSTEM - системное сообщение (вход/выход пользователя и т.д.)
     * JOIN - пользователь присоединился к чату
     * LEAVE - пользователь покинул чат
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType type = MessageType.TEXT;

    /**
     * Дата и время создания сообщения.
     * Устанавливается автоматически при сохранении.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Типы сообщений чата.
     */
    public enum MessageType {
        /** Обычное текстовое сообщение */
        TEXT,
        /** Системное уведомление */
        SYSTEM,
        /** Пользователь присоединился к комнате */
        JOIN,
        /** Пользователь покинул комнату */
        LEAVE
    }

    // Конструкторы
    public ChatMessage() {}

    public ChatMessage(Long id, User sender, String content, String roomId, 
                       MessageType type, LocalDateTime createdAt) {
        this.id = id;
        this.sender = sender;
        this.content = content;
        this.roomId = roomId;
        this.type = type;
        this.createdAt = createdAt;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Builder pattern
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private User sender;
        private String content;
        private String roomId = "public";
        private MessageType type = MessageType.TEXT;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder sender(User sender) { this.sender = sender; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder roomId(String roomId) { this.roomId = roomId; return this; }
        public Builder type(MessageType type) { this.type = type; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ChatMessage build() {
            return new ChatMessage(id, sender, content, roomId, type, createdAt);
        }
    }
}
