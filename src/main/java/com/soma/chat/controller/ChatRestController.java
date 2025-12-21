package com.soma.chat.controller;

import com.soma.chat.dto.ChatMessageResponse;
import com.soma.chat.dto.UserDto;
import com.soma.chat.service.ChatService;
import com.soma.chat.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST контроллер для работы с чатом.
 * 
 * Предоставляет HTTP API для:
 * - Получения истории сообщений
 * - Поиска по сообщениям
 * - Получения информации о пользователях
 * 
 * Этот контроллер дополняет WebSocket функционал,
 * позволяя получать данные через обычные HTTP запросы.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatRestController {

    private static final Logger log = LoggerFactory.getLogger(ChatRestController.class);

    private final ChatService chatService;
    private final UserService userService;

    public ChatRestController(ChatService chatService, UserService userService) {
        this.chatService = chatService;
        this.userService = userService;
    }

    /**
     * Получение истории сообщений комнаты.
     * 
     * GET /api/chat/rooms/{roomId}/messages
     * 
     * @param roomId идентификатор комнаты
     * @param limit максимальное количество сообщений (по умолчанию 50)
     * @return список сообщений в хронологическом порядке
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessageHistory(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "50") int limit) {
        
        log.debug("REST: Запрос истории комнаты {}, limit={}", roomId, limit);
        List<ChatMessageResponse> messages = chatService.getMessageHistory(roomId, limit);
        return ResponseEntity.ok(messages);
    }

    /**
     * Получение истории сообщений с пагинацией.
     * 
     * GET /api/chat/{roomId}/messages/paged?page=0&size=20
     * 
     * @param roomId идентификатор комнаты
     * @param page номер страницы (0-based)
     * @param size размер страницы
     * @return страница сообщений
     */
    @GetMapping("/{roomId}/messages/paged")
    public ResponseEntity<Page<ChatMessageResponse>> getMessageHistoryPaged(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("REST: Запрос страницы {} истории комнаты {}", page, roomId);
        Page<ChatMessageResponse> messages = chatService.getMessageHistoryPaged(roomId, page, size);
        return ResponseEntity.ok(messages);
    }

    /**
     * Поиск сообщений по содержимому.
     * 
     * GET /api/chat/{roomId}/search?q=текст&page=0&size=20
     * 
     * @param roomId идентификатор комнаты
     * @param q поисковый запрос
     * @param page номер страницы
     * @param size размер страницы
     * @return страница найденных сообщений
     */
    @GetMapping("/{roomId}/search")
    public ResponseEntity<Page<ChatMessageResponse>> searchMessages(
            @PathVariable String roomId,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("REST: Поиск '{}' в комнате {}", q, roomId);
        Page<ChatMessageResponse> messages = chatService.searchMessages(roomId, q, page, size);
        return ResponseEntity.ok(messages);
    }

    /**
     * Получение количества сообщений в комнате.
     * 
     * GET /api/chat/{roomId}/count
     * 
     * @param roomId идентификатор комнаты
     * @return количество сообщений
     */
    @GetMapping("/{roomId}/count")
    public ResponseEntity<Long> getMessageCount(@PathVariable String roomId) {
        long count = chatService.getMessageCount(roomId);
        return ResponseEntity.ok(count);
    }

    /**
     * Получение списка онлайн пользователей.
     * 
     * GET /api/chat/users/online
     * 
     * @return список онлайн пользователей
     */
    @GetMapping("/users/online")
    public ResponseEntity<List<UserDto>> getOnlineUsers() {
        List<UserDto> users = userService.getOnlineUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Получение списка всех пользователей.
     * 
     * GET /api/chat/users
     * 
     * @return список всех пользователей
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Получение информации о текущем пользователе.
     * 
     * GET /api/chat/users/me
     * 
     * @param principal текущий авторизованный пользователь
     * @return информация о пользователе
     */
    @GetMapping("/users/me")
    public ResponseEntity<UserDto> getCurrentUser(java.security.Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        log.debug("REST: Запрос текущего пользователя: {}", principal.getName());
        try {
            var user = userService.getByUsername(principal.getName());
            return ResponseEntity.ok(UserDto.fromEntity(user));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
