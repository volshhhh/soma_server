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
}
