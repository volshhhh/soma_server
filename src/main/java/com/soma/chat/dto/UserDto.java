package com.soma.chat.dto;

import com.soma.chat.model.entity.User;

import java.time.LocalDateTime;

/**
 * DTO для передачи информации о пользователе.
 * 
 * Используется для отображения пользователей в списке онлайн,
 * а также в ответах API.
 */
public class UserDto {

    private Long id;
    private String username;
    private String displayName;
    private String email;
    private boolean online;
    private LocalDateTime createdAt;

    // Конструкторы
    public UserDto() {}

    public UserDto(Long id, String username, String displayName, 
                   String email, boolean online, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.online = online;
        this.createdAt = createdAt;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String username;
        private String displayName;
        private String email;
        private boolean online;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder online(boolean online) { this.online = online; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public UserDto build() {
            return new UserDto(id, username, displayName, email, online, createdAt);
        }
    }

    /**
     * Преобразование Entity в DTO.
     * 
     * @param entity сущность User
     * @return DTO для передачи клиенту
     */
    public static UserDto fromEntity(User entity) {
        return UserDto.builder()
            .id(entity.getId())
            .username(entity.getUsername())
            .displayName(entity.getEffectiveDisplayName())
            .email(entity.getEmail())
            .online(entity.isOnline())
            .createdAt(entity.getCreatedAt())
            .build();
    }
}
