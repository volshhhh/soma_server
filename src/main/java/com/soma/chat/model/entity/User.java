package com.soma.chat.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность пользователя чата.
 * 
 * Представляет зарегистрированного пользователя, который может
 * отправлять и получать сообщения в чате.
 * 
 * В будущем эта сущность может быть расширена для интеграции
 * с функционалом трансфера музыки в Spotify.
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_username", columnList = "username", unique = true),
    @Index(name = "idx_user_email", columnList = "email", unique = true)
})
public class User {

    /**
     * Уникальный идентификатор пользователя.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Логин пользователя (уникальный).
     * Используется для аутентификации и идентификации в системе.
     */
    @NotBlank(message = "Username обязателен")
    @Size(min = 3, max = 50, message = "Username должен быть от 3 до 50 символов")
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * Отображаемое имя пользователя.
     * Показывается другим пользователям в чате.
     */
    @Size(max = 100, message = "Display name не должен превышать 100 символов")
    @Column(length = 100)
    private String displayName;

    /**
     * Email пользователя (уникальный).
     */
    @Email(message = "Некорректный формат email")
    @Column(unique = true, length = 255)
    private String email;

    /**
     * Дата и время создания аккаунта.
     * Устанавливается автоматически при создании записи.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Список сообщений, отправленных пользователем.
     * Связь OneToMany с ChatMessage.
     */
    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();

    /**
     * Статус онлайн/офлайн пользователя.
     * Используется для отображения активных пользователей в чате.
     */
    @Column(nullable = false)
    private boolean online = false;

    // Конструкторы
    public User() {}

    public User(Long id, String username, String displayName, String email, 
                LocalDateTime createdAt, List<ChatMessage> messages, boolean online) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.createdAt = createdAt;
        this.messages = messages;
        this.online = online;
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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    /**
     * Возвращает отображаемое имя.
     * Если displayName не задан, возвращает username.
     */
    public String getEffectiveDisplayName() {
        return displayName != null && !displayName.isBlank() 
            ? displayName 
            : username;
    }
}
