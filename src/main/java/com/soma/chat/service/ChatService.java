package com.soma.chat.service;

import com.soma.chat.dto.ChatMessageRequest;
import com.soma.chat.dto.ChatMessageResponse;
import com.soma.chat.model.entity.ChatMessage;
import com.soma.chat.model.entity.User;
import com.soma.chat.repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для работы с сообщениями чата.
 * 
 * Содержит бизнес-логику для:
 * - Создания и сохранения сообщений
 * - Получения истории сообщений
 * - Поиска по сообщениям
 * 
 * ВАЖНО: Все методы, модифицирующие данные, помечены @Transactional
 * для обеспечения целостности данных при работе с WebSocket.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatMessageRepository messageRepository;
    private final UserService userService;

    public ChatService(ChatMessageRepository messageRepository, UserService userService) {
        this.messageRepository = messageRepository;
        this.userService = userService;
    }

    /**
     * Количество сообщений для начальной загрузки истории.
     */
    private static final int DEFAULT_HISTORY_SIZE = 50;

    /**
     * Создание и сохранение нового сообщения.
     * 
     * Важно: метод транзакционный для гарантии сохранения в БД
     * перед отправкой через WebSocket.
     * 
     * @param request данные сообщения от клиента
     * @param username логин отправителя (из Principal)
     * @return сохраненное сообщение в виде DTO
     */
    @Transactional
    public ChatMessageResponse createMessage(ChatMessageRequest request, String username) {
        log.debug("Создание сообщения от {} в комнату {}", username, request.getRoomId());

        // Получаем пользователя из БД
        User sender = userService.getOrCreateUser(username);

        // Определяем тип сообщения
        ChatMessage.MessageType messageType = ChatMessage.MessageType.TEXT;
        if (request.getType() != null) {
            try {
                messageType = ChatMessage.MessageType.valueOf(request.getType().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Неизвестный тип сообщения: {}, используем TEXT", request.getType());
            }
        }

        // Создаем сущность сообщения
        ChatMessage message = ChatMessage.builder()
            .sender(sender)
            .content(request.getContent())
            .roomId(request.getRoomId())
            .type(messageType)
            .build();

        // Сохраняем в БД
        ChatMessage saved = messageRepository.save(message);
        log.info("Сообщение {} сохранено в комнату {}", saved.getId(), saved.getRoomId());

        return ChatMessageResponse.fromEntity(saved);
    }

    /**
     * Создание системного сообщения (вход/выход пользователя).
     * 
     * @param roomId идентификатор комнаты
     * @param username логин пользователя
     * @param type тип события (JOIN, LEAVE)
     * @return DTO системного сообщения
     */
    @Transactional
    public ChatMessageResponse createSystemMessage(
            String roomId, 
            String username, 
            ChatMessage.MessageType type) {
        
        User sender = userService.getOrCreateUser(username);
        
        String content;
        switch (type) {
            case JOIN:
                content = sender.getEffectiveDisplayName() + " присоединился к чату";
                break;
            case LEAVE:
                content = sender.getEffectiveDisplayName() + " покинул чат";
                break;
            default:
                content = "Системное сообщение";
        }

        ChatMessage message = ChatMessage.builder()
            .sender(sender)
            .content(content)
            .roomId(roomId)
            .type(type)
            .build();

        ChatMessage saved = messageRepository.save(message);
        log.info("Системное сообщение {} создано: {}", saved.getId(), content);

        return ChatMessageResponse.fromEntity(saved);
    }

    /**
     * Получение истории сообщений комнаты.
     * 
     * @param roomId идентификатор комнаты
     * @param limit максимальное количество сообщений (по умолчанию 50)
     * @return список сообщений в хронологическом порядке
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessageHistory(String roomId, int limit) {
        log.debug("Загрузка истории комнаты {} (limit={})", roomId, limit);

        Pageable pageable = PageRequest.of(0, limit > 0 ? limit : DEFAULT_HISTORY_SIZE);
        List<ChatMessage> messages = messageRepository.findLatestMessages(roomId, pageable);

        // Переворачиваем список, чтобы старые сообщения были первыми
        Collections.reverse(messages);

        return messages.stream()
            .map(ChatMessageResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Получение истории сообщений с пагинацией.
     * 
     * @param roomId идентификатор комнаты
     * @param page номер страницы (0-based)
     * @param size размер страницы
     * @return страница сообщений
     */
    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getMessageHistoryPaged(String roomId, int page, int size) {
        log.debug("Загрузка страницы {} истории комнаты {} (size={})", page, roomId, size);

        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable)
            .map(ChatMessageResponse::fromEntity);
    }

    /**
     * Поиск сообщений по содержимому.
     * 
     * @param roomId идентификатор комнаты
     * @param searchTerm поисковый запрос
     * @param page номер страницы
     * @param size размер страницы
     * @return страница найденных сообщений
     */
    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> searchMessages(
            String roomId, 
            String searchTerm, 
            int page, 
            int size) {
        
        log.debug("Поиск '{}' в комнате {}", searchTerm, roomId);

        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.searchMessages(roomId, searchTerm, pageable)
            .map(ChatMessageResponse::fromEntity);
    }

    /**
     * Получение количества сообщений в комнате.
     * 
     * @param roomId идентификатор комнаты
     * @return количество сообщений
     */
    @Transactional(readOnly = true)
    public long getMessageCount(String roomId) {
        return messageRepository.countByRoomId(roomId);
    }

    /**
     * Создание и сохранение приватного сообщения.
     * 
     * @param request данные сообщения от клиента
     * @param senderUsername логин отправителя (из Principal)
     * @param recipientUsername логин получателя
     * @return сохраненное сообщение в виде DTO
     */
    @Transactional
    public ChatMessageResponse createPrivateMessage(
            ChatMessageRequest request, 
            String senderUsername, 
            String recipientUsername) {
        
        log.debug("Создание приватного сообщения от {} к {}", senderUsername, recipientUsername);

        User sender = userService.getOrCreateUser(senderUsername);
        User recipient = userService.findByUsername(recipientUsername)
            .orElseThrow(() -> new IllegalArgumentException("Recipient not found: " + recipientUsername));

        ChatMessage.MessageType messageType = ChatMessage.MessageType.TEXT;
        if (request.getType() != null) {
            try {
                messageType = ChatMessage.MessageType.valueOf(request.getType().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Неизвестный тип сообщения: {}, используем TEXT", request.getType());
            }
        }

        // Создаем roomId на основе пользователей (алфавитный порядок для консистентности)
        String roomId = generatePrivateRoomId(senderUsername, recipientUsername);

        ChatMessage message = ChatMessage.builder()
            .sender(sender)
            .recipient(recipient)
            .content(request.getContent())
            .roomId(roomId)
            .type(messageType)
            .build();

        ChatMessage saved = messageRepository.save(message);
        log.info("Приватное сообщение {} сохранено от {} к {}", 
            saved.getId(), senderUsername, recipientUsername);

        return ChatMessageResponse.fromEntity(saved);
    }

    /**
     * Получение истории приватных сообщений между двумя пользователями.
     * 
     * @param username1 первый пользователь
     * @param username2 второй пользователь
     * @param limit максимальное количество сообщений
     * @return список сообщений в хронологическом порядке
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getPrivateMessageHistory(
            String username1, 
            String username2, 
            int limit) {
        
        log.debug("Загрузка приватной истории между {} и {} (limit={})", 
            username1, username2, limit);

        User user1 = userService.findByUsername(username1)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username1));
        User user2 = userService.findByUsername(username2)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username2));

        Pageable pageable = PageRequest.of(0, limit > 0 ? limit : DEFAULT_HISTORY_SIZE);
        List<ChatMessage> messages = messageRepository.findLatestPrivateMessages(user1, user2, pageable);

        // Переворачиваем список, чтобы старые сообщения были первыми
        Collections.reverse(messages);

        return messages.stream()
            .map(ChatMessageResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Генерация уникального roomId для приватного чата.
     * Использует алфавитную сортировку имен для консистентности.
     * 
     * @param username1 первый пользователь
     * @param username2 второй пользователь
     * @return roomId в формате "private:user1:user2"
     */
    public String generatePrivateRoomId(String username1, String username2) {
        String first = username1.compareTo(username2) < 0 ? username1 : username2;
        String second = username1.compareTo(username2) < 0 ? username2 : username1;
        return "private:" + first + ":" + second;
    }
}
