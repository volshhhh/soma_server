package com.soma.chat.repository;

import com.soma.chat.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с пользователями.
 * 
 * Предоставляет методы для CRUD операций и специализированных запросов
 * по сущности User.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Поиск пользователя по username.
     * 
     * @param username логин пользователя
     * @return Optional с пользователем, если найден
     */
    Optional<User> findByUsername(String username);

    /**
     * Поиск пользователя по email.
     * 
     * @param email email пользователя
     * @return Optional с пользователем, если найден
     */
    Optional<User> findByEmail(String email);

    /**
     * Проверка существования пользователя по username.
     * 
     * @param username логин пользователя
     * @return true, если пользователь существует
     */
    boolean existsByUsername(String username);

    /**
     * Проверка существования пользователя по email.
     * 
     * @param email email пользователя
     * @return true, если email уже используется
     */
    boolean existsByEmail(String email);

    /**
     * Получение списка онлайн пользователей.
     * 
     * @return список пользователей со статусом online = true
     */
    List<User> findByOnlineTrue();

    /**
     * Обновление статуса онлайн для пользователя.
     * 
     * @param username логин пользователя
     * @param online новый статус
     */
    @Modifying
    @Query("UPDATE User u SET u.online = :online WHERE u.username = :username")
    void updateOnlineStatus(@Param("username") String username, @Param("online") boolean online);
}
