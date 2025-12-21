package com.soma.chat.repository;

import com.soma.chat.model.entity.ChatMessage;
import com.soma.chat.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Репозиторий для работы с сообщениями чата.
 * 
 * Предоставляет методы для CRUD операций и специализированных запросов
 * по сущности ChatMessage.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Получение сообщений комнаты с сортировкой по времени создания.
     * Использует пагинацию для ограничения количества результатов.
     * 
     * @param roomId идентификатор комнаты
     * @param pageable параметры пагинации
     * @return страница сообщений
     */
    Page<ChatMessage> findByRoomIdOrderByCreatedAtDesc(String roomId, Pageable pageable);

    /**
     * Получение всех сообщений комнаты, отсортированных по времени.
     * 
     * @param roomId идентификатор комнаты
     * @return список сообщений в хронологическом порядке
     */
    List<ChatMessage> findByRoomIdOrderByCreatedAtAsc(String roomId);

    /**
     * Получение последних N сообщений комнаты.
     * Используется для начальной загрузки истории чата.
     * 
     * @param roomId идентификатор комнаты
     * @param pageable параметры пагинации (размер = количество сообщений)
     * @return список последних сообщений
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.roomId = :roomId ORDER BY m.createdAt DESC")
    List<ChatMessage> findLatestMessages(@Param("roomId") String roomId, Pageable pageable);

    /**
     * Получение сообщений комнаты после определенного времени.
     * Полезно для подгрузки новых сообщений.
     * 
     * @param roomId идентификатор комнаты
     * @param after время, после которого искать сообщения
     * @return список новых сообщений
     */
    List<ChatMessage> findByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(
        String roomId, 
        LocalDateTime after
    );

    /**
     * Получение сообщений определенного пользователя.
     * 
     * @param sender пользователь-отправитель
     * @return список сообщений пользователя
     */
    List<ChatMessage> findBySenderOrderByCreatedAtDesc(User sender);

    /**
     * Получение сообщений пользователя в конкретной комнате.
     * 
     * @param sender пользователь-отправитель
     * @param roomId идентификатор комнаты
     * @param pageable параметры пагинации
     * @return страница сообщений
     */
    Page<ChatMessage> findBySenderAndRoomIdOrderByCreatedAtDesc(
        User sender, 
        String roomId, 
        Pageable pageable
    );

    /**
     * Подсчет количества сообщений в комнате.
     * 
     * @param roomId идентификатор комнаты
     * @return количество сообщений
     */
    long countByRoomId(String roomId);

    /**
     * Поиск сообщений по содержимому (регистронезависимый).
     * 
     * @param roomId идентификатор комнаты
     * @param searchTerm поисковый запрос
     * @param pageable параметры пагинации
     * @return страница найденных сообщений
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.roomId = :roomId " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY m.createdAt DESC")
    Page<ChatMessage> searchMessages(
        @Param("roomId") String roomId, 
        @Param("searchTerm") String searchTerm, 
        Pageable pageable
    );

    /**
     * Получение приватных сообщений между двумя пользователями.
     * Возвращает сообщения где sender→recipient или recipient→sender.
     * 
     * @param user1 первый пользователь
     * @param user2 второй пользователь
     * @param pageable параметры пагинации
     * @return страница сообщений между пользователями
     */
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "(m.sender = :user1 AND m.recipient = :user2) OR " +
           "(m.sender = :user2 AND m.recipient = :user1) " +
           "ORDER BY m.createdAt DESC")
    Page<ChatMessage> findPrivateMessages(
        @Param("user1") User user1, 
        @Param("user2") User user2, 
        Pageable pageable
    );

    /**
     * Получение последних приватных сообщений между двумя пользователями.
     * 
     * @param user1 первый пользователь
     * @param user2 второй пользователь
     * @param pageable параметры пагинации (для ограничения количества)
     * @return список сообщений
     */
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "(m.sender = :user1 AND m.recipient = :user2) OR " +
           "(m.sender = :user2 AND m.recipient = :user1) " +
           "ORDER BY m.createdAt DESC")
    List<ChatMessage> findLatestPrivateMessages(
        @Param("user1") User user1, 
        @Param("user2") User user2, 
        Pageable pageable
    );
}
