package com.soma.chat.dto;

import com.soma.chat.model.entity.ChatMessage;

import java.time.LocalDateTime;

/**
 * DTO для исходящего сообщения чата к клиенту.
 * 
 * Используется для отправки сообщений через WebSocket
 * и в REST ответах. Содержит всю необходимую информацию
 * для отображения сообщения на клиенте.
 */
public class ChatMessageResponse {

    private Long id;
    private String roomId;
    private SenderInfo sender;
    private SenderInfo recipient;
    private String content;
    private String type;
    private LocalDateTime createdAt;

    // Конструкторы
    public ChatMessageResponse() {}

    public ChatMessageResponse(Long id, String roomId, SenderInfo sender, SenderInfo recipient,
                               String content, String type, LocalDateTime createdAt) {
        this.id = id;
        this.roomId = roomId;
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.type = type;
        this.createdAt = createdAt;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public SenderInfo getSender() { return sender; }
    public void setSender(SenderInfo sender) { this.sender = sender; }

    public SenderInfo getRecipient() { return recipient; }
    public void setRecipient(SenderInfo recipient) { this.recipient = recipient; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /**
     * Вложенный класс для информации об отправителе.
     * Содержит минимальный набор данных для отображения.
     */
    public static class SenderInfo {
        private Long id;
        private String username;
        private String displayName;

        public SenderInfo() {}

        public SenderInfo(Long id, String username, String displayName) {
            this.id = id;
            this.username = username;
            this.displayName = displayName;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private Long id;
            private String username;
            private String displayName;

            public Builder id(Long id) { this.id = id; return this; }
            public Builder username(String username) { this.username = username; return this; }
            public Builder displayName(String displayName) { this.displayName = displayName; return this; }

            public SenderInfo build() {
                return new SenderInfo(id, username, displayName);
            }
        }
    }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String roomId;
        private SenderInfo sender;
        private SenderInfo recipient;
        private String content;
        private String type;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder roomId(String roomId) { this.roomId = roomId; return this; }
        public Builder sender(SenderInfo sender) { this.sender = sender; return this; }
        public Builder recipient(SenderInfo recipient) { this.recipient = recipient; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ChatMessageResponse build() {
            return new ChatMessageResponse(id, roomId, sender, recipient, content, type, createdAt);
        }
    }

    /**
     * Статический метод для преобразования Entity в DTO.
     * 
     * @param entity сущность ChatMessage из БД
     * @return DTO для передачи клиенту
     */
    public static ChatMessageResponse fromEntity(ChatMessage entity) {
        SenderInfo recipientInfo = null;
        if (entity.getRecipient() != null) {
            recipientInfo = SenderInfo.builder()
                .id(entity.getRecipient().getId())
                .username(entity.getRecipient().getUsername())
                .displayName(entity.getRecipient().getEffectiveDisplayName())
                .build();
        }
        
        return ChatMessageResponse.builder()
            .id(entity.getId())
            .roomId(entity.getRoomId())
            .sender(SenderInfo.builder()
                .id(entity.getSender().getId())
                .username(entity.getSender().getUsername())
                .displayName(entity.getSender().getEffectiveDisplayName())
                .build())
            .recipient(recipientInfo)
            .content(entity.getContent())
            .type(entity.getType().name())
            .createdAt(entity.getCreatedAt())
            .build();
    }
}
