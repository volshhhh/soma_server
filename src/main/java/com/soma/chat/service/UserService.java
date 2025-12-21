package com.soma.chat.service;

import com.soma.chat.dto.UserDto;
import com.soma.chat.model.entity.User;
import com.soma.chat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Сервис для работы с пользователями.
 * 
 * Содержит бизнес-логику для операций с пользователями:
 * - Поиск и получение пользователей
 * - Управление статусом онлайн
 * - Создание новых пользователей
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Поиск пользователя по username.
     * 
     * @param username логин пользователя
     * @return Optional с пользователем
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Получение пользователя по username.
     * Выбрасывает исключение, если пользователь не найден.
     * 
     * @param username логин пользователя
     * @return пользователь
     * @throws RuntimeException если пользователь не найден
     */
    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));
    }

    /**
     * Получение пользователя по ID.
     * 
     * @param id идентификатор пользователя
     * @return Optional с пользователем
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Получение всех пользователей.
     * 
     * @return список всех пользователей в виде DTO
     */
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
            .map(UserDto::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Получение списка онлайн пользователей.
     * 
     * @return список онлайн пользователей в виде DTO
     */
    @Transactional(readOnly = true)
    public List<UserDto> getOnlineUsers() {
        return userRepository.findByOnlineTrue().stream()
            .map(UserDto::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Установка статуса онлайн для пользователя.
     * 
     * @param username логин пользователя
     * @param online статус онлайн
     */
    @Transactional
    public void setOnlineStatus(String username, boolean online) {
        log.info("Установка статуса online={} для пользователя: {}", online, username);
        userRepository.updateOnlineStatus(username, online);
    }

    /**
     * Создание нового пользователя.
     * 
     * @param username логин
     * @param displayName отображаемое имя
     * @param email email
     * @return созданный пользователь
     */
    @Transactional
    public User createUser(String username, String displayName, String email) {
        // Проверка уникальности username
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Пользователь с таким username уже существует: " + username);
        }

        // Проверка уникальности email
        if (email != null && userRepository.existsByEmail(email)) {
            throw new RuntimeException("Пользователь с таким email уже существует: " + email);
        }

        User user = new User();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setEmail(email);

        User saved = userRepository.save(user);
        log.info("Создан новый пользователь: {}", saved.getUsername());
        return saved;
    }

    /**
     * Получение или создание пользователя.
     * Используется для автоматического создания пользователя при первом входе.
     * 
     * @param username логин пользователя
     * @return существующий или новый пользователь
     */
    @Transactional
    public User getOrCreateUser(String username) {
        return userRepository.findByUsername(username)
            .orElseGet(() -> {
                log.info("Создание пользователя при первом входе: {}", username);
                return createUser(username, username, null);
            });
    }
}
